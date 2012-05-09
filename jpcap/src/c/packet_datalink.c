#include<jni.h>
#include<pcap.h>

#ifndef WIN32
#include<sys/param.h>
#define __FAVOR_BSD
#include<netinet/in.h>
//#include<net/bpf.h>
//#include<pcap-bpf.h>
#else
#include<winsock2.h>
#endif

#include<netinet/in_systm.h>
#include<netinet/ip.h>

#include"Jpcap_sub.h"
#include"Jpcap_ether.h"

/** analyze datalink layer (ethernet) **/
jobject analyze_datalink(JNIEnv *env,u_char *data,int linktype,int linktype_ext){
	struct ether_header *ether_hdr;
	struct pppoe_header *pppoe_hdr;
	jobject packet;
	jbyteArray src_addr,dst_addr,session_id;
	jbyte version,type,code;
	short pay_load_len;

#ifdef DEBUG
	puts("analyze datalink");
#endif

	switch(linktype){
	case DLT_EN10MB:
		switch (linktype_ext)
		{
		case ETHERTYPE_PPPOE:
			
			packet=AllocObject(PPPOEPacket);
			src_addr=(*env)->NewByteArray(env,6);
			dst_addr=(*env)->NewByteArray(env,6);
			session_id=(*env)->NewByteArray(env,2);

			pppoe_hdr=(struct pppoe_header *)data;
			(*env)->SetByteArrayRegion(env,src_addr,0,6,pppoe_hdr->m_ether_header.ether_src);
			(*env)->SetByteArrayRegion(env,dst_addr,0,6,pppoe_hdr->m_ether_header.ether_dest);
			(*env)->CallVoidMethod(env,packet,setEthernetValueMID,dst_addr,src_addr,
				(jchar)ntohs(pppoe_hdr->m_ether_header.ether_type));
			version = (jbyte)((pppoe_hdr->ver_type&ETHERTYPE_VERSION) >> 4);
			type = (jbyte)(pppoe_hdr->ver_type&ETHERTYPE_TYPE);
			code = (jbyte)ntohs(pppoe_hdr->code);
			(*env)->SetByteArrayRegion(env,session_id,0,2,pppoe_hdr->session_id);
			pay_load_len = (short)ntohs(pppoe_hdr->pay_load_len);
			(*env)->CallVoidMethod(env,packet,setPPPOEValueMID,version,type,code,session_id,pay_load_len);
			DeleteLocalRef(src_addr);
			DeleteLocalRef(dst_addr);
			DeleteLocalRef(session_id);
			break;
		default:
			packet=AllocObject(EthernetPacket);
			src_addr=(*env)->NewByteArray(env,6);
			dst_addr=(*env)->NewByteArray(env,6);
			ether_hdr=(struct ether_header *)data;
			(*env)->SetByteArrayRegion(env,src_addr,0,6,ether_hdr->ether_src);
			(*env)->SetByteArrayRegion(env,dst_addr,0,6,ether_hdr->ether_dest);
			(*env)->CallVoidMethod(env,packet,setEthernetValueMID,dst_addr,src_addr,
				(jchar)ntohs(ether_hdr->ether_type));
			DeleteLocalRef(src_addr);
			DeleteLocalRef(dst_addr);
			break;
		}

		break;
	default:
		packet=AllocObject(DatalinkPacket);
		break;
	}

	return packet;
}

int set_ether(JNIEnv *env,jobject packet,char *pointer){
	packet=GetObjectField(Packet,packet,"Ljpcap/packet/DatalinkPacket;","datalink");
	if(packet!=NULL && IsInstanceOf(packet,EthernetPacket)){
		struct ether_header *ether_hdr=(struct ether_header *)pointer;

		jbyteArray src=GetObjectField(EthernetPacket,packet,"[B","src_mac");
		jbyteArray dst=GetObjectField(EthernetPacket,packet,"[B","dst_mac");

		(*env)->GetByteArrayRegion(env,src,0,6,(char *)&ether_hdr->ether_src);
		(*env)->GetByteArrayRegion(env,dst,0,6,(char *)&ether_hdr->ether_dest);
		ether_hdr->ether_type=htons(GetShortField(EthernetPacket,packet,"frametype"));

		(*env)->ExceptionDescribe(env);
		return sizeof(struct ether_header);
	}
	return 0;
}
