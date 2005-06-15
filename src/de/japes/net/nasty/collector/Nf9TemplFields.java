/*
 * Created on 14.07.2004
 *
 */
package de.japes.net.nasty.collector;

/**
 * @author unrza88
 *  
 */

class Nf9TemplFldDef {
	short fieldID;
	byte fieldLen;
	String fieldName;
	
	Nf9TemplFldDef(int id, int len, String name) {
		fieldID=(short)id;
		fieldLen=(byte)len;
		fieldName=name;
	}
}

public interface Nf9TemplFields {
	
	Nf9TemplFldDef[] fields = {
			new Nf9TemplFldDef(0, 0, "NOT_USED"),
			new Nf9TemplFldDef(1, 4, "IN_BYTES"), //length not fixed
			new Nf9TemplFldDef(2, 4, "IN_PKTS"),  //length not fixed
			new Nf9TemplFldDef(3, 4, "FLOWS"),    //length not fixed
			new Nf9TemplFldDef(4, 1, "PROTOCOL"),
			new Nf9TemplFldDef(5, 1, "SRC_TOS"),
			new Nf9TemplFldDef(6, 1, "TCP_FLAGS"),
			new Nf9TemplFldDef(7, 2, "L4_SRC_PORT"),
			new Nf9TemplFldDef(8, 4, "IPV4_SRC_ADDR"),
			new Nf9TemplFldDef(9, 1, "SRC_MASK"),
			new Nf9TemplFldDef(10, 2, "INPUT_SNMP"),  //length not fixed
			new Nf9TemplFldDef(11, 2, "L4_DST_PORT"),
			new Nf9TemplFldDef(12, 4, "IPV4_DST_ADDR"),
			new Nf9TemplFldDef(13, 1, "DST_MASK"),
			new Nf9TemplFldDef(14, 2, "OUTPUT_SNMP"),  //length not fixed
			new Nf9TemplFldDef(15, 4, "IPV4_NEXT_HOP"),
			new Nf9TemplFldDef(16, 2, "SRC_AS"),  //length not fixed
			new Nf9TemplFldDef(17, 2, "DST_AS"),  //length not fixed
			new Nf9TemplFldDef(18, 4, "BGP_IPV4_NEXT_HOP"),
			new Nf9TemplFldDef(19, 4, "MUL_DST_PKTS"),  //length not fixed
			new Nf9TemplFldDef(20, 4, "MUL_DST_BYTES"), //length not fixed
			new Nf9TemplFldDef(21, 4, "LAST_SWITCHED"),
			new Nf9TemplFldDef(22, 4, "FIRST_SWITCHED"),
			new Nf9TemplFldDef(23, 4, "OUT_BYTES"),  //length not fixed
			new Nf9TemplFldDef(24, 4, "OUT_PKTS"),   //length not fixed
			new Nf9TemplFldDef(25, 2, "MIN_PKT_LENGTH"),
			new Nf9TemplFldDef(26, 2, "MAX_PKT_LENGTH"),
			new Nf9TemplFldDef(27, 16, "IPV6_SRC_ADDR"),
			new Nf9TemplFldDef(28, 16, "IPV6_DST_ADDR"),
			new Nf9TemplFldDef(29, 1, "IPV6_SRC_MASK"),
			new Nf9TemplFldDef(30, 1, "IPV6_DST_MASK"),
			new Nf9TemplFldDef(31, 3, "IPV6_FLOW_LABEL"),
			new Nf9TemplFldDef(32, 2, "ICMP_TYPE"),
			new Nf9TemplFldDef(33, 1, "MUL_IGMP_TYPE"),
			new Nf9TemplFldDef(34, 4, "SAMPLING_INTERVAL"),
			new Nf9TemplFldDef(35, 1, "SAMPLING_ALGORITHM"),
			new Nf9TemplFldDef(36, 2, "FLOW_ACTIVE_TIMEOUT"),
			new Nf9TemplFldDef(37, 2, "FLOW_INACTIVE_TIMEOUT"),
			new Nf9TemplFldDef(38, 1, "ENGINGE_TYPE"),
			new Nf9TemplFldDef(39, 1, "ENGINE_ID"),
			new Nf9TemplFldDef(40, 4, "TOTAL_BYTES_EXP"),  //length not fixed
			new Nf9TemplFldDef(41, 4, "TOTAL_PKTS_EXP"),   //length not fixed
			new Nf9TemplFldDef(42, 4, "TOTAL_FLOWS_EXP"),  //length not fixed
			new Nf9TemplFldDef(43, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(44, 4, "IPV4_SRC_PREFIX"),
			new Nf9TemplFldDef(45, 4, "IPV4_DST_PREFIX"),
			new Nf9TemplFldDef(46, 1, "MPLS_TOP_LABEL_TYPE"),
			new Nf9TemplFldDef(47, 4, "MPLS_TOP_LABEL_IP_ADDR"),
			new Nf9TemplFldDef(48, 1, "FLOW_SAMPLER_ID"),
			new Nf9TemplFldDef(49, 1, "FLOW_SAMPLER_MODE"),
			new Nf9TemplFldDef(50, 4, "FLOW_SAMPLER_RANDOM_INTERVAL"),
			new Nf9TemplFldDef(51, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(52, 1, "MIN_TTL"),
			new Nf9TemplFldDef(53, 1, "MAX_TTL"),
			new Nf9TemplFldDef(54, 2, "IPV4_IDENT"),
			new Nf9TemplFldDef(55, 1, "DST_TOS"),
			new Nf9TemplFldDef(56, 6, "IN_SRC_MAC"),
			new Nf9TemplFldDef(57, 6, "OUT_DST_MAC"),
			new Nf9TemplFldDef(58, 2, "SRC_WLAN"),
			new Nf9TemplFldDef(59, 2, "DST_VLAN"),
			new Nf9TemplFldDef(60, 1, "IP_PROTOCOL_VERSION"),
			new Nf9TemplFldDef(61, 1, "DIRECTION"),
			new Nf9TemplFldDef(62, 16, "IPV6_NEXT_HOP"),
			new Nf9TemplFldDef(63, 16, "BGP_IPV6_NEXT_HOP"),
			new Nf9TemplFldDef(64, 4, "IPV6_OPTION_HEADERS"),
			new Nf9TemplFldDef(65, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(66, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(67, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(68, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(69, 0, "VENDOR_PROPRIETARY"),
			new Nf9TemplFldDef(70, 3, "MPLS_LABLE_1"),
			new Nf9TemplFldDef(71, 3, "MPLS_LABLE_2"),
			new Nf9TemplFldDef(72, 3, "MPLS_LABLE_3"),
			new Nf9TemplFldDef(73, 3, "MPLS_LABLE_4"),
			new Nf9TemplFldDef(74, 3, "MPLS_LABLE_5"),
			new Nf9TemplFldDef(75, 3, "MPLS_LABLE_6"),
			new Nf9TemplFldDef(76, 3, "MPLS_LABLE_7"),
			new Nf9TemplFldDef(77, 3, "MPLS_LABLE_8"),
			new Nf9TemplFldDef(78, 3, "MPLS_LABLE_9"),
			new Nf9TemplFldDef(79, 3, "MPLS_LABLE_10"),
			new Nf9TemplFldDef(80, 6, "IN_DST_MAC"),
			new Nf9TemplFldDef(81, 6, "OUT_SRC_MAC"),
			new Nf9TemplFldDef(82, 0, "IF_NAME"),  //length specified in template (no default)
			new Nf9TemplFldDef(83, 0, "IF_DESC"),  //length specified in template (no default)
			new Nf9TemplFldDef(84, 0, "SAMPLER_NAME"), //length specified in template (no default)
			new Nf9TemplFldDef(85, 4, "IN_PERMANENT_BYTES"),  //length not fixed
			new Nf9TemplFldDef(86, 4, "IN_PERMANENT_PKTS"),   //length not fixed
			new Nf9TemplFldDef(87, 0, "VENDOR_PROPRIETARY")
	};
			
}
