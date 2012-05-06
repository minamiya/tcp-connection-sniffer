package com.mexhee.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Use byte array to store temporary data, it supports appending data into
 * stream dynamically, also supports mark stream EOF, and beginning to generate
 * next new input stream.
 * 
 * A sample:
 * 
 * <pre>
 * DynamicByteArrayInputStream stream = new DynamicByteArrayInputStream();
 * String string = &quot;Hello&quot;;
 * stream.append(string.getBytes());
 * stream.finish(true);
 * stream.append(&quot;World&quot;.getBytes());
 * stream.finish(true);
 * stream.append(&quot;!!&quot;.getBytes());
 * stream.finish(false);
 * while (stream.hasMoreInputStream()) {
 * 	byte[] buffer = new byte[50];
 * 	int size = 0;
 * 	while ((size = stream.read(buffer)) &gt; 0) {
 * 		System.out.print(new String(buffer, 0, size));
 * 	}
 * }
 * </pre>
 * 
 */
public class DynamicByteArrayInputStream extends TimeMeasurableCombinedInputStream {

	private byte[] buf;
	// current stream cursor position
	private int pos = 0;
	// current stream length
	private int count = 0;
	// total buffer size
	private int bufferSize = 0;
	private boolean isFinished = false;
	private boolean blocking = true;

	private Date currentStreamStartTime = new Date();

	private List<StreamMark> newInputStreamMarks = new ArrayList<StreamMark>();

	private final static int READ_TIMEOUT = 3000;
	private final static int MAX_BUFFER_SIZE = 2000 * 1024;

	/*
	 * used to support mark & rest action in input stream, if current read
	 * exceed readLimit, then we needn't keep the buffer
	 */
	private int readLimit = -1;
	private int markedPos = -1;

	/**
	 * initialize with data in buff
	 * 
	 * @param buff
	 *            create a new stream, and add those data in buff into current
	 *            stream
	 * @param startTime
	 *            the first stream start time
	 */
	public DynamicByteArrayInputStream(byte[] buff, Date startTime) {
		this.buf = buff;
		this.count = this.bufferSize = buff.length;
		this.currentStreamStartTime = startTime;
	}

	/**
	 * initialize instance
	 * 
	 * @param startTime
	 *            the first stream start time
	 */
	public DynamicByteArrayInputStream(Date startTime) {
		this.currentStreamStartTime = startTime;
	}

	/**
	 * initialize instance
	 * 
	 */
	public DynamicByteArrayInputStream() {
	}

	private boolean addMarkPos(Date endTime) {
		/**
		 * to avoid duplicated marking
		 */
		if (bufferSize > 0) {
			/**
			 * avoid duplicating mark
			 */
			if (!newInputStreamMarks.isEmpty()
					&& bufferSize == newInputStreamMarks.get(newInputStreamMarks.size() - 1).endPos) {
				return false;
			}
			newInputStreamMarks.add(new StreamMark(currentStreamStartTime, endTime, bufferSize));
			return true;
		}
		return false;
	}

	/**
	 * add EOF to the last stream
	 * 
	 * @param markFinish
	 *            if <code>true</code>, just mark the end of last stream, when
	 *            appending new data through {@link #append(byte[])} , those
	 *            data will be add into a new stream, and
	 *            {@link #hasMoreInputStream()} will be true, and there will be
	 *            one more stream in the buffer. If <code>false</code>, closing
	 *            this object, set stream into finish state, and cannot append
	 *            new data any more, if using {@link #append(byte[])} to add a
	 *            new data, AlreadyFinishedStreamException will be thrown.
	 * @param endTime
	 *            the marking end stream end time.
	 */
	public synchronized boolean finish(boolean markFinish, Date endTime) {
		boolean success = addMarkPos(endTime);
		if (!markFinish) {
			this.isFinished = true;
		}
		if (blocking) {
			this.notifyAll();
		}
		return success;
	}

	/**
	 * Use current date time as the marking end stream's end time.
	 * 
	 * @return whether add mark successfully, duplicating adding will lead to a
	 *         skip
	 * @see #finish(boolean, Date)
	 */
	public synchronized boolean finish(boolean markFinish) {
		return finish(markFinish, new Date());
	}

	/**
	 * have a look whether this stream has been finished, call
	 * <code>finish(false)</code> will finish this stream.
	 * 
	 * @return boolean
	 */
	public synchronized boolean isFinished() {
		return this.isFinished;
	}

