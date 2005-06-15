/*
 * Created on 03.09.2004
 */

package de.japes.servlets.nasty;

/**
 * @author unrza88
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.http.*;

public class PerlOutputCreator extends OutputCreator {

	private static final String contentType = "text/plain";
	private boolean tosMissing = false;
	
	public String createOutput(HttpServletRequest request, HttpServletResponse response, Statement s) {
		
		String query = "";
		String tmpOutput = "";
		String limitString = "";
		ResultSet result = null;
		PrintWriter out = null;
		int lastValue = 0;
		
		boolean showSrcIP = false, showDstIP = false, showSrcPort = false, showDstPort  = false;
		boolean showProto = false, showTos = false, showPackets = false, showBytes = false;
		boolean showFirstSwitched = false, showLastSwitched = false, showDuration = false;
		boolean showExporter = false;
		boolean furtherResults = true;
		
		output = "";
		
		try {
			 out = response.getWriter();
		} catch (IOException e) {
			return "Error creating Perl output.";
		}
		
		out.println("# Start: " + new GregorianCalendar().getTime());
		out.println("# Output created by nasty");
		
		try {
			
			s.execute("DROP TABLE IF EXISTS perlTmp");
			
			s.execute("CREATE TEMPORARY TABLE perlTmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
					"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, " +
					"proto TINYINT UNSIGNED, dstTos TINYINT UNSIGNED, " +
					"pkts BIGINT(20), bytes BIGINT(20) UNSIGNED, " +
					"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10)," +
					"exporterID INTEGER(10) UNSIGNED)");
			
			query = createSQLQuery(request, s);
			
			if (query == "") {
				output += "No valid Query could be produced\n";
				return output;
			}
			
			//result = s.executeQuery(query);
			
		} catch (SQLException e) {
			
			output += "Error using DB connection.\n";
			output += e.getMessage();
			return output;	
		}
		
		String[] checkValues = request.getParameterValues("checks");
		
		int i = 0;
		
		while (checkValues != null && i < checkValues.length) {

			if (checkValues[i].equalsIgnoreCase("showSrcIP"))
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
		
		out.println("$nastyArray = (");
		
		try {
			i=0;
			
			limitString = " LIMIT 0, 1000";
			
			result = s.executeQuery(query + limitString);
			
			while (furtherResults) {
				
				while (result.next()) {
					
					out.print("[");
					tmpOutput = (showSrcIP?("\"" + createIPOutput(result.getLong("srcIP"), false) + "\", "):"") +
					(showDstIP?("\"" + createIPOutput(result.getLong("dstIP"), false) + "\", "):"") +
					(showSrcPort?("\"" + createPortOutput(result.getInt("srcPort"), false) + "\", "):"") +
					(showDstPort?("\"" + createPortOutput(result.getInt("dstPort"), false) + "\", "):"") +
					(showProto?("\"" + createProtoOutput(result.getShort("proto"), false) + "\", "):"") +
					(showTos&&!tosMissing?("\"" + result.getShort("dstTos") + "\", "):"") +
					(showPackets?("\"" + result.getLong("pkts") + "\", "):"") +
					(showBytes?("\"" + result.getLong("bytes") + "\", "):"") +
					(showFirstSwitched?("\"" + (result.getLong("firstSwitched")) + "\", "):"") +
					(showLastSwitched?("\"" + (result.getLong("lastSwitched")) + "\", "):"") +
					(showExporter?("\"" + result.getLong("exporterID") + "\""):"");
					
					if (tmpOutput.endsWith(", "))
						tmpOutput = tmpOutput.substring(0, tmpOutput.length()-2);
					
					out.print(tmpOutput);
					out.println("],");
					
					i++;
				}
				
				if ((i%1000)==0) {
					if (lastValue==i) {	//this is true if the number of results is exactly 1000, 2000, 3000...
						furtherResults = false;
						break;
					}
					lastValue = i;
					limitString = " LIMIT " + i + ", 1000";
					result = s.executeQuery(query + limitString);
				} else
					furtherResults = false;
			}
			
			out.print(")");
			
		} catch (SQLException e) {
			
			out.println("Error using DB connection.");
			out.println(e.getMessage());
		}
		
		out.println("# End: " + new GregorianCalendar().getTime());
		
		out.close();
		
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
		
		it = tables.listIterator();
		
		while (it.hasNext()) {
				
			statement = "INSERT INTO perlTmp (";
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
				output += "#Error filling temporary table perlTmp: " + e.getMessage() + "\n";
				
			}
			
			notFirst=false;
		}
		
		if (statement == "") {
			output += "No data for given time range available.\n";
			return "";
		}
		
		if (tosMissing)
			statement = "SELECT SQL_BIG_RESULT SQL_CACHE srcIP, dstIP, srcPort, dstPort, proto, pkts, bytes, firstSwitched, lastSwitched, exporterID FROM perlTmp ";
		else
			statement = "SELECT SQL_BIG_RESULT SQL_CACHE srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, firstSwitched, lastSwitched, exporterID FROM perlTmp ";
		
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
		
		order = request.getParameter("order");
		
		if (order != null && !order.equalsIgnoreCase("none")) {
			if (order.equalsIgnoreCase("duration")) {
				statement += " ORDER BY (lastSwitched-firstSwitched) ";
			} else 
				statement += " ORDER BY " + order;
			
			String sort = request.getParameter("sort");
			
			if (sort != null && sort.equalsIgnoreCase("decrease"))
				statement += " DESC ";
		}
		
		//statement += " LIMIT 1000";
		
		return statement;	
	}
	
	/*private String createSQLQuery(HttpServletRequest request) {
		
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
		
		srcIPQuery = createIPQuery(request.getParameter("srcIP"), "srcIP");
		// comparing string with == works because "incorrect" is a constant string
		if (srcIPQuery == "incorrect") {
			srcIPQuery = "";
			output += "# Entered Source IP was invalid and therefore omitted.\n";
		}
		
		dstIPQuery = createIPQuery(request.getParameter("dstIP"), "dstIP");
		if (dstIPQuery == "incorrect") {
			dstIPQuery = "";
			output += "# Entered Destination IP was invalid and therefore omitted.\n";
		}
		
		srcPortQuery = createPortQuery(request.getParameter("srcPort"), "srcPort");
		if (srcPortQuery == "incorrect") {
			srcPortQuery = "";
			output += "# Entered Source Port was invalid and therefore omitted.\n";
		}
		
		dstPortQuery = createPortQuery(request.getParameter("dstPort"), "dstPort");
		if (dstPortQuery == "incorrect") {
			dstPortQuery = "";
			output += "# Entered Destination Port was invalid and therefore omitted.\n";
		}
		
		protoQuery = createProtoQuery(request.getParameter("proto"), "proto");
		if (protoQuery == "incorrect") {
			protoQuery = "";
			output += "# Entered Protocol was invalid and therefore omitted.\n";
		}
		
		tosQuery = createTosQuery(request.getParameter("tos"), "dstTos");
		if (tosQuery == "incorrect") {
			tosQuery = "";
			output += "# Entered ToS was invalid and therefore omitted.\n";
		}
		
		timeQuery = createTimeQuery(request);
		if (timeQuery == "incorrect") {
			timeQuery = "";
			output += "# Entered time range was invalid and therefore omitted.\n";
		}
		
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
		
		order = request.getParameter("order");
		
		if (!order.equalsIgnoreCase("none")) {
			if (order.equalsIgnoreCase("duration")) {
				statement += " ORDER BY (lastSwitched-firstSwitched) ";
			} else 
				statement += " ORDER BY " + order;
			if (request.getParameter("sort").equalsIgnoreCase("decrease"))
				statement += " DESC ";
		}
		
		return statement;	
	}*/
		
	public String getContentType() {
		return contentType;
	}
}
