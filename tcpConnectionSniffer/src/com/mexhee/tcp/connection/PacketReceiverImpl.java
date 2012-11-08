package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.mexhee.tcp.packet.TCPPacket;

/**
 * A implementation of {@link PacketReceiver}.
 */
public class PacketReceiverImpl implements PacketReceiver {

	private static final Logger logger = Logger.getLogger(PacketReceiverImpl.class);

	/**
	 * active connections that detected from tcp 3 handshakes or n continuous
	 * data packets
	 */
	private Map<ConnectionDetail, TCPConnectionImpl> activeConnections = new ConcurrentHashMap<ConnectionDetail, TCPConnectionImpl>();
	/**
	 * candidate connections that to be detected from n continuous data packets
	 */
	private Map<ConnectionDetail, TCPConnectionImpl> halfWayConnections = new ConcurrentHashMap<ConnectionDetail, TCPConnectionImpl>();

	private Queue<TCPConnection> establishedConnections = new ConcurrentLinkedQueue<TCPConnection>();

	private ConnectionFilter filter;

	public PacketReceiverImpl(ConnectionFilter filter) {
		this.filter = filter;
	}

	/**
	 * process the detected tcp packet:
	 * 
	 * <pre>
	 * 1. distinguish tcp packet type
	 * 2. if it is syn packet, create a new tcp connection instance, else find the existing tcp connection instance from cache
	 * 3. according to different types of tcp packet, pass the found tcp connection instance to process this tcp packet
	 * 4. consolidate those packets, if in a incorrect sequence, put them into buffer, and when correct packets arrives, pass to tcp connection with the buffered packets
	 * </pre>
	 */
	@Override
	public void pick(TCPPacket tcpPacket) throws IOException {
		if (tcpPacket.isHandsShake1Packet()) {
			if (logger.isDebugEnabled())
				logger.debug("hands shake 1 packet");
			ConnectionDetail connectionDetail = tcpPacket.getConnectionDetail();
			if (filter.isAcceptable(connectionDetail)) {
				TCPConnectionImpl connection = new TCPConnectionImpl(connectionDetail);
				activeConnections.put(connectionDetail, connection);
				connection.processSyncPacket(tcpPacket);
			} else {
				if (logger.isInfoEnabled())
					logger.info("discarded to listen to tcp connection " + connectionDetail.toString());
			}
			return;
		}
		TCPConnectionImpl connection = activeConnections.get(tcpPacket.getConnectionDetail());
		// the connect is not accepted, so ignore this packet
		if (connection == null) {
			handleHalfWayConnectionPackets(tcpPacket);
			return;
		}
		tcpPacket.detectPacketFlowDirection(connection.getConnectionDetail());
		if (connection.isOldPacket(tcpPacket)) {
			logger.info("old packet seq " + tcpPacket);
			return;
		}
		/*
		 * if both side sent FIN packets, but the connection hasn't removed from
		 * buffer, it only means it is waiting for the last ack packet
		 */
		if (connection.isFinished() && connection.getState() != TCPConnectionState.LastAck) {
			return;
		}
		if (tcpPacket.isHandsShake2Packet()) {
			if (logger.isDebugEnabled())
				logger.debug("hands shake 2 packet");
			if (connection.getState() == TCPConnectionState.SynSent) {
				connection.processSyncAckPacket(tcpPacket);
			} else {
				logger.warn(connection.getConnectionDetail().toString()
						+ " received syn/ack packet, but the connection state is" + connection.getState());
			}
			return;
		}
		/**
		 * check whether current packet could be processed at current stage, if
		 * yes, process it, otherwise, put it to buffer
		 */
		if (connection.isCanProcessPacket(tcpPacket)) {
			handlePacket(tcpPacket, connection);
		} else {
			if (tcpPacket.isSentByClient()) {
				connection.getPacketsBuffer().addToCSTemporaryStoredDataPackets(tcpPacket);
			} else {
				connection.getPacketsBuffer().addToSCTemporaryStoredDataPackets(tcpPacket);
			}
			tryToProcessPacketsInBuffer(connection);
		}
	}

