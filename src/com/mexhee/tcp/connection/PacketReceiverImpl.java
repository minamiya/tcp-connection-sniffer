package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mexhee.tcp.connection.configuration.PacketListener;
import com.mexhee.tcp.connection.configuration.TCPConnectionConfiguration;
import com.mexhee.tcp.packet.TCPPacket;

/**
 * A implementation of {@link PacketReceiver}.
 */
public class PacketReceiverImpl implements PacketReceiver {

	private Map<ConnectionDetail, TCPConnection> activeConnections = new ConcurrentHashMap<ConnectionDetail, TCPConnection>();

	private TCPConnectionConfiguration configuration;
	private PacketListener packetListener;

	public PacketReceiverImpl(TCPConnectionConfiguration configuration) {
		this.configuration = configuration;
		this.packetListener = configuration.getPacketListener();
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
			ConnectionDetail connectionDetail = tcpPacket.getConnectionDetail();
			if (configuration.isAcceptable(connectionDetail)) {
				TCPConnection connection = new TCPConnection(connectionDetail, packetListener,
						configuration.getConnectionStateListener());
				activeConnections.put(connectionDetail, connection);
				connection.processSyncPacket(tcpPacket);
			}
			return;
		}
		TCPConnection connection = activeConnections.get(tcpPacket.getConnectionDetail());
		// the connect is not accepted, so ignore this packet
		if (connection == null) {
			packetListener.ignoreTCPPacket(tcpPacket);
			return;
		}
		if (tcpPacket.isHandsShake2Packet()) {
			connection.processSyncAckPacket(tcpPacket);
			return;
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
}
