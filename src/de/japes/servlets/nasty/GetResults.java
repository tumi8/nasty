/*
 * Created on 30.08.2004
 */

package de.japes.servlets.nasty;

/**
 * @author unrza88
 */

import de.japes.beans.nasty.*;
import java.io.*;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.nio.ByteBuffer;
//import java.util.*;
import java.sql.*;
import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.util.Date;

public class GetResults extends HttpServlet {
	
	private ServletResults servResp = new ServletResults();
	private String srcIPQuery;
	private String dstIPQuery;
	private String srcPortQuery;
	private String dstPortQuery;
	private String protoQuery;
	private String tosQuery;
	private String timeQuery;
	private String output = "";
	private Date date = new Date();
	private HTMLOutputCreator htmlCreator = new HTMLOutputCreator();
	private PerlOutputCreator perlCreator = new PerlOutputCreator();
	private ChartOutputCreator chartCreator = new ChartOutputCreator();
	
	public void doPost(HttpServletRequest request,
					   HttpServletResponse response)
		throws ServletException, IOException {
		
		DataSource ds = null;
		String query = "";
		int numRows = 0;
		int currOffset = 0;
		short outputLength = 0;
		boolean htmlRequested = false;
		OutputCreator outputCreator;
		String format = "";
		
		if ((format=request.getParameter("outputFormat")).equalsIgnoreCase("html") ||
			 format.equalsIgnoreCase("transit")) {
			outputCreator = htmlCreator;
		} else if (format.equalsIgnoreCase("perl")) {
			outputCreator = perlCreator;
		} else if (format.equalsIgnoreCase("chart")) {
			outputCreator = chartCreator;
		} else {
			return;
		}
		
		output = "";
		
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");	
			ds = (DataSource)envCtx.lookup("jdbc/FlowDB");
		
		} catch (NamingException e) {
			
			response.setContentType("text/html");
			
			output += "<p>Error while trying to find DB connection pool.</p>";
			
			servResp.setQueryResult(output);
			
			request.setAttribute("result", servResp);
			
			this.getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
			
			return;
		}

		try {
			Connection conn = ds.getConnection();
			Statement s = conn.createStatement();
			
			output += outputCreator.createOutput(request, response, s);
			conn.close();
		
		} catch (SQLException e) {
			
			output += "<p>Error using DB connection.</p>";
			output += e.getMessage();
		} 
		
		response.setContentType(outputCreator.getContentType());
		
