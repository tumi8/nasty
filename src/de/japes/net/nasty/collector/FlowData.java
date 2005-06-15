/*
 * Created on 21.07.2004
 */

package de.japes.net.nasty.collector;

/**
 * @author unrza88
 */

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.io.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedList;

public class FlowData {

	private SimpleDateFormat date = new SimpleDateFormat("dd.MM HH:mm:ss");
	private StringBuffer strBuf = new StringBuffer();
	private FieldPosition fldPos = new FieldPosition(0);
	private PrintWriter out;
	private Statement s = null;
	private PreparedStatement preparedInsert = null;
	private PreparedStatement preparedSelect = null;
	private PreparedStatement preparedUpdate = null;
	LinkedList duplicates = new LinkedList();
	private static Calendar tmpTime = new GregorianCalendar();
	private static HashSet knownTables = new HashSet();
	private static final short CACHE_SIZE = 50;
	private static exportableFlow[] flowCache = new exportableFlow[CACHE_SIZE];
	private static short cachePtr = 0;
	
	public FlowData(Connection con) {
		
		try {
			out = new PrintWriter(new BufferedOutputStream(new FileOutputStream("output.txt")));
		} catch (FileNotFoundException e) {
			System.err.println("File not found.");
		}
		
		try {
			s = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
		} catch (SQLException e) {
			System.err.println("Failed to create statement.");
		}
		
		for (int i=0; i<flowCache.length; i++) {
			flowCache[i] = new exportableFlow();
		}
		
		/*try {
			preparedInsert = con.prepareStatement("INSERT INTO flow VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			preparedSelect = con.prepareStatement("SELECT firstSwitched, lastSwitched, exporterID, bytes, pkts, duplCount " +
						"FROM flow WHERE srcIP=?" +
						" AND dstIP=? AND srcPort=?" +
						" AND dstPort=? AND proto=?");
			preparedUpdate = con.prepareStatement("UPDATE flow SET duplCount=?" +
												" WHERE srcIP=? AND dstIP=?" +
												" AND srcPort=? AND dstPort=?" +
												" AND proto=? AND duplCount=?");
			
		} catch (SQLException e) {
			System.err.println("Falied to create prepared statement");
		}*/
		
	}
	
	/*public void printContent() {
		
		ByteBuffer buf = ByteBuffer.allocate(4);
		if (!containsData)
			return;
		
		try {
			out.print(proto + " ");
			strBuf.delete(0, strBuf.length());
			Date first = new Date(firstSwitched*1000L);
			
			strBuf = date.format(first, strBuf, fldPos);
			out.print(strBuf + " ");
			
			Date last = new Date(lastSwitched*1000L);
			
			strBuf.delete(0, strBuf.length());
			strBuf = date.format(last, strBuf, fldPos);
			out.print( strBuf + " ");
			
			buf.putInt((int)srcIP);
			out.print(InetAddress.getByAddress(buf.array()).getHostAddress() + " ");
			
			buf.rewind();
			buf.putInt((int)dstIP);
			out.print(InetAddress.getByAddress(buf.array()).getHostAddress() + " ");
			
			out.print(srcPort + " ");
			out.print(dstPort + " ");
			out.print(packets + " ");
			out.println(bytes);
			
			out.flush();
			
		} catch (UnknownHostException e) {
			System.err.println("Error getting host address.");
			return;
		}
	}*/
	
	/*public void clear() {
		
		srcIP=0;
		dstIP=0;
		srcPort=0;
		dstPort=0;
		proto=0;
		srcTos=0;
		dstTos=0;
		bytes=0;
		packets=0;
		firstSwitched=0;
		lastSwitched=0;
		containsData=false;
		
	}*/
	
