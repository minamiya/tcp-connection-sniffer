package com.mexhee.tcp.connection;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.listener.ConnectionFilter;
import com.mexhee.tcp.connection.listener.DefaultTCPConnectionSnifferListener;
import com.mexhee.tcp.connection.listener.TCPConnectionHandler;
import com.mexhee.tcp.connection.listener.TCPConnectionSnifferListener;
import com.mexhee.tcp.packet.TCPPacketImpl;

/**
 * TCP connection sniffer main class, including start and shutdown methods.
 */
public class TCPConnectionSniffer {

	private static final Logger logger = Logger.getLogger(TCPConnectionSniffer.class);
	private JpcapCaptor captor;
	/*
	 * there should be a default ConnectionFilter instance, to add TCP protocol
	 * filter
	 */
	private ConnectionFilter connectionFilter = new ConnectionFilter();
	private TCPConnectionSnifferListener snifferListener;

	/**
	 * begin to sniffer tcp connections on networkInterface, according to
	 * configuration
	 * 
	 * @param networkInterface
	 *            which network interface the data sniffer bases on, use
	 *            {@link #allInterfaces()} to list existing network interfaces
	 *            in this computer
	 * @throws IOException
	 *             open network interface failed or set filter failed
	 */
	public void startup(NetworkInterface networkInterface, TCPConnectionHandler connectionHandler) throws IOException {
		captor = JpcapCaptor.openDevice(networkInterface, 2000, false, 10000);
		captor.setJpcapFilter(connectionFilter.getJpcapFilter());
		// below code should be removed after setJpcapFilter is implemented
		captor.setFilter("tcp", true);
		snifferListener = new DefaultTCPConnectionSnifferListener(connectionHandler, connectionFilter);
		final PacketReceiverImpl picker = new PacketReceiverImpl(snifferListener);
		((DefaultTCPConnectionSnifferListener) snifferListener).setPacketReceiver(picker);
		logger.info("starting up tcp connection sniffer");
		/*
		 * start up obsolete resources cleaner worker, used to clean
		 * timeout/(long time no active packets) connections
		 */
		Thread cleaner = new Thread(new ObsoleteConnectionCleaner(picker, snifferListener.getConnectionStateListener()));
		cleaner.setDaemon(true);
		cleaner.start();
		logger.info("started timeout tcp connection cleaner");
		captor.loopPacket(0, new jpcap.PacketReceiver() {
			public void receivePacket(Packet packet) {
				try {
					picker.pick(new TCPPacketImpl((jpcap.packet.TCPPacket) packet));
					if (logger.isDebugEnabled())
						logger.debug("successfully process packet " + packet);
				} catch (Exception e) {
					logger.error("failed to handle packet " + packet, e);
				}
			}
		});
	}

	/**
	 * The filter used to do filter of those connection, this filter will be
	 * applied to Jpcap native library, please also see
	 * 
	 * @param connectionFilter
	 */
	public void setConnectionFilter(ConnectionFilter connectionFilter) {
		this.connectionFilter = connectionFilter;
	}

	/**
	 * stop data sniffer
	 */
	public void shutdown() {
		logger.info("stopping tcp connection sniffer");
		if (captor != null) {
			captor.breakLoop();
		}
		if (snifferListener != null) {
			snifferListener.tcpConnectionSnifferShutdown();
		}
	}

	/**
	 * list existing network interfaces in this computer
	 */
	public static NetworkInterface[] allInterfaces() {
		return JpcapCaptor.getDeviceList();
	}
}
