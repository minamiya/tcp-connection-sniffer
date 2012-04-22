package com.mexhee.tcp.packet;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MergedTCPPacket extends TCPPacket {

	// the first packet that has the smallest sequence number
	private TCPPacket earliestPacket;

	//all the packets that have the same ack numbers, they will be merged together sorted by sequence
	private Map<Long, TCPPacket> packets = new ConcurrentHashMap<Long, TCPPacket>();

	public MergedTCPPacket(TCPPacket packet1, TCPPacket packet2) throws DuplicatedPacketException {
		packets.put(packet1.getSequence(), packet1);
		earliestPacket = packet1;
		merge(packet2);
	}

	@Override
	public TCPPacket merge(TCPPacket anotherPacket) throws DuplicatedPacketException {
		if (packets.get(anotherPacket.getSequence()) != null) {
			throw new DuplicatedPacketException("duplicated sequence number " + anotherPacket.getSequence());
		}
		packets.put(anotherPacket.getSequence(), anotherPacket);
		if (earliestPacket.getAckNum() != anotherPacket.getAckNum()) {
			throw new RuntimeException("cannot merge two packets due to different ack number!");
		}
		if (anotherPacket.getSequence() < earliestPacket.getSequence()) {
			earliestPacket = anotherPacket;
		}
		return this;
	}

	@Override
	public long getSequence() {
		return earliestPacket.getSequence();
	}

	@Override
	public long getAckNum() {
		return earliestPacket.getSequence();
	}

	// it should be push packet
	@Override
	public boolean isPush() {
		return true;
	}

	@Override
	public boolean isAck() {
		return earliestPacket.isAck();
	}

	// it cannot be a syn packet
	@Override
	public boolean isSyn() {
		return false;
	}
	
	@Override
	public boolean isRest(){
		return false;
	}

	@Override
	public InetAddress getClientAddress() {
		return earliestPacket.getClientAddress();
	}

	@Override
	public InetAddress getServerAddress() {
		return earliestPacket.getServerAddress();
	}

	@Override
	public int getClientPort() {
		return earliestPacket.getClientPort();
	}

	@Override
	public int getServerPort() {
		return earliestPacket.getServerPort();
	}

	@Override
	public byte[] getData() {
		byte[] data = new byte[getAllByteSize()];
		List<Long> keys = new ArrayList<Long>(packets.keySet());
		Collections.sort(keys);
		Iterator<Long> iterator = keys.iterator();
		TCPPacket firstPacket = packets.get(iterator.next());
		System.arraycopy(firstPacket.getData(), 0, data, 0, firstPacket.getData().length);
		int copiedLen = firstPacket.getData().length;
		while (iterator.hasNext()) {
			TCPPacket secondPacket = packets.get(iterator.next());
			if (firstPacket.getData().length + firstPacket.getSequence() != secondPacket.getSequence()) {
				throw new LostPacketException("Those packets are not continuous!");
			}
			System.arraycopy(secondPacket.getData(), 0, data, copiedLen, secondPacket.getData().length);
			copiedLen += secondPacket.getData().length;
			firstPacket = secondPacket;
		}
		return data;
	}

	private int getAllByteSize() {
		int size = 0;
		for (TCPPacket packet : packets.values()) {
			size += packet.getData().length;
		}
		return size;
	}

	@Override
	public boolean isFinish() {
		return false;
	}

	public Collection<TCPPacket> getMergedPackets() {
		return packets.values();
	}

	@Override
	public Date getPacketCaptureTime() {
		return earliestPacket.getPacketCaptureTime();
	}
}
