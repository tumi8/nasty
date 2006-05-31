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
 * Title:   PerlOutputCreator
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */

package de.japes.servlets.nasty;


import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import javax.servlet.http.*;

/**
 * Queries given databases according to the parameters set by nasty's <code>index.jsp</code>
 * and returns the results as a perl array.
 */
public class PerlOutputCreator extends OutputCreator {

	/** MIME-content of results is <code>text/html</code> */
	private static final String contentType = "text/plain";
	
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
	public PerlOutputCreator(HttpServletRequest request, HttpServletResponse response, Statement s, ArrayList dbList) {
            super(request, response, s, dbList);
        }

        /**
         * Queries the given databases and writes out the results formatted as perl array.
         * 
         * @return  String that holds eventual messages such as error messages.
         */
        public String createOutput() {
		
		String tmpOutput = "";
		String limitString = "";
		PrintWriter out = null;
		int lastValue = 0;
		
                // time bounds of current flow
		long firstSwitched, lastSwitched;
                // name of the temporary tables (one with, one without double entries)
                String tmpname = "";
                String tmpname2 = "";
                // the results of the query
		ResultSet result = null;

                boolean furtherResults = true;
		
		try {
			 out = response.getWriter();
		} catch (IOException e) {
			return "Error creating Perl output.";
		}

                out.println("# Start: " + new GregorianCalendar().getTime());
		out.println("# Output created by nasty");
		
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
       
                if (remDoubles) {  
                    // remove identical entries by grouping the temporary table into a new view
                    tmpname2 = dq.getUniqueName("perlTmpNoDoubles");
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
                String queryTmp = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes, " +
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
		
		// ************ construct Perl-table from the results of the query *********
		
		out.println("$nastyArray = (");
		
		try {
			int i=0;
			
			limitString = " LIMIT 0, 1000";
			
			result = dq.queryTempDB(queryTmp + limitString);
                        if (result==null) {
                            dq.dropTable(tmpname2);
                            dq.dropTable(tmpname);
                            return "Database error." + dq.getOutput();
                        }
			
			while (furtherResults) {
				
				while (result.next()) {
        				// check time bounds of current flow
                			firstSwitched=result.getLong("firstSwitched");
                                        if (grpTime) firstSwitched=(firstSwitched/grpTimeDiv)*grpTimeDiv;
                                	lastSwitched=result.getLong("lastSwitched");
                                        if (grpTime) lastSwitched=((lastSwitched+grpTimeDiv)/grpTimeDiv)*grpTimeDiv;
					
					out.print("[");
					tmpOutput = (showSrcIP?("\"" + dq.createIPOutput((result.getLong("srcIP")/grpSrcIPDiv)*grpSrcIPDiv, false) + "\", "):"") +
					(showDstIP?("\"" + dq.createIPOutput((result.getLong("dstIP")/grpDstIPDiv)*grpDstIPDiv, false) + "\", "):"") +
					(showSrcPort?("\"" + dq.createPortOutput(result.getInt("srcPort"), false) + "\", "):"") +
					(showDstPort?("\"" + dq.createPortOutput(result.getInt("dstPort"), false) + "\", "):"") +
					(showProto?("\"" + dq.createProtoOutput(result.getShort("proto"), false) + "\", "):"") +
					(showTos&&!dq.isTosMissing()?("\"" + result.getShort("dstTos") + "\", "):"") +
					(showPackets?("\"" + result.getLong("pkts") + "\", "):"") +
					(showBytes?("\"" + ((result.getLong("bytes")/grpBytesDiv)*grpBytesDiv) + "\", "):"") +
					(showFirstSwitched?("\"" + (firstSwitched) + "\", "):"") +
					(showLastSwitched?("\"" + (lastSwitched) + "\", "):"") +
					(showDuration?("\"" + (long)(lastSwitched-firstSwitched) + "\", "):"") +
					(showExporter?("\"" + (result.getLong("exporterID")) + "\", "):"") +
					(showDatabase?("\"" + getDBName(result.getInt("databaseID")) + "\", "):"") +
					(!grpEverything?("\"" + result.getLong("grpcount") + "\""):"");
					
					if (tmpOutput.endsWith(", "))
						tmpOutput = tmpOutput.substring(0, tmpOutput.length()-2);
					
					out.print(tmpOutput);
					out.println("],");
					
					i++;
				}
				
				if ((i%1000)==0) {
					if (lastValue==i) {	//this is true if the number of results is exactly 1000, 2000, 3000...
                                            furtherResults = false;
					} else {
                                            lastValue = i;
                                            limitString = " LIMIT " + i + ", 1000";
                                            result = dq.queryTempDB(queryTmp + limitString);
                                            if (result==null) furtherResults = false;
                                            else furtherResults = true;
                                        }
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
		
		// drop the temporary tables
                dq.dropTable(tmpname2);
                dq.dropTable(tmpname);
                
                // return with eventual messages such as error messages
                return dq.getOutput();
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
