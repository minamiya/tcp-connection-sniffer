package com.mexhee.tcp.connection.jpacp;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;

import com.mexhee.tcp.connection.PacketReceiver;
import com.mexhee.tcp.connection.PacketReceiverImpl;
import com.mexhee.tcp.connection.configuration.TCPConnectionConfiguration;
import com.mexhee.tcp.connection.configuration.impl.DefaultTCPConnectionConfiguration;

public class TCPConnectionSniffer {

	private JpcapCaptor captor;
	public void startup(NetworkInterface networkInterface, TCPConnectionConfiguration configuration) throws IOException {
		captor = JpcapCaptor.openDevice(networkInterface, 2000, false, 10000);
		captor.setFilter(configuration.getConnectionFilter().toString(), true);
		final PacketReceiver picker = new PacketReceiverImpl(configuration);
		captor.loopPacket(0, new jpcap.PacketReceiver() {
			public void receivePacket(Packet packet) {
				try {
					picker.pick(new TCPPacketImpl((jpcap.packet.TCPPacket) packet));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void main(String[] args) throws IOException {
		TCPConnectionSniffer test = new TCPConnectionSniffer();
		test.startup(allInterfaces()[0], new DefaultTCPConnectionConfiguration());
	}

	public void shutdown() {
		if(captor != null){
			captor.breakLoop();
		}
	}

	public static NetworkInterface[] allInterfaces() {
		return JpcapCaptor.getDeviceList();
	}
}
