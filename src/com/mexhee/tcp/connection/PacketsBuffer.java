package com.mexhee.tcp.connection;

import java.util.SortedSet;
import java.util.TreeSet;

import com.mexhee.tcp.packet.TCPPacket;

public class PacketsBuffer {

	/**
	 * store the data packets (client to server) that cannot match seq number
	 * due to captured in incorrect sequence
	 */
	SortedSet<TCPPacket> csTemporaryStoredPackets = new TreeSet<TCPPacket>();
	/**
	 * store the data packets (server to client) that cannot match seq number
	 * due to captured in incorrect sequence
	 */
	SortedSet<TCPPacket> scTemporaryStoredPackets = new TreeSet<TCPPacket>();

	void addToCSTemporaryStoredDataPackets(TCPPacket packet) {
		csTemporaryStoredPackets.add(packet);
	}

	void addToSCTemporaryStoredDataPackets(TCPPacket packet) {
		scTemporaryStoredPackets.add(packet);
	}

	TCPPacket pickupPacket(SequenceCounter counter, TCPConnection connection) {
		return pickupCSBuffer(counter, connection);
	}

	private TCPPacket pickupCSBuffer(SequenceCounter counter, TCPConnection connection) {
		if (!csTemporaryStoredPackets.isEmpty()) {
			TCPPacket packet = csTemporaryStoredPackets.first();
			if (connection.isMatchSequence(packet)) {
				if (counter.clientCounter.ack == packet.getAckNum()) {
					csTemporaryStoredPackets.remove(packet);
					return packet;
				} else {
					/**
					 * if ack number is not the same, it means there is a server
					 * to client writting packet, so try to pickup
					 * scTemporaryStoredDataPackets
					 */
					return pickupSCBuffer(counter, connection);
				}
			} else if (connection.isOldPacket(packet)) {
				csTemporaryStoredPackets.remove(packet);
				return pickupCSBuffer(counter, connection);
			}
		} else {
			return pickupSCBuffer(counter, connection);
		}
		return null;
	}

	private TCPPacket pickupSCBuffer(SequenceCounter counter, TCPConnection connection) {
		if (!scTemporaryStoredPackets.isEmpty()) {
			TCPPacket packet = scTemporaryStoredPackets.first();
			if (connection.isMatchSequence(packet)) {
				if (counter.serverCounter.ack == packet.getAckNum()) {
					scTemporaryStoredPackets.remove(packet);
					return packet;
				}
			} else if (connection.isOldPacket(packet)) {
				scTemporaryStoredPackets.remove(packet);
				return pickupSCBuffer(counter, connection);
			}
		}
		return null;
	}
}
