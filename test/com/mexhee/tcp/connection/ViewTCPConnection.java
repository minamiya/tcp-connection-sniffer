package com.mexhee.tcp.connection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import sun.net.www.MessageHeader;

import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.listener.TCPConnectionHandler;

public class ViewTCPConnection implements TCPConnectionHandler {

	@Override
	public void processConnection(TCPConnection connection) {
		TimeMeasurableCombinedInputStream clientInputStream = connection.getClientInputStream();
		TimeMeasurableCombinedInputStream serverInputStream = connection.getServerInputStream();
		while (clientInputStream.hasMoreInputStream() || serverInputStream.hasMoreInputStream()) {
			outputClient(clientInputStream);
			outputClient(serverInputStream);
		}

	}

	private void outputServer(TimeMeasurableCombinedInputStream stream) {
		try {
			MessageHeader header = new MessageHeader(stream);
			System.out.println(header.getHeaders());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			IOUtils.copy(stream, new FileOutputStream("d:\\m.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();

	}

	private void outputClient(InputStream stream) {
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