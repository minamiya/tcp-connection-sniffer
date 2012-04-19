package com.mexhee.io;

import java.io.IOException;

/**
 * This exception will be thrown when the buffer in {@link DynamicByteArrayInputStream}
 * is full.
 * 
 */
public class BufferFullException extends IOException {

	private static final long serialVersionUID = -1280023656779605921L;

	public BufferFullException(String message) {
		super(message);
	}
}
