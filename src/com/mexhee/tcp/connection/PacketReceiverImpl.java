package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.listener.TCPConnectionSnifferListener;
import com.mexhee.tcp.packet.TCPPacket;

/**
 * A implementation of {@link PacketReceiver}.
 */
public class PacketReceiverImpl implements PacketReceiver {

	private static final Logger logger = Logger.getLogger(PacketReceiverImpl.class);

	private Map<ConnectionDetail, TCPConnection> activeConnections = new ConcurrentHashMap<ConnectionDetail, TCPConnection>();

	private TCPConnectionSnifferListener snifferListener;

	public PacketReceiverImpl(TCPConnectionSnifferListener snifferListener) {
		this.snifferListener = snifferListener;
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
			if (snifferListener.isAcceptable(connectionDetail)) {
				TCPConnection connection = new TCPConnection(connectionDetail,
						snifferListener.getConnectionStateListener());
				activeConnections.put(connectionDetail, connection);
				connection.processSyncPacket(tcpPacket);
			} else {
				if (logger.isInfoEnabled())
					logger.info("discarded to listen to tcp connection " + connectionDetail.toString());
			}
			return;
		}
		TCPConnection connection = activeConnections.get(tcpPacket.getConnectionDetail());
		// the connect is not accepted, so ignore this packet
		if (connection == null) {
			return;
		}
		tcpPacket.detectPacketFlowDirection(connection.getConnectionDetail());
		if (connection.isOldPacket(tcpPacket)) {
			logger.info("old packet seq " + tcpPacket);
			return;
		}

		if (connection.isFinished()) {
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
		if (connection.isMatchSequence(tcpPacket)) {
			handlePacket(tcpPacket, connection);
		} else {
			if (tcpPacket.isSentByClient()) {
				connection.getPacketsBuffer().addToCSTemporaryStoredDataPackets(tcpPacket);
			} else {
				connection.getPacketsBuffer().addToSCTemporaryStoredDataPackets(tcpPacket);
			}
			TCPPacket bufferedPacket = null;
			while ((bufferedPacket = connection.getPacketsBuffer().pickupPacket(connection.getSequenceNumCounter(),
					connection)) != null) {
				handlePacket(bufferedPacket, connection);
			}
		}
	}

	private void handlePacket(TCPPacket tcpPacket, TCPConnection connection) throws IOException {
		if (tcpPacket.isRest()) {
			connection.processRstPacket(tcpPacket);
		}
		if (tcpPacket.isAck()) {
			connection.processAckPacket(tcpPacket);
		}
		if (tcpPacket.isContainsData()) {
			connection.processDataPacket(tcpPacket);
		}
		if (tcpPacket.isFinish()) {
			connection.processFinishPacket(tcpPacket);
		}
		// TODO: take a look those expired packet according to state, and
		// sequence number
		// TODO: update the tcp connection instance last update time
	}

	/**
	 * return all active tcp connections that maintained by this receiver
	 */
	public Collection<TCPConnection> getActiveConnections() {
		return activeConnections.values();
	}
}
