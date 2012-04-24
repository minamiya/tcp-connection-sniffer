package com.mexhee.tcp.connection;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mexhee.tcp.connection.configuration.impl.DefaultTCPConnectionConfiguration;

public class TCPConnectionSnifferTest {

	private static TCPConnectionSniffer sniffer = null;

	@BeforeClass
	public static void setup() {
		sniffer = new TCPConnectionSniffer();
	}

	// didn't add Test annoation as the testStartUp method will block current
	// thread, just skip current testing if not a manual testing
	
	@Test
	public void testStartUp() throws IOException {
		sniffer.startup(TCPConnectionSniffer.allInterfaces()[0], new DefaultTCPConnectionConfiguration());
	}

	@Test
	public void testShutdown() {
		sniffer.shutdown();
	}
}
