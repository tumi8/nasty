/*
 * Created on 03.09.2004
 */

package de.japes.servlets.nasty;

/**
 * @author unrza88
 */

import java.util.HashSet;
import javax.servlet.http.*;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Iterator;

public class HTMLOutputCreator extends OutputCreator {

	private static final String contentType = "text/html";
	private HashSet affectedTables = new HashSet();
	private boolean tosMissing = false;
	
	public String createOutput(HttpServletRequest request, HttpServletResponse response, Statement s) {
		
		int numRows = 0;
		int currOffset = 0;
		short outputLength = 0;
		String outputUnit;
		String bytes;
		long firstSwitched, lastSwitched;
		boolean resolveIP = false;
		boolean showSrcIP = false, showDstIP = false, showSrcPort = false, showDstPort  = false;
		boolean showProto = false, showTos = false, showPackets = false, showBytes = false;
		boolean showFirstSwitched = false, showLastSwitched = false, showDuration = false;
		boolean showExporter = false;
		boolean restrictTime = false;
		ResultSet result = null;
		
		output = "";
		
		//output += "Start: " + new GregorianCalendar().getTime() + "\n";
			
		String query; 
		
		try {
			
			s.execute("DROP TABLE IF EXISTS htmlTmp");
			
			s.execute("CREATE TEMPORARY TABLE htmlTmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
					"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, " +
					"proto TINYINT UNSIGNED, dstTos TINYINT UNSIGNED, " +
					"pkts BIGINT(20), bytes BIGINT(20) UNSIGNED, " +
					"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10)," +
					"exporterID INTEGER(10) UNSIGNED)");
			
			query = createSQLQuery(request, s);
			
			if (query == "") {
				output += "No valid query could be produced";
				return output;
			}
			
			result = s.executeQuery(query);
			
		} catch (SQLException e) {
			
			output += "<p>Error using DB connection.</p>";
			output += e.getMessage();
			return output;	
		}
		
		
		currOffset = Integer.parseInt(request.getParameter("offset"));
		outputLength=Short.parseShort(request.getParameter("outputLength"));
		outputUnit = request.getParameter("unit");
		
		String[] checkValues = request.getParameterValues("checks");
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMaximumFractionDigits(2);
		
		int i = 0;
		
		while (checkValues != null && i < checkValues.length) {

			if (checkValues[i].equalsIgnoreCase("resolveIP"))
				resolveIP = true;
			else if (checkValues[i].equalsIgnoreCase("showSrcIP"))
				showSrcIP = true;
			else if (checkValues[i].equalsIgnoreCase("showDstIP"))
				showDstIP = true;
			else if (checkValues[i].equalsIgnoreCase("showSrcPort"))
				showSrcPort = true;
			else if (checkValues[i].equalsIgnoreCase("showDstPort"))
				showDstPort = true;
			else if (checkValues[i].equalsIgnoreCase("showProto"))
				showProto = true;
			else if (checkValues[i].equalsIgnoreCase("showTos"))
				showTos = true;
			else if (checkValues[i].equalsIgnoreCase("showPackets"))
				showPackets = true;
			else if (checkValues[i].equalsIgnoreCase("showBytes"))
				showBytes = true;
			else if (checkValues[i].equalsIgnoreCase("showFirstSwitched"))
				showFirstSwitched = true;
			else if (checkValues[i].equalsIgnoreCase("showLastSwitched"))
				showLastSwitched = true;
			else if (checkValues[i].equalsIgnoreCase("showDuration"))
				showDuration = true;
			else if (checkValues[i].equalsIgnoreCase("showExporter"))
				showExporter = true;
			
			i++;
		}
		
		output += 	"<table border='3' frame='box'><tr><th>No.</th>" +
			(showSrcIP?"<th>Source IP</th>":"") +
			(showDstIP?"<th align='center'>Destination IP</th>":"") +
			(showSrcPort?"<th align='center'>Source Port</th>":"") +
			(showDstPort?"<th align='center'>Destination Port</th>":"") +
			(showProto?"<th align='center'>Protocol</th>":"") +
			(showTos&&!tosMissing?"<th align='center'>ToS</th>":"") +
			(showPackets?"<th align='center'>Packets</th>":"") +
			(showBytes?"<th align='center'>Bytes</th>":"") +
			(showFirstSwitched?"<th align='center'>First Switched</th>":"") +
			(showLastSwitched?"<th align='center'>Last Switched</th>":"") +
			(showDuration?"<th align='center'>Duration</th>":"") +
			(showExporter?"<th align='center'>Exporter</th></tr>":"");
		
		try {	
			while (result.next()) {
				
				firstSwitched=result.getLong("firstSwitched");
				lastSwitched=result.getLong("lastSwitched");
				
				if (outputUnit.equalsIgnoreCase("bytes"))
					bytes = Long.toString(result.getLong("bytes"));
				else if(outputUnit.equalsIgnoreCase("kilo"))
					bytes = nf.format((double)result.getLong("bytes")/1024) + " kB";
				else if (outputUnit.equalsIgnoreCase("mega"))
					bytes = nf.format((double)result.getLong("bytes")/1024/1024) + " MB";
				else
					bytes = "Error";
				
				numRows++;
				output += 	"<tr><td align='center'>" + (numRows+currOffset) + "</td>" +
				(showSrcIP?("<td>" + createIPOutput(result.getLong("srcIP"), resolveIP) + "</td>"):"") +
				(showDstIP?("<td>" + createIPOutput(result.getLong("dstIP"), resolveIP) + "</td>"):"") +
				(showSrcPort?("<td align='center'>" + createPortOutput(result.getInt("srcPort"), true) + "</td>"):"") +
				(showDstPort?("<td align='center'>" + createPortOutput(result.getInt("dstPort"), true) + "</td>"):"") +
				(showProto?("<td align='center'>" + createProtoOutput(result.getShort("proto"), true) + "</td>"):"") +
				(showTos&&!tosMissing?("<td align='center'>" + result.getShort("dstTos") + "</td>"):"") +
				(showPackets?("<td align='center'>" + result.getLong("pkts") + "</td>"):"") +
				(showBytes?("<td align='center'>" + bytes + "</td>"):"") +
				(showFirstSwitched?("<td align='center'>" + createTimeOutput(firstSwitched*1000) + "</td>"):"") +
				(showLastSwitched?("<td align='center'>" + createTimeOutput(lastSwitched*1000) + "</td>"):"") +
				(showDuration?("<td align='center'>" + (lastSwitched-firstSwitched) + "</td>"):"") +
				(showExporter?("<td align='center'>" + result.getInt("exporterID") + "</td>"):"") + "</tr>";
			}
		} catch (SQLException e) {
			
			output += "<p>Error using DB connection.</p>";
			output += e.getMessage();
		}
		
		output += ("</table><br>");
		
		output += "<table><tr>";
		
		if (currOffset > 0) { 
			output += "<td><form method=\"POST\" action=\"/nasty/GetResults\">" +
			"<input type=\"hidden\" name=\"srcIP\" value=\"" + request.getParameter("srcIP") + "\">" +
			"<input type=\"hidden\" name=\"dstIP\" value=\"" + request.getParameter("dstIP") + "\">" +
			"<input type=\"hidden\" name=\"srcPort\" value=\"" + request.getParameter("srcPort") + "\">" +
			"<input type=\"hidden\" name=\"dstPort\" value=\"" + request.getParameter("dstPort") + "\">" +
			"<input type=\"hidden\" name=\"proto\" value=\"" + request.getParameter("proto") + "\">" +
			"<input type=\"hidden\" name=\"tos\" value=\"" + request.getParameter("tos") + "\">" +
			"<input type=\"hidden\" name=\"order\" value=\"" + request.getParameter("order") + "\">" +
			"<input type=\"hidden\" name=\"sort\" value=\"" + request.getParameter("sort") + "\">" +
			"<input type=\"hidden\" name=\"offset\" value=\"" + (currOffset-outputLength) + "\">" +
			"<input type=\"hidden\" name=\"outputLength\" value=\"" + outputLength + "\">" +
			"<input type=\"hidden\" name=\"outputFormat\" value=\"html\">" +
			"<input type=\"hidden\" name=\"unit\" value=\"" + request.getParameter("unit") + "\">" +
			"<input type=\"hidden\" name=\"chartSelect\" value=\"" + request.getParameter("chartSelect") + "\">" +
			"<input type=\"hidden\" name=\"startDay\" value=\"" + request.getParameter("startDay") + "\">" +
			"<input type=\"hidden\" name=\"startMonth\" value=\"" + request.getParameter("startMonth") + "\">" +
			"<input type=\"hidden\" name=\"startYear\" value=\"" + request.getParameter("startYear") + "\">" +
			"<input type=\"hidden\" name=\"startHour\" value=\"" + request.getParameter("startHour") + "\">" +
			"<input type=\"hidden\" name=\"startMin\" value=\"" + request.getParameter("startMin") + "\">" +
			"<input type=\"hidden\" name=\"endDay\" value=\"" + request.getParameter("endDay") + "\">" +
			"<input type=\"hidden\" name=\"endMonth\" value=\"" + request.getParameter("endMonth") + "\">" +
			"<input type=\"hidden\" name=\"endYear\" value=\"" + request.getParameter("endYear") + "\">" +
			"<input type=\"hidden\" name=\"endHour\" value=\"" + request.getParameter("endHour") + "\">" +
			"<input type=\"hidden\" name=\"endMin\" value=\"" + request.getParameter("endMin") + "\">";
		
			i = 0;
			
			while (i < checkValues.length) {
				output += "<input type=\"hidden\" name=\"checks\" value=\"" + checkValues[i] + "\">";
				i++;
			}
				
			output += "<input type=\"hidden\" name=\"submitted\" value=\"true\">" +
			"<input type=\"submit\" value=\"Previous\">" +
			"</form></td>";
		}
		
		if (numRows >= outputLength) {
		
			output += "<td><form method=\"POST\" action=\"/nasty/GetResults\">" +
			"<input type=\"hidden\" name=\"srcIP\" value=\"" + request.getParameter("srcIP") + "\">" +
			"<input type=\"hidden\" name=\"dstIP\" value=\"" + request.getParameter("dstIP") + "\">" +
			"<input type=\"hidden\" name=\"srcPort\" value=\"" + request.getParameter("srcPort") + "\">" +
			"<input type=\"hidden\" name=\"dstPort\" value=\"" + request.getParameter("dstPort") + "\">" +
			"<input type=\"hidden\" name=\"proto\" value=\"" + request.getParameter("proto") + "\">" +
			"<input type=\"hidden\" name=\"tos\" value=\"" + request.getParameter("tos") + "\">" +
			"<input type=\"hidden\" name=\"order\" value=\"" + request.getParameter("order") + "\">" +
			"<input type=\"hidden\" name=\"sort\" value=\"" + request.getParameter("sort") + "\">" +
			"<input type=\"hidden\" name=\"offset\" value=\"" + (currOffset+outputLength) + "\">" +
			"<input type=\"hidden\" name=\"outputLength\" value=\"" + outputLength + "\">" +
			"<input type=\"hidden\" name=\"outputFormat\" value=\"html\">" +
			"<input type=\"hidden\" name=\"unit\" value=\"" + request.getParameter("unit") + "\">" +
			"<input type=\"hidden\" name=\"chartSelect\" value=\"" + request.getParameter("chartSelect") + "\">" +
			"<input type=\"hidden\" name=\"startDay\" value=\"" + request.getParameter("startDay") + "\">" +
			"<input type=\"hidden\" name=\"startMonth\" value=\"" + request.getParameter("startMonth") + "\">" +
			"<input type=\"hidden\" name=\"startYear\" value=\"" + request.getParameter("startYear") + "\">" +
			"<input type=\"hidden\" name=\"startHour\" value=\"" + request.getParameter("startHour") + "\">" +
			"<input type=\"hidden\" name=\"startMin\" value=\"" + request.getParameter("startMin") + "\">" +
			"<input type=\"hidden\" name=\"endDay\" value=\"" + request.getParameter("endDay") + "\">" +
			"<input type=\"hidden\" name=\"endMonth\" value=\"" + request.getParameter("endMonth") + "\">" +
			"<input type=\"hidden\" name=\"endYear\" value=\"" + request.getParameter("endYear") + "\">" +
			"<input type=\"hidden\" name=\"endHour\" value=\"" + request.getParameter("endHour") + "\">" +
			"<input type=\"hidden\" name=\"endMin\" value=\"" + request.getParameter("endMin") + "\">";

			i = 0;
			
			while (i < checkValues.length) {
				output += "<input type=\"hidden\" name=\"checks\" value=\"" + checkValues[i] + "\">";
				i++;
			}

			output += "<input type=\"hidden\" name=\"submitted\" value=\"true\">" +
			"<input type=\"submit\" value=\"Next\">" +
			"</form></td>";
		}
		
		output += "</tr></table>";
		
		//output += "End: " + new GregorianCalendar().getTime() + "\n";
		
		return output;
		
	}
	