	public void exportToDB() {
		
		ResultSet results;
		int rows;
		int fswitch, lswitch;
		long expID;
		short dupCount;
		short currCount;
		long localBytes, localPackets;
		boolean foundMatching = false;
		
		duplicates.clear();
		
		if (flowCache[cachePtr].containsData) {
			
			String currTable = getCurrTable(flowCache[cachePtr].firstSwitched);
			
			try {
				if (!knownTables.contains(currTable)) {
					s.executeUpdate("CREATE TABLE IF NOT EXISTS " + currTable + " (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
							" srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT(3) UNSIGNED, dstTos TINYINT(3) UNSIGNED," +
							" bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED," +
							" exporterID SMALLINT(5) UNSIGNED)");
					
					knownTables.add(currTable);
				}
				
			} catch (SQLException e) {
				System.err.println("Couldn't create table " + currTable + ".");
				System.err.println(e.getMessage());
			}
		} else
			return;
		
		if (cachePtr<CACHE_SIZE-1) {
			
			cachePtr++;
			
		} else {
		
			try {
				String statement = "LOCK TABLES ";
				
				Iterator it = knownTables.iterator();
				
				boolean first = true;
				
				while (it.hasNext()) {
					
					if (first)
						first = false;
					else
						statement += ", ";
					
					statement += (String)it.next() + " WRITE";
					
					it.remove();
				}
				
				s.execute(statement);
				
			} catch (SQLException e) {
				System.err.println("Couldn't lock tables");
				System.err.println(e.getMessage());
			}
			
			for(int i=0; i<cachePtr; i++) {
				
				String currTable = getCurrTable(flowCache[i].firstSwitched);
				
				try {
					
					s.executeUpdate("INSERT INTO " + currTable + " VALUES(" +
							flowCache[i].srcIP + ", " + flowCache[i].dstIP + ", " + flowCache[i].srcPort + ", " + flowCache[i].dstPort + ", " + 
							flowCache[i].proto + ", " + flowCache[i].dstTos +", " + flowCache[i].bytes + ", " + 
							flowCache[i].packets + ", " + flowCache[i].firstSwitched + ", " + flowCache[i].lastSwitched + ", " + 
							flowCache[i].dbID + ")");
					
				} catch (SQLException e) {
					
					//check if error results from a duplicate
					/*if (e.getErrorCode() == 1062) {
						
						try {
							results = s.executeQuery("SELECT firstSwitched, lastSwitched, exporterID, bytes, pkts, duplCount " + 
									"FROM " + currTable + " WHERE srcIP=" + flowCache[i].srcIP +
									" AND dstIP=" + flowCache[i].dstIP + " AND srcPort=" + flowCache[i].srcPort +
									" AND dstPort=" + flowCache[i].dstPort + " AND proto=" + flowCache[i].proto);
							
							//results = preparedSelect.executeQuery();
							
							results.last();
							rows = results.getRow();
							
							//if (rows > 1) {
							
							for (int j=0; j<rows; j++)
								duplicates.add(new int[]{j});
							
							results.beforeFirst();
							
							while(results.next()) {
								
								fswitch = results.getInt(1);
								lswitch = results.getInt(2);
								expID 	= results.getLong(3);
								localBytes = results.getLong(4);
								localPackets = results.getLong(5);
								dupCount = results.getShort(6);
								
								if (expID != flowCache[i].exporterID) {
									//System.out.println("Verschiedene Exporter.");
									continue;
								}
								
								if (fswitch<=flowCache[i].firstSwitched && lswitch<=flowCache[i].lastSwitched) {
									
									System.out.println("new flow fragment");
									if (flowCache[i].firstSwitched-lswitch<30000) {
										
										//combine the two flows by adding bytes and packets and adjusting firstSwitched
										flowCache[i].firstSwitched = fswitch;
										System.out.println("Old: " + flowCache[i].bytes);
										flowCache[i].bytes += localBytes;
										System.out.println("New: " + flowCache[i].bytes);
										flowCache[i].packets += localPackets;
										
										results.deleteRow();
										duplicates.remove(dupCount-1);
										rows--;
										foundMatching = true;
									}
								} //existing flow after current flow
								else if (fswitch>=flowCache[i].firstSwitched && lswitch>=flowCache[i].lastSwitched) {
									
									if (fswitch-flowCache[i].lastSwitched<30000) {
										
										//combine the two flows by adding bytes and packets and adjusting lastSwitched
										flowCache[i].lastSwitched = lswitch;
										System.out.println("Old: " + flowCache[i].bytes);
										flowCache[i].bytes += localBytes;
										System.out.println("New: " + flowCache[i].bytes);
										flowCache[i].packets += localPackets;
										
										results.deleteRow();
										duplicates.remove(dupCount-1);
										rows--;
										foundMatching = true;
									}
								}
								
								if (foundMatching) {
									
									foundMatching = false;
									
									//adjust duplCount values because some rows were deleted from table
									for (int j=1; j<=rows; j++) {
										
										currCount = (short)((int[])duplicates.get(j-1))[0];
										
										if (currCount != j) {
											
											s.executeUpdate("UPDATE " + currTable + " SET duplCount=" + i +
													" WHERE srcIP=" + flowCache[i].srcIP + " AND dstIP=" + flowCache[i].dstIP +
													" AND srcPort=" + flowCache[i].srcPort + " AND dstPort=" + flowCache[i].dstPort +
													" AND proto=" + flowCache[i].proto + " AND duplCount=" + currCount);
										}
									}
								}
						
								
								s.executeUpdate("INSERT INTO " + currTable + " VALUES(" +
										flowCache[i].srcIP + ", " + flowCache[i].dstIP + ", " + flowCache[i].srcPort + ", " + flowCache[i].dstPort + ", " + 
										flowCache[i].proto + ", " + flowCache[i].dstTos +", " + flowCache[i].bytes + ", " + 
										flowCache[i].packets +", " + flowCache[i].firstSwitched + ", " + flowCache[i].lastSwitched + ", " + 
										flowCache[i].dbID + "," + (rows+1) + ")");
							}
							
						} catch (SQLException e2) {
							System.err.println(e2.getMessage());
							return;
						}
					} else {
						System.err.println(e.getMessage());
					}*/
					System.err.println("Couldn't insert flow into database.");
					System.err.println(e.getMessage());
				}
				
				flowCache[i].containsData = false;
			}
			
			try {
				s.execute("UNLOCK TABLES");
			} catch (SQLException e) {
				System.err.println("Couldn't unlock tables.");
			}
			
			cachePtr = 0;
		}
	}
	
