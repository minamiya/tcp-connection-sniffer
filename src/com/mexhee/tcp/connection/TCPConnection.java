package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mexhee.io.CombinedInputStream;
import com.mexhee.io.DynamicByteArrayInputStream;
import com.mexhee.tcp.connection.configuration.PacketListener;
import com.mexhee.tcp.connection.configuration.TCPConnectionStateListener;
import com.mexhee.tcp.packet.DuplicatedPacketException;
import com.mexhee.tcp.packet.MergedTCPPacket;
import com.mexhee.tcp.packet.TCPPacket;

/**
 * A tcp connection instance, when a syn request is detected, a new instance
 * will be created. Use
 * {@link TCPConnectionStateListener#onEstablished(TCPConnection)} to detect a
 * real tcp connection establishing after 3-way handshake.
 * 
 * @author beam.liu@gmail.com
 * 
 */
public class TCPConnection {

	// used to record the client and server side ip and port in current tcp
	// connection
	private ConnectionDetail connectionDetail;

	// to record client side sequence and ack number
	private Counter clientCounter = new Counter();
	// to record server side sequence and ack number
	private Counter serverCounter = new Counter();

	// key is the ack number
	private Map<Long, TCPPacket> temporaryStoredDataPackets = new ConcurrentHashMap<Long, TCPPacket>(200);

	private TCPConnectionState state;

	private PacketListener packetListener;
	private TCPConnectionStateListener stateListener;
	// indicate client or server send the FIN packet
	private boolean isClientRequestClosing = false;

	private DynamicByteArrayInputStream serverInputStream = new DynamicByteArrayInputStream();
	private DynamicByteArrayInputStream clientInputStream = new DynamicByteArrayInputStream();

	private StreamEvent latestEvent = null;

	public TCPConnection(ConnectionDetail connectionDetail, PacketListener packetListener,
			TCPConnectionStateListener stateListener) {
		this.connectionDetail = connectionDetail;
		this.packetListener = packetListener;
		this.stateListener = stateListener;
	}

	/**
	 * get a {@link CombinedInputStream} instance, from this stream, we could
	 * get all the data that server received through this tcp connection. If the
	 * server side finished the reading, and try to write data into tcp
	 * connection, then the {@link CombinedInputStream} will be marked EOF of
	 * last stream, and preparing for the next new input stream, at this case,
	 * whenever server side received a new set of data, then this new data will
	 * be included in the next stream. The {@link CombinedInputStream} will be
	 * finished when detected server side sent fin packet to client side.
	 * 
	 * @see CombinedInputStream
	 * @return CombinedInputStream
	 */
	public CombinedInputStream getServerInputStream() {
		return this.serverInputStream;
	}

	/**
	 * get a {@link CombinedInputStream} instance, from this stream, we could
	 * get all the data that client received through this tcp connection. If the
	 * client side finished the reading, and try to write data into tcp
	 * connection, then the {@link CombinedInputStream} will be marked EOF of
	 * last stream, and preparing for the next new input stream, at this case,
	 * whenever client side received a new set of data, then this new data will
	 * be included in the next stream. The {@link CombinedInputStream} will be
	 * finished when detected client side sent fin packet to server side.
	 * 
	 * @return CombinedInputStream
	 */

	public CombinedInputStream getClientInputStream() {
		return this.clientInputStream;
	}

	private void fireEvent() {
		// do nothing now
	}

	/**
	 * get the client and server side ip & port information in current tcp
	 * connection.
	 * 
	 * @return ConnectionDetail
	 */

	public ConnectionDetail getConnectionDetail() {
		return this.connectionDetail;
	}

	/**
	 * get current tcp connection state
	 * 
	 * @see TCPConnectionState
	 * @return TCPConnectionState
	 */
	public TCPConnectionState getState() {
		return state;
	}

	private void updateClientCounter(TCPPacket packet) {
		updateCounter(clientCounter, packet);
	}

	private void updateServerCounter(TCPPacket packet) {
		updateCounter(serverCounter, packet);
	}

	private void updateCounter(Counter counter, TCPPacket packet) {
		counter.update(packet.getSequence(), packet.getAckNum());
	}

	/*
	 * detected there was a syn packet transfered in current tcp connection, and
	 * process this packet. Usually, it means a "half open" connection will be
	 * created, it is the first packet in 3-way handshake.
	 */
	protected void processSyncPacket(TCPPacket syncPacket) {
		updateClientCounter(syncPacket);
		state = TCPConnectionState.SynSent;
		stateListener.onSynSent(this);
		packetListener.consumeTCPPacket(syncPacket);
	}

