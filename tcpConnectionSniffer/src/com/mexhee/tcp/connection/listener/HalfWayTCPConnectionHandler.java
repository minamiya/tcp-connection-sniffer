package com.mexhee.tcp.connection.listener;

import com.mexhee.tcp.connection.TCPConnection;

/**
 * The handler used to handle the half way detected connections which means we
 * didn't fully detected tcp 3 handshakes packets due to the time frame has been
 * passed, if detected the tcp connection stream reverted, then this connection
 * is thought as a new detected connection, how to define the tcp connection
 * stream reverted? When the program detected previous packet is cs data packet,
 * and current is a sc data packet, and current packet is the next packet of
 * previous packet according to seq number and ack number, or the previous
 * packet is sc and current is cs.
 */
public interface HalfWayTCPConnectionHandler extends TCPConnectionHandler {

	/**
	 * A new half way connection detected
	 * 
	 * @param connection
	 *            new detected connection
	 */
	public void onConnectionDetected(final TCPConnection connection);

}
