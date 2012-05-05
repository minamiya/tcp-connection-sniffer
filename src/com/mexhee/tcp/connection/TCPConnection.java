package com.mexhee.tcp.connection;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.mexhee.io.DynamicByteArrayInputStream;
import com.mexhee.io.TimeMeasurableCombinedInputStream;
import com.mexhee.tcp.connection.SequenceCounter.Counter;
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

	// to record client & server side sequence & ack number
	private SequenceCounter counter = new SequenceCounter();

	private TCPConnectionState state;

	private TCPConnectionStateListener stateListener;
	// indicate client or server send the FIN packet
	private boolean isClientRequestClosing = false;

	private DynamicByteArrayInputStream serverInputStream = new DynamicByteArrayInputStream();
	private DynamicByteArrayInputStream clientInputStream = new DynamicByteArrayInputStream();

	private Date lastUpdated = new Date();

	private boolean maybeBroken = false;

	private PacketsBuffer packetsBuffer = new PacketsBuffer(this);

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
		if (syncAckPacket.getAckNum() != counter.clientCounter.seq + 1) {
			throw new RuntimeException("sync packet ack number is incorrect!");
		}
		counter.updateServerCounter(syncAckPacket);
		counter.clientSequenceAddOne();
		state = TCPConnectionState.SynReceived;
		stateListener.onSynReceived(this);
	}

	/**
	 * get a {@link Counter} object that represents client and server side
	 * sequence and ack number
	 * 
	 * @return a {@link Counter} object that represents client and server side
	 *         sequence and ack number
	 */
	public SequenceCounter getSequenceNumCounter() {
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
	}

	private void processFinishWait1AckPacket(TCPPacket ackPacket) {
		if (ackPacket.isSentByClient()) {
			counter.updateClientCounter(ackPacket);
		} else {
			counter.updateServerCounter(ackPacket);
		}
		state = TCPConnectionState.CloseWait;
		stateListener.onCloseWait(this);
	}

	/**
	 * both side received FIN and sent ACK, then the connection is going to be
	 * CLOSED state
	 */
	private void processLastAckPacket(TCPPacket ackPacket) {
		if (ackPacket.isSentByClient()) {
			if (ackPacket.getAckNum() == counter.serverCounter.seq + 1) {
				logger.warn(connectionDetail.toString() + ":" + "incorrect seq number for last ACK");
				maybeBroken = true;
			}
			counter.updateClientCounter(ackPacket);
		} else {
			if (ackPacket.getAckNum() == counter.clientCounter.seq + 1) {
				logger.warn(connectionDetail.toString() + ":" + "incorrect seq number for last ACK");
				maybeBroken = true;
			}
			counter.updateServerCounter(ackPacket);
		}
		state = TCPConnectionState.Closed;
		stateListener.onClosed(this);
	}

	private void processHandshake3AckPacket(TCPPacket ackPacket) {
		if (ackPacket.getAckNum() != counter.serverCounter.seq + 1) {
			throw new RuntimeException("syn packet ack number " + ackPacket.getAckNum() + " is incorrect!");
		}
		if (logger.isInfoEnabled())
			logger.info("new tcp connection detected " + connectionDetail.toString());
		counter.updateClientCounter(ackPacket);
		// // server seq should be +1
		counter.serverSequenceAddOne();
		state = TCPConnectionState.Established;
		stateListener.onEstablished(this);
	}

	private void processDataAckPacket(TCPPacket ackPacket) throws IOException {
		// do nothing
	}

	/**
	 * check whether the packet sequence number is the same as the counter in
	 * current instance
	 */
	private boolean isMatchSequence(TCPPacket packet) {
		boolean isSentByClient = connectionDetail.isTheSame(packet.getConnectionDetail());
		if (isSentByClient) {
			return counter.isMatchClientSeq(packet);
		} else {
			return counter.isMatchServerSeq(packet);
		}
	}

	/**
	 * whether the packet can be handled in current stage according to those
	 * sequence number, ack number and those connection state
	 */
	boolean isCanProcessPacket(TCPPacket tcpPacket) {
		/**
		 * A least, all those packets should be match the sequence number, then
		 * it could be processed
		 */
		boolean canProcess = isMatchSequence(tcpPacket);

		if (canProcess) {
			if (tcpPacket.isContainsData() || tcpPacket.isRest() || tcpPacket.isFinish()) {
				if (tcpPacket.isSentByClient()) {
					/**
					 * <pre>
					 * 1. if ack number is not the same, usually it means there  is a server to client writing packet, so cannot process
					 * 2. but if it is just the writing direction changing, then, the ack number won't be the same, 
					 * 	  but the ack should be the same as the other side's seq
					 * </pre>
					 */
					return (tcpPacket.getAckNum() == counter.clientCounter.ack || tcpPacket.getAckNum() == counter.serverCounter.seq);
				} else {
					/**
					 * <pre>
					 * 1. if ack number is not the same, usually it means there  is a client to server writing packet, so cannot process
					 * 2. but if it is just the writing direction changing, then, the ack number won't be the same, 
					 * 	  but the ack should be the same as the other side's seq
					 * </pre>
					 */
					return (tcpPacket.getAckNum() == counter.serverCounter.ack || tcpPacket.getAckNum() == counter.clientCounter.seq);
				}
			}
		}
		return canProcess;
	}

	protected void processDataPacket(TCPPacket dataPacket) throws IOException {
		if (dataPacket.isSentByClient()) {
			processCSDataPacket(dataPacket);
		} else {
			processSCDataPacket(dataPacket);
		}
	}

	/**
	 * process server to client data packet, append data into
	 * {@link #serverInputStream}
	 */
	private void processSCDataPacket(TCPPacket dataPacket) throws IOException {
		if (counter.clientCounter.ack == dataPacket.getSequence()) {
			if (clientInputStream.finish(true)) {
				clientInputStream.markStreamStartTime(new Date(counter.clientCounter.latestPacketUpdateTime));
			}
		}
		/**
		 * first packet
		 */
		if (counter.serverCounter.seq == 0) {
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
		if (counter.serverCounter.ack == dataPacket.getSequence()) {
			if (serverInputStream.finish(true)) {
				serverInputStream.markStreamStartTime(new Date(counter.serverCounter.latestPacketUpdateTime));
			}
		}
		/**
		 * first packet
		 */
		if (counter.clientCounter.seq == 0) {
			clientInputStream.markStreamStartTime(dataPacket.getPacketCaptureTime());
		}
		clientInputStream.append(dataPacket.getData());
		counter.updateClientCounter(dataPacket);
	}

	protected void processFinishPacket(TCPPacket tcpPacket) throws IOException {
		if (tcpPacket.isSentByClient()) {
			clientInputStream.finish(false);
			updateConnectionStateAfterReceivedFinishPacket(tcpPacket);
			counter.updateClientCounter(tcpPacket);
			counter.clientSequenceAddOne();
		} else {
			serverInputStream.finish(false);
			updateConnectionStateAfterReceivedFinishPacket(tcpPacket);
			counter.updateServerCounter(tcpPacket);
			counter.serverSequenceAddOne();
		}
		validateDataPacketsInBuffer();
	}

	private void validateDataPacketsInBuffer() {
		// verify there is no left data packet in the cache
		if (clientInputStream.isFinished() && packetsBuffer.isStillHaveDataPacketsInCSBuffer()) {
			logger.warn("There are stil some client to server data packets that haven't been consumed! total packets in buffer "
					+ packetsBuffer.csTemporaryStoredPackets.size());
			maybeBroken = true;
		}
		if (serverInputStream.isFinished() && packetsBuffer.isStillHaveDataPacketsInSCBuffer()) {
			logger.warn("There are stil some server to client data packets that haven't been consumed! total packets in buffer "
					+ packetsBuffer.scTemporaryStoredPackets.size());
			maybeBroken = true;
		}
	}

	/**
	 * According to tcp protocol, if any side sends out rst packet, then these
	 * tcp connection will be closed, and all those data in buffer will also be
	 * thrown away.
	 */
	protected void processRstPacket(TCPPacket rstPacket) {
		if (rstPacket.isSentByClient()) {
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
		validateDataPacketsInBuffer();
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
	 * used to update the {@link #lastUpdated} time from external
	 */
	void updated() {
		this.lastUpdated = new Date();
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

	PacketsBuffer getPacketsBuffer() {
		return this.packetsBuffer;
	}

	boolean isOldPacket(TCPPacket packet) {
		if (packet.isSentByClient()) {
			return packet.getSequence() < counter.clientCounter.seq;
		} else {
			return packet.getSequence() < counter.serverCounter.seq;
		}
	}

	/**
	 * close current connection, usually, it is closed by outside monitoring
	 * thread, such as TimeWait state to Closed state after some time
	 * 
	 * @throws IOException
	 *             exception when handling the data in buffer
	 */
	public void close() throws IOException {
		// clearBufferedCSDataPacket();
		// clearBufferedSCDataPacket();
		stateListener.onClosed(this);
		state = TCPConnectionState.Closed;
	}
}
