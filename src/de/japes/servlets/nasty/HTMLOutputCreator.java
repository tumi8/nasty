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

/**
 * Title:   HTMLOutputCreator
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
 * and puts the results into an HTML table.
 */
public class HTMLOutputCreator extends OutputCreator {

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
        public HTMLOutputCreator(HttpServletRequest request, HttpServletResponse response, Statement s, ArrayList dbList) {
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
                // current offset of rows of the HTML-table within all the rows of the query results
		int currOffset = 0;
                // maximum number of rows to be shown in HTML-table
		short outputLength = 0;
                // number of (mega-/kilo)bytes in current flow
		String bytes;
                // time bounds of current flow
		long firstSwitched, lastSwitched;
                // name of the temporary tables (one with, one without double entries)
                String tmpname = "";
                String tmpname2 = "";
                // the results of the query
		ResultSet result = null;
		
                if (!grpSrcIP) grpSrcIPDiv = 1;
                if (!grpDstIP) grpDstIPDiv = 1;
                if (!grpBytes) grpBytesDiv = 1;

                // ************* build the SQL-query ***************
                
                // prepare filling the temporary table
                tmpname = dq.getUniqueName("htmlTmp");
                String createStr = "CREATE TEMPORARY TABLE "+tmpname+" (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
				"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, " +
				"proto TINYINT UNSIGNED, dstTos TINYINT UNSIGNED, " +
				"pkts BIGINT(20), bytes BIGINT(20) UNSIGNED, " +
				"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10)," +
				"exporterID INTEGER(10) UNSIGNED, exporterIP INTEGER(10) UNSIGNED," +
                                "databaseID SMALLINT(5) UNSIGNED"+(!grpEverything?", grpcount INTEGER(10) UNSIGNED)":")");
                String fillStr = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, " +
                                "firstSwitched, lastSwitched, exporterID, exporterIP, databaseID" + 
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
                    if (grpPackets) fillStr += "pkts,";
                    else fillStr = fillStr.replaceFirst("pkts", "SUM(pkts) AS pkts");
                    if (grpBytes) fillStr += "bytes DIV "+grpBytesDiv+",";
                    else fillStr = fillStr.replaceFirst("bytes", "SUM(bytes) AS bytes");
                    if (grpTime) {
                        fillStr += "firstSwitched DIV "+grpTimeDiv+",";
                    }
                    else {
                        fillStr = fillStr.replaceFirst("firstSwitched", "MIN(firstSwitched) AS firstSwitched");
                    }
                    fillStr = fillStr.replaceFirst("lastSwitched", "MAX(lastSwitched) AS lastSwitched");
                    if (grpDuration) fillStr += "lastSwitched-firstSwitched,";
                    if (grpExporter) fillStr += "exporterID,";
                    else fillStr = fillStr.replaceFirst("exporterID", "MIN(exporterID) AS exporterID");
                    if (fillStr.endsWith(",")) fillStr = fillStr.substring(0, fillStr.length()-1);
                    else if (fillStr.endsWith("GROUP BY "))  fillStr += "NULL";
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
                  
                // prepare querying the temporary table
                String queryTmp = "SELECT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, " +
                                        "firstSwitched, lastSwitched, exporterID, exporterIP, databaseID FROM "+tmpname2;
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
                    if (grpPackets) queryTmp += "pkts,";
                    else queryTmp = queryTmp.replaceFirst("pkts", "SUM(pkts) AS pkts");
                    if (grpBytes) queryTmp += "bytes DIV "+grpBytesDiv+",";
                    else queryTmp = queryTmp.replaceFirst("bytes", "SUM(bytes) AS bytes");
                    if (grpTime) {
                        queryTmp = queryTmp.replaceFirst("firstSwitched", "(firstSwitched DIV "+grpTimeDiv+")*"+grpTimeDiv+" AS firstSwitched");
                        queryTmp += "firstSwitched DIV "+grpTimeDiv+",";
                        queryTmp = queryTmp.replaceFirst("lastSwitched", "(MAX(lastSwitched) DIV "+grpTimeDiv+")*"+grpTimeDiv+" AS lastSwitched");
                    }
                    else {
                        queryTmp = queryTmp.replaceFirst("firstSwitched", "MIN(firstSwitched) AS firstSwitched");
                        queryTmp = queryTmp.replaceFirst("lastSwitched", "MAX(lastSwitched) AS lastSwitched");
                    }
                    if (grpDuration) queryTmp += "((lastSwitched DIV "+grpTimeDiv+")-(firstSwitched DIV "+grpTimeDiv+")),";
                    if (grpExporter) queryTmp += "exporterID,";
                    else queryTmp = queryTmp.replaceFirst("exporterID", "MIN(exporterID) AS exporterID");
                    if (grpDatabase) queryTmp += "databaseID,";
                    else queryTmp = queryTmp.replaceFirst("databaseID", "MIN(databaseID) AS databaseID");
                    if (grpAnything) queryTmp = queryTmp.substring(0, queryTmp.length()-1);
                    else queryTmp += "NULL";
                }
         
