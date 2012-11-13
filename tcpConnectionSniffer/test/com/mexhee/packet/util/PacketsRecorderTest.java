package com.mexhee.packet.util;

import java.io.File;
import java.io.IOException;

import jpcap.JpcapCaptor;

import org.junit.BeforeClass;

import com.mexhee.tcp.connection.ConnectionFilter;

public class PacketsRecorderTest {
	private static String dumpFolder = "";

	@BeforeClass
	public static void setup() {
		dumpFolder = new File("tcpConnectionSniffer/test/dump").getAbsolutePath();
		System.out.println(dumpFolder);
	}

	public void testDumpFile() throws IOException {
		ConnectionFilter filter = new ConnectionFilter();
		// filter.addServerHostFilter("192.168.1.1");
		PacketsRecorder recorder = new PacketsRecorder(filter, JpcapCaptor.getDeviceList()[4], dumpFolder);
		recorder.start();
	}

	public void testStopDumpFile() throws IOException {
		PacketsRecorder recorder = new PacketsRecorder(new ConnectionFilter(), JpcapCaptor.getDeviceList()[2],
				dumpFolder);
		recorder.stop();
	}
}
