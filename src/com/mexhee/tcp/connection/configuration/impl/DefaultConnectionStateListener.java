package com.mexhee.tcp.connection.configuration.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.TCPConnection;
import com.mexhee.tcp.connection.configuration.TCPConnectionStateListener;

public class DefaultConnectionStateListener implements TCPConnectionStateListener {

	private ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 50, 60, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(20), new ThreadPoolExecutor.CallerRunsPolicy());

	public ThreadPoolExecutor getExecutor() {
		return this.executor;
	}

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
		System.out.println("new connection " + connection.getConnectionDetail().toString());
		Runnable handler = new Runnable() {
			@Override
			public void run() {
				TimeMeasurableCombinedInputStream clientInputStream = connection.getClientInputStream();
				TimeMeasurableCombinedInputStream serverInputStream = connection.getServerInputStream();
				while (clientInputStream.hasMoreInputStream() || serverInputStream.hasMoreInputStream()) {
					output(clientInputStream);
					output(serverInputStream);
				}
			}

			private void output(InputStream stream) {
				byte[] buffer = new byte[50];
				int size = 0;
				try {
					while ((size = stream.read(buffer)) > 0) {
						System.out.print(new String(buffer, 0, size));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println();
			}
		};
		new Thread(handler).start();
	}

	@Override
	public void onClosed(final TCPConnection connection) {
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
	}

}
