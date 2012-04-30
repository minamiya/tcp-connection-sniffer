package com.mexhee.tcp.connection;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mexhee.tcp.connection.listener.ConnectionFilter;

public class TCPConnectionSnifferTest {

	private static TCPConnectionSniffer sniffer = null;

	@BeforeClass
	public static void setup() throws UnknownHostException {
		sniffer = new TCPConnectionSniffer();
		ConnectionFilter filter = new ConnectionFilter();
		filter.addServerFilter("192.168.1.1", 80);
		sniffer.setConnectionFilter(filter);
	}

	// didn't add Test annoation as the testStartUp method will block current
	// thread, just skip current testing if not a manual testing

	//@Test
	public void testStartUp() throws IOException {
		sniffer.startup(TCPConnectionSniffer.allInterfaces()[2], new ViewTCPConnection());
	}

	@Test
	public void testShutdown() {
		sniffer.shutdown();
	}
}
