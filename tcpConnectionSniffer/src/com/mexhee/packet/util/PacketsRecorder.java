package com.mexhee.packet.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpcap.JpcapCaptor;
import jpcap.JpcapWriter;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;

import com.mexhee.tcp.connection.ConnectionDetail;
import com.mexhee.tcp.connection.ConnectionFilter;
import com.mexhee.tcp.packet.TCPPacket;
import com.mexhee.tcp.packet.TCPPacketImpl;

/**
 * Sniffer tcp packets, and serialize them into file system (dump file) in
 * received sequence, those tcp packets sent in the same tcp connection will be
 * written in the same file, in the other word, one tcp connection will generate
 * one pcap file.
 * 
 * A sample code used to dump tcp connections to/from 192.168.1.1:
 * 
 * <pre>
 * ConnectionFilter filter = new ConnectionFilter();
 * filter.addHost(&quot;192.168.1.1&quot;);
 * TCPPacketsRecorder recorder = new TCPPacketsRecorder(filter, JpcapCaptor.getDeviceList()[0], &quot;c:/dump&quot;);
 * recorder.start();
 * </pre>
 * 
 * 
 * 
 * A sample code to print all the packet information
 * 
 * <pre>
 * TCPPacketsRecorder.open(&quot;c:/dump/192.168.1.100(2350)-192.168.1.1(80)_1334495098578.dump&quot;, new PacketReceiver() {
 * 	&#064;Override
 * 	public void pick(TCPPacket tcpPacket) throws IOException {
 * 		StringBuffer sb = new StringBuffer();
 * 		sb.append(tcpPacket.toString());
 * 		sb.append(&quot;\n&quot;);
 * 		if (tcpPacket.isContainsData()) {
 * 			sb.append(new String(tcpPacket.getData()));
 * 		}
 * 		sb.append(&quot;\n&quot;);
 * 		System.out.println(sb.toString());
 * 	}
 * });
 * </pre>
 * 
 */
public class PacketsRecorder {

	//
	private ConnectionFilter filter;
	private NetworkInterface nif;
	private JpcapCaptor captor;
	private String folder;
	private File runningFlagFile;

	private static final String DUMP_FILE_EXTENATION = ".pcap";

	private Map<ConnectionDetail, List<Packet>> cache = new HashMap<ConnectionDetail, List<Packet>>();

	/**
	 * initialize with parameters
	 * 
	 * @param filter
	 *            some criteria used to filter packets, tcp filter will be
	 *            default added
	 * @param nif
	 *            network interface, could get it through
	 *            {@link JpcapCaptor#getDeviceList()}
	 * @param folder
	 *            dump files will be written into this folder
	 */
	public PacketsRecorder(ConnectionFilter filter, NetworkInterface nif, String folder) {
		this.filter = filter;
		this.nif = nif;
		this.folder = folder;
	}

	private JpcapCaptor getCaptor() throws IOException {
		if (captor == null) {
			captor = JpcapCaptor.openDevice(nif, 2000, false, 10000);
		}
		return captor;
	}

	/**
	 * start to record
	 * 
	 * @throws IOException
	 */
	public void start() throws IOException {
		if (getRunningFlagFile().exists()) {
			throw new IOException("another recorder instance is running, otherwise, please clean .running file at "
					+ getRunningFlagFile().getAbsolutePath());
		}
		if (!getRunningFlagFile().createNewFile()) {
			throw new IOException("cannot touch running flag file");
		}
		getRunningFlagFile().deleteOnExit();
		getCaptor().setFilter("tcp", false);
		getCaptor().setJpcapFilter(filter.getJpcapFilter());
		getCaptor().setNonBlockingMode(true);
		while (true) {
			if (!getRunningFlagFile().exists()) {
				break;
			}
			getCaptor().loopPacket(1, new InMemoryPacketReceiver());
		}
		for (ConnectionDetail detail : cache.keySet()) {
			List<Packet> packets = cache.get(detail);
			JpcapWriter writer = JpcapWriter.openDumpFile(getCaptor(), getFileName(detail));
			for (Packet packet : packets) {
				writer.writePacket(packet);
			}
		}
		getCaptor().close();
	}

	private File getRunningFlagFile() {
		if (runningFlagFile == null) {
			runningFlagFile = new File(folder + "/" + ".running");
		}
		return runningFlagFile;
	}

	/**
	 * stop recording
	 * 
	 * @throws IOException
	 */
	public void stop() throws IOException {
		getRunningFlagFile().delete();
	}

	/**
	 * open a dump file, and loop all those tcp packets serialized in dump file,
	 * transfer them into a tcp connection instance
	 * 
	 */

	public static void open() throws IOException, ClassNotFoundException {
	}

	/**
	 * scan all dump files in dumpFileFolder, and process it
	 * 
	 * @param dumpFileFolder
	 *            a folder that contains dump file
	 * @param handler
	 *            tcp connection handler
	 * 
	 * @see #open(String, TCPConnectionHandler)
	 */

	public static void scan(String dumpFileFolder) throws IOException, ClassNotFoundException {
	}

	private String getFileName(ConnectionDetail detail) {
		return folder + "/" + detail.toString().replaceAll(">", "") + "_" + System.currentTimeMillis()
				+ DUMP_FILE_EXTENATION;
	}

	class InMemoryPacketReceiver implements jpcap.PacketReceiver {

		@Override
		public void receivePacket(Packet packet) {
			TCPPacket tcpPacket = new TCPPacketImpl((jpcap.packet.TCPPacket) packet);
			ConnectionDetail detail = tcpPacket.getConnectionDetail();
			List<Packet> packets = cache.get(detail);
			if (packets == null) {
				packets = new ArrayList<Packet>();
				cache.put(detail, packets);
			}
			packets.add(packet);
		}
	}
}
