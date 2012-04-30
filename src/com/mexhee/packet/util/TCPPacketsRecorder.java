package com.mexhee.packet.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;

import com.mexhee.tcp.connection.ConnectionDetail;
import com.mexhee.tcp.connection.PacketReceiver;
import com.mexhee.tcp.connection.PacketReceiverImpl;
import com.mexhee.tcp.connection.listener.ConnectionFilter;
import com.mexhee.tcp.connection.listener.DefaultTCPConnectionSnifferListener;
import com.mexhee.tcp.connection.listener.TCPConnectionHandler;
import com.mexhee.tcp.connection.listener.TCPConnectionSnifferListener;
import com.mexhee.tcp.packet.TCPPacket;
import com.mexhee.tcp.packet.TCPPacketImpl;

/**
 * Sniffer tcp packets, and serialize them into file system (dump file) in
 * received sequence, those tcp packets sent in the same tcp connection will be
 * written in the same file, in the other word, one tcp connection will generate
 * one dump file.
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
 * TODO: make the output file zipable, then it will reduce file size
 * significantly.
 * 
 */
// Won't use the JpcapWriter to serialize a standard readable packet for
// WinDump,
// as it seems there is a bug in Jpacap or WinDump, it cann't write dump file
// parallel, it means the it will write packet to wrong dump file
public class TCPPacketsRecorder {

	//
	private ConnectionFilter filter;
	private NetworkInterface nif;
	private JpcapCaptor captor;
	private String folder;

	private static final String DUMP_FILE_EXTENATION = ".dump";

	private Map<ConnectionDetail, ObjectOutputStream> writers = new HashMap<ConnectionDetail, ObjectOutputStream>();

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
	public TCPPacketsRecorder(ConnectionFilter filter, NetworkInterface nif, String folder) {
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
		getCaptor().setJpcapFilter(filter.getJpcapFilter());
		//TODO: after jpcapFilter function is ready, remove below code
		getCaptor().setFilter("tcp and host 192.168.1.1", true);
		getCaptor().loopPacket(0, new DumpPacketReceiver());
	}

	/**
	 * stop recording
	 * 
	 * @throws IOException
	 */
	public void stop() throws IOException {
		getCaptor().breakLoop();
		for (ObjectOutputStream writer : writers.values()) {
			writer.close();
		}
	}

	/**
	 * open a dump file, and loop all those tcp packets serialized in dump file,
	 * transfer them into a tcp connection instance
	 * 
	 * @param dumpFile
	 *            dumpFile full path
	 * @param handler
	 *            tcp connection handler
	 * @throws ClassNotFoundException
	 *             thrown by deserialize objects from dump file
	 * @throws IOException
	 */

	public static void open(String dumpFile, final TCPConnectionHandler handler) throws IOException,
			ClassNotFoundException {
		File f = new File(dumpFile);
		if (!f.exists() || !f.isFile() || !f.getName().endsWith(DUMP_FILE_EXTENATION)) {
			throw new IOException("please check dump file " + dumpFile);
		}
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
		jpcap.packet.TCPPacket tcpPacket = null;
		TCPConnectionSnifferListener snifferListener = new DefaultTCPConnectionSnifferListener(handler,
				new ConnectionFilter());
		PacketReceiver receiver = new PacketReceiverImpl(snifferListener);
		try {
			while ((tcpPacket = (jpcap.packet.TCPPacket) ois.readObject()) != null) {
				receiver.pick(new TCPPacketImpl(tcpPacket));
			}
		} catch (EOFException e) {
			// there will be EOFException at the end of stream reading, just
			// ignore it.
		} finally {
			ois.close();
		}
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

	public static void scan(String dumpFileFolder, final TCPConnectionHandler handler) throws IOException,
			ClassNotFoundException {
		File folder = new File(dumpFileFolder);
		if (!folder.isDirectory()) {
			throw new IOException("should be a folder");
		}
		for (File f : folder.listFiles()) {
			if (f.isFile() && f.getName().endsWith(DUMP_FILE_EXTENATION)) {
				open(f.getAbsolutePath(), handler);
			}
		}
	}

	private ObjectOutputStream getPacketWriter(ConnectionDetail conDetail) throws IOException {
		ObjectOutputStream writer = writers.get(conDetail);
		if (writer == null) {
			String dumpFileName = conDetail.toString().replaceAll(">", "") + "_" + System.currentTimeMillis()
					+ DUMP_FILE_EXTENATION;
			writer = new ObjectOutputStream(new FileOutputStream(folder + File.separator + dumpFileName));
			writers.put(conDetail, writer);
		}
		return writer;
	}

	class DumpPacketReceiver implements jpcap.PacketReceiver {

		@Override
		public void receivePacket(Packet packet) {
			TCPPacket tcpPacket = new TCPPacketImpl((jpcap.packet.TCPPacket) packet);
			ConnectionDetail detail = tcpPacket.getConnectionDetail();
			ObjectOutputStream writer;
			try {
				writer = getPacketWriter(detail);
				writer.writeObject(packet);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
