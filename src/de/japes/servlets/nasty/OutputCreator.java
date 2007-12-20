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
 * Title:   OutputCreator
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */
package de.japes.servlets.nasty;


import javax.servlet.http.*;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.ArrayList;

/**
 * Abstract class to query given databases and to compose the results in a specific format.
 * This format and the specific query to be run has to be determined by implementations of
 * this class, but much of the needed functionality (such as easily querying multiple databases)
 * is encapsulated here.
 */
abstract class OutputCreator {
	
        /** Servlet input parameters as selected by user. */
        protected HttpServletRequest request = null;
        /** Servlet output (for example to display error messages). */
        protected HttpServletResponse response = null;
        /** String into which output is written. */
	protected String output = "";
        /** Object that provides the functionality to query multiple nasty databases. */
        protected DistributedQuery dq;
	/** Selected start time of query. */
        protected long startTime;
	/** Selected end time of query. */
	protected long endTime;
        /** True if ports higher than 1023 should be ignored. */
        protected boolean ignoreHighPorts = false;
        // various flags for selected output format and grouping
        protected boolean restrictTime = false;
	protected boolean resolveIP = false;
	protected boolean showSrcIP = false, showDstIP = false, showSrcPort = false, showDstPort  = false;
	protected boolean showProto = false, showTos = false, showPackets = false, showBytes = false;
	protected boolean showFirstSwitched = false, showLastSwitched = false, showDuration = false;
	protected boolean showExporter = false, showDatabase = false, remDoubles = false, remExporterID = false;
	protected boolean grpSrcIP = false, grpDstIP = false, grpSrcPort = false, grpDstPort  = false;
	protected boolean grpProto = false, grpTos = false, grpPackets = false, grpBytes = false;
	protected boolean grpTime = false, grpDuration = false;
	protected boolean grpExporter = false, grpDatabase = false, grpAnything = false;
        protected boolean grpEverything = false;
        // special grouping parameters
        protected int remTimeDiv = 1;
        protected long grpSrcIPDiv = 1;
        protected long grpDstIPDiv = 1;
        protected long grpBytesDiv = 1;
        protected long grpTimeDiv = 1;
        // selected order of HTML-table
	protected String order = "";
        protected String sort = "";
        // selected output unit for byte values
        protected String outputUnit = "";
	