	private String createSQLQuery(HttpServletRequest request, Statement s) {
		
		String statement = "";
		String srcIPQuery = "";
		String dstIPQuery = "";
		String srcPortQuery = "";
		String dstPortQuery = "";
		String protoQuery = "";
		String tosQuery = "";
		String timeQuery = "";
		boolean notFirst = false;
		String order = "";
		String selectedChart;
		long[] timeBounds = new long[2];

		boolean portsAggregated = false;
		boolean showTransit = false;
		
		timeQuery = createTimeQuery(request, timeBounds);
		
		LinkedList tables = getTables(s, timeBounds);
			
		Iterator it = tables.listIterator();
		
		while (it.hasNext()) {
			
			String currTable = (String)it.next();
			
			if (currTable.charAt(0) == 'w') {
				tosMissing = true;
				portsAggregated = true;
			} else if (currTable.charAt(0) == 'd') {
				tosMissing = true;
			}
		}
		
		srcIPQuery = createIPQuery(request.getParameter("srcIP"), "srcIP");
		
		dstIPQuery = createIPQuery(request.getParameter("dstIP"), "dstIP");
		
		srcPortQuery = createPortQuery(request.getParameter("srcPort"), "srcPort", portsAggregated);
		
		dstPortQuery = createPortQuery(request.getParameter("dstPort"), "dstPort", portsAggregated);
		
		protoQuery = createProtoQuery(request.getParameter("proto"), "proto");
		
		if (!tosMissing) {
			tosQuery = createTosQuery(request.getParameter("tos"), "dstTos");
		}
		
		if (request.getParameter("outputFormat").equalsIgnoreCase("transit"))
			showTransit = true;
	
		it = tables.listIterator();
		
		while (it.hasNext()) {
				
			statement = "INSERT INTO htmlTmp (";
			//statement += "SELECT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, firstSwitched, lastSwitched FROM ";
			if (tosMissing)
				statement += "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, 0, pkts, bytes, firstSwitched, lastSwitched, exporterID FROM ";
			else
				statement += "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, firstSwitched, lastSwitched, exporterID FROM ";
			
			statement += (String)it.next();
			
			if (srcIPQuery != "") {
				statement += " WHERE " + srcIPQuery;
				notFirst=true;
			} 
			if (dstIPQuery != "") {
				if (notFirst)
					statement += " AND ";
				else 
					statement += " WHERE ";
				statement += dstIPQuery;
				notFirst=true;
			}
			if (srcPortQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += srcPortQuery;
				notFirst=true;
			}
			if (dstPortQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += dstPortQuery;
				notFirst=true;
			}
			if (protoQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += protoQuery;
				notFirst=true;
			}
			if (tosQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += "WHERE ";
				statement += tosQuery;
				notFirst=true;
			}
			
			if (timeQuery != "") {
				if(notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += timeQuery;
				notFirst=true;
			}
			
			statement += ")";
					
			try {
				s.execute(statement);
			} catch (SQLException e) {
				//output += "Couldn't insert data into temporary table.";
				output += "Error: " + e.getMessage() + "\n";
				
			}
			
			notFirst=false;
		}
		
		if (statement == "") {
			output += "No data for given time range available.\n";
			return "";
		}
		
		if (tosMissing)
			statement = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, pkts, bytes, firstSwitched, lastSwitched, exporterID FROM htmlTmp ";
		else
			statement = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, firstSwitched, lastSwitched, exporterID FROM htmlTmp ";
		
		if (srcIPQuery != "") {
			statement += " WHERE " + srcIPQuery;
			notFirst=true;
		} 
		if (dstIPQuery != "") {
			if (notFirst)
				statement += " AND ";
			else 
				statement += " WHERE ";
			statement += dstIPQuery;
			notFirst=true;
		}
		if (srcPortQuery != "") {
			if (notFirst)
				statement += " AND ";
			else
				statement += " WHERE ";
			statement += srcPortQuery;
			notFirst=true;
		}
		if (dstPortQuery != "") {
			if (notFirst)
				statement += " AND ";
			else
				statement += " WHERE ";
			statement += dstPortQuery;
			notFirst=true;
		}
		if (protoQuery != "") {
			if (notFirst)
				statement += " AND ";
			else
				statement += " WHERE ";
			statement += protoQuery;
			notFirst=true;
		}
		if (tosQuery != "") {
			if (notFirst)
				statement += " AND ";
			else
				statement += "WHERE ";
			statement += tosQuery;
			notFirst=true;
		}
		
		if (timeQuery != "") {
			if(notFirst)
				statement += " AND ";
			else
				statement += " WHERE ";
			statement += timeQuery;
			notFirst=true;
		}
		
		if (showTransit)
			statement += " GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID";
		
		order = request.getParameter("order");
		
		if (!order.equalsIgnoreCase("none")) {
			if (order.equalsIgnoreCase("duration")) {
				statement += " ORDER BY (lastSwitched-firstSwitched) ";
			} else 
				statement += " ORDER BY " + order;
			if (request.getParameter("sort").equalsIgnoreCase("decrease"))
				statement += " DESC ";
		}
		
		statement += " LIMIT " + request.getParameter("offset") + ", " + request.getParameter("outputLength");
		
		return statement;	
	}
	
	public String getContentType() {
		return contentType;
	}
}
