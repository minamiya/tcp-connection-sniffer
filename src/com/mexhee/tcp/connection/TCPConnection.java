package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.mexhee.io.AlreadyFinishedStreamException;
import com.mexhee.io.BufferFullException;
import com.mexhee.io.DynamicByteArrayInputStream;
import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.listener.TCPConnectionStateListener;
import com.mexhee.tcp.packet.DuplicatedPacketException;
import com.mexhee.tcp.packet.TCPPacket;

/**
 * A tcp connection instance, when a syn request is detected, a new instance
 * will be created. Use
 * {@link TCPConnectionStateListener#onEstablished(TCPConnection)} to detect a
 * real tcp connection establishing after 3-way handshake.
 * 
 */
public class TCPConnection {

	private static final Logger logger = Logger.getLogger(TCPConnection.class);
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

	private TCPConnectionStateListener stateListener;
	// indicate client or server send the FIN packet
	private boolean isClientRequestClosing = false;

	private DynamicByteArrayInputStream serverInputStream = new DynamicByteArrayInputStream();
	private DynamicByteArrayInputStream clientInputStream = new DynamicByteArrayInputStream();

	private StreamEvent latestEvent = null;
	private Date lastUpdated = new Date();

	private boolean maybeBroken = false;

	public TCPConnection(ConnectionDetail connectionDetail, TCPConnectionStateListener stateListener) {
		this.connectionDetail = connectionDetail;
		this.stateListener = stateListener;
	}

	/**
	 * get a {@link TimeMeasurableCombinedInputStream} instance, from this
	 * stream, we could get all the data that server received through this tcp
	 * connection. If the server side finished the reading, and try to write
	 * data into tcp connection, then the
	 * {@link TimeMeasurableCombinedInputStream} will be marked EOF of last
	 * stream, and preparing for the next new input stream, at this case,
	 * whenever server side received a new set of data, then this new data will
	 * be included in the next stream. The
	 * {@link TimeMeasurableCombinedInputStream} will be finished when detected
	 * server side sent fin packet to client side.
	 * 
	 * @see TimeMeasurableCombinedInputStream
	 * @return CombinedInputStream
	 */
	public TimeMeasurableCombinedInputStream getServerInputStream() {
		return this.serverInputStream;
	}

	/**
	 * get a {@link TimeMeasurableCombinedInputStream} instance, from this
	 * stream, we could get all the data that client received through this tcp
	 * connection. If the client side finished the reading, and try to write
	 * data into tcp connection, then the
	 * {@link TimeMeasurableCombinedInputStream} will be marked EOF of last
	 * stream, and preparing for the next new input stream, at this case,
	 * whenever client side received a new set of data, then this new data will
	 * be included in the next stream. The
	 * {@link TimeMeasurableCombinedInputStream} will be finished when detected
	 * client side sent fin packet to server side.
	 * 
	 * @return CombinedInputStream
	 */

	public TimeMeasurableCombinedInputStream getClientInputStream() {
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

	/**
	 * return whether both client stream and server stream are finished, just
	 * need waiting for fin packets.
	 */
	public boolean isFinished() {
		return clientInputStream.isFinished() && serverInputStream.isFinished();
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
		lastUpdated = new Date();
	}

	/*
	 * detected there was a syn/ack packet transfered in current tcp connection,
	 * and process this packet.
	 */
	protected void processSyncAckPacket(TCPPacket syncAckPacket) {
		if (state.isGreaterThan(TCPConnectionState.SynReceived)) {
			return;
		}
		if (syncAckPacket.getAckNum() != clientCounter.sequence + 1) {
			throw new RuntimeException("sync packet ack number is incorrect!");
		}
		updateServerCounter(syncAckPacket);
		state = TCPConnectionState.SynReceived;
		stateListener.onSynReceived(this);
		lastUpdated = new Date();
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
		case LastAck:
			processLastAckPacket(ackPacket);
			break;
		}
		lastUpdated = new Date();
	}

	private void processFinishWait1AckPacket(TCPPacket ackPacket) {
		if (isSentByClient(ackPacket)) {
			updateClientCounter(ackPacket);
		} else {
			updateServerCounter(ackPacket);
		}
		state = TCPConnectionState.CloseWait;
		stateListener.onCloseWait(this);
	}

