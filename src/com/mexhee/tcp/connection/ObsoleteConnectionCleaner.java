package com.mexhee.tcp.connection;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.listener.TCPConnectionStateListener;

/**
 * A daemon thread, used to clean timeout/(long time no active packets)
 * connections
 */
public class ObsoleteConnectionCleaner implements Runnable {

	private static final Logger logger = Logger.getLogger(ObsoleteConnectionCleaner.class);
	private PacketReceiverImpl receiver;
	private TCPConnectionStateListener stateListener;

	/**
	 * connection timeout in millisecond unit
	 */
	public static final int TCP_CONNECTION_TIME_OUT = 120 * 1000;

	public ObsoleteConnectionCleaner(PacketReceiverImpl receiver, TCPConnectionStateListener stateListener) {
		this.receiver = receiver;
		this.stateListener = stateListener;
	}

	@Override
	public void run() {
		while (true) {
			try {
				for (TCPConnection connection : receiver.getActiveConnections()) {
					if (System.currentTimeMillis() - connection.getLastUpdated().getTime() >= TCP_CONNECTION_TIME_OUT) {
						stateListener.onTimeoutDetected(connection);
						if (logger.isInfoEnabled())
							logger.info(connection.toString() + " has been timeout");
					}
				}
				if (logger.isInfoEnabled()) {
					Collection<TCPConnection> connections = receiver.getActiveConnections();
					logger.info("Active tcp connection count:" + connections.size() + ", streaming connection count:"
							+ getStreamingConnectionCount(connections));
				}
				Thread.sleep(10000);
			} catch (Exception e) {
				logger.error("exception while cleaning timeout connections", e);
			}
		}
	}

	/**
	 * return the tcp connection count, who is still reading client or server
	 * stream
	 */
	private int getStreamingConnectionCount(Collection<TCPConnection> connections) {
		int count = 0;
		for (TCPConnection connection : connections) {
			if (!connection.isFinished()) {
				count++;
			}
		}
		return count;
	}
}