                // add ordering parameters
                queryTmp = "SELECT SQL_BIG_RESULT * FROM (" + queryTmp + ") AS x ";
		if (!order.equalsIgnoreCase("none")) {
			if (order.equalsIgnoreCase("duration")) {
                                queryTmp += " ORDER BY (lastSwitched-firstSwitched) ";
			} else 
				queryTmp += " ORDER BY " + order;
			if (sort.equalsIgnoreCase("decrease"))
				queryTmp += " DESC ";
		} else if (!grpEverything) {
                    queryTmp += " ORDER BY grpcount ";
                    if (sort.equalsIgnoreCase("decrease"))
                        queryTmp += " DESC ";
                }
                // add limit of rows to retrieve
		queryTmp += " LIMIT " + request.getParameter("offset") + ", " + request.getParameter("outputLength");
                
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
		
                // check current offset within the query-results and the maximum output length
                currOffset = Integer.parseInt(request.getParameter("offset"));
		outputLength=Short.parseShort(request.getParameter("outputLength"));
		// prepare to format the byte value
                NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(2);
		
		// construct head of HTML-table
                output += 	"<table border='3' frame='box'><tr><th>No.</th>" +
			(showSrcIP?"<th>Source IP</th>":"") +
			(showDstIP?"<th align='center'>Destination IP</th>":"") +
			(showSrcPort?"<th align='center'>Source Port</th>":"") +
			(showDstPort?"<th align='center'>Destination Port</th>":"") +
			(showProto?"<th align='center'>Protocol</th>":"") +
			(showTos&&!dq.isTosMissing()?"<th align='center'>ToS</th>":"") +
			(showPackets?"<th align='center'>Packets</th>":"") +
			(showBytes?"<th align='center'>Bytes</th>":"") +
			(showFirstSwitched?"<th align='center'>First Switched</th>":"") +
			(showLastSwitched?"<th align='center'>Last Switched</th>":"") +
			(showDuration?"<th align='center'>Duration</th>":"") +
			(showExporter?"<th align='center'>Exporter</th>":"") +
			(showDatabase?"<th align='center'>Database</th>":"") +
                        (!grpEverything?"<th align='center'># Grouped</th>":"") + "</tr>";
                if (!grpSrcIP) output = output.replaceFirst("Source IP", "Min SrcIP");
                if (!grpDstIP) output = output.replaceFirst("Destination IP", "Min DstIP");
                if (!grpSrcPort) output = output.replaceFirst("Source Port", "Min SrcPort");
                if (!grpDstPort) output = output.replaceFirst("Destination Port", "Min DstPort");
                if (!grpProto) output = output.replaceFirst("Protocol", "Min Proto");
                if (!grpTos) output = output.replaceFirst("ToS", "Min ToS");
                if (!grpPackets) output = output.replaceFirst("Packets", "Sum Packets");
                if (!grpBytes) output = output.replaceFirst("Bytes", "Sum Bytes");
                if (!grpTime) output = output.replaceFirst("First Switched", "Min First Switched");
                if (!grpTime) output = output.replaceFirst("Last Switched", "Max Last Switched");
                if (!grpExporter) output = output.replaceFirst("Exporter", "Min Exporter");
                if (!grpDatabase) output = output.replaceFirst("Database", "Min Database");
		
