/**************************************************************************/
/*  NASTY - Network Analysis and STatistics Yielding                      */
/*                                                                        */
/*  Copyright (C) 2006 History Project, http://www.history-project.net    */
/*  History (HIgh-Speed neTwork mOniToring and analYsis) is a research    */
/*  project by the Universities of Tuebingen and Erlangen-Nuremberg,      */
/*  Germany                                                               */
/*                                                                        */
/*  Authors of NASTY are:                                                 */
/*      Christian Japes, University of Erlangen-Nuremberg                 */
/*      Thomas Schurtz, University of Tuebingen                           */
/*      David Halsband, University of Tuebingen                           */
/*      Gerhard Muenz <muenz@informatik.uni-tuebingen.de>,                */
/*          University of Tuebingen                                       */
/*      Falko Dressler <dressler@informatik.uni-erlangen.de>,             */
/*          University of Erlangen-Nuremberg                              */
/*                                                                        */
/*  This program is free software; you can redistribute it and/or modify  */
/*  it under the terms of the GNU General Public License as published by  */
/*  the Free Software Foundation; either version 2 of the License, or     */
/*  (at your option) any later version.                                   */
/*                                                                        */
/*  This program is distributed in the hope that it will be useful,       */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of        */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         */
/*  GNU General Public License for more details.                          */
/*                                                                        */
/*  You should have received a copy of the GNU General Public License     */
/*  along with this program; if not, write to the Free Software           */
/*  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA  */
/**************************************************************************/

/*
 * Created on 12.07.2004
 *
 */
package de.japes.net.nasty.collector;

/**
 * @author unrza88
 *
 */

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import de.japes.parser.nasty.*;


public class Collector {
	
	private DatagramSocket sock;
	private static final int MAX_UDP_SIZE = 65535;
	private UdpBuffer udpBuf = new UdpBuffer();
	private Connection con = null;
	private Statement s = null;
	Timer aggTimer;
	TimerTask aggTask;
	
	private static FlowsetPacket netflowPack = null;
	private static FlowsetPacket ipfixPack = null;
	
	public Collector() {
		
		try {
			sock = new DatagramSocket(2055);
		} catch(SocketException e) {
			System.out.println("Can't open socket on port 2055.");
			System.exit(-1);
		}
		
		if (!ConfigParser.isInitialized()) {
			
			try {
				new ConfigParser(new FileInputStream(new File("./collector.cfg")));
			} catch (FileNotFoundException e) {
				System.err.println("Config file not found.");
				System.exit(-1);
			}
			
			try {
				ConfigParser.Start();
			} catch(ParseException e) {
				System.err.println("Syntax error in config file:");
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		}
		
		openDatabase();
		
		if (con == null || s==null) {
			
			System.err.println("Database connection not established.");
			System.exit(-1);
		}
		
		try {
			s.executeUpdate("DELETE FROM config");
			s.executeUpdate("INSERT INTO config VALUES(" + ConfigParser.getDaysDetailedData() + "," + ConfigParser.getWeeksDailyData() + ")");
			
		} catch (SQLException e) {
			System.err.println("Couldn't write config to database.");
			System.err.println(e.getMessage());
		}
		
		netflowPack = new NetFlowPacket(con);
		ipfixPack 	= new IPFIXPacket(con);
		
	}
	
	public void startCollecting() {
		
		byte[] buf = new byte[MAX_UDP_SIZE+4];	//add 4 bytes to be able to attach address 
												//even to big packet
		byte[] address;
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		int length;
		
		new PacketReaderThread().start();
		
		Calendar aggregationTime = new GregorianCalendar();
		
		aggregationTime.add(Calendar.DATE, 1);
		aggregationTime.set(Calendar.HOUR, 5);
		aggregationTime.set(Calendar.MINUTE, 0);
		aggregationTime.set(Calendar.SECOND, 0);
		aggregationTime.set(Calendar.AM_PM, Calendar.AM);
		
		System.out.println("Aggregation Time: " + aggregationTime.getTime());
		aggTimer = new Timer();
		aggTask = new AggregationTask();
		
		aggTimer.scheduleAtFixedRate(aggTask, aggregationTime.getTime(), 86400000l); //every 24h
		
		System.out.println("Scheduled Time: " + new Date(aggTask.scheduledExecutionTime()));
		
		while (true) {
			
			try {
				sock.receive(packet);
			} catch (IOException e) {
				
				System.out.println("Can't receive data from socket.");
				sock.close();
				System.exit(-1);
			}
			
//			 the following to lines copy the source IP to the packet buffer so that this information is
			// available for the PacketReaderThread (don't like this solution but it works)
			//System.arraycopy(packet.getAddress().getAddress(), 0, buf, length=packet.getLength(), 4);
			//packet.setLength(length + 4); //length instead of packet.getLength to prevent one method call
			
			address = packet.getAddress().getAddress();  //HIER!!!!!
			length = packet.getLength();
			buf[length]=address[0];
			buf[length+1]=address[1];
			buf[length+2]=address[2];
			buf[length+3]=address[3];
			udpBuf.push(packet.getData(), length+4);
			
			//packet.setLength(MAX_UDP_SIZE+4);
		}	
		
	}		

	private void openDatabase() {
		
		String url = "jdbc:mysql://" + ConfigParser.getDBServer() + "/" + ConfigParser.getDBName();
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			System.err.println("Failed to load MySQL driver.");
			return;
		}
		
		try {
			con = DriverManager.getConnection(url, ConfigParser.getDBUser(), ConfigParser.getDBPass()); 
			s = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		} catch (SQLException e) {
			System.err.println("Failed to connect to database.");
			System.err.println(url + " " + ConfigParser.getDBUser() + " " + ConfigParser.getDBPass());
			System.err.println(e.getMessage());
			return;
		}
		
		return;
	}
	
