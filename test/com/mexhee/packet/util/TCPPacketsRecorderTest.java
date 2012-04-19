package com.mexhee.packet.util;

import java.io.File;
import java.io.IOException;

import jpcap.JpcapCaptor;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mexhee.tcp.connection.PacketReceiver;
import com.mexhee.tcp.connection.PacketReceiverImpl;
import com.mexhee.tcp.connection.configuration.ConnectionFilter;
import com.mexhee.tcp.connection.configuration.impl.DefaultConnectionStateListener;
import com.mexhee.tcp.connection.configuration.impl.DefaultTCPConnectionConfiguration;
import com.mexhee.tcp.packet.TCPPacket;

public class TCPPacketsRecorderTest {
	private static String dumpFolder = "";

	@BeforeClass
	public static void setup() {
		dumpFolder = new File("test/dump").getAbsolutePath();
		System.out.println(dumpFolder);
	}
	
	public void testDumpFile() throws IOException {
		ConnectionFilter filter = new ConnectionFilter();
		filter.addHost("192.168.1.1");
		TCPPacketsRecorder recorder = new TCPPacketsRecorder(filter, JpcapCaptor.getDeviceList()[0], dumpFolder);
		recorder.start();
	}

	
	public void testRunAppFromDumpFile() throws IOException, ClassNotFoundException {
		DefaultTCPConnectionConfiguration config = new DefaultTCPConnectionConfiguration();
		DefaultConnectionStateListener stateListener = new DefaultConnectionStateListener();
		config.setStateListener(stateListener);
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(4606)-192.168.1.1(80)_1162393258000.dump",
				new PacketReceiverImpl(config));
		stateListener.getExecutor().shutdown();
	}
	
	@Test
	public void testRunAppFromDumpFolder() throws IOException, ClassNotFoundException {
		DefaultTCPConnectionConfiguration config = new DefaultTCPConnectionConfiguration();
		DefaultConnectionStateListener stateListener = new DefaultConnectionStateListener();
		config.setStateListener(stateListener);
		TCPPacketsRecorder.scan(dumpFolder, new PacketReceiverImpl(new DefaultTCPConnectionConfiguration()));
		stateListener.getExecutor().shutdown();
	}

	public void testViewPacketsFromDumpFile() throws IOException, ClassNotFoundException {
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(2350)-192.168.1.1(80)_1334495098578.dump",
				new PacketReceiver() {
					@Override
					public void pick(TCPPacket tcpPacket) throws IOException {
						StringBuffer sb = new StringBuffer();
						sb.append(tcpPacket.toString());
						sb.append("\n");
						if (tcpPacket.isContainsData()) {
							sb.append(new String(tcpPacket.getData()));
						}
						sb.append("\n");
						System.out.println(sb.toString());
					}
				});
	}
}