	/*
	 * detected there was a syn/ack packet transfered in current tcp connection,
	 * and process this packet.
	 */
	protected void processSyncAckPacket(TCPPacket syncAckPacket) {
		if (syncAckPacket.getAckNum() != clientCounter.sequence + 1) {
			throw new RuntimeException("sync packet ack number is incorrect!");
		}
		updateServerCounter(syncAckPacket);
		state = TCPConnectionState.SynReceived;
		stateListener.onSynReceived(this);
		packetListener.consumeTCPPacket(syncAckPacket);
	}

	protected void processAckPacket(TCPPacket ackPacket) throws IOException {
		switch (state) {
		case Established:
			processDataAckPacket(ackPacket);
			break;
		case SynReceived:
			processHandshake3AckPacket(ackPacket);
			break;
		case FinishWait1:
			processFinishWait1AckPacket(ackPacket);
			break;
		case FinishWait2:
			processFinishWait2AckPacket(ackPacket);
			stateListener.onClosing(this);
			break;
		}
	}

	private void processFinishWait1AckPacket(TCPPacket ackPacket) {
		if (isSentByClient(ackPacket)) {
			updateClientCounter(ackPacket);
		} else if (isSentByServer(ackPacket)) {
			updateServerCounter(ackPacket);
		} else {
			throw new RuntimeException("incorrect seq number");
		}
		state = TCPConnectionState.CloseWait;
		stateListener.onSynReceived(this);
		packetListener.consumeTCPPacket(ackPacket);
	}

