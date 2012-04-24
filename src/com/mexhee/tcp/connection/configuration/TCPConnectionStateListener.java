package com.mexhee.tcp.connection.configuration;

import com.mexhee.tcp.connection.ObsoleteConnectionCleaner;
import com.mexhee.tcp.connection.TCPConnection;

/* 
 * September 1981                                                          
 Transmission Control Protocol
 Functional Specification

 +---------+ ---------\      active OPEN  
 |  CLOSED |            \    -----------  
 +---------+<---------\   \   create TCB  
 |     ^              \   \  snd SYN    
 passive OPEN |     |   CLOSE        \   \           
 ------------ |     | ----------       \   \         
 create TCB  |     | delete TCB         \   \       
 V     |                      \   \     
 +---------+            CLOSE    |    \   
 |  LISTEN |          ---------- |     |  
 +---------+          delete TCB |     |  
 rcv SYN      |     |     SEND              |     |  
 -----------   |     |    -------            |     V  
 +---------+      snd SYN,ACK  /       \   snd SYN          +---------+
 |         |<-----------------           ------------------>|         |
 |   SYN   |                    rcv SYN                     |   SYN   |
 |   RCVD  |<-----------------------------------------------|   SENT  |
 |         |                    snd ACK                     |         |
 |         |------------------           -------------------|         |
 +---------+   rcv ACK of SYN  \       /  rcv SYN,ACK       +---------+
 |           --------------   |     |   -----------                  
 |                  x         |     |     snd ACK                    
 |                            V     V                                
 |  CLOSE                   +---------+                              
 | -------                  |  ESTAB  |                              
 | snd FIN                  +---------+                              
 |                   CLOSE    |     |    rcv FIN                     
 V                  -------   |     |    -------                     
 +---------+          snd FIN  /       \   snd ACK          +---------+
 |  FIN    |<-----------------           ------------------>|  CLOSE  |
 | WAIT-1  |------------------                              |   WAIT  |
 +---------+          rcv FIN  \                            +---------+
 | rcv ACK of FIN   -------   |                            CLOSE  |  
 | --------------   snd ACK   |                           ------- |  
 V        x                   V                           snd FIN V  
 +---------+                  +---------+                   +---------+
 |FINWAIT-2|                  | CLOSING |                   | LAST-ACK|
 +---------+                  +---------+                   +---------+
 |                rcv ACK of FIN |                 rcv ACK of FIN |  
 |  rcv FIN       -------------- |    Timeout=2MSL -------------- |  
 |  -------              x       V    ------------        x       V  
 \ snd ACK                 +---------+delete TCB         +---------+
 ------------------------>|TIME WAIT|------------------>| CLOSED  |
 +---------+                   +---------+
 */
//please refer to RCF793
public interface TCPConnectionStateListener {

	public void onSynSent(final TCPConnection connection);

	public void onSynReceived(final TCPConnection connection);

	public void onEstablished(final TCPConnection connection);

	public void onFinishWait1(final TCPConnection connection);

	public void onFinishWait2(final TCPConnection connection);

	public void onCloseWait(final TCPConnection connection);

	public void onLastAck(final TCPConnection connection);

	public void onClosing(final TCPConnection connection);

	public void onTimeWait(final TCPConnection connection);

	public void onClosed(final TCPConnection connection);

	/**
	 * this is not the standard tcp state, but a exception state detected by tcp
	 * connection sniffer, if there is a long time (
	 * {@link ObsoleteConnectionCleaner#TCP_CONNECTION_TIME_OUT_SEC}) no any
	 * packets transfered in this connection, then tcp connection sniffer will
	 * make this connection as timeouted and invoke this listener, at last,
	 * distory this connection instance
	 */
	public void onTimeoutDetected(final TCPConnection connection);
}
