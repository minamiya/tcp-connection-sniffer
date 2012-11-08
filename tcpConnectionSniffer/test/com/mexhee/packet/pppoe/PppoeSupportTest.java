package com.mexhee.packet.pppoe;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.JpcapFilter;
import jpcap.JpcapFilter.Protocol;
import jpcap.PacketReceiver;
import jpcap.packet.PPPOEPacket;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

import org.junit.Assert;
import org.junit.Test;

public class PppoeSupportTest {

	@Test
	public void testSupportPppoeParse() throws IOException {
		JpcapCaptor captor = JpcapCaptor.openFile("tcpConnectionSniffer/test/pppoe/pppoe.pcap");
		captor.loopPacket(0, new PacketReceiver() {

			@Override
			public void receivePacket(Packet packet) {
				Assert.assertNotNull(packet.datalink);
				Assert.assertTrue(packet.datalink instanceof PPPOEPacket);
			}
		});
	}
	
	@Test
	public void testJpcapFilter() throws IOException {
		JpcapFilter filter = new JpcapFilter();
		filter.addProtocol(Protocol.TCP);
		filter.addHost("14.116.24.179");
		filter.addSrcHost("14.116.24.179");
		filter.addDestHost("110.189.112.59");
		filter.addPort(20001);
		filter.addSrcPort(20001);
		filter.addDestPort(4090);
		JpcapCaptor captor = JpcapCaptor.openFile("test/pppoe/pppoe.pcap");
		captor.setJpcapFilter(filter);
		captor.loopPacket(0, new PacketReceiver() {

			@Override
			public void receivePacket(Packet packet) {
				Assert.assertNotNull(packet.datalink);
				Assert.assertTrue(packet instanceof TCPPacket);
				Assert.assertEquals(((TCPPacket)packet).src_ip.getHostAddress(), "14.116.24.179");
				Assert.assertEquals(((TCPPacket)packet).dst_ip.getHostAddress(), "110.189.112.59");
				Assert.assertEquals(((TCPPacket)packet).src_port, 20001);
				Assert.assertEquals(((TCPPacket)packet).dst_port, 4090);
			}
		});
	}
}
