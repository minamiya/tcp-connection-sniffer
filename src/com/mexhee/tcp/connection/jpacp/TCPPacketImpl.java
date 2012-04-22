package com.mexhee.tcp.connection.jpacp;

import java.net.InetAddress;
import java.util.Date;

import com.mexhee.tcp.packet.TCPPacket;

public class TCPPacketImpl extends TCPPacket {

	private jpcap.packet.TCPPacket packet;

	public TCPPacketImpl(jpcap.packet.TCPPacket packet) {
		this.packet = packet;
	}

	@Override
	public long getSequence() {
		return packet.sequence;
	}

	@Override
	public long getAckNum() {
		return packet.ack_num;
	}

	@Override
	public boolean isPush() {
		return packet.psh;
	}

	@Override
	public boolean isAck() {
		return packet.ack;
	}

	@Override
	public InetAddress getClientAddress() {
		return packet.src_ip;
	}

	@Override
	public InetAddress getServerAddress() {
		return packet.dst_ip;
	}

	@Override
	public int getClientPort() {
		return packet.src_port;
	}

	@Override
	public int getServerPort() {
		return packet.dst_port;
	}

	@Override
	public boolean isSyn() {
		return packet.syn;
	}

	@Override
	public boolean isRest() {
		return packet.rst;
	}

	@Override
	public byte[] getData() {
		return packet.data;
	}

	@Override
	public boolean isFinish() {
		return packet.fin;
	}

	@Override
	public Date getPacketCaptureTime() {
		return new Date(packet.sec);
	}
}
