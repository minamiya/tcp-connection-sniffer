package com.mexhee.tcp.packet;

import java.net.InetAddress;
import java.util.Date;

import com.mexhee.tcp.connection.ConnectionDetail;

/**
 * A representing of TCP packet abstraction
 */
public abstract class TCPPacket implements Comparable<TCPPacket> {

	/**
	 * indicate whether current tcp packet is processed by tcp connection
	 * sniffer
	 */
	private boolean consumed = false;
	/**
	 * datalink level data, the tcp packet is sent from which host & port, and
	 * delivered to which host & port
	 */
	private ConnectionDetail connectionDetail = null;

	/**
	 * the packet sent direction, whether it is from client to server, this
	 * attribute is meaningful only when comparing with a connection
	 */
	private Boolean isSentByClient = null;

	/**
	 * get the sequence number
	 */
	public abstract long getSequence();

	/**
	 * get the ack number
	 */
	public abstract long getAckNum();

	/**
	 * is push flag
	 */
	public abstract boolean isPush();

	/**
	 * is ack flag
	 */
	public abstract boolean isAck();

	/**
	 * is syn flag
	 */
	public abstract boolean isSyn();

	/**
	 * is rest flag
	 */
	public abstract boolean isRest();

	/**
	 * is fin flag
	 */
	public abstract boolean isFinish();

	/**
	 * packet sent from ip
	 */
	public abstract InetAddress getClientAddress();

	/**
	 * packet sent to ip
	 */
	public abstract InetAddress getServerAddress();

	/**
	 * packet sent from port
	 */
	public abstract int getClientPort();

	/**
	 * packet sent to port
	 */
	public abstract int getServerPort();

	/**
	 * tcp packet payload data excluding header
	 */
	public abstract byte[] getData();

	/**
	 * whether this packet is a syn packet, which is the first packet to do
	 * hands shake connection
	 */
	public boolean isHandsShake1Packet() {
		return isSyn() && !isAck();
	}

	/**
	 * whether this packet is a syn/ack packet, which is the second packet to do
	 * hands shake connection
	 */
	public boolean isHandsShake2Packet() {
		return isSyn() && isAck();
	}

	/**
	 * whether current tcp packet has data, but no only tcp header
	 */
	public boolean isContainsData() {
		return getData() != null && getData().length > 0;
	}

	/**
	 * get the packet datalink level information, from host & port and to host &
	 * port
	 */
	public ConnectionDetail getConnectionDetail() {
		if (connectionDetail == null) {
			connectionDetail = new ConnectionDetail(getClientAddress(), getServerAddress(), getClientPort(),
					getServerPort());
		}
		return connectionDetail;
	}

	/**
	 * set current packet consumed flag to true
	 */
	public void consumedPacket() {
		this.consumed = true;
	}

	/**
	 * indicate whether current tcp packet is processed by tcp connection
	 * sniffer
	 */
	public boolean isPacketConsumed() {
		return this.consumed;
	}

	/**
	 * detect the packet flow, from client to server or from server to client
	 * 
	 * @param connectionDetail
	 *            physical tcp connection detail
	 */
	public void detectPacketFlowDirection(ConnectionDetail connectionDetail) {
		isSentByClient = this.connectionDetail.isTheSame(connectionDetail);
	}

	/**
	 * whether the packet flow is from client to server
	 * 
	 * @return whether the packet flow is from client to server
	 * 
	 * @throws NullPointerException
	 *             when {@link #detectPacketFlowDirection(ConnectionDetail)}
	 *             hasn't been invoked
	 */
	public boolean isSentByClient() {
		return isSentByClient;
	}

	/**
	 * get the packet capture time in kernel
	 */
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
		sb.append(isPush() ? ",psh" : (isContainsData() ? ",data" : ""));
		sb.append(isRest() ? ",rst" : "");
		return sb.toString();
	}

	/**
	 * TCP packet is comparable according to
	 */
	@Override
	public int compareTo(TCPPacket anotherPacket) {
		return (int) (getSequence() - anotherPacket.getSequence());
	}
}
