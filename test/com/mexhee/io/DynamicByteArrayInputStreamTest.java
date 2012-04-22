package com.mexhee.io;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DynamicByteArrayInputStreamTest {

	private StringBuffer sb = null;

	@Before
	public void before() {
		sb = new StringBuffer();
	}

	@After
	public void after() {
		sb = null;
	}

	@Test
	public void testEmptyStream() throws IOException {
		DynamicByteArrayInputStream stream = new DynamicByteArrayInputStream();
		stream.finish(false);
		stream.read();
		stream = new DynamicByteArrayInputStream(new byte[0], new Date());
		stream.finish(false);
		stream.read();
	}

	@Test
	public void testReadEOFFlagWillRollToNextStreamAutomatically() throws Exception {
		DynamicByteArrayInputStream stream = createInputStreamWithoutFinishedFlag();
		printContent("Hello", stream);
		stream.read();
		stream.assertNewInputStream();
		printContent("World", stream);
		stream.read();
		stream.assertNewInputStream();
		printContent("!!", stream);
		stream.read();
		stream.append("\n".getBytes());
		stream.append("My name is beam".getBytes());
		printContent("\nMy name is beam", stream);
		stream.reset();
		
		Assert.assertEquals("HelloWorld!!\nMy name is beam", sb.toString());
	}

	@Test
	public void testSkipCurrentInputStreamWillRollToNextStreamAutomatically() throws Exception {
		DynamicByteArrayInputStream stream = createInputStreamWithoutFinishedFlag();
		printContent("Hello", stream);
		stream.skipCurrentInputStream();
		stream.assertNewInputStream();
		printContent("World", stream);
		stream.skipCurrentInputStream();
		stream.assertNewInputStream();
		printContent("!!", stream);
		stream.skipCurrentInputStream();
		stream.append("\n".getBytes());
		stream.append("My name is beam".getBytes());
		printContent("\nMy name is beam", stream);
		stream.reset();
		
		Assert.assertEquals("HelloWorld!!\nMy name is beam", sb.toString());
	}

	@Test
	public void testSetFinishFlagWillFinishStreamAutomatically() throws Exception {
		TimeMeasurableCombinedInputStream stream = createInputStreamWithFinishedFlag();
		while (stream.hasMoreInputStream()) {
			byte[] buffer = new byte[10];
			int size = 0;
			while ((size = stream.read(buffer)) > 0) {
				print(new String(buffer, 0, size));
			}
		}
		Assert.assertEquals("FFFF!!", sb.toString());
	}

	@Test
	public void testFinishStreamTwiceShouldBeFine() throws Exception {
		DynamicByteArrayInputStream stream = createInputStreamWithoutFinishedFlag();
		stream.finish(false);
		while (stream.hasMoreInputStream()) {
			byte[] buffer = new byte[10];
			int size = 0;
			while ((size = stream.read(buffer)) > 0) {
				print(new String(buffer, 0, size));
			}
		}
		Assert.assertEquals("HelloWorld!!", sb.toString());
	}

	private void printContent(String content, TimeMeasurableCombinedInputStream stream) throws IOException {
		byte[] buf = new byte[100];
		int len = stream.read(buf);
		if (len != content.length()) {
			throw new RuntimeException("Failed");
		}
		Assert.assertEquals(content, new String(buf, 0, len));
		print(new String(buf, 0, len));
	}

	private void print(String string) {
		sb.append(string);
	}

	private DynamicByteArrayInputStream createInputStreamWithoutFinishedFlag() throws Exception {
		DynamicByteArrayInputStream stream = new DynamicByteArrayInputStream();
		String string = "Hello";
		stream.append(string.getBytes());
		stream.finish(true);
		stream.append("World".getBytes());
		stream.finish(true);
		stream.append("!!".getBytes());
		stream.finish(true);
		return stream;
	}

	private DynamicByteArrayInputStream createInputStreamWithFinishedFlag() throws Exception {
		DynamicByteArrayInputStream stream = new DynamicByteArrayInputStream();
		String string = "FF";
		stream.append(string.getBytes());
		stream.finish(true);
		stream.append("FF".getBytes());
		stream.finish(true);
		stream.append("!!".getBytes());
		stream.finish(false);
		return stream;
	}
}
