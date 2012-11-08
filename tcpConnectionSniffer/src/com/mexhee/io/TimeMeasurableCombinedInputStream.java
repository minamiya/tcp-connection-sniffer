package com.mexhee.io;

import java.io.InputStream;
import java.util.Date;

/**
 * Combining one or more than one input stream into this object, it provides a
 * way to scan those input streams one by one, also providing a timing feature,
 * as those data are buffered, it also provide a way to record when the current
 * stream is started to put into buffer, and when current stream is finished.
 * 
 * @see #hasMoreInputStream()
 * @see #getCurrentInputStreamStartTime()
 */
public abstract class TimeMeasurableCombinedInputStream extends InputStream {

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
	 * have a look whether there are more input stream in current buffer.
	 * 
	 * @return boolean
	 */
	public abstract boolean hasMoreInputStream();
	
	/**
	 * 
	 */
	public abstract boolean isFinished();

	/**
	 * mark finish current stream and skip all the left data in current stream
	 * if there is , also release the buffer for current stream, at last, jump
	 * to the beginning of next input stream directly if there is.
	 */
	public abstract void finishCurrentInputStream();

	/**
	 * get the time when the current input stream is beginning
	 * 
	 * @return Date
	 */
	public abstract Date getCurrentInputStreamStartTime();

	/**
	 * get the time when the current input stream is ended, return null when
	 * current input stream has not been ended
	 * 
	 * @return Date
	 */

	public abstract Date getCurrentInputStreamEndTime();
}
