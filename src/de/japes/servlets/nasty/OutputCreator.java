/*
 * Created on 03.09.2004
 */

package de.japes.servlets.nasty;

/**
 * @author unrza88
 */

import javax.servlet.http.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import de.japes.parser.*;

abstract class OutputCreator {
	
	private String[] tcpServices = null;
	private String[] udpServices = null;
	private String[] protocols = null;
	private HashMap portMap = null;
	private HashMap protocolMap = null;
	protected String output = "";
	
	Date date = new Date();
	
	public OutputCreator() {
		
		String os = System.getProperty("os.name").toLowerCase();
		
		if (!ServicesParser.isInitialized()) {
			String servicesFile;
			
			if (os.indexOf("windows") > -1)
				servicesFile = System.getProperty("user.home") + "\\system32\\drivers\\etc\\services";
			else
				servicesFile = "/etc/services";
			
			try {
				new ServicesParser(new FileInputStream(new File(servicesFile)));
				ServicesParser.Start();
			} catch (FileNotFoundException e) {
				return;
			} catch (ParseException e) {
				return;
			}
		}
		
		tcpServices = ServicesParser.getTcpServices();
		udpServices = ServicesParser.getUdpServices();
		portMap = ServicesParser.getPortHash();
		
		if (!ProtocolsParser.isInitialized()) {
			String protocolsFile;
			
			if (os.indexOf("windows") > -1)
				protocolsFile = System.getProperty("user.home") + "\\system32\\drivers\\etc\\protocols";
			else
				protocolsFile = "/etc/protocols";
			
			try {
				new ProtocolsParser(new FileInputStream(new File(protocolsFile)));
				ProtocolsParser.Start();
			} catch (FileNotFoundException e) {
				return;
			} catch (ParseException e) {
				return;
			}
		}
		
		protocols = ProtocolsParser.getProtocols();
		protocolMap = ProtocolsParser.getProtoHash();
		
		System.setProperty("java.awt.headless", "true");
	}
	
	abstract String createOutput(HttpServletRequest request, HttpServletResponse response, Statement s);
	
	abstract String getContentType();
	
	protected String createIPOutput(long addr, boolean resolve) {
		
		InetAddress ip;
		ByteBuffer buf = ByteBuffer.allocate(4);
		
		buf.putInt((int)addr);
		
		try {
			ip = InetAddress.getByAddress(buf.array());
		} catch (UnknownHostException e) {
			return Long.toString(addr);
		}

		if (resolve) 
			return ip.getCanonicalHostName();
		else
			return ip.getHostAddress();
		
	}
	
	protected String createPortOutput(int port, boolean resolve) {
		
		String output = "";
		
		if (port == 0)
			return "0";
		
		if (tcpServices != null) {
			
			if ((port < 1024) && resolve) {
				
				if ((tcpServices != null) && (output=tcpServices[(int)port-1]) != null) {
				} else if ((udpServices != null) && (output=udpServices[(int)port-1]) != null) {
				} else
					output = "" + port;
			} else
				output = "" + port;
		} else
			output = "" + port;
		
		return output;
	}
	
	protected String createProtoOutput(short proto, boolean resolve) {
		
		String output = "";
		
		if (protocols != null) {
			
			if (proto < 256 && resolve) {
				if ((output=protocols[proto]) != null) {} 
				else
					output += proto;
			} else
				output += proto;
		} else
			output += proto;
		
		return output;
	}
	
	protected String createTimeOutput(long time) {
		
		date.setTime(time);
		
		return date.toString();
	}
	
