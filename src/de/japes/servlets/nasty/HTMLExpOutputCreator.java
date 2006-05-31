/**
 * Title:   HTMLExpOutputCreator
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */
package de.japes.servlets.nasty;

import javax.servlet.http.*;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Queries given databases according to the parameters set by nasty's <code>index.jsp</code>
 * and puts the results, with the amount of bytes and packets for each flow split up to show
 * how much of it was recorded by each exporter, into an HTML table.
 */
public class HTMLExpOutputCreator extends OutputCreator {

	/** MIME-content of results is <code>text/html</code> */
        private static final String contentType = "text/html";
	
	/**
         * Class constructor, just calls the constructor of super class.
         *
         * @param request   Holds the parameters selected by the user in <code>index.jsp</code>.
         * @param response  Could be used to set response parameters or write output messages to.
         * @param s         JDBC-<code>Statement</code>-object for local database (for storing
         *                  temporary results).
         * @param dbList    List of JDBC-<code>Statement</code>-objects for the databases that should
         *                  be queried. (May include one for local database!)
         * @see   Statement
         * @see   OutputCreator
         */
        public HTMLExpOutputCreator(HttpServletRequest request, HttpServletResponse response, Statement s, ArrayList dbList) {
            super(request, response, s, dbList);
        }
        
