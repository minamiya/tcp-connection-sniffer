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
	SynSent(1),
	/**
	 * server side received syn packet, and sent back syn/ack packet
	 */
	SynReceived(2),
	/**
	 * client side received ack packet, and tcp connection is established
	 * successfully
	 */
	Established(3),
	/**
	 * the first fin packet is sent to try to close connection, then the fin
	 * packet sender won't send data to the other side any more
	 */
	FinishWait1(4),
	/**
	 * first fin/ack is sent
	 */
	CloseWait(5),
	/**
	 * One side connection has been closed, but the other side doesn't still
	 * have data to send, doesn't send the second FIN packet
	 */
	FinishWait2(5.5f),
	/**
	 * the second fin packet was received, wait to receive the corresponding ACK
	 */
	LastAck(6),
	/**
	 * usually, after the first FIN is sent, the sender expects to receive a ACK
	 * packet, however, it receives a FIN packet from the other side, it means
	 * both side is sending the FIN packet at the same time, this seldomly
	 * happens
	 */
	Closing(4.5f),
	/**
	 * received ACK for FIN
	 */
	TimeWait(5.7f),
	/**
	 * tcp connection was closed by two side, after received the ACK for the
	 * second FIN packet, or received RST packet, even wait for 2MSL after
	 * TimeWait
	 */
	Closed(7);

	float seq;

	TCPConnectionState(float seq) {
		this.seq = seq;
	}

	/**
	 * whether current state sequence is greater than the given one
	 * 
	 * @param state
	 *            another state
	 * @return current state sequence is greater than the given one
	 */
	public boolean isGreaterThan(TCPConnectionState state) {
		return this.seq > state.seq;
	}
}
