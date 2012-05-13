package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.ConnectionDetail;

public interface TCPConnectionSnifferListener {

	TCPConnectionStateListener getConnectionStateListener();

	void tcpConnectionSnifferShutdown();

	boolean isAcceptable(ConnectionDetail connectionDetail);
}
