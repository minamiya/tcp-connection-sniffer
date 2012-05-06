package com.mexhee.io;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class ChunkedInputStreamTest {

	@Test
	public void testChunkedInputStreamSupport() throws IOException {
		String str = "Hello World !!";
		DynamicByteArrayInputStream inputStream = prepareChunkedStream("Hello ", "World ", "!!");
		ChunkedInputStream chunkedStream = new ChunkedInputStream(inputStream);
		byte[] buffer = new byte[20];
		int len = chunkedStream.read(buffer);
		Assert.assertEquals(str.length(), len);
		Assert.assertEquals(str, new String(buffer, 0, len));
		chunkedStream.close();
		Assert.assertFalse(inputStream.hasMoreInputStream());
	}

	private DynamicByteArrayInputStream prepareChunkedStream(String... contents) throws IOException {
		DynamicByteArrayInputStream inputStream = new DynamicByteArrayInputStream();
		StringBuffer sb = new StringBuffer();
		for (String str : contents) {
			int len = str.length();
			sb.append(Integer.toHexString(len) + "\r\n");
			sb.append(str + "\r\n");
		}
		sb.append("0\r\n");
		inputStream.append(sb.toString().getBytes());
		return inputStream;
	}
}
