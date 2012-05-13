package com.mexhee.io;

import java.io.IOException;

/**
 * This exception will be thrown when trying to append data into
 * {@link DynamicByteArrayInputStream} when it is already finished
 * 
 */
public class AlreadyFinishedStreamException extends IOException {

	private static final long serialVersionUID = -3047725335716058817L;

	public AlreadyFinishedStreamException(String message) {
		super(message);
	}
}
