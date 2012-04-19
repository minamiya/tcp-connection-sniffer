package com.mexhee.tcp.connection.configuration.impl;

import com.mexhee.tcp.connection.ConnectionDetail;
import com.mexhee.tcp.connection.configuration.ConnectionFilter;
import com.mexhee.tcp.connection.configuration.TCPConnectionStateListener;
import com.mexhee.tcp.connection.configuration.PacketListener;
import com.mexhee.tcp.connection.configuration.TCPConnectionConfiguration;

public class DefaultTCPConnectionConfiguration implements TCPConnectionConfiguration {

	private ConnectionFilter connectionFilter = new ConnectionFilter();
	private PacketListener packetListener;
	private TCPConnectionStateListener stateListener;

	@Override
	public ConnectionFilter getConnectionFilter() {
		return connectionFilter;
	}

	@Override
	public PacketListener getPacketListener() {
		if (packetListener == null) {
			packetListener = new PacketStatistics();
		}
		return packetListener;
	}

	public void setPacketListener(PacketListener packetListener) {
		this.packetListener = packetListener;
	}

	public void setStateListener(TCPConnectionStateListener stateListener) {
		this.stateListener = stateListener;
	}

	@Override
	public TCPConnectionStateListener getConnectionStateListener() {
		if (stateListener == null) {
			stateListener = new DefaultConnectionStateListener();
		}
		return stateListener;
	}

	@Override
	public boolean isAcceptable(ConnectionDetail connectionDetail) {
		return true;
	}
}