		if (format.equalsIgnoreCase("perl")) {
			
				if (output != "") {
					servResp.setQueryResult(output);
					request.setAttribute("result", servResp);
					
					this.getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);	
				}
					
		} else {
		
			servResp.setQueryResult(output);

			request.setAttribute("result", servResp);
		
			this.getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
		
		}
		
		return;
	}
	
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws IOException, ServletException {
		
		doPost(request, response);
	}
	
	
	/*private String createIPQuery(String addr, String colName) {
		
		InetAddress ip = null;
		short subnetMask = 32;
		String[] splitString;
		long ipAddr = 0;
		long lowerBound = 0;
		long upperBound = 0;
		
		if (addr.equalsIgnoreCase("") || addr.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=addr.split("/")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					subnetMask = Short.parseShort(splitString[1]);
				} catch (NumberFormatException e) {
					return "incorrect";
				}
				if (subnetMask > 32)
					return "incorrect";
			} else
				return "incorrect";
		}
		
		try {
			output += "<p>Address: " + splitString[0] + "\tSubnetMask: " + subnetMask + "</p>";
			ip = InetAddress.getByName(splitString[0]);
		} catch (UnknownHostException e) {
			return "incorrect";
		}
		
		ipAddr = (long)ByteBuffer.wrap(ip.getAddress()).getInt() & 0xffffffffL;
		
		if (subnetMask != 32) {
			lowerBound = ipAddr & (Long.MAX_VALUE-((long)Math.pow(2, 32-subnetMask)-1));
			upperBound = ipAddr | ((long)Math.pow(2, 32-subnetMask)-1);
			
			return colName + " BETWEEN " + lowerBound + " AND " + upperBound + " ";
		}
		
		return colName + "=" + ipAddr; 
	}

	private String createPortQuery(String port, String colName) {
		
		String[] splitString;
		int lowerBound = 0;
		int upperBound = 0;
		int portNum;
		boolean rangeGiven = false;
		
		if (port.equalsIgnoreCase("")  || port.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=port.split("-")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					lowerBound = Integer.parseInt(splitString[0]);
					if (lowerBound > 65535)
						return "incorrect";
				
					upperBound = Integer.parseInt(splitString[1]);
					if (upperBound > 65535)
						return "incorrect";
					
				} catch (NumberFormatException e) {
					return "incorrect";
				}
				
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else
				return "incorrect";
		
		} else {
			
			try {
				portNum = Integer.parseInt(port);
			} catch (NumberFormatException e) {
				return "incorrect";
			}
			
			if (portNum > 65535)
				return "incorrect";
			
			return colName + "=" + port;
		}
	}
	
	private String createByteValueQuery(String byteValue, String colName) {
		
		String[] splitString;
		short lowerBound;
		short upperBound;
		short byteNum;
		
		if (byteValue.equalsIgnoreCase("") || byteValue.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=byteValue.split("-")).length > 1) {
			
			if (splitString.length == 2) {
				try {
					lowerBound = Short.parseShort(splitString[0]);
					if (lowerBound > 255)
						return "incorrect";
				
					upperBound = Short.parseShort(splitString[1]);
					if (upperBound > 255)
						return "incorrect";
					
				} catch (NumberFormatException e) {
					return "incorrect";
				}
				
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else
				return "incorrect";
		
		} else {
			
			try {
				byteNum = Short.parseShort(byteValue);
			} catch (NumberFormatException e) {
				return "incorrect";
			}
			
			if (byteNum > 255)
				return "incorrect";
			
			return colName + "=" + byteValue;
		}
	}
	
	private String createSQLQuery(HttpServletRequest request, String format) {
		
		String statement = "";
		boolean notFirst = false;
		boolean htmlRequested = false;
		boolean perlRequested = false;
		boolean chartRequested = false;
		String order = "";
		String selectedChart;
		
		if (format.equalsIgnoreCase("html"))
			htmlRequested = true;
		else if (format.equalsIgnoreCase("perl"))
			perlRequested = true;
		else if (format.equalsIgnoreCase("chart"))
			chartRequested = true;
		
		srcIPQuery = createIPQuery(request.getParameter("srcIP"), "srcIP");
		// comparing string with == works because "incorrect" is a constant string
		if (srcIPQuery == "incorrect") {
			srcIPQuery = "";
			output += "<p>Entered Source IP was invalid and therefore omitted.</p><br>";
		}
		
		dstIPQuery = createIPQuery(request.getParameter("dstIP"), "dstIP");
		if (dstIPQuery == "incorrect") {
			dstIPQuery = "";
			output += "<p>Entered Destination IP was invalid and therefore omitted.</p><br>";
		}
		
		srcPortQuery = createPortQuery(request.getParameter("srcPort"), "srcPort");
		if (srcPortQuery == "incorrect") {
			srcPortQuery = "";
			output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
		}
		
		dstPortQuery = createPortQuery(request.getParameter("dstPort"), "dstPort");
		if (dstPortQuery == "incorrect") {
			dstPortQuery = "";
			output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
		}
		
		protoQuery = createByteValueQuery(request.getParameter("proto"), "proto");
		if (protoQuery == "incorrect") {
			protoQuery = "";
			output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
		}
		
		tosQuery = createByteValueQuery(request.getParameter("tos"), "dstTos");
		if (tosQuery == "incorrect") {
			tosQuery = "";
			output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
		}
		
		timeQuery = createTimeQuery(request);
		if (timeQuery == "incorrect") {
			timeQuery = "";
			output += "<p>Entered time range was invalid and therefore omitted.</p><br>";
		}
		
		if (srcIPQuery == "" && dstIPQuery == "" &&
			srcPortQuery == "" && dstPortQuery == "" &&
			protoQuery == "" && tosQuery == "" &&
			timeQuery == "")
			return "";
		
		if (chartRequested) {
			if ((selectedChart=request.getParameter("chartSelect")).equalsIgnoreCase("IP-Protocol Distribution"))
				statement = "SELECT proto, SUM(bytes) FROM flow WHERE ";
			else if (selectedChart.equalsIgnoreCase("Application Protocol Distribution"))
				statement = "SELECT srcPort as port, SUM(bytes) FROM flow WHERE srcPort between 1 and 1023 GROUP BY port UNION" +
							"SELECT dstPort as port, SUM(bytes) FROM flow WHERE dstPort between 1 and 1023 GROUP BY port";
				
		} else
			statement = "SELECT SQL_CACHE srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, firstSwitched, lastSwitched FROM flow " +
						"WHERE ";
		
		if (srcIPQuery != "") {
			statement += srcIPQuery;
			notFirst=true;
		} 
		if (dstIPQuery != "") {
			if (notFirst)
				statement += " AND ";
			statement += dstIPQuery;
			notFirst=true;
		}
		if (srcPortQuery != "") {
			if (notFirst)
				statement += " AND ";
			statement += srcPortQuery;
			notFirst=true;
		}
		if (dstPortQuery != "") {
			if (notFirst)
				statement += " AND ";
			statement += dstPortQuery;
			notFirst=true;
		}
		if (protoQuery != "") {
			if (notFirst)
				statement += " AND ";
			statement += protoQuery;
			notFirst=true;
		}
		if (tosQuery != "") {
			if (notFirst)
				statement += " AND ";
			statement += tosQuery;
			notFirst=true;
		}
		
		if (timeQuery != "") {
			if(notFirst)
				statement += " AND ";
			statement += timeQuery;
			notFirst=true;
		}
		
		if (chartRequested)
			statement += " GROUP BY proto ORDER BY proto";
		else {
			order = request.getParameter("order");
			
			if (!order.equalsIgnoreCase("none")) {
				if (order.equalsIgnoreCase("duration")) {
					statement += " ORDER BY (lastSwitched-firstSwitched) ";
				} else 
					statement += " ORDER BY " + order;
				if (request.getParameter("sort").equalsIgnoreCase("decrease"))
					statement += " DESC ";
			}
		}
		
		if (htmlRequested)
			statement += " LIMIT " + request.getParameter("offset") + ", " + request.getParameter("outputLength");
		
		return statement;	
	}
	
	private String createTimeQuery(HttpServletRequest request) {
		
		String[] checkValues = request.getParameterValues("checks");
		boolean restrictTime = false;
		GregorianCalendar cal = new GregorianCalendar();
		long startTime;
		long endTime;
		
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
			
			startTime = cal.getTimeInMillis()/1000;
			
			cal.set(Integer.parseInt(request.getParameter("endYear")), Integer.parseInt(request.getParameter("endMonth"))-1,
					Integer.parseInt(request.getParameter("endDay")), Integer.parseInt(request.getParameter("endHour")),
					Integer.parseInt(request.getParameter("endMin")), 0);
			
			endTime = cal.getTimeInMillis()/1000;	
			
			if (startTime < endTime)
				return 	" firstSwitched BETWEEN " + startTime + " AND " + endTime + 
						" AND lastSwitched BETWEEN " + startTime + " AND " + endTime + " ";
			
			else
				return "incorrect";
		}
		
		return "";
	}
	
	private String createTimeOutput(long time) {
		
		date.setTime(time);
		
		return date.toString();
	}*/
}
