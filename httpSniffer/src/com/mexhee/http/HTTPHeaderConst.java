package com.mexhee.http;

import java.util.Arrays;
import java.util.List;

public class HTTPHeaderConst {
	/**
	 * CRLF.
	 */
	public static final String CRLF = "\r\n";

	/**
	 * CR.
	 */
	public static final byte CR = (byte) '\r';

	/**
	 * LF.
	 */
	public static final byte LF = (byte) '\n';

	/**
	 * space.
	 */
	public static final byte SP = (byte) ' ';

	/**
	 * HT.
	 */
	public static final byte HT = (byte) '\t';

	/**
	 * COLON.
	 */
	public static final byte COLON = (byte) ':';

	/**
	 * http request header actions.
	 */
	public static final List<String> REQUEST_ACTIONS = Arrays.asList(new String[] { "GET", "POST", "HEAD", "PUT",
			"DELETE", "TRACE", "OPTIONS" });
}
