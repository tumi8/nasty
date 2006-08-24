/*
 * Created on 27.09.2004
 */

package de.japes.net.nasty.collector;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * @author unrza88
 */
public class IPFIXPacket implements FlowsetPacket {

	private int version;
	private int length;
	private int unixSecs;
	private int sequence;
	private long sourceID;
	private long sourceAddress;
	private byte[] content;
	private Statement s = null;
	
	static Identifier sourceIdObj = new Identifier();
	static Identifier flowsetIdObj = new Identifier();
	
	static FlowData flowData = null;
	
	public IPFIXPacket(Connection con) {
		
		flowData = new FlowData(con);
		
		try {
			s = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
		} catch (SQLException e) {
			System.err.println("Failed to create statement.");
		}
	}
	
	public void readContents() throws IOException, FlowFormatException {
		
		int flowsetID;
		int flowsetLength;
		int remainingBytes;
		Template templ;
		Identifier tmpId;
		Exporter exporter=null;
		LinkedList exporterList;
		boolean exporterFound = false;
		
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(content));
		
		try {
			
			version = in.readUnsignedShort();
			length	= in.readUnsignedShort();
			
			if (length == (short)0) {
				throw new FlowFormatException(
						"NetFlow packet contains neither data nor template flowsets.");
			}
			
			unixSecs= in.readInt();
			sequence= in.readInt();
			sourceID= (long)in.readInt() & 0xffffffffL;
			
		} catch(IOException e) {
			System.err.println("Error while reading from NetFlow packet");
			in.close();
			throw e;
		} catch(FlowFormatException e) {
			in.close();
			throw e;
		}
		
		sourceIdObj.setID(sourceID);
		
		int i = length;
		
		while(i>0) {
			
			try {
				flowsetID		=	in.readUnsignedShort();
				flowsetLength	=	in.readUnsignedShort();
				
				if (flowsetLength == 0) {
					if (flowsetID!= 0) 
						throw new FlowFormatException("FlowSet has length 0.");
					else if(flowsetID == 0)	//padding bytes
						return;
				}
					
				i -= flowsetLength;
				
				remainingBytes = flowsetLength-4; //without id and length field of flowset
					
				if (flowsetID == 2) {
					do {
						templ = new Template(in, sourceID, sourceAddress, s);
						remainingBytes -= (templ.getFieldCount()*4 + 4 /*template header*/);
						
						if (remainingBytes > 0) {
							in.mark(0);
							
							if (in.readInt()==0) {	//padding bytes
								remainingBytes -= 4;
							} else
								in.reset();
						}
						
					} while (remainingBytes > 0);
					
				} else if (flowsetID == 3) {
					//options template set
					System.out.println("Options flowset received.");
					
					flowsetLength=in.readUnsignedShort();
					
					in.skipBytes(flowsetLength-4);
				} else {
					//data flowset
					
					flowsetIdObj.setID(flowsetID);
					
					if (Template.knownExporters.containsKey(sourceIdObj)) {
						
						exporterList = (LinkedList)Template.knownExporters.get(sourceIdObj);
						
						int j=0;
						
						while(j < exporterList.size()) {
							
							if ((exporter=(Exporter)exporterList.get(j)).exporterAddress == sourceAddress) {
								exporterFound = true;
								break;
							}
							j++;
						}
						
						if (exporterFound)
							if (exporter.exporterTemplates.containsKey(flowsetIdObj)) {
							
								templ = (Template)exporter.exporterTemplates.get(flowsetIdObj);
							
							} else {
								return;
						} else {
							return;
						}
					} else {
						return;
					}
					
					//try {
						//s.execute("LOCK TABLES flow WRITE");
					
						do {
							ListIterator e = templ.getTemplateFields().listIterator();
							
							while(e.hasNext()) {
								getFlowContent(in, flowData, (TemplateDescr)e.next());
							}
							
							flowData.setExporterID(sourceID);
							
							flowData.setDbID(exporter.dbID);
							
							//flowData.printContent();
							
							flowData.exportToDB();
							
							remainingBytes -= templ.getDataFlowsetLength();
							
							if (remainingBytes > 0) {
								in.mark(0);
								
								if (in.readInt()==0) {	//padding bytes
									remainingBytes -= 4;
								} else
									in.reset();
							}
							
							//flowData.clear();
							
							
						} while (remainingBytes > 0);
						
						//s.execute("UNLOCK TABLES");
						
					//} catch (SQLException e) {
						//System.err.println("Couldn't lock/unlock flow table");
						//System.err.println(e.getMessage());
					//}
				}
				
				
			} catch (IOException e) {
				System.err.println("Error while reading from NetFlow packet.");
				throw e;
			}
		}
	}	

	private void getFlowContent(DataInputStream in, FlowData flowData, TemplateDescr templ) 
		throws IOException {
		
		try {
		
			switch (templ.fieldType) {
			
			case 1: flowData.setBytes((long)in.readLong()); //only works for values less than 2^63-1
					flowData.setContainsData(true);
					break;
			case 2: flowData.setPackets((long)in.readLong()); //only works for values less than 2^63-1
					flowData.setContainsData(true);
					break;
			case 4: flowData.setProto(in.readUnsignedByte());
					flowData.setContainsData(true);
					break;
			case 5: flowData.setSrcTos(in.readUnsignedByte());
					flowData.setContainsData(true);
					break;
			case 7: flowData.setSrcPort(in.readUnsignedShort());
					flowData.setContainsData(true);
					break;
			case 8:	flowData.setSrcIP((long)in.readInt() & 0xffffffffL);
					flowData.setContainsData(true);
					break;
			case 11: flowData.setDstPort(in.readUnsignedShort());
					 flowData.setContainsData(true);
					 break;
			case 12: flowData.setDstIP((long)in.readInt() & 0xffffffffL);
					 flowData.setContainsData(true);
					 break;
			case 21: // obsolete IPFIX IE: flowEndTime
			case 151: flowData.setLastSwitched((long)in.readInt() & 0xffffffffL);
					 flowData.setContainsData(true);
					 break;
			case 22: // obsolete IPFIX IE: flowCreationTime
			case 150: flowData.setFirstSwitched((long)in.readInt() & 0xffffffffL);
					 flowData.setContainsData(true);
					 break;
			default: in.skipBytes(templ.fieldLength);
			}		
		} catch(IOException e) {
			System.err.println("Error filling FlowData object.");
			throw e;
		}
	}
		
	public void setContent(byte[] buf) {
		content = buf;
	}
	
	public void setSrcAddress(long addr) {
		
		sourceAddress = addr;
	}
	
	private class SwitchTimes {
		
		private int firstSwitched;
		private int lastSwitched;
		
	}
}
