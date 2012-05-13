package com.mexhee.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Put a fix length stream content into random access file, provide a simple
 * store and retrieve feature
 * 
 */
public class FixLengthStreamFileCache implements Serializable {

	private static final long serialVersionUID = 8393250212216603174L;
	/*
	 * it has 20k header, after that is the data content, so the max content
	 * items count should be 20*1024
	 */
	private transient RandomAccessFile tempFile;
	private Map<String, Cursor> dataCursors = new HashMap<String, Cursor>(2000);

	private long headerLen = 20 * 1024;

	private long cacheFileLen = headerLen;

	public FixLengthStreamFileCache(String folder) throws IOException {
		final String filename = folder + File.separator + "mexhee-stream.cache";
		tempFile = new RandomAccessFile(filename, "rw");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					tempFile.close();
					new File(filename).delete();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public synchronized String allocate(int size) throws IOException {
		Cursor newAllocation = new Cursor(cacheFileLen, size);
		String uuid = UUID.randomUUID().toString();
		dataCursors.put(uuid, newAllocation);
		tempFile.writeInt(size);
		cacheFileLen += 4;
		return uuid;
	}

	public synchronized void write(String uuid, byte[] data) throws IOException {
		if (data == null || data.length == 0) {
			return;
		}
		Cursor cursor = getCursor(uuid);
		tempFile.seek(cursor.writePos);
		tempFile.write(data);
		int len = data.length;
		cursor.moveWriteForward(len);
		cacheFileLen += len;
	}

	public synchronized int read(String uuid, byte[] data) throws IOException {
		Cursor cursor = getCursor(uuid);
		tempFile.seek(cursor.startPos);
		int contentLen = tempFile.readInt();
		if (contentLen != cursor.dataLen) {
			throw new IOException("data may be broken");
		}
		int readLen = data.length;
		if (cursor.readPos + readLen >= cursor.getEndPos()) {
			readLen = (int) (cursor.getEndPos() - cursor.readPos);
			if (readLen <= 0) {
				return 0;
			}
		}
		cursor.moveReadForward(readLen);
		return tempFile.read(data, 0, readLen);
	}

	public int getDataLength(String uuid) throws IOException {
		Cursor cursor = getCursor(uuid);
		return cursor.dataLen;
	}

	public boolean isContentFull(String uuid) throws IOException {
		return getCursor(uuid).isFull();
	}

	private Cursor getCursor(String uuid) throws IOException {
		Cursor cursor = dataCursors.get(uuid);
		if (cursor == null) {
			throw new IOException("incorrect uuid, has not allocate space.");
		}
		return cursor;
	}

	private class Cursor implements Serializable {

		private static final long serialVersionUID = -59030187445465549L;

		long startPos;
		int dataLen;
		long writePos;
		long readPos;

		Cursor(long startPos, int dataLen) {
			this.startPos = startPos;
			this.dataLen = dataLen;
			this.writePos = startPos + 4;
			this.readPos = startPos + 4;
		}

		long moveReadForward(int len) throws IOException {
			/*
			 * exceed the allocated range
			 */
			if (getEndPos() < readPos + len) {
				throw new IOException("exceed the allocated range");
			}
			readPos += len;
			return readPos;
		}

		long moveWriteForward(int len) throws IOException {
			/*
			 * exceed the allocated range
			 */
			if (getEndPos() < writePos + len) {
				throw new IOException("exceed the allocated range");
			}
			writePos += len;
			return writePos;
		}

		/*
		 * 4 byte int to indicate the data content length
		 */
		long getEndPos() {
			return startPos + dataLen + 4;
		}

		boolean isFull() {
			return writePos >= startPos + dataLen + 2;
		}
	}
}