	private class exportableFlow {
		
		public long srcIP;
		public long dstIP;
		public int srcPort;
		public int dstPort;
		public int proto;
		public int srcTos;
		public int dstTos;
		public long bytes;
		public long packets;
		public long firstSwitched;
		public long lastSwitched;
		public long exporterID;
		public long exporterAddress;
		public int dbID;
		public boolean containsData;
	}
	
	
	private String getCurrTable(long time) {
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMinimumIntegerDigits(2);
	    
		tmpTime.setTimeInMillis(time*1000);
		
		return 	"h_" + tmpTime.get(Calendar.YEAR) + nf.format(tmpTime.get(Calendar.MONTH)+1) + 
				nf.format(tmpTime.get(Calendar.DAY_OF_MONTH)) + "_" + nf.format(tmpTime.get(Calendar.HOUR_OF_DAY)) +
				"_" + ((long)tmpTime.get(Calendar.MINUTE)<30?0:1);
	}
	
	/**
	 * @return Returns the firstSwitched.
	 */
	public long getFirstSwitched() {
		return flowCache[cachePtr].firstSwitched;
	}
	/**
	 * @param firstSwitched The firstSwitched to set.
	 */
	public void setFirstSwitched(long firstSwitched) {
		flowCache[cachePtr].firstSwitched = firstSwitched;
	}
	/**
	 * @return Returns the lastSwitched.
	 */
	public long getLastSwitched() {
		return flowCache[cachePtr].lastSwitched;
	}
	/**
	 * @param lastSwitched The lastSwitched to set.
	 */
	public void setLastSwitched(long lastSwitched) {
		flowCache[cachePtr].lastSwitched = lastSwitched;
	}

