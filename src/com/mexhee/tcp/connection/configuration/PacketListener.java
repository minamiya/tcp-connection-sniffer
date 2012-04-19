package com.mexhee.tcp.connection.configuration;

import com.mexhee.tcp.packet.TCPPacket;

public interface PacketListener {

	// ignore the packet due to the connection is ignored
	public void ignoreTCPPacket(TCPPacket tcpPacket);

	// successfully consumed the packet
	public void consumeTCPPacket(TCPPacket tcpPacket);

	// discard the packet, usually due to the sequence is expired
	public void discardTCPPacket(TCPPacket tcpPacket);

	//due to network issue, the packet is resent by server or client
	public void duplicatedTCPPacket(TCPPacket tcpPacket);
}
