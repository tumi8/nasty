/*
 * Created on 22.09.2004
 */

package de.japes.parser;

/**
 * @author unrza88
 */

import java.util.HashMap;
import java.io.*;

public class Tester {

	public static void main(String[] args) {
		
		HashMap hash;
		String[] protos = null;
		try {
			//new ServicesParser(new FileInputStream(new File("/etc/services")));
			new ProtocolsParser(new FileInputStream(new File("/etc/protocols")));
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
		}
		
		try {
			ProtocolsParser.Start();
		} catch (ParseException e) {
			System.err.println("Parse Exception");
		}
		
		//hash = ServicesParser.getPortHash();
		hash = ProtocolsParser.getProtoHash();
		protos = ProtocolsParser.getProtocols();
		System.out.println(ProtocolsParser.isInitialized());
		
		//System.out.println("Name: http, Port: " + ((int[])hash.get("http"))[0]);
		System.out.println("Name: udp, Port: " + ((int[])hash.get("udp"))[0]);
	}
}
