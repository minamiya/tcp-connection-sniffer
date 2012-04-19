package com.mexhee.tcp.connection;

/**
 * IO Event that represents a starting of client writing or server writing. In a
 * typical tcp connection, after the tcp connection is established, client
 * writing for a request, then server writing for a response, after that client
 * writing for another request again, server writing for another response again,
 * those writing data will happen in a loop, till any side tries to close the
 * tcp connection.
 * 
 */
public class StreamEvent {

	public static final int CLIENT_WRITING = 2;
	public static final int SERVER_WRITING = 3;

	private long generationTime;
	private TCPConnection tcpConnection;
	private int type = 0;

	public StreamEvent(TCPConnection tcpConnection, int type) {
		this.tcpConnection = tcpConnection;
		this.type = type;
		this.generationTime = System.nanoTime();
	}

	/**
	 * returns the event generation nano time
	 */
	public long getGenerationTime() {
		return generationTime;
	}

	/**
	 * a int value represents event type, CLIENT_WRITING is 2, SERVER_WRITING is
	 * 3
	 */
	public int getType() {
		return type;
	}

	/**
	 * returns the tcp connection instance that contains this event.
	 */
	public TCPConnection getTcpConnection() {
		return this.tcpConnection;
	}

	/**
	 * whether it is a client writing event.
	 */
	public boolean isClientWriting() {
		return type == CLIENT_WRITING;
	}

	/**
	 * whether it is a server writing event.
	 */

	public boolean isServerWriting() {
		return type == SERVER_WRITING;
	}

	/**
	 * create a client writing event instance
	 */
	public static StreamEvent createClientWritingEvent(TCPConnection tcpConnection) {
		return new StreamEvent(tcpConnection, CLIENT_WRITING);
	}

	/**
	 * create a server writing event instance
	 */
	public static StreamEvent createServerWritingEvent(TCPConnection tcpConnection) {
		return new StreamEvent(tcpConnection, SERVER_WRITING);
	}
}
