package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.TCPConnection;

public interface TCPConnectionHandler {

	public void processConnection(TCPConnection connection);
}