	private void handleHalfWayConnectionPackets(TCPPacket tcpPacket) throws IOException {
		if (!tcpPacket.isContainsData()) {
			return;
		}
		/*
		 * actually, for the half way connection, we cannot really detect the
		 * connection direction, which is client & which is server, so just use
		 * the first data packet direction as its connection direction.
		 */
		TCPConnectionImpl connection = halfWayConnections.get(tcpPacket.getConnectionDetail());
		if (connection == null) {
			connection = new TCPConnectionImpl(tcpPacket.getConnectionDetail());
			halfWayConnections.put(tcpPacket.getConnectionDetail(), connection);
			tcpPacket.detectPacketFlowDirection(connection.getConnectionDetail());
			connection.getPacketsBuffer().addToCSTemporaryStoredDataPackets(tcpPacket);
			return;
		} else {
			tcpPacket.detectPacketFlowDirection(connection.getConnectionDetail());
			boolean processed = false;
			if (tcpPacket.isSentByClient()) {

				for (TCPPacket p : connection.getPacketsBuffer().scTemporaryStoredPackets) {
					if (isPreviousAnotherDirectionPacket(tcpPacket, p)) {
						connection.getSequenceNumCounter().serverCounter.seq = tcpPacket.getAckNum();
						connection.getSequenceNumCounter().serverCounter.ack = tcpPacket.getSequence();
						processed = true;
						break;
					}

				}
				if (!processed) {
					connection.getPacketsBuffer().addToCSTemporaryStoredDataPackets(tcpPacket);
				}
			} else {
				for (TCPPacket p : connection.getPacketsBuffer().csTemporaryStoredPackets) {
					if (isPreviousAnotherDirectionPacket(tcpPacket, p)) {
						connection.getSequenceNumCounter().clientCounter.seq = tcpPacket.getAckNum();
						connection.getSequenceNumCounter().clientCounter.ack = tcpPacket.getSequence();
						processed = true;
						break;
					}

				}
				if (!processed) {
					connection.getPacketsBuffer().addToSCTemporaryStoredDataPackets(tcpPacket);
				}
			}
			if (processed) {
				halfWayConnections.remove(connection.getConnectionDetail());
				activeConnections.put(connection.getConnectionDetail(), connection);
				connection.setState(TCPConnectionState.Established);
				establishedNewConnection(connection);
				connection.processDataPacket(tcpPacket);
				tryToProcessPacketsInBuffer(connection);
			}
		}
	}

	public boolean isPreviousAnotherDirectionPacket(TCPPacket currentPacket, TCPPacket previousPacket) {
		return currentPacket.getAckNum() == previousPacket.getSequence()
				+ (previousPacket.getData() != null ? previousPacket.getData().length : 0)
				&& currentPacket.getSequence() == previousPacket.getAckNum();
	}

	/**
	 * try to pick up those
	 * "could continue process according to sequence number" packets, and put it
	 * to connection instance to process
	 */
	private void tryToProcessPacketsInBuffer(TCPConnectionImpl connection) throws IOException {
		TCPPacket bufferedPacket = null;
		while ((bufferedPacket = connection.getPacketsBuffer().pickupPacket()) != null) {
			handlePacket(bufferedPacket, connection);
		}
	}

	private void handlePacket(TCPPacket tcpPacket, TCPConnectionImpl connection) throws IOException {
		if (tcpPacket.isRest()) {
			connection.processRstPacket(tcpPacket);
		}
		if (tcpPacket.isAck()) {
			TCPConnectionState previousState = connection.getState();
			connection.processAckPacket(tcpPacket);
			if (previousState == TCPConnectionState.SynReceived
					&& connection.getState() == TCPConnectionState.Established) {
				// new established connection
				establishedNewConnection(connection);
			}
		}
		if (tcpPacket.isContainsData()) {
			connection.processDataPacket(tcpPacket);
		}
		if (tcpPacket.isFinish()) {
			connection.processFinishPacket(tcpPacket);
			tryToProcessPacketsInBuffer(connection);
		}
		connection.updated();
	}

	/**
	 * return all active tcp connections that maintained by this receiver
	 */
	public Collection<TCPConnectionImpl> getActiveConnections() {
		return activeConnections.values();
	}
	
	private void establishedNewConnection(TCPConnection connection){
		establishedConnections.add(connection);
		synchronized (this) {
			this.notify();
		}
	}

	public TCPConnection poll() {
		return establishedConnections.poll();
	}
}