	protected String createIPQuery(String addr, String colName) {
		
		InetAddress ip = null;
		short subnetMask = 32;
		String[] splitString;
		long ipAddr = 0;
		long lowerBound = 0;
		long upperBound = 0;
		
		if (addr==null || colName==null || addr.equalsIgnoreCase("") || addr.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=addr.split("/")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					subnetMask = Short.parseShort(splitString[1]);
				} catch (NumberFormatException e) {
					if (colName.equalsIgnoreCase("srcIP"))
						output += "<p>Entered Source IP was invalid and therefore omitted.</p><br>";
					else if (colName.equalsIgnoreCase("dstIP"))
						output += "<p>Entered Destination IP was invalid and therefore omitted.</p><br>";
					return "";
				}
				if (subnetMask > 32) {
					if (colName.equalsIgnoreCase("srcIP"))
						output += "<p>Entered Subnetmask was invalid and Source IP therefore omitted.</p><br>";
					else if (colName.equalsIgnoreCase("dstIP"))
						output += "<p>Entered Subnetmask was invalid and Destination IP therefore omitted.</p><br>";
					return "";
				}
			} else {
				if (colName.equalsIgnoreCase("srcIP"))
					output += "<p>Entered Source IP was invalid and therefore omitted.</p><br>";
				else if (colName.equalsIgnoreCase("dstIP"))
					output += "<p>Entered Destination IP was invalid and therefore omitted.</p><br>";
				
				return "";
			}
		}
		
		try {
			ip = InetAddress.getByName(splitString[0]);
		} catch (UnknownHostException e) {
			if (colName.equalsIgnoreCase("srcIP"))
				output += "<p>Entered Source IP could not be resolved and was therefore omitted.</p><br>";
			else if (colName.equalsIgnoreCase("dstIP"))
				output += "<p>Entered Destination IP could not be resolved and was therefore omitted.</p><br>";
			return "";
		}
		
		ipAddr = (long)ByteBuffer.wrap(ip.getAddress()).getInt() & 0xffffffffL;
		
		if (subnetMask != 32) {
			lowerBound = ipAddr & (Long.MAX_VALUE-((long)Math.pow(2, 32-subnetMask)-1));
			upperBound = ipAddr | ((long)Math.pow(2, 32-subnetMask)-1);
			
			return colName + " BETWEEN " + lowerBound + " AND " + upperBound + " ";
		}
		
		return colName + "=" + ipAddr; 
	}

	protected String createPortQuery(String port, String colName, boolean aggregated) {
		
		String[] splitString;
		int[] portNumArray;
		int lowerBound = 0;
		int upperBound = 0;
		int portNum;
		
		if (port==null || colName==null || port.equalsIgnoreCase("")  || port.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=port.split("-")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					lowerBound = Integer.parseInt(splitString[0]);
					if (lowerBound > 65535) {
						if (colName.equalsIgnoreCase("srcPort"))
							output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
						if (colName.equalsIgnoreCase("dstPort"))
							output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
						
						return "";
					}

					upperBound = Integer.parseInt(splitString[1]);
					if (upperBound > 65535) {
						if (colName.equalsIgnoreCase("srcPort"))
							output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
						if (colName.equalsIgnoreCase("dstPort"))
							output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
						
						return "";
					}
				
				} catch (NumberFormatException e) {
					
					if (portMap != null) {
						portNumArray = (int[])portMap.get(port.toLowerCase());
						
						if (portNumArray != null)
							return colName + "=" + portNumArray[0];
						else {
							if (colName.equalsIgnoreCase("srcPort"))
								output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
							if (colName.equalsIgnoreCase("dstPort"))
								output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
							
							return "";
						}
					} else {
						if (colName.equalsIgnoreCase("srcPort"))
							output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
						if (colName.equalsIgnoreCase("dstPort"))
							output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
						
						return "";
					}
					
				}
				
				if (aggregated && (lowerBound >= 1024 || upperBound >= 1024)) {
					output += "<p>The requested data was already aggregated. Port Query was therefore omitted.</p><br>";
					return "";
				}
				
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				if (colName.equalsIgnoreCase("srcPort"))
					output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
				if (colName.equalsIgnoreCase("dstPort"))
					output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
				
				return "";
			}
		
		} else {
			
			try {
				portNum = Integer.parseInt(port);
			} catch (NumberFormatException e) {
				
				if (portMap != null) {
					portNumArray = (int[])portMap.get(port.toLowerCase());
					
					if (portNumArray != null)
						portNum = portNumArray[0];
					else {
						if (colName.equalsIgnoreCase("srcPort"))
							output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
						if (colName.equalsIgnoreCase("dstPort"))
							output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
						
						return "";
					}
				} else {
					if (colName.equalsIgnoreCase("srcPort"))
						output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
					if (colName.equalsIgnoreCase("dstPort"))
						output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
					
					return "";
				}
			}
			
			if (portNum > 65535) {
				if (colName.equalsIgnoreCase("srcPort"))
					output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
				if (colName.equalsIgnoreCase("dstPort"))
					output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
				
				return "";
			}
			
			if (aggregated && (portNum >= 1024)) {
				output += "<p>The requested data was already aggregated. Port Query was therefore omitted.</p><br>";
				return "";
			}
			
			return colName + "=" + portNum;	
		}
	}
	
	protected String createProtoQuery(String proto, String colName) {
		
		String[] splitString;
		short[] protoNumArray;
		int lowerBound = 0;
		int upperBound = 0;
		short protoNum;
		
		if (proto==null || colName==null || proto.equalsIgnoreCase("")  || proto.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=proto.split("-")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					lowerBound = Integer.parseInt(splitString[0]);
					
					if (lowerBound > 65535) {
						output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}

					upperBound = Integer.parseInt(splitString[1]);
					if (upperBound > 65535) {
						output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}
				
				} catch (NumberFormatException e) {
					
					if (protocolMap != null) {
						
						protoNumArray = (short[])protocolMap.get(proto.toLowerCase());
						
						if (protoNumArray != null)
							return colName + "=" + protoNumArray[0];
						else {
							output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
							return "";
						}
					} else {
						output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}
					
				}
				
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
				return "";
			}
		
		} else {
			
			try {
				protoNum = Short.parseShort(proto);
			} catch (NumberFormatException e) {
				
				if (protocolMap != null) {
					
					protoNumArray = (short[])protocolMap.get(proto.toLowerCase());
					
					if (protoNumArray != null)
						protoNum = protoNumArray[0];
					else {
						output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}
				} else {
					output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
					return "";
				}
			}
			
			if (protoNum > 256) {
				output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
				return "";
			}
			
			return colName + "=" + protoNum;	
		}
	}
	
	protected String createTosQuery(String tos, String colName) {
		
		String[] splitString;
		short lowerBound;
		short upperBound;
		short byteNum;
		
		if (tos==null || colName==null || tos.equalsIgnoreCase("") || tos.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=tos.split("-")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					lowerBound = Short.parseShort(splitString[0]);
					if (lowerBound > 255) {
						output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
						return "";
					}
				
					upperBound = Short.parseShort(splitString[1]);
					if (upperBound > 255) {
						output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
						return "";
					}
					
				} catch (NumberFormatException e) {
					output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
					return "incorrect";
				}
				
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
				return "";
			}
		
		} else {
			
			try {
				byteNum = Short.parseShort(tos);
			} catch (NumberFormatException e) {
				output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
				return "";
			}
			
			if (byteNum > 255) {
				output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
				return "";
			}
			
			return colName + "=" + tos;
		}
	}
	
	protected String createTimeQuery(HttpServletRequest request, long[] bounds) {
		
		String[] checkValues = request.getParameterValues("checks");
		boolean restrictTime = false;
		GregorianCalendar cal = new GregorianCalendar();
		long startTime;
		long endTime;
		
		if (checkValues == null)
			return "";
		
		for (int i=0; i<checkValues.length; i++) {
			if (checkValues[i].equalsIgnoreCase("restrictTime")) {
				restrictTime = true;
				break;
			}
		}
		
		if (restrictTime) {
				
			cal.set(Calendar.MILLISECOND, 0);
			
			cal.set(Integer.parseInt(request.getParameter("startYear")), Integer.parseInt(request.getParameter("startMonth"))-1,
					Integer.parseInt(request.getParameter("startDay")), Integer.parseInt(request.getParameter("startHour")),
					Integer.parseInt(request.getParameter("startMin")), 0);
			
			startTime = cal.getTimeInMillis();
			
			cal.set(Integer.parseInt(request.getParameter("endYear")), Integer.parseInt(request.getParameter("endMonth"))-1,
					Integer.parseInt(request.getParameter("endDay")), Integer.parseInt(request.getParameter("endHour")),
					Integer.parseInt(request.getParameter("endMin")), 0);
			
			endTime = cal.getTimeInMillis();	
			
			if (startTime < endTime) {
				
				bounds[0] = startTime;
				bounds[1] = endTime;
				
				return 	" firstSwitched BETWEEN " + startTime/1000 + " AND " + (endTime/1000-1); //endTime not included 
						//" AND lastSwitched BETWEEN " + startTime/1000 + " AND " + endTime/1000 + " ";
			}
			
			else {
				output += "<p>Entered time range was invalid and therefore omitted.</p><br>";
				return "";
			}
		}
		
		return "";
	}
	
	protected LinkedList getTables(Statement s, long[] bounds) {
		
		LinkedList tables = new LinkedList();
		ResultSet result;
		String tableName;
		String year;
		String month;
		String day;
		String hour = "0";
		String half = "0";
		String statement = "";
		long lowerBound;
		long upperBound;
		boolean hourTable = false;
		boolean dayTable = false;
		boolean weekTable = false;
		
		Calendar tableTime = new GregorianCalendar();
		
		if (bounds[0]==0 && bounds[1]==0) {	//standard 24h query
			
			Calendar upperTime = new GregorianCalendar();
			
			upperBound = upperTime.getTimeInMillis();
			lowerBound = upperBound - (24*60*60*1000+30*60*1000);
			
			statement = "SHOW TABLES LIKE 'h\\_%'";
			
		} else {
			
			lowerBound = bounds[0];
			upperBound = bounds[1];
			
			statement = "SHOW TABLES LIKE '_\\_%'";
		}
		
			
		try {
			result = s.executeQuery(statement);
			
			while (result.next()) {
				
				tableName = result.getString(1);
				String[] parts = tableName.split("_");
				year = parts[1].substring(0,4);
				month= parts[1].substring(4,6);
				day	 = parts[1].substring(6);
				
				if (parts[0].equalsIgnoreCase("h")) {
					hour = parts[2];
					half = parts[3];
					hourTable = true;
				} else if (parts[0].equalsIgnoreCase("d"))
					
					dayTable = true;
				
				else if (parts[0].equalsIgnoreCase("w"))
					
					weekTable = true;
				
				
				tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
						Integer.parseInt(day), Integer.parseInt(hour),
						Integer.parseInt(half)==0?0:30, 0);
				
				tableTime.set(Calendar.MILLISECOND, 0);
				
				long tabTime = tableTime.getTimeInMillis();
				
				if (hourTable) {
					if (tabTime <= upperBound && tabTime+30*60*1000 >= lowerBound)  {
						tables.add(tableName);
					}
				} else if (dayTable) {
					if (tabTime <= upperBound && tabTime+24*60*60*1000 >= lowerBound) {
						tables.add(tableName);
					}
				} else if (weekTable) {
					if (tabTime <= upperBound && tabTime+7*24*60*60*1000 >= lowerBound) {
						tables.add(tableName);
					}
				}
					
				hourTable = false;
				dayTable = false;
				weekTable = false;
				
			}
			
			
		} catch (SQLException e) {
			return null;
		}
		
		return tables;
	}
}