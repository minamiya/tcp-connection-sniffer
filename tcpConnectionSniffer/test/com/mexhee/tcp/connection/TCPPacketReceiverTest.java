package com.mexhee.tcp.connection;

import java.io.IOException;

import org.junit.Test;

import com.mexhee.tcp.connection.listener.ConnectionFilter;
import com.mexhee.tcp.connection.listener.DefaultTCPConnectionSnifferListener;
import com.mexhee.tcp.connection.listener.TCPConnectionSnifferListener;

public class TCPPacketReceiverTest {

	private String localIP = "192.168.1.100";
	private String serverIP = "128.195.54.92";

	@Test
	public void testATypicalHttpConnection() throws IOException {
		TCPConnectionSnifferListener snifferListener = new DefaultTCPConnectionSnifferListener(new ViewTCPConnection(),
				new ConnectionFilter());
		PacketReceiver receiver = new PacketReceiverImpl(snifferListener);
		shakehands(receiver, 4841, 8478);
		receiver.pick(defaultBuilder().localToServer().setSeqAckNum(4842, 8479).data("GET XXX/XX.html").ack().build());
		receiver.pick(defaultBuilder().serverToLocal().setSeqAckNum(8479, 5273).ack().build());
		receiver.pick(defaultBuilder().serverToLocal().setSeqAckNum(8479, 5273).data("Status 200OK HTTP 1.1\nXX.html")
				.build());
		receiver.pick(defaultBuilder().localToServer().setSeqAckNum(5273, 9467).data("GET XXX/XX2.html").ack().build());
		receiver.pick(defaultBuilder().serverToLocal().setSeqAckNum(9467, 5764).data("Status 200OK HTTP 1.1\nXX2.html")
				.ack().build());
		receiver.pick(defaultBuilder().localToServer().setSeqAckNum(5764, 0305).data("GET XXX/XX3.html").ack().build());
		receiver.pick(defaultBuilder().serverToLocal().setSeqAckNum(0305, 6334).data("Status 200OK HTTP 1.1\nXX3.html")
				.ack().fin().build());
		receiver.pick(defaultBuilder().localToServer().setSeqAckNum(6334, 0305).ack().fin().build());
		receiver.pick(defaultBuilder().serverToLocal().setSeqAckNum(0305, 198).ack().build());
	}

	private void shakehands(PacketReceiver receiver, long initLocalSeq, long initServerSeq) throws IOException {
		receiver.pick(defaultBuilder().localToServer().setSeqAckNum(initLocalSeq, 0).syn().build());
		receiver.pick(defaultBuilder().serverToLocal().setSeqAckNum(initServerSeq, initLocalSeq + 1).syn().ack()
				.build());
		receiver.pick(defaultBuilder().localToServer().setSeqAckNum(initLocalSeq + 1, initServerSeq + 1).ack().build());
	}

	private TCPPacketBuilder defaultBuilder() {
		return new TCPPacketBuilder(16278, 80, localIP, serverIP);
	}
}
