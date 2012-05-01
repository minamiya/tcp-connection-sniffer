package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.mexhee.io.DynamicByteArrayInputStream;
import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.listener.TCPConnectionStateListener;
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

	// to record client & server side sequence number
	private Counter counter = new Counter();

	// key is the ack number
	// private Map<Long, TCPPacket> temporaryStoredDataPackets = new
	// ConcurrentHashMap<Long, TCPPacket>(200);

	private TCPConnectionState state;

	private TCPConnectionStateListener stateListener;
	// indicate client or server send the FIN packet
	private boolean isClientRequestClosing = false;

	private DynamicByteArrayInputStream serverInputStream = new DynamicByteArrayInputStream();
	private DynamicByteArrayInputStream clientInputStream = new DynamicByteArrayInputStream();

	/**
	 * store the data packets (client to server) that cannot match seq number
	 * due to captured in incorrect sequence
	 */
	private SortedSet<TCPPacket> csTemporaryStoredDataPackets = new TreeSet<TCPPacket>();
	/**
	 * store the data packets (server to client) that cannot match seq number
	 * due to captured in incorrect sequence
	 */
	private SortedSet<TCPPacket> scTemporaryStoredDataPackets = new TreeSet<TCPPacket>();

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

	/*
	 * detected there was a syn packet transfered in current tcp connection, and
	 * process this packet. Usually, it means a "half open" connection will be
	 * created, it is the first packet in 3-way handshake.
	 */
	protected void processSyncPacket(TCPPacket syncPacket) {
		counter.updateClientCounter(syncPacket);
		state = TCPConnectionState.SynSent;
		stateListener.onSynSent(this);
	}

	/*
	 * detected there was a syn/ack packet transfered in current tcp connection,
	 * and process this packet.
	 */
	protected void processSyncAckPacket(TCPPacket syncAckPacket) {
		if (syncAckPacket.getAckNum() != counter.clientSeq + 1) {
			throw new RuntimeException("sync packet ack number is incorrect!");
		}
		counter.updateServerCounter(syncAckPacket);
		state = TCPConnectionState.SynReceived;
		stateListener.onSynReceived(this);
	}

	/**
	 * get a {@link Counter} object that represents client and server side
	 * sequence number
	 * 
	 * @return a {@link Counter} object that represents client and server side
	 *         sequence number
	 */
	public Counter getSequenceNumCounter() {
		return counter;
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
			counter.updateClientCounter(ackPacket);
			counter.serverSeq++;
		} else {
			counter.updateServerCounter(ackPacket);
			counter.clientSeq++;
		}
		state = TCPConnectionState.CloseWait;
		stateListener.onCloseWait(this);
	}

	/**
	 * both side received FIN and sent ACK, then the connection is going to be
	 * CLOSED state
	 */
	private void processLastAckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() == counter.clientSeq + 1) {
			counter.updateClientCounter(ackPacket);
		} else if (ackPacket.getAckNum() == counter.serverSeq + 1) {
			counter.updateServerCounter(ackPacket);
		} else {
			logger.warn(connectionDetail.toString() + ":" + "incorrect seq number for last ACK");
			maybeBroken = true;
		}
		state = TCPConnectionState.Closed;
		stateListener.onClosed(this);
	}

	private void processHandshake3AckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() != counter.serverSeq + 1) {
			throw new RuntimeException("syn packet ack number " + ackPacket.getAckNum() + " is incorrect!");
		}
		if (logger.isInfoEnabled())
			logger.info("new tcp connection detected " + connectionDetail.toString());
		counter.updateClientCounter(ackPacket);
		// // server seq should be +1
		counter.serverSeq++;
		state = TCPConnectionState.Established;
		stateListener.onEstablished(this);
	}

	private void processDataAckPacket(TCPPacket ackPacket) throws IOException {
		// do nothing
	}

	private boolean isSentByClient(TCPPacket packet, DismatchedSequenceNumberHandler handler) {
		boolean isSentByClient = connectionDetail.isTheSame(packet.getConnectionDetail());
		/**
		 * whether the checking is necessary, and packet's sequence hasn't been
		 * applied to the Counter
		 */
		if (handler.isEnableChecking() && !packet.isPacketConsumed()) {
			if (isSentByClient) {
				if (counter.clientSeq > 0 && !counter.isMatchClientSeq(packet)) {
					handler.dismatchedSequenceNumber(isSentByClient, packet);
				}
			} else {
				if (counter.serverSeq > 0 && !counter.isMatchServerSeq(packet)) {
					handler.dismatchedSequenceNumber(isSentByClient, packet);
				}
			}

		}
		return isSentByClient;
	}

	private boolean isSentByClient(TCPPacket packet) {
		return isSentByClient(packet, new LoggingDismatchedSequenceNumberHandler());
	}

	protected void processDataPacket(TCPPacket dataPacket) throws IOException {
		try {
			if (isSentByClient(dataPacket, new StoreToBufferDismatchedSequenceNumberHandler())) {
				processCSDataPacket(dataPacket);
			} else {
				processSCDataPacket(dataPacket);
			}
		} catch (DismatchedSequenceNumberException e) {
			// try to process it combining with
			if (e.isSentByClient) {
				clearBufferedCSDataPacket();
			} else {
				clearBufferedSCDataPacket();
			}
		}
	}

	/**
	 * take a look whether the buffered client to server data packet could be
	 * consumed
	 * 
	 * @throws IOException
	 *             streaming exception
	 */
	private void clearBufferedCSDataPacket() throws IOException {
		List<TCPPacket> successfullyHandledPackets = new ArrayList<TCPPacket>();
		for (TCPPacket packet : csTemporaryStoredDataPackets) {
			if (counter.isMatchClientSeq(packet)) {
				processCSDataPacket(packet);
				successfullyHandledPackets.add(packet);
			} else if (counter.clientSeq > packet.getSequence()) {
				successfullyHandledPackets.add(packet);
			} else {
				// they are already sorted by sequence number, if one cannot
				// match, then no need continue
				break;
			}
		}
		csTemporaryStoredDataPackets.removeAll(successfullyHandledPackets);
		if (!successfullyHandledPackets.isEmpty() && logger.isInfoEnabled())
			logger.info("successfully processed " + successfullyHandledPackets.size() + " cs packets in buffer");
	}

	/**
	 * take a look whether the buffered server to client data packet could be
	 * consumed
	 * 
	 * @throws IOException
	 *             streaming exception
	 */
	private void clearBufferedSCDataPacket() throws IOException {
		List<TCPPacket> successfullyHandledPackets = new ArrayList<TCPPacket>();
		for (TCPPacket packet : scTemporaryStoredDataPackets) {
			if (counter.isMatchServerSeq(packet)) {
				processSCDataPacket(packet);
				successfullyHandledPackets.add(packet);
			} else if (counter.serverSeq > packet.getSequence()) {
				successfullyHandledPackets.add(packet);
			} else {
				// they are already sorted by sequence number, if one cannot
				// match, then no need continue
				break;
			}
		}
		scTemporaryStoredDataPackets.removeAll(successfullyHandledPackets);
		if (!successfullyHandledPackets.isEmpty() && logger.isInfoEnabled())
			logger.info("successfully processed " + successfullyHandledPackets.size() + " sc packets in buffer");
	}

	/**
	 * process server to client data packet, append data into
	 * {@link #serverInputStream}
	 */
	private void processSCDataPacket(TCPPacket dataPacket) throws IOException {
		if (latestEvent == null) {
			latestEvent = StreamEvent.createServerWritingEvent(this);
			serverInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
		} else if (!latestEvent.isServerWriting()) {
			if (latestEvent.isClientWriting()) {
				clientInputStream.finish(true);
			}
			latestEvent = StreamEvent.createServerWritingEvent(this);
			serverInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
		}
		serverInputStream.append(dataPacket.getData());
		counter.updateServerCounter(dataPacket);
	}

	/**
	 * process client to server data packet, append data into
	 * {@link #clientInputStream}
	 */
	private void processCSDataPacket(TCPPacket dataPacket) throws IOException {
		if (latestEvent == null) {
			latestEvent = StreamEvent.createClientWritingEvent(this);
			clientInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
		} else if (!latestEvent.isClientWriting()) {
			if (latestEvent.isServerWriting()) {
				serverInputStream.finish(true);
			}
			latestEvent = StreamEvent.createClientWritingEvent(this);
			clientInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
		}
		clientInputStream.append(dataPacket.getData());
		counter.updateClientCounter(dataPacket);
	}

	protected void processFinishPacket(TCPPacket tcpPacket) throws IOException {
		if (isSentByClient(tcpPacket)) {
			clearBufferedCSDataPacket();
			clientInputStream.finish(false);
			updateConnectionStateAfterReceivedFinishPacket(tcpPacket);
			counter.updateClientCounter(tcpPacket);
		} else {
			clearBufferedSCDataPacket();
			serverInputStream.finish(false);
			updateConnectionStateAfterReceivedFinishPacket(tcpPacket);
			counter.updateServerCounter(tcpPacket);
		}
		validatePacketsInBuffer();
	}

	private void validatePacketsInBuffer() throws IOException {
		// verify there is no left data packet in the cache
		if (clientInputStream.isFinished() && csTemporaryStoredDataPackets.size() > 0) {
			throw new IOException("There are stil " + csTemporaryStoredDataPackets.size()
					+ " client to server packets that haven't been consumed!");
		}
		if (serverInputStream.isFinished() && scTemporaryStoredDataPackets.size() > 0) {
			logger.warn("There are stil " + scTemporaryStoredDataPackets.size()
					+ " server to client packets that haven't been consumed!");
		}
	}

	/**
	 * According to tcp protocol, if any side sends out rst packet, then these
	 * tcp connection will be closed, and all those data in buffer will also be
	 * thrown away.
	 */
	protected void processRstPacket(TCPPacket rstPacket) {
		if (isSentByClient(rstPacket)) {
			counter.updateClientCounter(rstPacket);
		} else {
			counter.updateServerCounter(rstPacket);
		}
		if (!clientInputStream.isFinished()) {
			clientInputStream.finish(false);
		}
		if (!serverInputStream.isFinished()) {
			serverInputStream.finish(false);
		}
		stateListener.onClosed(this);
	}

	private boolean updateConnectionStateAfterReceivedFinishPacket(TCPPacket tcpPacket) {
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

	protected boolean isOldPacket(TCPPacket packet) {
		boolean isOld = false;
		if (isSentByClient(packet, new DisableSequenceNumberCheckingHandler())) {
			isOld = packet.getSequence() < counter.clientSeq;
			if (isOld && logger.isInfoEnabled()) {
				logger.info("old packet seq " + packet.getSequence() + ", current counter client seq "
						+ counter.clientSeq + " captured at " + packet.getPacketCaptureTime());
			}
		} else {
			isOld = packet.getSequence() < counter.serverSeq;
			if (isOld && logger.isInfoEnabled()) {
				logger.info("old packet seq " + packet.getSequence() + ", current counter server seq "
						+ counter.serverSeq + " captured at " + packet.getPacketCaptureTime());
			}
		}
		return isOld;
	}

	/**
	 * close current connection, usually, it is closed by outside monitoring
	 * thread, such as TimeWait state to Closed state after some time
	 * 
	 * @throws IOException
	 *             exception when handling the data in buffer
	 */
	public void close() throws IOException {
		clearBufferedCSDataPacket();
		clearBufferedSCDataPacket();
		stateListener.onClosed(this);
		state = TCPConnectionState.Closed;
	}

	private class LoggingDismatchedSequenceNumberHandler implements DismatchedSequenceNumberHandler {
		@Override
		public boolean isEnableChecking() {
			return true;
		}

		@Override
		public void dismatchedSequenceNumber(boolean isSentByClient, TCPPacket packet) {
			if (logger.isInfoEnabled()) {
				if (isSentByClient) {
					logger.info(packet.toString() + " doesn't match client seq number " + state
							+ ", current client seq " + counter.clientSeq);
				} else {
					logger.info(packet.toString() + " doesn't match server seq number " + state
							+ ", current server seq " + counter.serverSeq);
				}
			}
		}

	}

	private class DisableSequenceNumberCheckingHandler implements DismatchedSequenceNumberHandler {
		@Override
		public boolean isEnableChecking() {
			return false;
		}

		@Override
		public void dismatchedSequenceNumber(boolean isSentByClient, TCPPacket packet) {
			throw new IllegalAccessError("disabled checking");
		}
	}

	private class StoreToBufferDismatchedSequenceNumberHandler extends LoggingDismatchedSequenceNumberHandler {
		@Override
		public boolean isEnableChecking() {
			return true;
		}

		@Override
		public void dismatchedSequenceNumber(boolean isSentByClient, TCPPacket packet) {
			super.dismatchedSequenceNumber(isSentByClient, packet);
			if (isSentByClient) {
				csTemporaryStoredDataPackets.add(packet);
			} else {
				scTemporaryStoredDataPackets.add(packet);
			}
			if (logger.isInfoEnabled())
				logger.info("added " + packet + " into buffer due to mismatching sequence number");
			/**
			 * need throw this exception to break the continue handling of this
			 * packet, just need add into buffer
			 */
			throw new DismatchedSequenceNumberException(isSentByClient);
		}

	}

	private class DismatchedSequenceNumberException extends RuntimeException {

		private static final long serialVersionUID = 7240762499196328405L;

		private boolean isSentByClient;

		DismatchedSequenceNumberException(boolean isSentByClient) {
			this.isSentByClient = isSentByClient;
		}
	}

	private class Counter {
		private long clientSeq;
		private long serverSeq;

		void updateClientCounter(TCPPacket packet) {
			clientSeq = packet.getSequence();
			if (packet.isContainsData()) {
				clientSeq += packet.getData().length;
			}
			packet.consumedPacket();
		}

		boolean isMatchClientSeq(TCPPacket packet) {
			checkWhetherPakcetSeqUpdatedToCounter(packet);
			return clientSeq == packet.getSequence();
		}

		private void checkWhetherPakcetSeqUpdatedToCounter(TCPPacket packet) {
			if (packet.isPacketConsumed()) {
				throw new RuntimeException(
						"This packet's sequence is already updated to current Counter, cannot do comparsion!");
			}
		}

		void updateServerCounter(TCPPacket packet) {
			serverSeq = packet.getSequence();
			if (packet.isContainsData()) {
				serverSeq += packet.getData().length;
			}
			packet.consumedPacket();
		}

		boolean isMatchServerSeq(TCPPacket packet) {
			checkWhetherPakcetSeqUpdatedToCounter(packet);
			return serverSeq == packet.getSequence();
		}

		@Override
		public String toString() {
			return "client seq:" + clientSeq + ", server seq:" + serverSeq;
		}
	}
}
