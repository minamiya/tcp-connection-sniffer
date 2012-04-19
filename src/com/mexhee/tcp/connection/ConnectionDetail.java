package com.mexhee.tcp.connection;

import java.net.InetAddress;
import java.util.Date;

/**
 * used to record the client and server side ip and port in current tcp
 * connection. When two ConnectionDetail instances have the same ip&port pair,
 * then these two instances will have same hashcode and should be equal. A
 * sample:
 * 
 * <pre>
 * a physical tcp connection from 192.168.1.100 (1327) to 10.30.168.10 (80)
 * 
 * tcp packet from 192.168.1.100 (1327) to 10.30.168.10 (80) could construct below ConnectionDetail instance:
 * clientAddress: 192.168.1.100
 * serverAddress: 10.30.168.10
 * clientPort: 1327
 * serverPort: 80
 * 
 * but tcp packet from 10.30.168.10 (80) to 192.168.1.100 (1327) will construct another ConnectionDetail instance: 
 * clientAddress: 10.30.168.10
 * serverAddress: 192.168.1.100
 * clientPort: 80
 * serverPort: 1327
 * </pre>
 * 
 * In above case, the two ConnectionDetail instance will be equal and have the
 * same hashcode.
 * 
 */
public class ConnectionDetail {

	private InetAddress clientAddress;
	private InetAddress serverAddress;
	private int clientPort;
	private int serverPort;
	private final Date creationDate = new Date();

	public ConnectionDetail(InetAddress clientAddress, InetAddress serverAddress, int clientPort, int serverPort) {
		this.clientAddress = clientAddress;
		this.serverAddress = serverAddress;
		this.clientPort = clientPort;
		this.serverPort = serverPort;
	}

	/**
	 * return connection detail creation date, it also means its presenting tcp
	 * connection creation date
	 */

	public Date getCreationData() {
		return this.creationDate;
	}

	@Override
	public String toString() {
		return clientAddress.getHostAddress() + "(" + clientPort + ")->" + serverAddress.getHostAddress() + "("
				+ serverPort + ")";
	}

	/**
	 * the same ip&port pairs will have the same hashcode
	 */
	@Override
	public int hashCode() {
		int result = (clientAddress.toString() + "/" + clientPort).hashCode();
		result += (serverAddress.toString() + "/" + serverPort).hashCode();
		return result;
	}

	/**
	 * compare whether two objects' hashcode are the same
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return this.hashCode() == obj.hashCode();
	}
}
