/*
 * Created on 14.07.2004
 */
package de.japes.net.nasty.collector;

/**
 * @author unrza88
 */

public class Nf9Header {
	
	private short version;
	private short count;
	private int uptime;
	private int unixSecs;
	private int sequence;
	private int sourceID;
	
	public static int getSize() {
		
		return 32;
	}

}
