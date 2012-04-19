package com.mexhee.tcp.packet;

public class LostPacketException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public LostPacketException(String message){
		super(message);
	}
}
