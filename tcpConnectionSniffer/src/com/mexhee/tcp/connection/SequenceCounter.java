package com.mexhee.tcp.connection;

import com.mexhee.tcp.packet.TCPPacket;

class SequenceCounter {

	Counter clientCounter = new Counter();
	Counter serverCounter = new Counter();

	class Counter {
		long seq;
		long ack;
		long latestPacketUpdateTime;

		@Override
		public String toString() {
			return "seq:" + seq + "," + "ack:" + ack;
		}

		void reset() {
			seq = 0;
			ack = 0;
			latestPacketUpdateTime = 0;
		}
	}

	void updateClientCounter(TCPPacket packet) {
		updateCounter(clientCounter, packet);
	}

	private void updateCounter(Counter counter, TCPPacket packet) {
		counter.seq = packet.getSequence();
		counter.ack = packet.getAckNum();
		counter.latestPacketUpdateTime = packet.getPacketCaptureTime().getTime();
		if (packet.isContainsData()) {
			counter.seq += packet.getData().length;
		}
		packet.consumedPacket();
	}

	boolean isMatchClientSeq(TCPPacket packet) {
		checkWhetherPakcetSeqUpdatedToCounter(packet);
		return clientCounter.seq == packet.getSequence();
	}

	private void checkWhetherPakcetSeqUpdatedToCounter(TCPPacket packet) {
		if (packet.isPacketConsumed()) {
			throw new RuntimeException(
					"This packet's sequence is already updated to current Counter, cannot do comparsion!");
		}
	}

	void updateServerCounter(TCPPacket packet) {
		updateCounter(serverCounter, packet);
	}

	boolean isMatchServerSeq(TCPPacket packet) {
		checkWhetherPakcetSeqUpdatedToCounter(packet);
		return serverCounter.seq == packet.getSequence();
	}

	@Override
	public String toString() {
		return "client " + clientCounter + ", server " + serverCounter;
	}

	void serverSequenceAddOne() {
		serverCounter.seq++;
	}

	void clientSequenceAddOne() {
		clientCounter.seq++;
	}

	void reset() {
		clientCounter.reset();
		serverCounter.reset();
	}
}
