package com.mexhee.tcp.connection;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.mexhee.tcp.connection.TCPConnection;
import com.mexhee.tcp.connection.server.Server;

public class ServerTest {

	private String getFullFilename(String name) {
		return new File("tcpConnectionSniffer/test/dump").getAbsolutePath() + "/" + name;
	}

	@Test
	public void test() throws IOException, InterruptedException {
		String filename = getFullFilename("192.168.1.101(17931)-192.168.1.1(80)_1352020113101.pcap");
		final Server server = Server.openFile(filename);
		final TCPConnection connection = server.accept();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					connection.getClientInputStream().configureBlocking(false);
					connection.getServerInputStream().configureBlocking(false);
					while (!connection.getClientInputStream().isFinished()
							|| !connection.getServerInputStream().isFinished()
							|| connection.getClientInputStream().hasMoreInputStream()
							|| connection.getServerInputStream().hasMoreInputStream()) {
						byte[] buffer = new byte[1024];
						int size = connection.getClientInputStream().read(buffer);
						if (size > 0) {
							System.out.println(new String(buffer));
						}
						buffer = new byte[1024];
						size = connection.getServerInputStream().read(buffer);
						if (size > 0) {
							System.out.println(new String(buffer));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("done");
				server.shutdown();
			}
		});
		t.setName("outputer");
		t.start();
		Thread.sleep(2000);
	}
}
