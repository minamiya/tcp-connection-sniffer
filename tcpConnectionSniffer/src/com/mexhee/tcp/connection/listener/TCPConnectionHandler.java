package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.TCPConnection;

/**
 * The handler used to handle the new connection from tcp 3 handshakes
 */
public interface TCPConnectionHandler {

	/**
	 * A callback when tcp connection is established after tcp 3 handshakes
	 * 
	 * @param connection
	 *            the new connection
	 */
	public void onEstablished(final TCPConnection connection);

	/**
	 * A callback when tcp connection is just closed
	 * 
	 * @param connection
	 *            the closing connection
	 */
	public void onClosed(final TCPConnection connection);

	/**
	 * A callback class to handle streaming events occur in the tcp connections
	 * 
	 * @return the callback used to handle streaming events occur in the tcp
	 *         connections
	 */
	public TCPConnectionStreamCallback getTcpConnectionStreamCallback();
}
