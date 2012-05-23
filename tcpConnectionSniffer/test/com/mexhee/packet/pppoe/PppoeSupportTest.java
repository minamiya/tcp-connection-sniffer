package com.mexhee.packet.pppoe;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.JpcapFilter;
import jpcap.JpcapFilter.Protocol;
import jpcap.PacketReceiver;
import jpcap.packet.PPPOEPacket;
import jpcap.packet.Packet;

import org.junit.Assert;
import org.junit.Test;

public class PppoeSupportTest {

	@Test
	public void testSupportPppoeParse() throws IOException {
		JpcapCaptor captor = JpcapCaptor.openFile("test/pppoe/pppoe.pcap");
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
		filter.addProtocol(Protocol.ICMP);
		JpcapCaptor captor = JpcapCaptor.openFile("test/pppoe/ICMP.pcap");
		captor.setJpcapFilter(filter);
		captor.loopPacket(0, new PacketReceiver() {

			@Override
			public void receivePacket(Packet packet) {
				Assert.assertNotNull(packet.datalink);
			}
		});
	}
}