	private void processFinishWait2AckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() == clientCounter.sequence + 1) {
			updateClientCounter(ackPacket);
		} else if (ackPacket.getAckNum() == serverCounter.sequence + 1) {
			updateServerCounter(ackPacket);
		} else {
			throw new RuntimeException("incorrect seq number");
		}
		state = TCPConnectionState.LastAck;
		stateListener.onLastAck(this);
		packetListener.consumeTCPPacket(ackPacket);
	}

	private void processHandshake3AckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() != serverCounter.sequence + 1) {
			throw new RuntimeException("sync packet ack number is incorrect!");
		}
		updateClientCounter(ackPacket);
		// server seq should be +1
		serverCounter.sequence++;
		state = TCPConnectionState.Established;
		stateListener.onEstablished(this);
		packetListener.consumeTCPPacket(ackPacket);
	}

	private void processDataAckPacket(TCPPacket ackPacket) throws IOException {
		TCPPacket dataPacket = temporaryStoredDataPackets.remove(ackPacket.getSequence());
		if (dataPacket != null) {
			boolean canConsumePacket = false;
			if (isSentByClient(dataPacket)) {
				clientCounter.update(ackPacket.getAckNum(), ackPacket.getSequence());
				serverCounter.update(ackPacket.getSequence(), ackPacket.getAckNum());
				canConsumePacket = true;
				if (latestEvent == null) {
					latestEvent = StreamEvent.createClientWritingEvent(this);
					fireEvent();
				} else if (!latestEvent.isClientWriting()) {
					if (latestEvent.isServerWriting()) {
						serverInputStream.finish(true);
					}
					latestEvent = StreamEvent.createClientWritingEvent(this);
					fireEvent();
				}
				clientInputStream.append(dataPacket.getData());
			} else if (isSentByServer(dataPacket)) {
				clientCounter.update(ackPacket.getSequence(), ackPacket.getAckNum());
				serverCounter.update(ackPacket.getAckNum(), ackPacket.getSequence());
				canConsumePacket = true;
				if (latestEvent == null) {
					latestEvent = StreamEvent.createServerWritingEvent(this);
					fireEvent();
				} else if (!latestEvent.isServerWriting()) {
					if (latestEvent.isClientWriting()) {
						clientInputStream.finish(true);
					}
					latestEvent = StreamEvent.createServerWritingEvent(this);
					fireEvent();
				}
				serverInputStream.append(dataPacket.getData());
			}
			if (canConsumePacket) {
				consumeDataPacket(dataPacket);
				packetListener.consumeTCPPacket(ackPacket);
			}
		} else {
			packetListener.discardTCPPacket(ackPacket);
		}
	}

	private void consumeDataPacket(TCPPacket dataPacket) {
		if (dataPacket instanceof MergedTCPPacket) {
			for (TCPPacket packet : ((MergedTCPPacket) dataPacket).getMergedPackets()) {
				packetListener.consumeTCPPacket(packet);
			}
		} else {
			packetListener.consumeTCPPacket(dataPacket);
		}
	}

	private boolean isSentByClient(TCPPacket packet) {
		return clientCounter.sequence == packet.getSequence();
	}

	private boolean isSentByServer(TCPPacket packet) {
		return serverCounter.sequence == packet.getSequence();
	}

	protected void processDataPacket(TCPPacket dataPacket) {
		TCPPacket packet = temporaryStoredDataPackets.remove(dataPacket.getAckNum());
		if (packet != null) {
			try {
				temporaryStoredDataPackets.put(dataPacket.getAckNum(), packet.merge(dataPacket));
			} catch (DuplicatedPacketException e) {
				packetListener.duplicatedTCPPacket(dataPacket);
				temporaryStoredDataPackets.put(dataPacket.getAckNum(), packet);
			}
		} else {
			temporaryStoredDataPackets.put(dataPacket.getAckNum(), dataPacket);
		}
	}

	protected void processFinishPacket(TCPPacket tcpPacket) throws IOException {
		TCPPacket dataPacket = temporaryStoredDataPackets.remove(tcpPacket.getAckNum());
		boolean consumedPacket = false;
		if (dataPacket != null) {
			if (isSentByClient(dataPacket)) {
				clientInputStream.append(dataPacket.getData());
				clientInputStream.finish(false);
				if (updateConnectionStateAfterReceivedFinishPacket(tcpPacket, clientCounter)) {
					isClientRequestClosing = true;
				}
				consumedPacket = true;
				packetListener.consumeTCPPacket(dataPacket);
			} else if (isSentByServer(dataPacket)) {
				serverInputStream.append(dataPacket.getData());
				serverInputStream.finish(false);
				updateConnectionStateAfterReceivedFinishPacket(tcpPacket, serverCounter);
				consumedPacket = true;
				packetListener.consumeTCPPacket(dataPacket);
			}
		} else {
			if (isSentByClient(tcpPacket)) {
				clientInputStream.finish(false);
				updateConnectionStateAfterReceivedFinishPacket(tcpPacket, clientCounter);
				consumedPacket = true;
			} else if (isSentByServer(tcpPacket)) {
				serverInputStream.finish(false);
				updateConnectionStateAfterReceivedFinishPacket(tcpPacket, serverCounter);
				consumedPacket = true;
			}
		}
		// verify there is no left data packet in the cache
		if (clientInputStream.isFinished() && serverInputStream.isFinished() && temporaryStoredDataPackets.size() > 0) {
			throw new IOException("There are stil " + temporaryStoredDataPackets.size()
					+ " packets that haven't been consumed!");
		}
		if (consumedPacket) {
			packetListener.consumeTCPPacket(tcpPacket);
		} else {
			packetListener.discardTCPPacket(tcpPacket);
		}
		fireEvent();
	}

	private boolean updateConnectionStateAfterReceivedFinishPacket(TCPPacket tcpPacket, Counter counter) {
		/*
		 * whether current packet is the first fin packet, but not the second
		 * one, as when closing a connection, it usually needs 4-way handshakes
		 * to really close a tcp connection, in these 4-way handshakes, it
		 * includes two fin packet
		 */
		boolean isFirstFinPacket = false;
		if (state == TCPConnectionState.Established) {
			stateListener.onFinishWait1(this);
			state = TCPConnectionState.FinishWait1;
			isFirstFinPacket = true;
		} else {
			stateListener.onFinishWait2(this);
			state = TCPConnectionState.FinishWait2;
			stateListener.onClosed(this);
		}
		updateCounter(counter, tcpPacket);
		return isFirstFinPacket;
	}

	/**
	 * whether fin packet is sent and sent by client side.
	 * 
	 * @return true when the fin packet is detected in current tcp connection
	 *         and it is sent by client side
	 */
	public boolean isClientRequestClosing() {
		return isClientRequestClosing;
	}

	class Counter {
		long sequence;
		long ackNum;

		void update(long sequence, long ackNum) {
			this.sequence = sequence;
			this.ackNum = ackNum;
		}

		@Override
		public String toString() {
			return "seq:" + sequence + ",ack:" + ackNum;
		}
	}
}
