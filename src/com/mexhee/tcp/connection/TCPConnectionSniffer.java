package com.mexhee.tcp.connection;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;

import org.apache.log4j.Logger;

import com.mexhee.tcp.connection.configuration.TCPConnectionConfiguration;
import com.mexhee.tcp.connection.jpacp.TCPPacketImpl;

/**
 * TCP connection sniffer main class, including start and shutdown methods.
 */
public class TCPConnectionSniffer {

	private static final Logger logger = Logger.getLogger(TCPConnectionSniffer.class);
	private JpcapCaptor captor;

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
	public void startup(NetworkInterface networkInterface, TCPConnectionConfiguration configuration) throws IOException {
		captor = JpcapCaptor.openDevice(networkInterface, 2000, false, 10000);
		captor.setFilter(configuration.getConnectionFilter().toString(), true);
		final PacketReceiverImpl picker = new PacketReceiverImpl(configuration);
		logger.info("starting up tcp connection sniffer");
		// start up obsolete resources cleaner worker, used to clean
		// timeout/(long time no active packets) connections
		Thread cleaner = new Thread(new ObsoleteConnectionCleaner(picker, configuration.getConnectionStateListener()));
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
}
