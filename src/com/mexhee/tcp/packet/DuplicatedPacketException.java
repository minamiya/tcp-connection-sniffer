package com.mexhee.tcp.packet;

public class DuplicatedPacketException extends Exception {
	private static final long serialVersionUID = 1L;

	public DuplicatedPacketException(String message) {
		super(message);
	}
}
