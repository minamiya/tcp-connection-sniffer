package com.mexhee.packet.util;

import java.io.File;
import java.io.IOException;

import jpcap.JpcapCaptor;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mexhee.tcp.connection.ViewTCPConnection;
import com.mexhee.tcp.connection.listener.ConnectionFilter;

public class TCPPacketsRecorderTest {
	private static String dumpFolder = "";

	@BeforeClass
	public static void setup() {
		dumpFolder = new File("test/dump1").getAbsolutePath();
		System.out.println(dumpFolder);
	}
	
	public void testDumpFile() throws IOException {
		ConnectionFilter filter = new ConnectionFilter();
//		filter.addServerHostFilter("192.168.1.1");
		TCPPacketsRecorder recorder = new TCPPacketsRecorder(filter, JpcapCaptor.getDeviceList()[2], dumpFolder);
		recorder.start();
	}
	
	public void testRunAppFromDumpFile() throws IOException, ClassNotFoundException {
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(6582)-218.77.130.160(80)_1335780709026.dump",
				new ViewTCPConnection());
	}

	@Test
	public void testRunAppFromDumpFolder() throws IOException, ClassNotFoundException {
		TCPPacketsRecorder.scan(dumpFolder, new ViewTCPConnection());
	}

	public void testViewPacketsFromDumpFile() throws IOException, ClassNotFoundException {
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(2350)-192.168.1.1(80)_1334495098578.dump",
				new ViewTCPConnection());
	}
}
