package com.mexhee.tcp.connection.server;

import java.io.IOException;

import com.mexhee.tcp.connection.TCPConnection;
import com.mexhee.tcp.connection.TCPConnectionSniffer;


public class ServerImpl extends Server{

	TCPConnectionSniffer sniffer = new TCPConnectionSniffer();

	public void open(String filename) throws IOException {
		sniffer.startup(filename);
	}

	@Override
	public TCPConnection accept() {
		return sniffer.acceptConnection();
	}

	@Override
	public void shutdown() {
		sniffer.shutdown();
	}
}
