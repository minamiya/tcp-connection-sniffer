package com.mexhee.tcp.connection;

import java.util.Collection;

import org.apache.log4j.Logger;


/**
 * A daemon thread, used to clean timeout/(long time no active packets)
 * connections
 */
public class ObsoleteConnectionCleaner implements Runnable {

	private static final Logger logger = Logger.getLogger(ObsoleteConnectionCleaner.class);
	private PacketReceiverImpl receiver;

	/**
	 * connection timeout in millisecond unit
	 */
	public static final int TCP_CONNECTION_TIME_OUT = 120 * 1000;

	/**
	 * When the FIN packet is already sent and usually means the connection just
	 * need wait for 2MSL to close connection physically.
	 */
	public static final int HALF_CLOSED_TIMEOUT = 30 * 1000;

	/**
	 * when tcp connection sniffer detected that current connection may be
	 * broken
	 * 
	 * @see TCPConnectionImpl#isMaybeBroken()
	 */
	public static final int BROKEN_TIMEOUT = 20 * 1000;

	/**
	 * those half handshake connections timeout, such as client sends a SYN
	 * packet out, but server has no corresponding service, so there won't be
	 * any response
	 * 
	 */
	public static final int HALF_HANDSHAKE_TIMEOUT = 10 * 1000;

	public ObsoleteConnectionCleaner(PacketReceiverImpl receiver) {
		this.receiver = receiver;
	}

	@Override
	public void run() {
		while (true) {
			try {
				for (TCPConnectionImpl connection : receiver.getActiveConnections()) {
					long duration = System.currentTimeMillis() - connection.getLastUpdated().getTime();
					if (connection.isMaybeBroken() && duration >= BROKEN_TIMEOUT) {
						connectionTimeout(connection);
					} else if (connection.getState().isGreaterThan(TCPConnectionState.Established)
							&& duration >= HALF_CLOSED_TIMEOUT) {
						connection.close();
					} else if (connection.getState().isLessThan(TCPConnectionState.Established)) {
						connectionTimeout(connection);
					} else if (duration >= TCP_CONNECTION_TIME_OUT) {
						connectionTimeout(connection);
					}
				}
				if (logger.isInfoEnabled()) {
					Collection<TCPConnectionImpl> connections = receiver.getActiveConnections();
					logger.info("Active tcp connection count:" + connections.size() + ", streaming connection count:"
							+ getStreamingConnectionCount(connections));
				}
				Thread.sleep(8000);
			} catch (Exception e) {
				logger.error("exception while cleaning timeout connections", e);
			}
		}
	}

	private void connectionTimeout(TCPConnectionImpl connection) {
		if (logger.isInfoEnabled() && connection.getState().isEqualsGreaterThan(TCPConnectionState.Established))
			logger.info(connection.toString() + " has been timeout");
		receiver.getActiveConnections().remove(connection);
	}

	/**
	 * return the tcp connection count, who is still reading client or server
	 * stream
	 */
	private int getStreamingConnectionCount(Collection<TCPConnectionImpl> connections) {
		int count = 0;
		for (TCPConnectionImpl connection : connections) {
			if (!connection.isFinished()) {
				count++;
			}
		}
		return count;
	}
}
