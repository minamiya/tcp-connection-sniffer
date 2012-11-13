package com.mexhee.tcp.connection.server;

import java.io.IOException;

import jpcap.NetworkInterface;

import com.mexhee.tcp.connection.ConnectionFilter;
import com.mexhee.tcp.connection.TCPConnection;

public abstract class Server {

	public abstract TCPConnection accept();

	public static Server bind(NetworkInterface networkInterface, ConnectionFilter filter) throws IOException {
		ServerImpl server = new ServerImpl();
		server.bindWithFilter(networkInterface, filter);
		return server;
	}

	public static Server bind(NetworkInterface networkInterface) throws IOException {
		return bind(networkInterface, null);
	}

	public static Server openFile(String filename) throws IOException {
		ServerImpl server = new ServerImpl();
		server.open(filename);
		return server;
	}

	public abstract void shutdown();

}
