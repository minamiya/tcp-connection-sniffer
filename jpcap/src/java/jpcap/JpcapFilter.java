package jpcap;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to do a very simple filtering setting for packets, all of
 * those criteria are "and" operation, usually, if it is not a pppoe/vpn network
 * environment, suggest to use {@link JpcapCaptor#setFilter(String, boolean)},
 * which is much more powerful. This filter action is done at native level, so
 * it will be a bit faster than doing it in Java after packet instances are
 * created.
 */
public class JpcapFilter {
	private List<String> hosts = new ArrayList<String>();
	private List<String> srcHosts = new ArrayList<String>();
	private List<String> destHosts = new ArrayList<String>();
	private List<String> ports = new ArrayList<String>();
	private List<String> srcPorts = new ArrayList<String>();
	private List<String> destPorts = new ArrayList<String>();
	private List<Protocol> protocols = new ArrayList<Protocol>();

	/**
	 * get the host ip in the filter, either src host or dest host
	 * 
	 * @return host ip list
	 */
	public List<String> getHosts() {
		return hosts;
	}

	/**
	 * Add a host filter, it should be a ip, either src host or dest host
	 * matches the given host, then the packet will pass this filter
	 * 
	 * @param host
	 *            src host ip or dest host ip
	 */

	public void addHost(String host) {
		this.hosts.add(host);
	}

	/**
	 * get the host port in the filter, either src port or dest port
	 * 
	 * @return host port list
	 */
	public List<String> getPorts() {
		return ports;
	}

	/**
	 * Add a port filter, either src port or dest port matches the given port,
	 * then the packet will pass this filter
	 * 
	 * @param port
	 *            src port or dest port
	 */
	public void addPort(int port) {
		this.ports.add(port + "");
	}

	/**
	 * get the protocols in the filter
	 * 
	 * @return protocols in String format in the filer
	 */
	public List<String> getProtocols() {
		List<String> protocolStrs = new ArrayList<String>();
		for (Protocol p : protocols) {
			protocolStrs.add(p.name());
		}
		return protocolStrs;
	}

	/**
	 * Add a protocol filter, currently supports TCP,UDP,ICMP,ARP
	 * 
	 * @param protocol
	 *            protocol,should be either of TCP,UDP,ICMP,ARP
	 * 
	 * @see Protocol
	 */
	public void addProtocol(Protocol protocol) {
		this.protocols.add(protocol);
	}

	/**
	 * get the src host ip in the filter
	 * 
	 * @return src host ip list
	 */
	public List<String> getSrcHosts() {
		return srcHosts;
	}

	/**
	 * Add a host filter, it should be a src host ip
	 * 
	 * @param srcHost
	 *            src host ip
	 */
	public void addSrcHost(String srcHost) {
		this.srcHosts.add(srcHost);
	}

	/**
	 * get the dest host ip in the filter
	 * 
	 * @return dest host ip list
	 */
	public List<String> getDestHosts() {
		return destHosts;
	}

	/**
	 * Add a host filter, it should be a dest host ip
	 * 
	 * @param destHost
	 *            dest host ip
	 */
	public void addDestHost(String destHost) {
		this.destHosts.add(destHost);
	}

	/**
	 * get the src ports in the filter
	 * 
	 * @return src port list
	 */
	public List<String> getSrcPorts() {
		return srcPorts;
	}

	/**
	 * Add a port filter, it should be src port
	 * 
	 * @param srcPort
	 *            src port
	 */
	public void addSrcPort(int srcPort) {
		this.srcPorts.add(srcPort + "");
	}

	/**
	 * get the dest ports in the filter
	 * 
	 * @return dest port list
	 */
	public List<String> getDestPorts() {
		return destPorts;
	}

	/**
	 * Add a port filter, it should be dest port
	 * 
	 * @param destPort
	 *            dest port
	 */
	public void addDestPort(int destPort) {
		this.destPorts.add(destPort + "");
	}

	public enum Protocol {
		/**
		 * TCP packet
		 */
		TCP,
		/**
		 * UDP packet
		 */
		UDP,
		/**
		 * ICMP packet
		 */
		ICMP,
		/**
		 * ARP packet
		 */
		ARP
	}
	//Signature: (Ljava/lang/String;)I
	public int compareProtocol(String protocol){
		int match = 0;
		for(Protocol pro : this.protocols){
			if(pro.toString().equals(protocol.toString())){
				match = 1;
				break;
			}
		}
		return match;
	}
	//Signature: (Ljava/util/List;[B)I
	public int compareAddress(List<String> containedAddr, byte[] address){
		int match = 0;
		for(String addr:containedAddr){
			try {
				if(InetAddress.getByName(addr).equals(InetAddress.getByAddress(address))){
					match = 1;
					break;
				}
			} catch (UnknownHostException e) {
			}
		}
		return match;
	}
	// Signature: (Ljava/util/List;I)I
	public int comparePort(List<String> containdPorts, int port){
		int match = 0;
		for(String pt: containdPorts){
			if(pt.equals(port + "")){
				match = 1;
				break;
			}
		}
		return match;
	}
	
	
	
}


