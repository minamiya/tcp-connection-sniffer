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
	 * </pre>
	 */
	@Override
	public void pick(TCPPacket tcpPacket) throws IOException {
		if (tcpPacket.isHandsShake1Packet()) {
			if (logger.isDebugEnabled())
				logger.debug("hands shake 1 packet");
			ConnectionDetail connectionDetail = tcpPacket.getConnectionDetail();
			if (snifferListener.isAcceptable(connectionDetail)) {
				TCPConnection connection = new TCPConnection(connectionDetail, snifferListener.getConnectionStateListener());
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
		if (tcpPacket.isHandsShake2Packet()) {
			if (logger.isDebugEnabled())
				logger.debug("hands shake 2 packet");
			connection.processSyncAckPacket(tcpPacket);
			return;
		}
		if(tcpPacket.isRest()){
			connection.processRstPacket(tcpPacket);
		}
		if (tcpPacket.isAck()) {
			connection.processAckPacket(tcpPacket);
		}
		if (tcpPacket.isPush() || tcpPacket.isContainsData()) {
			connection.processDataPacket(tcpPacket);
		}
		if (tcpPacket.isFinish()) {
			connection.processFinishPacket(tcpPacket);
		}
	}

	/**
	 * return all active tcp connections that maintained by this receiver
	 */
	public Collection<TCPConnection> getActiveConnections() {
		return activeConnections.values();
	}
}
