package com.mexhee.tcp.connection;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;

import org.apache.log4j.Logger;

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
	private PacketReceiverImpl picker;

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
	public void startup(NetworkInterface networkInterface) throws IOException {
		captor = JpcapCaptor.openDevice(networkInterface, 2000, false, 10000);
		startupInNewThread();
	}

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
	public void startup(String filename) throws IOException {
		captor = JpcapCaptor.openFile(filename);
		startupInNewThread();
	}

	private void startupInNewThread() {
		captor.setJpcapFilter(connectionFilter.getJpcapFilter());
		picker = new PacketReceiverImpl(connectionFilter);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				startup();
			}
		});
		t.setName("PacketsPicker");
		t.start();
	}

	private void startup() {
		logger.info("starting up tcp connection sniffer");
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
		Thread cleaner = new Thread(new ObsoleteConnectionCleaner(picker));
		cleaner.setDaemon(true);
		cleaner.setName("ObsoleteConnectionCleaner");
		// cleaner.start();
		logger.info("started timeout tcp connection cleaner");
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
	}

	/**
	 * list existing network interfaces in this computer
	 */
	public static NetworkInterface[] allInterfaces() {
		return JpcapCaptor.getDeviceList();
	}

	public TCPConnection acceptConnection() {
		TCPConnection connection = picker.poll();
		if (connection == null) {
			synchronized (picker) {
				try {
					picker.wait();
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
			connection = picker.poll();
		}
		return connection;
	}
}
