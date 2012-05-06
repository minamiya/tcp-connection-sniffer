package com.mexhee.http;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import sun.net.www.MessageHeader;

import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.TCPConnection;
import com.mexhee.tcp.connection.listener.TCPConnectionHandler;
import com.mexhee.tcp.connection.listener.TCPConnectionStreamCallback;

public class HTTPViewer implements TCPConnectionHandler, TCPConnectionStreamCallback {

	private Boolean firstStream = true;
	private static final Logger logger = Logger.getLogger(HTTPViewer.class);

	private String requestingFile = null;

	@Override
	public void onWriting(boolean isClientWriting, TCPConnection connection) {
		try {
			synchronized (firstStream) {
				if (firstStream) {
					if (!isClientWriting || !isValidHttpRequest(connection.getClientInputStream())) {
						connection.close();
					}
					firstStream = false;
				}
			}
			while (connection.getClientInputStream().hasMoreInputStream()
					|| connection.getServerInputStream().hasMoreInputStream()) {
				outputClientStream(connection);
				outputServerStream(connection);
			}
		} catch (IOException e) {
			logger.error("exception when handling http stream", e);
		}
	}

	private void outputServerStream(TCPConnection connection) throws IOException {
		TimeMeasurableCombinedInputStream inputstream = connection.getServerInputStream();
		MessageHeader messageHeader = new MessageHeader(inputstream);
		Object o = messageHeader.getHeaders().get("content-length");
		if (o != null) {
			int len = Integer.parseInt((String) o);
			if (len > 0) {
				byte[] content = new byte[len];
				inputstream.read(content);
				IOUtils.write(content, new FileOutputStream(getFileName()));
				inputstream.finishCurrentInputStream();
			}
		} else {
			byte[] buffer = new byte[50];
			int size = 0;
			FileOutputStream outputStream = new FileOutputStream(getFileName());
			while ((size = inputstream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, size);
			}
			IOUtils.closeQuietly(outputStream);
		}
		requestingFile = null;
	}

	private String getFileName() {
		return "d:/temp/" + (requestingFile == null ? System.currentTimeMillis() : requestingFile);
	}

	private void outputClientStream(TCPConnection connection) throws IOException {
		TimeMeasurableCombinedInputStream inputstream = connection.getClientInputStream();
		// MessageHeader messageHeader = new MessageHeader(inputstream);
		// requestingFile = FilenameUtils.getBaseName((String)
		// messageHeader.getValue(0));
		byte[] buffer = new byte[50];
		int size = 0;
		while ((size = inputstream.read(buffer)) > 0) {
			System.out.print(new String(buffer, 0, size));
		}
	}

	private boolean isValidHttpRequest(TimeMeasurableCombinedInputStream inputStream) throws IOException {
		inputStream.mark(8);
		byte[] b = new byte[8];
		int len = inputStream.read(b);
		String requestAction = new String(b, 0, len);
		boolean valid = false;
		for (String action : HTTPHeaderConst.REQUEST_ACTIONS) {
			if (requestAction.startsWith(action + " ")) {
				valid = true;
				break;
			}
		}
		inputStream.reset();
		return valid;
	}

	@Override
	public void onEstablished(TCPConnection connection) {
	}

	@Override
	public void onClosed(TCPConnection connection) {
	}

	@Override
	public TCPConnectionStreamCallback getTcpConnectionStreamCallback() {
		return this;
	}

}
