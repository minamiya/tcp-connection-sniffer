package com.mexhee.tcp.connection;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GlobalThreadPool {
	
	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 100, 60, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(20), new ThreadPoolExecutor.CallerRunsPolicy());
	
}
