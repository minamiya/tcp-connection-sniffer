package jpcap.packet;

/** This class represents a pppoe packet. */
public class PPPOEPacket extends EthernetPacket{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9105268385272644734L;

	/** version, 4 bit length */
	public byte version;
	
	/** type, 4 bit length*/
	public byte type;
	
	/** code, 8 bit length*/
	public byte code;
	
	/** session id, 2 byte length*/
	public byte[] sessionId;
	
	/** payload length, 2 byte length*/
	public short payloadLen;

	/**
	 * Creates a PPPOEPacket packet. 
	 * @param version version
	 * @param type type
	 * @param code code
	 * @param sessionId session id
	 * @param payloadLen payload length
	 */
	public PPPOEPacket(byte version, byte type, byte code, byte[] sessionId, short payloadLen) {
		this.version = version;
		this.type = type;
		this.code = code;
		this.sessionId = sessionId;
		this.payloadLen = payloadLen;
	}
	
}
