package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.TCPConnection;

public interface TCPConnectionStreamCallback {

	public void onWriting(boolean isClientWriting, final TCPConnection connection);
}
