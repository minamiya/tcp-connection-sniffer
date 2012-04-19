Tcp connection sniffer is a java based project, using those tcp packets captured from Jpcap lib which is based on libcap/wincap, and then following tcp standard, to reconstruct those tcp packets into stream, to simulate/reproduce what the tcp stack is doing in kernel. 

It is just a sniffer, it cannot block nor change any packets/connection in the existing system. Currently, it only supports x86 jvm.

You should install wincap (http://www.winpcap.org/) in windows environment before starting to use tcp connection sniffer, or install libcap (http://www.tcpdump.org/) for other linux/unix environment.

As libcap and wincap doesn't support pppoe, so tcp connection sniffer cannot sniffer out those tcp connections at ADSL or vpn environment.

At jvm startup, you should add vm parameter to let jvm could find its native libary, here is a sample for the starting up parameter:
java -Djava.library.path=C:\tcp-connection-sniffer\lib com.mexhee.MyClass