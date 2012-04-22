package com.mexhee.tcp.packet;

import java.net.InetAddress;
import java.util.Date;

import com.mexhee.tcp.connection.ConnectionDetail;

public abstract class TCPPacket {

	private ConnectionDetail connectionDetail = null;

	public abstract long getSequence();

	public abstract long getAckNum();

	public abstract boolean isPush();

	public abstract boolean isAck();

	public abstract boolean isSyn();

	public abstract boolean isRest();

	public abstract InetAddress getClientAddress();

	public abstract InetAddress getServerAddress();

	public abstract int getClientPort();

	public abstract int getServerPort();

	public abstract byte[] getData();

	public abstract boolean isFinish();

	public boolean isHandsShake1Packet() {
		return isSyn() && !isAck();
	}

	public boolean isHandsShake2Packet() {
		return isSyn() && isAck();
	}

	public TCPPacket merge(TCPPacket anotherPacket) throws DuplicatedPacketException {
		return new MergedTCPPacket(this, anotherPacket);
	}

	public boolean isContainsData() {
		return getData() != null && getData().length > 0;
	}

	public ConnectionDetail getConnectionDetail() {
		if (connectionDetail == null) {
			connectionDetail = new ConnectionDetail(getClientAddress(), getServerAddress(), getClientPort(),
					getServerPort());
		}
		return connectionDetail;
	}
	
	public abstract Date getPacketCaptureTime();

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getClientAddress().getHostAddress() + "->" + getServerAddress().getHostAddress());
		sb.append(",seq:" + getSequence());
		sb.append(",ack:" + getAckNum());
		sb.append(isSyn() ? ",syn" : "");
		sb.append(isAck() ? ",ack" : "");
		sb.append(isFinish() ? ",fin" : "");
		return sb.toString();
	}
}
