package com.mexhee.tcp.connection.listener;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.ConnectionDetail;
import com.mexhee.tcp.connection.GlobalThreadPool;
import com.mexhee.tcp.connection.PacketReceiverImpl;
import com.mexhee.tcp.connection.TCPConnection;

public class DefaultTCPConnectionSnifferListener implements TCPConnectionSnifferListener {

	private static final Logger logger = Logger.getLogger(DefaultTCPConnectionSnifferListener.class);

	private TCPConnectionHandler handler;
	private ConnectionFilter connectionFilter;
	private PacketReceiverImpl receiver;
	private boolean isHalfWayConnectionDetectionEnabled = false;

	public DefaultTCPConnectionSnifferListener(TCPConnectionHandler handler, ConnectionFilter connectionFilter) {
		this.handler = handler;
		this.connectionFilter = connectionFilter;
		if (handler instanceof HalfWayTCPConnectionHandler) {
			isHalfWayConnectionDetectionEnabled = true;
		}
	}

	@Override
	public boolean isHalfWayConnectionDetectionEnabled() {
		return isHalfWayConnectionDetectionEnabled;
	}

	public void onConnectionDetected(final TCPConnection connection) {
		if (!isHalfWayConnectionDetectionEnabled) {
			throw new IllegalAccessError("Half way connection detection is not enabled");
		}
		if (connectionFilter.isAcceptable(connection.getConnectionDetail())) {
			((HalfWayTCPConnectionHandler) handler).onConnectionDetected(connection);
			connection.registerWritingCallback(handler.getTcpConnectionStreamCallback());
		}
	}

	public void setPacketReceiver(PacketReceiverImpl receiver) {
		this.receiver = receiver;
	}

	@Override
	public final void tcpConnectionSnifferShutdown() {
		GlobalThreadPool.executor.shutdown();
	}

	@Override
	public final TCPConnectionStateListener getConnectionStateListener() {
		return new DefaultConnectionStateListener();
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
				handler.onEstablished(connection);
				connection.registerWritingCallback(handler.getTcpConnectionStreamCallback());
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
			handler.onClosed(connection);
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
