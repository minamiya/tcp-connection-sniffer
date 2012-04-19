package com.mexhee.tcp.connection;

/**
 * tcp connection state, please refer to <a href=
 * "http://tuxgraphics.org/electronics/200611/article06111_tcpstates.html">RCF
 * 793</a>
 */
public enum TCPConnectionState {
	/**
	 * client side first sent syn tcp packet
	 */
	SynSent,
	/**
	 * server side received syn packet, and sent back syn/ack packet
	 */
	SynReceived,
	/**
	 * client side received ack packet, and tcp connection is established
	 * successfully
	 */
	Established,
	/**
	 * the first fin packet is sent to try to close connection, then the fin
	 * packet sender won't send data to the other side any more
	 */
	FinishWait1,
	/**
	 * first fin/ack is sent
	 */
	CloseWait,
	/**
	 * the first fin packet receiver sent the second fin packet to close the
	 * connection from its side to the other side
	 */
	FinishWait2,
	/**
	 * the second fin packet was received
	 */
	LastAck, Closing, TimeWait,
	/**
	 * tcp connection was closed by two side
	 */
	Closed;
}
