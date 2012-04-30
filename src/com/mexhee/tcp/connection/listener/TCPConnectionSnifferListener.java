package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.ConnectionDetail;

public interface TCPConnectionSnifferListener extends TCPConnectionHandler {

	TCPConnectionStateListener getConnectionStateListener();

	void tcpConnectionSnifferShutdown();

	boolean isAcceptable(ConnectionDetail connectionDetail);
}
