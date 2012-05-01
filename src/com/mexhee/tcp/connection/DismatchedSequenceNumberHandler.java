package com.mexhee.tcp.connection;

import com.mexhee.tcp.packet.TCPPacket;

public interface DismatchedSequenceNumberHandler {
	
	boolean isEnableChecking();

	void dismatchedSequenceNumber(boolean isSentByClient, TCPPacket packet);
}