        /**
         * Queries the given databases and writes the results formatted as an HTML-table
         * into a string.
         * 
         * @return  String that holds the HTML-table and/or eventual messages such as error messages.
         */
        public String createOutput() {
		// counter for the rows of the HTML-table
                int numRows = 0;
                int rowOffset = 0;
                // current offset of rows of the HTML-table within all the rows of the query results
		int currOffset = 0;
                // maximum number of rows to be shown in HTML-table
		short outputLength = 0;
                // number of (mega-/kilo)bytes and packets for current flow recorded by one exporter
		String bytes;
                String packets;
                // time bounds of current flow
		long firstSwitched, lastSwitched;
                // name of the temporary tables (one with, one without double entries)
                String tmpname = "";
                String tmpname2 = "";
                // the results of the query
		ResultSet result = null;
                // list of exporters
                ArrayList expIDList = new ArrayList();
                ArrayList expIPList = new ArrayList();
		
                // prepare flags and values for grouping
                if (grpSrcIP && grpDstIP && grpSrcPort && grpDstPort && grpProto && grpTos 
                        && grpTime && grpDuration && (grpSrcIPDiv==1) && (grpDstIPDiv==1) 
                        && (grpTimeDiv==1)) {
                    grpEverything = true;
                }
                grpDuration = false; grpBytes = false; grpPackets = false; grpDatabase = false;
                if (!grpSrcIP) grpSrcIPDiv = 1;
                if (!grpDstIP) grpDstIPDiv = 1;
                grpBytesDiv = 1;
                grpAnything = true;
                grpExporter = true;

                // ************* build the SQL-query ***************
                
                // prepare filling the temporary table
                tmpname = dq.getUniqueName("htmlTmp");
                String createStr = "CREATE TEMPORARY TABLE "+tmpname+" (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
				"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, " +
				"proto TINYINT UNSIGNED, dstTos TINYINT UNSIGNED, " +
				"pkts BIGINT(20), bytes BIGINT(20) UNSIGNED, " +
				"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10)," +
				"exporterID INTEGER(10) UNSIGNED, exporterIP INTEGER(10) UNSIGNED" + 
                                (!grpEverything?", grpcount INTEGER(10) UNSIGNED":"") +")";
                String fillStr = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, " +
                                "firstSwitched, lastSwitched, exporterID, exporterIP" + 
                                (!grpEverything?", count(*) AS grpcount":"") + " FROM #srctable# " +
                                "WHERE #params#";
                // add grouping parameters
                if (!grpEverything) {
                    fillStr += " GROUP BY ";
                    if (grpSrcIP) fillStr += "srcIP DIV "+grpSrcIPDiv+",";
                    else fillStr = fillStr.replaceFirst("srcIP", "MIN(srcIP) AS srcIP");
                    if (grpDstIP) fillStr += "dstIP DIV "+grpDstIPDiv+",";
                    else fillStr = fillStr.replaceFirst("dstIP", "MIN(dstIP) AS dstIP");
                    if (grpSrcPort) fillStr += "srcPort,";
                    else fillStr = fillStr.replaceFirst("srcPort", "MIN(srcPort) AS srcPort");
                    if (grpDstPort) fillStr += "dstPort,";
                    else fillStr = fillStr.replaceFirst("dstPort", "MIN(dstPort) AS dstPort");
                    if (grpProto) fillStr += "proto,";
                    else fillStr = fillStr.replaceFirst("proto", "MIN(proto) AS proto");
                    if (grpTos) fillStr += "dstTos,";
                    else fillStr = fillStr.replaceFirst("dstTos", "MIN(dstTos) AS dstTos");
                    fillStr = fillStr.replaceFirst("pkts", "SUM(pkts) AS pkts");
                    fillStr = fillStr.replaceFirst("bytes", "SUM(bytes) AS bytes");
                    if (grpTime) {
                        fillStr += "firstSwitched DIV "+grpTimeDiv+",";
                    }
                    else {
                        fillStr = fillStr.replaceFirst("firstSwitched", "MIN(firstSwitched) AS firstSwitched");
                    }
                    fillStr = fillStr.replaceFirst("lastSwitched", "MAX(lastSwitched) AS lastSwitched");
                    fillStr += "exporterID";
                }

                // create and fill the temporary table
                if (remDoubles) {
                    if (dq.fillTable(tmpname, null, null, false, false, 1)==false) {
                        output += dq.getOutput();
                        return "Error creating and filling temporary table! <p>"+output;
                    }
                } else {
                    if (dq.fillTable(tmpname, createStr, fillStr, remDoubles, remExporterID, remTimeDiv)==false) {
                        output += dq.getOutput();
                        return "Error creating and filling temporary table! <p>"+output;
                    }
                }
	        
                // ************ PERFORMANCE WATCH ***************
                long perfWatch = System.currentTimeMillis();
                // **********************************************
                
                if (remDoubles) {  
                    // remove identical entries by grouping the temporary table into a new temp. table
                    tmpname2 = dq.getUniqueName("htmlTmpNoDoubles");
                    dq.removeIdenticalData(tmpname, tmpname2, remTimeDiv, remExporterID, true);
                    
                    // count amount of removed entries
                    int x=0; int y=0;
                    result = dq.queryTempDB("SELECT count(*) AS x FROM "+tmpname);
                    try {
                        result.next();
                        x = result.getInt("x");
                    } catch (Exception e) {
                        // an error has occured
                        dq.dropTable(tmpname2);
                        dq.dropTable(tmpname);
                        output += dq.getOutput();
                        return "Error removing double entries from temporary table! <p>"+output;
                    }
                    result = dq.queryTempDB("SELECT count(*) AS y FROM "+tmpname2);
                    try {
                        result.next();
                        y = result.getInt("y");
                    } catch (Exception e) {
                        // an error has occured
                        dq.dropTable(tmpname2);
                        dq.dropTable(tmpname);
                        output += dq.getOutput();
                        return "Error removing double entries from temporary table! <p>"+output;
                    }
                    if (x>0) output += "<b>Removed "+(x-y)+" of "+x+" entries as identical ("+(((x-y)*100)/x)+"%)!</b><p>";
               } else {
                    // no removal of identical entries, just copy the name of the temp. table
                    tmpname2 = tmpname;
                }
                  
                // read list of exporters from temporary table
                String queryTmp = "SELECT exporterID, exporterIP FROM "+tmpname2+" GROUP BY exporterID ORDER BY exporterID";
                result = dq.queryTempDB(queryTmp);
                try {
                    while (result.next()) {
                        expIDList.add(new Long(result.getLong("exporterID")));
                        expIPList.add(new Long(result.getLong("exporterIP")));
                    }
                } catch (Exception e) {
                    // an error has occured
                    dq.dropTable(tmpname2);
                    dq.dropTable(tmpname);
                    output += dq.getOutput();
                    return "Error retrieving exporter data! <p>"+output;
                }

                // prepare querying the temporary table
                queryTmp = "SELECT DISTINCT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, " +
                                        "firstSwitched, lastSwitched, exporterID FROM "+tmpname2;
                // add grouping parameters
                if (!grpEverything) {
                    if (remDoubles) queryTmp = queryTmp.replaceFirst(" FROM", ",count(*) AS grpcount FROM");
                    else queryTmp = queryTmp.replaceFirst(" FROM", ",SUM(grpcount) AS grpcount FROM");
                    queryTmp += " GROUP BY ";
                    if (grpSrcIP) queryTmp += "srcIP DIV "+grpSrcIPDiv+",";
                    else queryTmp = queryTmp.replaceFirst("srcIP", "MIN(srcIP) AS srcIP");
                    if (grpDstIP) queryTmp += "dstIP DIV "+grpDstIPDiv+",";
                    else queryTmp = queryTmp.replaceFirst("dstIP", "MIN(dstIP) AS dstIP");
                    if (grpSrcPort) queryTmp += "srcPort,";
                    else queryTmp = queryTmp.replaceFirst("srcPort", "MIN(srcPort) AS srcPort");
                    if (grpDstPort) queryTmp += "dstPort,";
                    else queryTmp = queryTmp.replaceFirst("dstPort", "MIN(dstPort) AS dstPort");
                    if (grpProto) queryTmp += "proto,";
                    else queryTmp = queryTmp.replaceFirst("proto", "MIN(proto) AS proto");
                    if (grpTos) queryTmp += "dstTos,";
                    else queryTmp = queryTmp.replaceFirst("dstTos", "MIN(dstTos) AS dstTos");
                    queryTmp = queryTmp.replaceFirst("pkts", "SUM(pkts) AS pkts");
                    queryTmp = queryTmp.replaceFirst("bytes", "SUM(bytes) AS bytes");
                    if (grpTime) {
                        queryTmp = queryTmp.replaceFirst("firstSwitched", "(firstSwitched DIV "+grpTimeDiv+")*"+grpTimeDiv+" AS firstSwitched");
                        queryTmp += "firstSwitched DIV "+grpTimeDiv+",";
                        queryTmp = queryTmp.replaceFirst("lastSwitched", "(MAX(lastSwitched) DIV "+grpTimeDiv+")*"+grpTimeDiv+" AS lastSwitched");
                    }
                    else {
                        queryTmp = queryTmp.replaceFirst("firstSwitched", "MIN(firstSwitched) AS firstSwitched");
                        queryTmp = queryTmp.replaceFirst("lastSwitched", "MAX(lastSwitched) AS lastSwitched");
                    }
                    queryTmp += "exporterID";
                }

                // add ordering parameters
                queryTmp = "SELECT SQL_BIG_RESULT * FROM (" + queryTmp + ") AS x ";
		queryTmp += " ORDER BY ";
                if (!order.equalsIgnoreCase("none") && !order.equalsIgnoreCase("bytes")
                   && !order.equalsIgnoreCase("pkts") && !order.equalsIgnoreCase("duration")) {
			queryTmp += order;
                        if (sort.equalsIgnoreCase("decrease"))
                            queryTmp += " DESC";
                        queryTmp += ",";
		}
                if (grpSrcIP) queryTmp += "srcIP DIV "+grpSrcIPDiv+",";
                else          queryTmp += "srcIP,";
                if (grpDstIP) queryTmp += "dstIP DIV "+grpDstIPDiv+",";
                else          queryTmp += "dstIP,";
                queryTmp += "srcPort,dstPort,proto,dstTos,firstSwitched,lastSwitched,exporterID";
                // add limit of rows to retrieve
                if (request.getParameter("rowOffset")==null) rowOffset = 0;
                else rowOffset = Integer.parseInt(request.getParameter("rowOffset"));
                numRows = rowOffset;
                currOffset = Integer.parseInt(request.getParameter("offset"));
		outputLength=Short.parseShort(request.getParameter("outputLength"));
		queryTmp += " LIMIT " + currOffset + ", " + (outputLength*expIDList.size());
                
                // execute the query
                result = dq.queryTempDB(queryTmp);
                if (result==null) {
                    // no result, so an error has occured
                    dq.dropTable(tmpname2);
                    dq.dropTable(tmpname);
                    output += dq.getOutput();
                    return "Error querying databases! <p>"+output;
                }

		// ************ construct HTML-table from the results of the query *********
		
		// prepare to format the byte value
                NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(2);
		
		// construct head of HTML-table
                output += 	"<table border='3' frame='box'><tr><th rowspan=\"2\">No.</th>" +
			(showSrcIP?"<th rowspan=\"2\">Source IP</th>":"") +
			(showDstIP?"<th rowspan=\"2\" align='center'>Destination IP</th>":"") +
			(showSrcPort?"<th rowspan=\"2\" align='center'>Source Port</th>":"") +
			(showDstPort?"<th rowspan=\"2\" align='center'>Destination Port</th>":"") +
			(showProto?"<th rowspan=\"2\" align='center'>Protocol</th>":"") +
			(showTos&&!dq.isTosMissing()?"<th rowspan=\"2\" align='center'>ToS</th>":"") +
			(showFirstSwitched?"<th rowspan=\"2\" align='center'>First Switched</th>":"") +
			(showLastSwitched?"<th rowspan=\"2\" align='center'>Last Switched</th>":"");
		for (int i=0; i<expIDList.size(); i++) {
                    output += "<th colspan=\"2\" align='center'>Exp. " + (Long)expIDList.get(i) 
                            + " (" + dq.createIPOutput(((Long)expIPList.get(i)).longValue(),false) + ")</th>";
                }
                output += "<tr>";
		for (int i=0; i<expIDList.size(); i++) {
                    output += "<th>Packets</th><th>Bytes</th>";
                }
                output += "</tr>";
                if (grpAnything) {
                    if (!grpSrcIP) output = output.replaceFirst("Source IP", "Min SrcIP");
                    if (!grpDstIP) output = output.replaceFirst("Destination IP", "Min DstIP");
                    if (!grpSrcPort) output = output.replaceFirst("Source Port", "Min SrcPort");
                    if (!grpDstPort) output = output.replaceFirst("Destination Port", "Min DstPort");
                    if (!grpProto) output = output.replaceFirst("Protocol", "Min Proto");
                    if (!grpTos) output = output.replaceFirst("ToS", "Min ToS");
                    if (!grpTime) output = output.replaceFirst("First Switched", "Min First Switched");
                    if (!grpTime) output = output.replaceFirst("Last Switched", "Max Last Switched");
                }
		
		// fill the rows of the HTML-table
                int dbRowCount = 0;
                try {	
			while (result.next() && (numRows-rowOffset<outputLength)) {
				// check time bounds of current flow
				firstSwitched=result.getLong("firstSwitched");
				lastSwitched=result.getLong("lastSwitched");
                                if (grpTime) lastSwitched=(lastSwitched+grpTimeDiv);
				
				numRows++;
                                
                                // construct the row, start with common values of flow
				output += 	"<tr><td align='center'>" + numRows + "</td>" +
				(showSrcIP?("<td>" + dq.createIPOutput((result.getLong("srcIP")/grpSrcIPDiv)*grpSrcIPDiv, resolveIP) + "</td>"):"") +
				(showDstIP?("<td>" + dq.createIPOutput((result.getLong("dstIP")/grpDstIPDiv)*grpDstIPDiv, resolveIP) + "</td>"):"") +
				(showSrcPort?("<td align='center'>" + dq.createPortOutput(result.getInt("srcPort"), true) + "</td>"):"") +
				(showDstPort?("<td align='center'>" + dq.createPortOutput(result.getInt("dstPort"), true) + "</td>"):"") +
				(showProto?("<td align='center'>" + dq.createProtoOutput(result.getShort("proto"), true) + "</td>"):"") +
				(showTos&&!dq.isTosMissing()?("<td align='center'>" + result.getShort("dstTos") + "</td>"):"") +
				(showFirstSwitched?("<td align='center'>" + dq.createTimeOutput(firstSwitched*1000) + "</td>"):"") +
				(showLastSwitched?("<td align='center'>" + dq.createTimeOutput(lastSwitched*1000) + "</td>"):"");
				// add data for each exporter
                                for (int i=0; i<expIDList.size(); i++) {
                                    if (((Long)expIDList.get(i)).longValue() == result.getLong("exporterID")) {
                                        // get and format the number of bytes and packets for this exporter
                			if (outputUnit.equalsIgnoreCase("bytes"))
                        			bytes = Long.toString(result.getLong("bytes"));
                                	else if(outputUnit.equalsIgnoreCase("kilo"))
                                        	bytes = nf.format((double)(result.getLong("bytes")/1024)) + " kB";
                                        else if (outputUnit.equalsIgnoreCase("mega"))
                                		bytes = nf.format((double)(result.getLong("bytes")/1024/1024)) + " MB";
                                	else
                                        	bytes = "Error";
                                        packets = ""+result.getLong("pkts");
                                        dbRowCount++;
                                    } else {
                                        // no data recorded for this flow by this exporter
                                        bytes = "";
                                        packets = "";
                                        result.previous();
                                    }
                                    output += "<td align='center'>" + packets + "</td>" +
                                              "<td align='center'>" + bytes + "</td>";
                                    if (i<expIDList.size()-1) result.next();
                                }
                                output += "</tr>";
			}
                        
                        // drop the temporary tables (has to be done especially for mysqldump-version of program!)
                        dq.dropTable(tmpname2);
                        dq.dropTable(tmpname);
                } catch (SQLException e) {
			// error
			output += "<p>Error using DB connection.</p>";
			output += e.getMessage();
                        dq.dropTable(tmpname2);
                        dq.dropTable(tmpname);
		}
		// end of table
		output += ("</table><br>");

                // ********* construct "previous" button if appropriate

                output += "<table><tr>";
		// "next" button
                if (numRows >= outputLength) {
			// all currently selected parameters must be kept when user presses button
			output += "<td><form method=\"POST\" action=\"/nasty/GetResults\">" +
			"<input type=\"hidden\" name=\"srcIP\" value=\"" + request.getParameter("srcIP") + "\">" +
			"<input type=\"hidden\" name=\"dstIP\" value=\"" + request.getParameter("dstIP") + "\">" +
			"<input type=\"hidden\" name=\"srcPort\" value=\"" + request.getParameter("srcPort") + "\">" +
			"<input type=\"hidden\" name=\"dstPort\" value=\"" + request.getParameter("dstPort") + "\">" +
			"<input type=\"hidden\" name=\"proto\" value=\"" + request.getParameter("proto") + "\">" +
			"<input type=\"hidden\" name=\"tos\" value=\"" + request.getParameter("tos") + "\">" +
			"<input type=\"hidden\" name=\"exporterID\" value=\"" + request.getParameter("exporterID") + "\">" +
			"<input type=\"hidden\" name=\"remTimeDiv\" value=\"" + request.getParameter("remTimeDiv") + "\">" +
			"<input type=\"hidden\" name=\"grpSrcIPDiv\" value=\"" + request.getParameter("grpSrcIPDiv") + "\">" +
			"<input type=\"hidden\" name=\"grpDstIPDiv\" value=\"" + request.getParameter("grpDstIPDiv") + "\">" +
			"<input type=\"hidden\" name=\"grpBytesDiv\" value=\"" + request.getParameter("grpBytesDiv") + "\">" +
			"<input type=\"hidden\" name=\"grpTimeDiv\" value=\"" + request.getParameter("grpTimeDiv") + "\">" +
			"<input type=\"hidden\" name=\"order\" value=\"" + request.getParameter("order") + "\">";
                        String[] dbNames = request.getParameterValues("dbSelects");
                        for (int j=0; j<dbNames.length; j++) {
                            output += "<input type=\"hidden\" name=\"dbSelects\" value=\"" + dbNames[j] + "\">";
                        }
                        output += "<input type=\"hidden\" name=\"dqSelect\" value=\"" + request.getParameter("dqSelect") + "\">" +
			"<input type=\"hidden\" name=\"sort\" value=\"" + request.getParameter("sort") + "\">" +
			"<input type=\"hidden\" name=\"rowOffset\" value=\"" + numRows + "\">" +
			"<input type=\"hidden\" name=\"offset\" value=\"" + (currOffset+dbRowCount) + "\">" +
			"<input type=\"hidden\" name=\"outputLength\" value=\"" + outputLength + "\">" +
			"<input type=\"hidden\" name=\"outputFormat\" value=\"" + request.getParameter("outputFormat") + "\">" +
			"<input type=\"hidden\" name=\"search\" value=\"" + request.getParameter("search") + "\">" +
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
			int i = 0;
                        String[] groupValues = request.getParameterValues("group");
                        if (groupValues!=null) { 
                            while (i < groupValues.length) {
				output += "<input type=\"hidden\" name=\"group\" value=\"" + groupValues[i] + "\">";
				i++;
                            }
                        }
			i = 0;
                        String[] checkValues = request.getParameterValues("checks");
                        if (checkValues!=null) { 
                            while (i < checkValues.length) {
				output += "<input type=\"hidden\" name=\"checks\" value=\"" + checkValues[i] + "\">";
				i++;
                            }
                        }
			output += "<input type=\"hidden\" name=\"submitted\" value=\"true\">" +
			"<input type=\"submit\" value=\"Next\">" +
			"</form></td>";
		}
		output += "</tr></table>";
		
                // ************ finished, return the constructed output **************
                
                output += dq.getOutput();

                // ************ PERFORMANCE WATCH ***************
                output += "<p>Time to rearrange data and query temp. tables: " + (System.currentTimeMillis()-perfWatch) + " ms<p>";
                // **********************************************
		
                return output;
		
	}	
	
	/**
         * Returns the MIME-type of the output produced by this class.
         *
         * @return  String that holds the MIME-type of the output produced by this class.
         */
        public String getContentType() {
		return contentType;
	}
}
