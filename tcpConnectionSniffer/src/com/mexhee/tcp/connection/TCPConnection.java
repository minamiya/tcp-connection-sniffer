package com.mexhee.tcp.connection;

import java.util.Date;

import com.mexhee.io.TimeMeasurableCombinedInputStream;

public interface TCPConnection {
	
	TimeMeasurableCombinedInputStream getServerInputStream();

	TimeMeasurableCombinedInputStream getClientInputStream();

	ConnectionDetail getConnectionDetail();

	TCPConnectionState getState();

	Date getLastUpdated();
}
