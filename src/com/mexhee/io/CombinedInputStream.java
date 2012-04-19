package com.mexhee.io;

import java.io.InputStream;

/**
 * Combining one or more than one input stream into this object, it provides a
 * way to scan those input streams one by one.
 * 
 * @see com.mexhee.io.CombinedInputStream#hasMoreInputStream()
 * 
 */
public abstract class CombinedInputStream extends InputStream {

	/**
	 * configure whether this stream is blocking model.
	 * 
	 * @param blocking
	 *            if true, current thread will be blocked until new data arrived
	 *            or current stream encountered EOF, or the whole stream is
	 *            finished, otherwise, return directly.
	 */
	public abstract void configureBlocking(boolean blocking);

	/**
	 * assert current cursor is referring to a new stream
	 * beginning,java.lang.AssertionError will be thrown if assertion fails.
	 * 
	 * @throws java.lang.AssertionError
	 */
	public abstract void assertNewInputStream();

	/**
	 * have a look whether there are more input stream in current object.
	 * 
	 * @return boolean
	 */
	public abstract boolean hasMoreInputStream();

	/**
	 * skip all the left data in current stream, jump to the beginning of next
	 * input stream directly if there is.
	 */
	public abstract void skipCurrentInputStream();
}