	private class PacketReaderThread extends Thread {
		
		private byte[] buf = new byte[MAX_UDP_SIZE];
		//private Statement s = openDatabase();
		//private Connection con = openDatabase();
		
		public PacketReaderThread() {
			
			/*if (con==null) {
				sock.close();
				System.exit(-1);
			}*/
		}
		
		public void run() {
			
			FlowsetPacket flowPack;
			int length;
			
			while(true) {
								
				length=udpBuf.pull(buf);
				
				if ((flowPack=getFlowset(buf, length)) == null) {
					
					
				} else {
					try {
						flowPack.readContents();
					} catch (IOException e) {
						System.err.println("Error reading flowset contents.");
						return;
					} catch (FlowFormatException e) {
						System.err.println("Error reading flowset contents.");
						return;
					}
				}
			}
		}
		
	}
	
	private FlowsetPacket getFlowset(byte[] buf, int bufLen) {
		
		int version=0;
		
		//this needs to be changed
		if (bufLen-4 < Nf9Header.getSize()) 
			return null;
		
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf));
		
		try {
			version = in.readUnsignedShort();
			in.close();
		
		
		switch(version) {
		
		case 9: netflowPack.setContent(buf);
				netflowPack.setSrcAddress((long)ByteBuffer.wrap(buf, bufLen-4, 4).getInt()&0xffffffffL);
	    		return netflowPack;
		case 10: ipfixPack.setContent(buf);
				 ipfixPack.setSrcAddress((long)ByteBuffer.wrap(buf, bufLen-4, 4).getInt()&0xffffffffL);
				 return ipfixPack;
		default: return null;
		}
		
		} catch(IOException e) {
			System.out.println("Couldn't readShort.");
			return null;
		}
	}
		
	private class AggregationTask extends TimerTask {
		
		Aggregator agg = new Aggregator();
		
		public void run() {
			
			agg.start();
			
		}
	}
	
	private class UdpBuffer {
		
		public UdpBuffer() {
			
			for(int i=0; i<idx.length; i++)
				idx[i] = new Index();
		}
		
		private static final int BUFFER_LENGTH = MAX_UDP_SIZE*500;
		
		private byte[] udpBuffer = new byte[BUFFER_LENGTH];
		private int count = 0;
		private int usedBufferLength = 0;
		private int dropped = 0;
		
		private class Index {
			int offset;
			int length;
		}
		
		private Index[] idx = new Index[2000];
		
		
		public synchronized void push(byte[] pkt, int length) {
	
			if ((count >= idx.length-1) || (length+usedBufferLength > BUFFER_LENGTH)) {
				dropped++;
				System.err.println(dropped + " packet(s) dropped.");	
				return;
			}
			System.arraycopy(pkt, 0, udpBuffer, usedBufferLength, length);
			idx[count].offset = usedBufferLength;
			idx[count].length = length;
			count++;
			//System.out.println("rein(" + count + ")");
			usedBufferLength += length;
			
			if (count == 1)
				notify();
		}
		
		public synchronized int pull(byte[] buf) {
			
			if(count==0) {
				try {
					wait();
				} catch (InterruptedException e) {
					System.err.println("Interrupted.");
				}
			}
			
			if (buf.length < idx[count-1].length) {
				System.err.println("Buffer to small for UDP packet.");
				return 0;
			}
			
			System.arraycopy(udpBuffer, idx[count-1].offset, buf, 0, idx[count-1].length);
			usedBufferLength -= idx[count-1].length;
			count--;
			//System.out.println("raus(" + count + ")");
			
			return idx[count].length;
		}
	}
	
	public static void main(String[] args) {
		
 		Collector col = new Collector();
		
		col.startCollecting();
		
	}
}