	/**
         * Class constructor. Receives parameters and tries to retrieve proper names for ports and
         * protocols used on current system.
         *
         * @param request   Holds the parameters selected by the user in <code>index.jsp</code>.
         * @param response  Could be used to set response parameters or write output messages to.
         * @param s         JDBC-<code>Statement</code>-object for local database (for storing
         *                  temporary results).
         * @param dbList    List of JDBC-<code>Statement</code>-objects for the databases that should
         *                  be queried. (May include one for local database!)
         * @see   Statement
         */
	public OutputCreator(HttpServletRequest request, HttpServletResponse response, Statement s, ArrayList dbList) {
                
                dq = new DistributedQuery(s, dbList);
                this.request = request;
                this.response = response;
            
                // check various flags controlling the format of the HTML table
		String[] checkValues = request.getParameterValues("checks");
		if (checkValues != null) {
                    for (int i=0; i<checkValues.length; i++) {
                        if (checkValues[i].equalsIgnoreCase("ignoreHighPorts"))
                                ignoreHighPorts = true;
                        else if (checkValues[i].equalsIgnoreCase("restrictTime")) 
				restrictTime = true;
                        else if (checkValues[i].equalsIgnoreCase("resolveIP"))
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
			else if (checkValues[i].equalsIgnoreCase("showDatabase"))
				showDatabase = true;
			else if (checkValues[i].equalsIgnoreCase("remDoubles"))
				remDoubles = true;
			else if (checkValues[i].equalsIgnoreCase("remExporterID")) {
				remExporterID = true;
                                remDoubles = true;
                        }
                    }
		}
		
                // retrieve selected time bounds
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+00:00"));
                if (restrictTime) {
                    // read time bounds set by user and transform them into millisecond-value
                    // (month value in Calendar-class is 0-based (January is 0)!)
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.set(Integer.parseInt(request.getParameter("startYear")), Integer.parseInt(request.getParameter("startMonth"))-1,
			Integer.parseInt(request.getParameter("startDay")), Integer.parseInt(request.getParameter("startHour")),
			Integer.parseInt(request.getParameter("startMin")), 0);
                    startTime = cal.getTimeInMillis();
                    cal.set(Integer.parseInt(request.getParameter("endYear")), Integer.parseInt(request.getParameter("endMonth"))-1,
			Integer.parseInt(request.getParameter("endDay")), Integer.parseInt(request.getParameter("endHour")),
			Integer.parseInt(request.getParameter("endMin")), 0);
                    endTime = cal.getTimeInMillis();	
		} else {
                      // if no time-bounds have been provided, execute standard 24h query
                      endTime = cal.getTimeInMillis();
                      startTime = endTime - (24*60*60*1000+30*60*1000);
                }
                
                // check size of time blocks double entries must be in
                String remTimeDivStr = request.getParameter("remTimeDiv");
                try {
                    remTimeDiv = Integer.parseInt(remTimeDivStr);
                    if (remTimeDiv<1) remTimeDiv=1;
                } catch (NumberFormatException e) {
                    remTimeDiv=1;
                }

                // check grouping parameters
		String[] groupValues = request.getParameterValues("group");
                if (groupValues==null) groupValues = new String[0];
        	int i = 0;
        	while (i < groupValues.length) {
                        if (groupValues[i].equalsIgnoreCase("grpSrcIP"))
                                { grpSrcIP = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpDstIP"))
                                { grpDstIP = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpSrcPort"))
                                { grpSrcPort = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpDstPort"))
                                { grpDstPort = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpProto"))
				{ grpProto = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpTos"))
				{ grpTos = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpPackets"))
				{ grpPackets = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpBytes"))
				{ grpBytes = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpTime"))
				{ grpTime = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpDuration"))
				{ grpDuration = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpExporter"))
				{ grpExporter = true; grpAnything = true; }
			else if (groupValues[i].equalsIgnoreCase("grpDatabase"))
				{ grpDatabase = true; grpAnything = true; }
			i++;
		}

                // check special parameters for grouping
                try {
                    grpSrcIPDiv = Short.parseShort(request.getParameter("grpSrcIPDiv"));
                    if ((grpSrcIPDiv<1) || (grpSrcIPDiv>32)) grpSrcIPDiv=32;
                } catch (NumberFormatException e) {
                    grpSrcIPDiv=32;
                }
                grpSrcIPDiv = (long) Math.pow(2, 32-grpSrcIPDiv);
                try {
                    grpDstIPDiv = Short.parseShort(request.getParameter("grpDstIPDiv"));
                    if ((grpDstIPDiv<1) || (grpDstIPDiv>32)) grpDstIPDiv=32;
                } catch (NumberFormatException e) {
                    grpDstIPDiv=32;
                }
                grpDstIPDiv = (long) Math.pow(2, 32-grpDstIPDiv);
                try {
                    grpBytesDiv = Long.parseLong(request.getParameter("grpBytesDiv"));
                    if (grpBytesDiv<1) grpBytesDiv=1;
                } catch (NumberFormatException e) {
                    grpBytesDiv=1;
                }
                try {
                    grpTimeDiv = Long.parseLong(request.getParameter("grpTimeDiv"));
                    if (grpTimeDiv<1) grpTimeDiv=1;
                } catch (NumberFormatException e) {
                    grpTimeDiv=1;
                }
                if (grpSrcIP && grpDstIP && grpSrcPort && grpDstPort && grpProto && grpTos 
                        && grpPackets && grpBytes && grpTime && grpDuration && grpExporter
                        && grpDatabase && (grpSrcIPDiv==1) && (grpDstIPDiv==1) && (grpBytesDiv==1)
                        && (grpTimeDiv==1)) {
                    grpEverything = true;
                }
                
                // get order of tables
                order = request.getParameter("order");
                sort = request.getParameter("sort");            
                // get output unit for byte-values
                outputUnit = request.getParameter("unit");
                
                // retrieve rest of parameters and send them to distributed query object
                dq.setParams(startTime, endTime, 
                        request.getParameter("srcIP"),  request.getParameter("dstIP"), 
                        request.getParameter("srcPort"), request.getParameter("dstPort"), 
                        request.getParameter("proto"), request.getParameter("tos"),
                        request.getParameter("exporterID"));
	}

        /**
         * By calling this method the alternate method for the distributed query is selected,
         * which consists of the use of the external tool "mysqldump" for transferring data
         * from remote databases into the local database.
         *
         * @param dbInfoList    List of objects that contain data needed to address
         *                      the databases that should be queried (host, user, password, name
         *                      of database). Needed for calling "mysqldump".
         * @param dbLocalInfo   Object that contains data needed to address the local
         *                      database into which data from remote tables should be copied 
         *                      (host, user, password, name of database). Needed for calling
         *                      the mysql-command-line-tool into which mysqldump-results are piped.
         * @see DBInfo
         */
        public void useMysqldumpVersion(ArrayList dbInfoList, DBInfo dbLocalInfo) {
            dq.useMysqldumpVersion(dbInfoList, dbLocalInfo);
        }
        
        /**
         * Looks up the description of the database with given ID number in the servlet context.
         *
         * @param   databaseID    ID number of database.
         * @return  Description of database or number of database in case no description has been found.
         */
        public String getDBName(int databaseID) {
            // retrieve database number
            String[] dbNames = request.getParameterValues("dbSelects");
            int db = -1;
            try { db = Integer.parseInt(dbNames[databaseID]); } catch (Exception e) {}
            if (db==-1) return dbNames[databaseID];
            
            // retrieve corresponding database description
            String dbName=request.getSession().getServletContext().getInitParameter("dbDescription"+db); 
            
            if (dbName==null) return dbNames[databaseID];
            else return dbName;
        }
        
        /**
         * Abstract method. Implementations must perform database queries, compose output
         * from the results in a certain format and return it as a string.
         * 
         * @return  String that holds the output and eventual messages such as error messages.
         */
	abstract String createOutput();
	
	/**
         * Abstract method. Implementations must return the MIME-content-type of their output
         * as string.
         *
         * @return  String that holds the MIME-type of the output produced by this class.
         */
        abstract String getContentType();

}