		// fill the rows of the HTML-table
                try {	
			while (result.next()) {
				// check time bounds of current flow
				firstSwitched=result.getLong("firstSwitched");
                                if (grpTime) firstSwitched=(firstSwitched/grpTimeDiv)*grpTimeDiv;
				lastSwitched=result.getLong("lastSwitched");
                                if (grpTime) lastSwitched=((lastSwitched+grpTimeDiv)/grpTimeDiv)*grpTimeDiv;
                                // format the number of bytes of current flow as selected
				if (outputUnit.equalsIgnoreCase("bytes"))
					bytes = Long.toString((result.getLong("bytes")/grpBytesDiv)*grpBytesDiv);
				else if(outputUnit.equalsIgnoreCase("kilo"))
					bytes = nf.format((double)((result.getLong("bytes")/grpBytesDiv)*grpBytesDiv)/1024) + " kB";
				else if (outputUnit.equalsIgnoreCase("mega"))
					bytes = nf.format((double)((result.getLong("bytes")/grpBytesDiv)*grpBytesDiv)/1024/1024) + " MB";
				else
					bytes = "Error";
				
				numRows++;
                                
                                // construct the row
				output += 	"<tr><td align='center'>" + (numRows+currOffset) + "</td>" +
				(showSrcIP?("<td>" + dq.createIPOutput((result.getLong("srcIP")/grpSrcIPDiv)*grpSrcIPDiv, resolveIP) + "</td>"):"") +
				(showDstIP?("<td>" + dq.createIPOutput((result.getLong("dstIP")/grpDstIPDiv)*grpDstIPDiv, resolveIP) + "</td>"):"") +
				(showSrcPort?("<td align='center'>" + dq.createPortOutput(result.getInt("srcPort"), true) + "</td>"):"") +
				(showDstPort?("<td align='center'>" + dq.createPortOutput(result.getInt("dstPort"), true) + "</td>"):"") +
				(showProto?("<td align='center'>" + dq.createProtoOutput(result.getShort("proto"), true) + "</td>"):"") +
				(showTos&&!dq.isTosMissing()?("<td align='center'>" + result.getShort("dstTos") + "</td>"):"") +
				(showPackets?("<td align='center'>" + result.getLong("pkts") + "</td>"):"") +
				(showBytes?("<td align='center'>" + bytes + "</td>"):"") +
				(showFirstSwitched?("<td align='center'>" + dq.createTimeOutput(firstSwitched*1000) + "</td>"):"") +
				(showLastSwitched?("<td align='center'>" + dq.createTimeOutput(lastSwitched*1000) + "</td>"):"") +
				(showDuration?("<td align='center'>" + (long)(lastSwitched-firstSwitched) + "</td>"):"") +
				(showExporter?("<td align='center'>" + result.getInt("exporterID") + " (" + dq.createIPOutput(result.getLong("exporterIP"), resolveIP) + ")</td>"):"") + 
                                (showDatabase?("<td align='center'>"+getDBName(result.getInt("databaseID"))+"</td>"):"") +
                                (!grpEverything?("<td align='center'>"+result.getLong("grpcount")+"</td>"):"") + "</tr>";
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

                // ********* construct "next" and "previous" buttons as appropriate

                output += "<table><tr>";
		// "previous" button
                if (currOffset > 0) { 
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
			"<input type=\"hidden\" name=\"offset\" value=\"" + (currOffset-outputLength) + "\">" +
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
			"<input type=\"submit\" value=\"Previous\">" +
			"</form></td>";
		}
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
			"<input type=\"hidden\" name=\"offset\" value=\"" + (currOffset+outputLength) + "\">" +
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
