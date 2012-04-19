package com.mexhee.tcp.connection.configuration;

import com.mexhee.tcp.connection.ConnectionDetail;

public interface TCPConnectionConfiguration {

	public boolean isAcceptable(ConnectionDetail connectionDetail);
	
	public ConnectionFilter getConnectionFilter();

	public PacketListener getPacketListener();

	public TCPConnectionStateListener getConnectionStateListener();
}