	/**
	 * @return Returns the dstIP.
	 */
	public long getDstIP() {
		return flowCache[cachePtr].dstIP;
	}
	/**
	 * @param dstIP The dstIP to set.
	 */
	public void setDstIP(long dstIP) {
		flowCache[cachePtr].dstIP = dstIP;
	}
	/**
	 * @return Returns the dstPort.
	 */
	public int getDstPort() {
		return flowCache[cachePtr].dstPort;
	}
	/**
	 * @param dstPort The dstPort to set.
	 */
	public void setDstPort(int dstPort) {
		flowCache[cachePtr].dstPort = dstPort;
	}
	/**
	 * @return Returns the dstTos.
	 */
	public int getDstTos() {
		return flowCache[cachePtr].dstTos;
	}
	/**
	 * @param dstTos The dstTos to set.
	 */
	public void setDstTos(int dstTos) {
		flowCache[cachePtr].dstTos = dstTos;
	}
	/**
	 * @return Returns the proto.
	 */
	public int getProto() {
		return flowCache[cachePtr].proto;
	}
	/**
	 * @param proto The proto to set.
	 */
	public void setProto(int proto) {
		flowCache[cachePtr].proto = proto;
	}
	/**
	 * @return Returns the srcIP.
	 */
	public long getSrcIP() {
		return flowCache[cachePtr].srcIP;
	}
	/**
	 * @param srcIP The srcIP to set.
	 */
	public void setSrcIP(long srcIP) {
		flowCache[cachePtr].srcIP = srcIP;
	}
	/**
	 * @return Returns the srcPort.
	 */
	public int getSrcPort() {
		return flowCache[cachePtr].srcPort;
	}
	/**
	 * @param srcPort The srcPort to set.
	 */
	public void setSrcPort(int srcPort) {
		flowCache[cachePtr].srcPort = srcPort;
	}
	/**
	 * @return Returns the srcTos.
	 */
	public int getSrcTos() {
		return flowCache[cachePtr].srcTos;
	}
	/**
	 * @param srcTos The srcTos to set.
	 */
	public void setSrcTos(int srcTos) {
		flowCache[cachePtr].srcTos = srcTos;
	}
	/**
	 * @return Returns the bytes.
	 */
	public long getBytes() {
		return flowCache[cachePtr].bytes;
	}
	/**
	 * @param bytes The bytes to set.
	 */
	public void setBytes(long bytes) {
		flowCache[cachePtr].bytes = bytes;
	}
	/**
	 * @return Returns the packets.
	 */
	public long getPackets() {
		return flowCache[cachePtr].packets;
	}
	/**
	 * @param packets The packets to set.
	 */
	public void setPackets(long packets) {
		flowCache[cachePtr].packets = packets;
	}
	/**
	 * @return Returns the containsData.
	 */
	public boolean containsData() {
		return flowCache[cachePtr].containsData;
	}
	/**
	 * @param containsData The containsData to set.
	 */
	public void setContainsData(boolean containsData) {
		flowCache[cachePtr].containsData = containsData;
	}
	/**
	 * @return Returns the exporterID.
	 */
	public long getExporterID() {
		return flowCache[cachePtr].exporterID;
	}
	/**
	 * @param exporterID The exporterID to set.
	 */
	public void setExporterID(long exporterID) {
		flowCache[cachePtr].exporterID = exporterID;
	}
	/**
	 * @return Returns the dbID.
	 */
	public int getDbID() {
		return flowCache[cachePtr].dbID;
	}
	/**
	 * @param dbID The dbID to set.
	 */
	public void setDbID(int dbID) {
		flowCache[cachePtr].dbID = dbID;
	}
}