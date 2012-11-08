package com.mexhee.tcp.connection.server;

import java.io.IOException;

import com.mexhee.tcp.connection.TCPConnection;


public abstract class Server {

	public abstract TCPConnection accept();

	public static Server bind() {
		return null;
	}

	public static Server openFile(String filename) throws IOException {
		ServerImpl server = new ServerImpl();
		server.open(filename);
		return server;
	}
	
	public abstract void shutdown();

}
