package com.mexhee.tcp.connection;

import java.io.IOException;

import com.mexhee.tcp.packet.TCPPacket;

/**
 * it will be called whenever a new tcp packet detected.
 *
 */
public interface PacketReceiver {

	/**
	 * process the detected tcp packet.
	 * 
	 * @param tcpPacket detected tcp packet instance
	 */
	void pick(TCPPacket tcpPacket) throws IOException;

}
