package com.mexhee.tcp.connection;

import java.io.IOException;
import java.io.InputStream;

import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.listener.TCPConnectionHandler;

public class ViewTCPConnection implements TCPConnectionHandler {

	@Override
	public void processConnection(TCPConnection connection) {
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
//				System.out.print(new String(buffer, 0, size));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();
	}
}