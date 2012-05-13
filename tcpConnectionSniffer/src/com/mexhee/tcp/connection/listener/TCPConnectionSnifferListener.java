package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.ConnectionDetail;
import com.mexhee.tcp.connection.TCPConnection;

public interface TCPConnectionSnifferListener {

	TCPConnectionStateListener getConnectionStateListener();

	void tcpConnectionSnifferShutdown();

	boolean isAcceptable(ConnectionDetail connectionDetail);

	void onConnectionDetected(final TCPConnection connection);

	boolean isHalfWayConnectionDetectionEnabled();
}
