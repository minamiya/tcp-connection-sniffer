package com.mexhee.tcp.connection.listener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.ConnectionDetail;
import com.mexhee.tcp.connection.PacketReceiverImpl;
import com.mexhee.tcp.connection.TCPConnection;

public class DefaultTCPConnectionSnifferListener implements TCPConnectionSnifferListener {

	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 100, 60, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(20), new ThreadPoolExecutor.CallerRunsPolicy());
	private static final Logger logger = Logger.getLogger(DefaultTCPConnectionSnifferListener.class);

	private TCPConnectionHandler handler;
	private ConnectionFilter connectionFilter;
	private PacketReceiverImpl receiver;

	public DefaultTCPConnectionSnifferListener(TCPConnectionHandler handler, ConnectionFilter connectionFilter) {
		this.handler = handler;
		this.connectionFilter = connectionFilter;
	}

	public void setPacketReceiver(PacketReceiverImpl receiver) {
		this.receiver = receiver;
	}

	@Override
	public final void tcpConnectionSnifferShutdown() {
		executor.shutdown();
	}

	@Override
	public final TCPConnectionStateListener getConnectionStateListener() {
		return new DefaultConnectionStateListener();
	}

	@Override
	public void processConnection(TCPConnection connection) {
		handler.processConnection(connection);
	}

	@Override
	public boolean isAcceptable(ConnectionDetail connectionDetail) {
		return connectionFilter.isAcceptable(connectionDetail);
	}

	class DefaultConnectionStateListener implements TCPConnectionStateListener {

		@Override
		public void onSynSent(final TCPConnection connectionDetail) {
			// nop
		}

		@Override
		public void onSynReceived(final TCPConnection connectionDetail) {
			// nop
		}

		@Override
		public void onEstablished(final TCPConnection connection) {
			if (connectionFilter.isAcceptable(connection.getConnectionDetail())) {
				Runnable handler = new Runnable() {
					@Override
					public void run() {

						processConnection(connection);
					}

				};
				executor.execute(handler);
			}
		}

		@Override
		public void onClosed(TCPConnection connection) {
			// release connection instance
			releaseConnection(connection);
		}

		private void releaseConnection(TCPConnection connection) {
			if (receiver != null) {
				receiver.getActiveConnections().remove(connection);
			}
			if (logger.isInfoEnabled())
				logger.info(connection.getConnectionDetail().toString() + " is released successfully.");
			connection = null;
		}

		@Override
		public void onFinishWait1(TCPConnection connection) {
		}

		@Override
		public void onFinishWait2(TCPConnection connection) {
		}

		@Override
		public void onCloseWait(TCPConnection connection) {
		}

		@Override
		public void onLastAck(TCPConnection connection) {
		}

		@Override
		public void onClosing(TCPConnection connection) {
		}

		@Override
		public void onTimeWait(TCPConnection connection) {
		}

		@Override
		public void onTimeoutDetected(TCPConnection connection) {
			// release connection instance
			releaseConnection(connection);
		}

	}
}
