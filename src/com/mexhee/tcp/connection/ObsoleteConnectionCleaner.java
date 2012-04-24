package com.mexhee.tcp.connection;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.configuration.TCPConnectionStateListener;

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
	public static final int TCP_CONNECTION_TIME_OUT = 20 * 1000;

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
					}
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				logger.error("exception while cleaning timeout connections", e);
			}
		}
	}
}