	/**
	 * clear all data in this stream, clean buffer, set cursor to 0, and set
	 * isFinished flag to false
	 */
	public synchronized void closeWholeStream() {
		this.buf = null;
		this.pos = 0;
		this.count = 0;
		this.isFinished = false;
		this.newInputStreamMarks.clear();
		this.bufferSize = 0;
	}

	/**
	 * close the current input stream, it is the same behavior with
	 * {@link #finishCurrentInputStream()}
	 */
	@Override
	public void close() {
		finishCurrentInputStream();
	}

	@Override
	public synchronized void reset() throws IOException {
		if (this.markedPos == -1) {
			throw new IOException("cannot reset due to the connect has been finished or no marker.");
		}
		this.pos = this.markedPos;
		this.markedPos = -1;
	}

	@Override
	public synchronized void mark(int readlimit) {
		if (readlimit > 0) {
			this.readLimit = readlimit;
			this.markedPos = this.pos;
		}
	}

	@Override
	public void configureBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	/**
	 * return max buffer size
	 * 
	 * @return int max buffer size
	 */
	public int capacity() {
		return MAX_BUFFER_SIZE;
	}

	@Override
	public synchronized void assertNewInputStream() {
		if (this.pos != 0) {
			throw new AssertionError("Not a new inputstream");
		}
	}

	/**
	 * append data into buffer
	 * 
	 * @param newBytes
	 *            data to be added into buffer
	 * @throws AlreadyFinishedStreamExceptionF
	 *             if {@link #isFinished} is true BufferFullException if adding
	 *             this data into buffer will exceed {@link #capacity()} size
	 */
	public synchronized void append(byte[] newBytes) throws AlreadyFinishedStreamException, BufferFullException {
		if (isFinished) {
			throw new AlreadyFinishedStreamException("stream is already finished!");
		}
		if (markedPos >= 0 && pos - markedPos <= readLimit) {
			appendOnly(newBytes);
		} else {
			appendAndShrink(newBytes);
		}
		this.notifyAll();
	}

	/*
	 * due to support mark in current stream, cannot clear the buffer, so just
	 * append it
	 */
	private void appendOnly(byte[] newBytes) throws BufferFullException {
		int newSize = this.bufferSize + newBytes.length;
		if (newSize > MAX_BUFFER_SIZE) {
			throw new BufferFullException(this.toString() + " is full, capacity is " + (MAX_BUFFER_SIZE / 1024) + "k");
		}
		byte[] b = new byte[newSize];
		if (bufferSize > 0) {
			System.arraycopy(buf, 0, b, 0, bufferSize);
		}
		System.arraycopy(newBytes, 0, b, bufferSize, newBytes.length);
		this.buf = b;
		this.bufferSize = newSize;
		if (this.count == 0 || this.newInputStreamMarks.size() == 0) {
			this.count = this.bufferSize;
		}
	}

	/*
	 * append buffer and shrink the buffer
	 */
	private void appendAndShrink(byte[] newBytes) throws BufferFullException {
		int available = this.bufferSize - this.pos;
		int newSize = available + newBytes.length;
		if (newSize > MAX_BUFFER_SIZE) {
			throw new BufferFullException(this.toString() + " is full, capacity is " + (MAX_BUFFER_SIZE / 1024) + "k");
		}
		/*
		 * removed pos bytes data, so the count and newInputStreamMarks should
		 * be minus those pos, and the new value should be greater than 0
		 */
		this.count -= this.pos;
		moveForwardMarks(pos);
		if (available > 0) {
			byte[] b = new byte[newSize];
			System.arraycopy(buf, pos, b, 0, available);
			System.arraycopy(newBytes, 0, b, available, newBytes.length);
			this.buf = null;
			this.buf = b;
		} else {
			this.buf = null;
			this.buf = newBytes;
		}
		this.pos = 0;
		this.bufferSize = this.buf.length;
		if (this.count == 0 || this.newInputStreamMarks.size() == 0) {
			this.count = this.bufferSize;
		}
	}

	private void moveForwardMarks(int pos) {
		for (int i = 0; i < newInputStreamMarks.size(); i++) {
			newInputStreamMarks.set(i, newInputStreamMarks.get(i).moveEndPosForward(pos));
		}
		if (!newInputStreamMarks.isEmpty()
				&& newInputStreamMarks.get(newInputStreamMarks.size() - 1).getEndPos() > this.bufferSize) {
			throw new IndexOutOfBoundsException();
		}
	}

