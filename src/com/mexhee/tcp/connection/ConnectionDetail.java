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

	/**
	 * initialize connection detail instance with parameters
	 * 
	 * @param clientAddress
	 *            connection client address
	 * @param serverAddress
	 *            connection server address
	 * @param clientPort
	 *            connection client port
	 * @param serverPort
	 *            connection server port
	 */
	public ConnectionDetail(InetAddress clientAddress, InetAddress serverAddress, int clientPort, int serverPort) {
		this.clientAddress = clientAddress;
		this.serverAddress = serverAddress;
		this.clientPort = clientPort;
		this.serverPort = serverPort;
	}

	/**
	 * initialize connection detail instance
	 */
	public ConnectionDetail() {
	}

	/**
	 * return connection detail creation date, it also means its presenting tcp
	 * connection creation date
	 */

	public Date getCreationData() {
		return this.creationDate;
	}

	/**
	 * return client address in the connection
	 */
	public InetAddress getClientAddress() {
		return clientAddress;
	}

	/**
	 * return server address in the connection
	 */
	public InetAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * return the client port in the connection
	 */
	public int getClientPort() {
		return clientPort;
	}

	/**
	 * return the server port in the connection
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * set connection client address
	 * 
	 * @param clientAddress
	 *            client address
	 */
	public void setClientAddress(InetAddress clientAddress) {
		this.clientAddress = clientAddress;
	}

	/**
	 * set connection server address
	 * 
	 * @param serverAddress
	 *            server address
	 */
	public void setServerAddress(InetAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * set connection client port
	 * 
	 * @param clientPort
	 *            client port
	 */
	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	/**
	 * set connection server port
	 * 
	 * @param serverPort
	 *            server port
	 */

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
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
