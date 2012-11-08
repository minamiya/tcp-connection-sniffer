package com.mexhee.tcp.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import jpcap.JpcapFilter;
import jpcap.JpcapFilter.Protocol;


/**
 * This class is used to do the filtering of tcp connection
 */
public class ConnectionFilter {
	private final JpcapFilter jpcapFilter = new JpcapFilter();

	private List<ConnectionDetail> filters = new ArrayList<ConnectionDetail>();

	public ConnectionFilter() {
		jpcapFilter.addProtocol(Protocol.TCP);
	}

	/**
	 * add server ip/hostname and port filtering
	 * 
	 * @param serverHost
	 *            server host ip or host name
	 * @param serverPort
	 *            server port
	 * @throws UnknownHostException
	 *             incorrect server ip format or unknown host
	 */
	public void addServerFilter(String serverHost, int serverPort) throws UnknownHostException {
		addServerHostFilter(null, 0, serverHost, serverPort);
	}

	/**
	 * add client ip/hostname and port filtering
	 * 
	 * @param clientHost
	 *            client host ip or host name
	 * @param clientPort
	 *            client host port
	 * @throws UnknownHostException
	 *             incorrect client ip format or unknown host
	 */
	public void addClientFilter(String clientHost, int clientPort) throws UnknownHostException {
		addServerHostFilter(clientHost, clientPort, null, 0);
	}

	/**
	 * add client host ip/hostname and server host ip/hostname filtering
	 * 
	 * @param clientHost
	 *            client host ip or host name
	 * @param serverHost
	 *            server host ip or host name
	 * @throws UnknownHostException
	 *             clientHost or serverHost is not in a correct ip format, or
	 *             either of them is a unknown host
	 */
	public void addHostPair(String clientHost, String serverHost) throws UnknownHostException {
		addServerHostFilter(clientHost, 0, serverHost, 0);
	}

	/**
	 * add client port and server port filtering
	 * 
	 * @param clientPort
	 *            client host port
	 * @param serverPort
	 *            server host port
	 */
	public void addPortPair(int clientPort, int serverPort) {
		try {
			addServerHostFilter(null, clientPort, null, serverPort);
		} catch (UnknownHostException e) {
		}
	}

	/**
	 * add client host ip filtering
	 * 
	 * @param clientHost
	 *            client ip or host name
	 * @throws UnknownHostException
	 *             incorrect client ip format or unknown host
	 */
	public void addClientHostFilter(String clientHost) throws UnknownHostException {
		addServerHostFilter(clientHost, 0, null, 0);
	}

	/**
	 * add server host ip/hostname filtering
	 * 
	 * @param serverHost
	 *            server host ip or host name
	 * @throws UnknownHostException
	 *             incorrect server ip format or unknown host
	 */
	public void addServerHostFilter(String serverHost) throws UnknownHostException {
		addServerHostFilter(null, 0, serverHost, 0);
	}

	/**
	 * add client ip/hostname and port, server ip/hostname and port filtering
	 * 
	 * @param clientHost
	 *            client host ip or host name
	 * @param clientPort
	 *            client host port
	 * @param serverHost
	 *            server host ip or host name
	 * @param serverPort
	 *            server host port
	 * @throws UnknownHostException
	 *             clientHost or serverHost is not in a correct ip format, or
	 *             either of them is a unknown host
	 */
	public void addServerHostFilter(String clientHost, int clientPort, String serverHost, int serverPort)
			throws UnknownHostException {
		ConnectionDetail connectionDetail = new ConnectionDetail();
		if (notEmpty(clientHost)) {
			jpcapFilter.addHost(clientHost);
			connectionDetail.setClientAddress(InetAddress.getByName(clientHost));
		}
		if (notEmpty(serverHost)) {
			jpcapFilter.addHost(serverHost);
			connectionDetail.setServerAddress(InetAddress.getByName(serverHost));
		}
		if (clientPort > 0) {
			jpcapFilter.addPort(clientPort);
			connectionDetail.setClientPort(clientPort);
		}
		if (serverPort > 0) {
			jpcapFilter.addPort(serverPort);
			connectionDetail.setServerPort(serverPort);
		}
		filters.add(connectionDetail);
	}

	/**
	 * get packet level filtering for Jpcap native library
	 * 
	 * @return packet level filtering for Jpcap native library
	 */
	public JpcapFilter getJpcapFilter() {
		return jpcapFilter;
	}

	/**
	 * according to the connection filter, whether the given connection passes
	 * the filtering
	 * 
	 * @param connectionDetail
	 *            the connection detail
	 * @return whether the give connection pass the filtering
	 */
	public boolean isAcceptable(ConnectionDetail connectionDetail) {
		if (filters.isEmpty()) {
			return true;
		}
		for (ConnectionDetail criteria : filters) {
			if (isMatch(criteria, connectionDetail)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMatch(ConnectionDetail criteria, ConnectionDetail connInstance) {
		if (criteria.getClientAddress() != null && !criteria.getClientAddress().equals(connInstance.getClientAddress())) {
			return false;
		}
		if (criteria.getServerAddress() != null && !criteria.getServerAddress().equals(connInstance.getServerAddress())) {
			return false;
		}
		if (criteria.getClientPort() > 0 && criteria.getClientPort() != connInstance.getClientPort()) {
			return false;
		}
		if (criteria.getServerPort() > 0 && criteria.getServerPort() != connInstance.getServerPort()) {
			return false;
		}
		return true;
	}

	private boolean notEmpty(String str) {
		return str != null && str.trim().length() > 0;
	}
}