	private boolean isCurrentStreamFinished() {
		if (newInputStreamMarks.size() > 0 && this.pos >= newInputStreamMarks.get(0).getEndPos()) {
			shrinkToNextInputStream();
			return true;
		}
		if (newInputStreamMarks.size() == 0 && isFinished()) {
			return true;
		}
		return false;
	}

	/**
	 * read one byte data
	 * 
	 * @see java.io.InputStream#read()
	 */
	@Override
	public synchronized int read() throws IOException {
		if (pos < count) {
			return (buf[pos++] & 0xff);
		} else if (!isCurrentStreamFinished() && blocking) {
			try {
				long start = System.currentTimeMillis();
				this.wait(READ_TIMEOUT);
				if ((System.currentTimeMillis() - start) >= READ_TIMEOUT) {
					throw new RuntimeException("Read timeout");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return read();
		} else {
			return -1;
		}
	}

	@Override
	public synchronized boolean hasMoreInputStream() {
		return this.newInputStreamMarks.size() > 0 || this.pos < this.bufferSize;
	}

	@Override
	public synchronized void finishCurrentInputStream() {
		this.pos = this.count;
		shrinkToNextInputStream();
	}

	private synchronized void shrinkToNextInputStream() {
		if (newInputStreamMarks.isEmpty()) {
			return;
		}
		int newSize = this.bufferSize - this.pos;
		byte[] b = new byte[newSize];
		System.arraycopy(buf, pos, b, 0, newSize);
		this.buf = null;
		this.buf = b;
		this.bufferSize = this.buf.length;
		newInputStreamMarks.remove(0);
		moveForwardMarks(pos);
		this.count = this.newInputStreamMarks.size() > 0 ? this.newInputStreamMarks.get(0).getEndPos()
				: this.bufferSize;
		this.pos = 0;
		this.markedPos = -1;
	}

	/**
	 * read data into given buffer
	 * 
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public synchronized int read(byte b[], int off, int len) {
		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		}
		if (pos >= count) {
			if (!isCurrentStreamFinished() && blocking) {
				try {
					long start = System.currentTimeMillis();
					this.wait(READ_TIMEOUT);
					if ((System.currentTimeMillis() - start) >= READ_TIMEOUT) {
						throw new RuntimeException("Read timeout");
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return read(b, off, len);
			} else {
				return -1;
			}
		}
		if (pos + len > count) {
			len = count - pos;
		}
		if (len <= 0) {
			return 0;
		}
		System.arraycopy(buf, pos, b, off, len);
		pos += len;
		return len;
	}

	/**
	 * whether is the mark supported, it is true
	 */
	@Override
	public boolean markSupported() {
		return true;
	}

	/**
	 * skip n byte data in <b>current</b> stream, not all data in buffer
	 * 
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public synchronized long skip(long n) {
		if (pos + n > count) {
			n = count - pos;
		}
		if (n < 0) {
			return 0;
		}
		pos += n;
		return n;
	}

	public void markStreamStartTime(Date startTime) {
		this.currentStreamStartTime = startTime;
	}

	@Override
	public Date getCurrentInputStreamStartTime() {
		/*
		 * if there is a marker, then it means current reading/operating stream
		 * should be in the marker buffer
		 */
		if (newInputStreamMarks.size() > 0) {
			return newInputStreamMarks.get(0).getStartTime();
		}
		return currentStreamStartTime;
	}

	@Override
	public Date getCurrentInputStreamEndTime() {
		if (newInputStreamMarks.size() > 0) {
			return newInputStreamMarks.get(0).getEndTime();
		}
		return null;
	}

	class StreamMark {
		private Date startTime;
		private Date endTime;
		private int endPos;

		StreamMark(Date startTime, Date endTime, int endPos) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.endPos = endPos;
		}

		StreamMark moveEndPosForward(int delta) {
			endPos -= delta;
			return this;
		}

		int getEndPos() {
			return endPos;
		}

		void markEndPos(int endPos, Date endTime) {
			this.endPos = endPos;
			this.endTime = endTime;
		}

		Date getStartTime() {
			return startTime;
		}

		Date getEndTime() {
			return endTime;
		}
	}
}
