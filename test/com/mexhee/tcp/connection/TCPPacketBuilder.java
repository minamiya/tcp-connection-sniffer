package com.mexhee.tcp.connection;


import java.net.InetAddress;
import java.net.UnknownHostException;

import com.mexhee.tcp.connection.jpacp.TCPPacketImpl;

import jpcap.packet.TCPPacket;

public class TCPPacketBuilder {

	private int srcPort,destPort;
	private String srcIp,destIp;
	private boolean rsv1, rsv2, urg = false;
	private int window, urgent = 0;
	
	private TCPPacket packet;
	
	public TCPPacketBuilder(int srcPort, int destPort, String srcIp,
			String destIp) {
		this.srcPort = srcPort;
		this.destPort = destPort;
		this.srcIp = srcIp;
		this.destIp = destIp;
	}
	
	public TCPPacketBuilder setSeqAckNum(long seq, long ack){
		packet.sequence = seq;
		packet.ack_num = ack;
		return this;
	}
	
	public TCPPacketBuilder syn(){
		packet.syn = true;
		return this;
	}
	
	public TCPPacketBuilder ack(){
		packet.ack = true;
		return this;
	}
	
	public TCPPacketBuilder fin(){
		packet.fin=true;
		return this;
	}
	
	private TCPPacketBuilder psh(){
		packet.psh = true;
		return this;
	}

	public TCPPacketBuilder localToServer() {
		this.packet = new TCPPacket(srcPort, destPort, 0, 0, urg, false, false,
				false, false, false, rsv1, rsv2, window, urgent);
		try {
			packet.src_ip = InetAddress.getByName(srcIp);
			packet.dst_ip = InetAddress.getByName(destIp);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public  TCPPacketBuilder serverToLocal() {
		this.packet = new TCPPacket(destPort, srcPort, 0, 0, urg, false, false,
				false, false, false, rsv1, rsv2, window, urgent);
		try {
			packet.dst_ip = InetAddress.getByName(srcIp);
			packet.src_ip = InetAddress.getByName(destIp);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	public com.mexhee.tcp.packet.TCPPacket build(){
		return new TCPPacketImpl(packet);
	}
	
	//push data continue, there won't be psh flag set
	public TCPPacketBuilder cData(String data) {
		packet.data = data.getBytes();
		return this;
	}
	
	public TCPPacketBuilder data(String data) {
		return data(data.getBytes());
	}

	public TCPPacketBuilder data(byte[] bytes) {
		packet.data = bytes;
		if(bytes != null){
			psh();
		}
		return this;
	}
}
