#define ETHERTYPE_PUP           0x0200  /* PUP protocol */
#define ETHERTYPE_IP            0x0800  /* IP protocol */
#define ETHERTYPE_IP_PACKET     0x0021  /* IP Packet */
#define ETHERTYPE_PPPOE         0x8864  /* PPPOE protocol */
#define ETHERTYPE_ARP           0x0806  /* Addr. resolution protocol */
#define ETHERTYPE_REVARP        0x8035  /* reverse Addr. resolution protocol */
#define ETHERTYPE_VLAN          0x8100  /* IEEE 802.1Q VLAN tagging */
#define ETHERTYPE_IPV6          0x86dd  /* IPv6 */
#define ETHERTYPE_LOOPBACK      0x9000  /* used to test interfaces */
#define ETHERTYPE_VERSION       0xf0
#define ETHERTYPE_TYPE			0x0f

struct ether_header {
  jbyte ether_dest[6],ether_src[6];
  jchar ether_type;
};

struct pppoe_header{
	struct ether_header m_ether_header;
	u_char ver_type;
	jbyte code;
	jbyte session_id[2];
	jshort pay_load_len;
};
