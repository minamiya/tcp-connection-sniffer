package com.mexhee.packet.util;

import java.io.File;
import java.io.IOException;

import jpcap.JpcapCaptor;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mexhee.http.HTTPViewer;
import com.mexhee.tcp.connection.TCPConnection;
import com.mexhee.tcp.connection.ViewTCPConnection;
import com.mexhee.tcp.connection.listener.ConnectionFilter;
import com.mexhee.tcp.connection.listener.HalfWayTCPConnectionHandler;

public class TCPPacketsRecorderTest {
	private static String dumpFolder = "";

	@BeforeClass
	public static void setup() {
		dumpFolder = new File("tcpConnectionSniffer/test/dump").getAbsolutePath();
		System.out.println(dumpFolder);
	}

	public void testDumpFile() throws IOException {
		ConnectionFilter filter = new ConnectionFilter();
		// filter.addServerHostFilter("192.168.1.1");
		TCPPacketsRecorder recorder = new TCPPacketsRecorder(filter, JpcapCaptor.getDeviceList()[2], dumpFolder);
		recorder.start();
	}

	@Test
	public void testRunAppFromDumpFile() throws Exception {
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(4598)-192.168.1.1(80)_1162393257937.dump",
				new HTTPViewer());
		Thread.sleep(1000);
	}

	@Test
	public void testRunAppFromDumpFileHalfWayConnection() throws Exception {
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(4604)-192.168.1.1(80)_1162393257984.dump", 3,
				new HalfWayConnectionHandler());
		Thread.sleep(1000);
	}
	@Test
	public void testRunAppFromDumpFolder() throws IOException, ClassNotFoundException {
		TCPPacketsRecorder.scan(dumpFolder, new HTTPViewer());
	}

	public void testViewPacketsFromDumpFile() throws IOException, ClassNotFoundException {
		TCPPacketsRecorder.open(dumpFolder + "/192.168.1.100(4608)-192.168.1.1(80)_1162393258000.dump",
				new HTTPViewer());
	}

	class HalfWayConnectionHandler extends ViewTCPConnection implements HalfWayTCPConnectionHandler {

		@Override
		public void onConnectionDetected(TCPConnection connection) {
			System.out.println(connection + " is detected");
		}
	}
}