	/**
	 * both side received FIN and sent ACK, then the connection is going to be
	 * CLOSED state
	 */
	private void processLastAckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() == clientCounter.sequence + 1) {
			updateClientCounter(ackPacket);
		} else if (ackPacket.getAckNum() == serverCounter.sequence + 1) {
			updateServerCounter(ackPacket);
		} else {
			logger.warn(connectionDetail.toString() + ":" + "incorrect seq number for last ACK");
			maybeBroken = true;
		}
		state = TCPConnectionState.Closed;
		stateListener.onClosed(this);
	}

	private void processHandshake3AckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() != serverCounter.sequence + 1) {
			throw new RuntimeException("syn packet ack number " + ackPacket.getAckNum() + " is incorrect!");
		}
		if (logger.isInfoEnabled())
			logger.info("new tcp connection detected " + connectionDetail.toString());
		updateClientCounter(ackPacket);
		// server seq should be +1
		serverCounter.sequence++;
		state = TCPConnectionState.Established;
		stateListener.onEstablished(this);
	}

	private void processDataAckPacket(TCPPacket ackPacket) throws IOException {
		TCPPacket dataPacket = temporaryStoredDataPackets.remove(ackPacket.getSequence());
		if (dataPacket != null) {
			if (isSentByClient(dataPacket)) {
				clientCounter.update(ackPacket.getAckNum(), ackPacket.getSequence());
				serverCounter.update(ackPacket.getSequence(), ackPacket.getAckNum());
				if (latestEvent == null) {
					latestEvent = StreamEvent.createClientWritingEvent(this);
					clientInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
					fireEvent();
				} else if (!latestEvent.isClientWriting()) {
					if (latestEvent.isServerWriting()) {
						serverInputStream.finish(true);
					}
					latestEvent = StreamEvent.createClientWritingEvent(this);
					clientInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
					fireEvent();
				}
				clientInputStream.append(dataPacket.getData());
			} else {
				clientCounter.update(ackPacket.getSequence(), ackPacket.getAckNum());
				serverCounter.update(ackPacket.getAckNum(), ackPacket.getSequence());
				if (latestEvent == null) {
					latestEvent = StreamEvent.createServerWritingEvent(this);
					serverInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
					fireEvent();
				} else if (!latestEvent.isServerWriting()) {
					if (latestEvent.isClientWriting()) {
						clientInputStream.finish(true);
					}
					latestEvent = StreamEvent.createServerWritingEvent(this);
					serverInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
					fireEvent();
				}
				serverInputStream.append(dataPacket.getData());
			}
		}
	}

	private boolean isSentByClient(TCPPacket packet) {
		boolean isSentByClient = connectionDetail.isTheSame(packet.getConnectionDetail());
		if (clientCounter.sequence != packet.getSequence() && serverCounter.sequence != packet.getSequence()) {
			maybeBroken = true;
			logger.warn(connectionDetail.toString() + ":" + (isSentByClient ? "client" : "server")
					+ " packet sequence number doesn't match!");
		}
		return isSentByClient;
	}

	protected void processDataPacket(TCPPacket dataPacket) {
		TCPPacket packet = temporaryStoredDataPackets.remove(dataPacket.getAckNum());
		if (packet != null) {
			try {
				temporaryStoredDataPackets.put(dataPacket.getAckNum(), packet.merge(dataPacket));
			} catch (DuplicatedPacketException e) {
				temporaryStoredDataPackets.put(dataPacket.getAckNum(), packet);
			}
		} else {
			temporaryStoredDataPackets.put(dataPacket.getAckNum(), dataPacket);
		}
		lastUpdated = new Date();
	}

	protected void processFinishPacket(TCPPacket tcpPacket) throws IOException {
		TCPPacket dataPacket = temporaryStoredDataPackets.remove(tcpPacket.getAckNum());
		if (dataPacket != null) {
			if (isSentByClient(dataPacket)) {
				clientInputStream.append(dataPacket.getData());
				clientInputStream.finish(false);
				if (updateConnectionStateAfterReceivedFinishPacket(tcpPacket, clientCounter)) {
					isClientRequestClosing = true;
				}
			} else {
				serverInputStream.append(dataPacket.getData());
				serverInputStream.finish(false);
				updateConnectionStateAfterReceivedFinishPacket(tcpPacket, serverCounter);
			}
		} else {
			if (isSentByClient(tcpPacket)) {
				clientInputStream.finish(false);
				updateConnectionStateAfterReceivedFinishPacket(tcpPacket, clientCounter);
			} else {
				serverInputStream.finish(false);
				updateConnectionStateAfterReceivedFinishPacket(tcpPacket, serverCounter);
			}
		}
		// verify there is no left data packet in the cache
		if (clientInputStream.isFinished() && serverInputStream.isFinished() && temporaryStoredDataPackets.size() > 0) {
			throw new IOException("There are stil " + temporaryStoredDataPackets.size()
					+ " packets that haven't been consumed!");
		}
		fireEvent();
		lastUpdated = new Date();
	}

	/**
	 * According to tcp protocol, if any side sends out rst packet, then these
	 * tcp connection will be closed, and all those data in buffer will also be
	 * thrown away.
	 */
	protected void processRstPacket(TCPPacket rstPacket) {
		if (isSentByClient(rstPacket)) {
			updateCounter(clientCounter, rstPacket);
		} else {
			updateCounter(serverCounter, rstPacket);
		}
		if (!clientInputStream.isFinished()) {
			clientInputStream.finish(false);
		}
		if (!serverInputStream.isFinished()) {
			serverInputStream.finish(false);
		}
		temporaryStoredDataPackets.clear();
		stateListener.onClosed(this);
		lastUpdated = new Date();
	}

	private boolean updateConnectionStateAfterReceivedFinishPacket(TCPPacket tcpPacket, Counter counter) {
		/*
		 * return whether current packet is the first fin packet, but not the
		 * second one, as when closing a connection, it usually needs 4-way
		 * handshakes to really close a tcp connection, in these 4-way
		 * handshakes, it includes two fin packet
		 */
		boolean isFirstFinPacket = false;
		if (state == TCPConnectionState.Established) {
			/*
			 * first FIN packet is detected
			 */
			stateListener.onFinishWait1(this);
			state = TCPConnectionState.FinishWait1;
			isFirstFinPacket = true;
		} else if (state == TCPConnectionState.CloseWait) {
			/*
			 * After first FIN packet sent, and this is the second FIN
			 */
			state = TCPConnectionState.LastAck;
			stateListener.onLastAck(this);
		} else if (state == TCPConnectionState.FinishWait1) {
			/*
			 * just sent FIN, and expect to receive a ACK from another side, but
			 * received a FIN, then it means another side is also sending FIN
			 * packet, then it should be going to CLOSING state
			 */
			stateListener.onClosing(this);
			state = TCPConnectionState.Closing;
		}
		updateCounter(counter, tcpPacket);
		return isFirstFinPacket;
	}

	/**
	 * indicate whether the tcp connection is healthy, sometimes, if there are
	 * some packets lost by kernel or in a incorrect sequence, it will lead to
	 * the sequence number is not continuous, in those cases, tcp connection
	 * sniffer will set this {@link #maybeBroken} to true.
	 */
	public boolean isMaybeBroken() {
		return this.maybeBroken;
	}

	/**
	 * get time of last packet transferred through this connection, this time is
	 * not the packet capture time in kernel, but the time processed by tcp
	 * connection sniffer
	 */
	public Date getLastUpdated() {
		return this.lastUpdated;
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

	/**
	 * close current connection, usually, it is closed by outside monitoring
	 * thread, such as TimeWait state to Closed state after some time
	 * 
	 * @throws IOException
	 *             exception when handling the data in buffer
	 */
	public void close() throws IOException {
		flushDataPacketsInMemory();
		stateListener.onClosed(this);
		state = TCPConnectionState.Closed;
	}

	private void flushDataPacketsInMemory() throws IOException {
		if (!temporaryStoredDataPackets.isEmpty()) {
			this.maybeBroken = true;
			/**
			 * flush those data in buffer, it may be an incorrect flushing, as
			 * it cannot determinate whether the sequence number is continuous
			 */
			List<TCPPacket> clientPackets = new ArrayList<TCPPacket>();
			List<TCPPacket> serverPackets = new ArrayList<TCPPacket>();
			for (TCPPacket packet : temporaryStoredDataPackets.values()) {
				if (connectionDetail.isTheSame(packet.getConnectionDetail())) {
					clientPackets.add(packet);
				} else {
					serverPackets.add(packet);
				}
			}
			flushPacketsDataToStream(clientPackets, clientInputStream);
			flushPacketsDataToStream(serverPackets, serverInputStream);
			temporaryStoredDataPackets.clear();
		}
	}

	private void flushPacketsDataToStream(List<TCPPacket> packets, DynamicByteArrayInputStream stream)
			throws IOException {
		Collections.sort(packets, new Comparator<TCPPacket>() {
			@Override
			public int compare(TCPPacket o1, TCPPacket o2) {
				return (int) (o1.getSequence() - o2.getSequence());
			}
		});
		for (TCPPacket packet : packets) {
			stream.append(packet.getData());
		}
		stream.finish(false);
	}

	private class Counter {
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
