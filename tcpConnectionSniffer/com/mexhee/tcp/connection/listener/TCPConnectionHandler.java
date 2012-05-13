package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.TCPConnection;

public interface TCPConnectionHandler {

	public void onEstablished(final TCPConnection connection);

	public void onClosed(final TCPConnection connection);
	
	public TCPConnectionStreamCallback getTcpConnectionStreamCallback();
}
