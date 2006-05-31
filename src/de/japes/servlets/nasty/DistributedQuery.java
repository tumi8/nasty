/**
 * Title:   DistributedQuery
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */
package de.japes.servlets.nasty;


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
import java.util.ArrayList;
import de.japes.parser.*;

/**
 * Provides the functionality to query multiple nasty-collector-databases and several
 * helper methods to present the results.
 */
public class DistributedQuery {
	
        /** String into which output messages are written. */
	private String output = "";
        /**
         * Preferrably local database that is used to collect results of queries on
         * possibly many remote databases.
         */
        private Statement tempDB = null;
        /** List of source databases to be queried (remote and/or local databases). */
        private ArrayList dbList = null;
        /** Flag to signal if tos-data is missing in any source table. */
        private boolean tosMissing = false;
        /** Flag to signal if ports are aggregated in any source table. */
        private boolean portsAggregated = false;

        /** Clear text names of tcp-services on current system. */
	private String[] tcpServices = null;
        /** Clear text names of udp-services on current system. */
	private String[] udpServices = null;
        /** Clear text names of protocols on current system. */
	private String[] protocols = null;
        /** Hash table with ports and their clear text name. */
	private HashMap portMap = null;
        /** Hash table with protocols and their clear text name. */
	private HashMap protocolMap = null;

        /** 
         * Flag to signal if alternate version (mysqldump) of querying multiple databases 
         * should be used.
         */
        private boolean useAlternate = false;
        /** List of string-arrays that hold information on source databases for mysqldump. */
        private ArrayList dbInfoList = null;
        /** String-array that holds information on local database for mysqldump. */
        private DBInfo dbLocalInfo = null;

        /** Start time of query in milliseconds since 1970. */
        private long startTime = 0;
        /** End time of query in milliseconds since 1970. */
	private long endTime = 0;
        /** Source IP the flows should have. */
        private String srcIP = "";
        /** Destination IP the flows should have. */
        private String dstIP = "";
        /** Source port the flows should have. */
        private String srcPort = "";
        /** Destination port the flows should have. */
        private String dstPort = "";
        /** Protocol that should have been used by flows. */
        private String proto = "";
        /** Tos-value that should have been used by flows. */
        private String tos = "";
        /** Exporter that should have monitored the flow. */
        private String exporterID = "";
	
	/**
         * Class constructor. Receives parameters and tries to retrieve proper names for ports and
         * protocols used on current system.
         *
         * @param s         JDBC-<code>Statement</code>-object for local database (for storing
         *                  temporary results).
         * @param dbList    List of JDBC-<code>Statement</code>-objects for the databases that should
         *                  be queried. (May include one for local database!)
         * @see   Statement
         */
	public DistributedQuery(Statement s, ArrayList dbList) {
		
                this.tempDB = s;
                this.dbList = dbList;
                
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

	/**
         * Sets parameters for the query.
         *
         * @param startTime     Start time of query in milliseconds since 1970.
         * @param endTime       End time of query in milliseconds since 1970.
         * @param srcIP         Source IP the flows should have.
         * @param dstIP         Destination IP the flows should have.
         * @param srcPort       Source port the flows should have.
         * @param dstPort       Destination port the flows should have.
         * @param proto         Protocol that should have been used by flows.
         * @param tos           Tos-value that should have been used by flows.
         * @param exporterID    Exporter that should have monitored the flow.
         */
        public void setParams(long startTime, long endTime, String srcIP, String dstIP,
                String srcPort, String dstPort, String proto, String tos, String exporterID) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.srcIP = srcIP;
            this.dstIP = dstIP;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            this.proto = proto;
            this.tos = tos;
            this.exporterID = exporterID;
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
            this.dbInfoList = dbInfoList;
            this.dbLocalInfo = dbLocalInfo;
            this.useAlternate = true;
        }
        
        /**
         * Retrieves output messages generated so far.
         *
         * @return  Output messages generated so far.
         */
        public String getOutput() { return output; }
        
        /**
         * Retuns tosMissing-flag which signals if tos-data is missing in any relevant source table.
         *
         * @return    true if tos-data is missing in any relevant source table, false otherwise.
         */
        public boolean isTosMissing() { return tosMissing; }
  
        /**
         * Returns portsAggregated-flag which signals if ports are aggregated in any relevant source table.
         *
         * @return    true if ports are aggregated in any relevant source table, false otherwise.
         */
        public boolean isPortsAggregated() { return portsAggregated; }

        /**
         * Gets number of source databases.
         *
         * @return              Number of source databases.
         */
        public int getDBCount() {
            return dbList.size();
        }

        /**
         * Creates unique name which should be used for temporary tables.
         *
         * @param   tmpname     String on which unique name should be based.
         *                      (Unique part is added to this string.)
         * @return              Unique name as string.
         */
        public String getUniqueName(String tmpname) {
	   String hostname;
           if (tmpname==null) tmpname = "";
           try{
               // add name of current host
               hostname = InetAddress.getLocalHost().getHostName();
	       // replace dots and minus by underline
	       hostname = hostname.replace('.', '_');
	       tmpname += hostname.replace('-', '_');
           }
           catch(Exception e){
               // in case of error just add the hash code of this object
               tmpname += this.hashCode();
           }
           // add the current time in milliseconds since 1970
           String ret = tmpname + System.currentTimeMillis();
           // change new name of temporary table to lower case and return it
           return ret.toLowerCase(); 
        }

        /**
         * Creates temporary table in local database.
         *
         * @param   tmpname     Name of temporary table.
         * @param   statement   SQL-CREATE-statement needed to create temp. table.
         * @return              False on error, true on success.
         */
        private boolean createTempTable(String tmpname, String statement) {
            boolean ok=true;
            
            dropTable(tmpname);

            // if mysqldump-version of program was selected, temporary tables in all remote tables
            // must be created
            if (useAlternate) {
                // (cannot use MySQL-TEMPORARY-tables because they are connection-specific
                // and would not be seen by the mysqldump-tool because it uses a new connection) 
                String helpstr = statement.toLowerCase();
                if (helpstr.startsWith("create temporary table")) {
                    statement="CREATE TABLE"+statement.substring(22);
                }
                // create remote temporary tables
                for (int i=0; i<dbList.size(); i++) {
                    Statement remDB = (Statement)dbList.get(i);
                    try {
                        // execute statement to create temporary table
                        remDB.execute(statement);
                    } catch (SQLException e) {
                        // error
                        output+="<p>Error while creating remote temporary table "+(i+1)+" for request.";
                        output+="<p>"+e.getSQLState()+"<p>"+e.getMessage()+"<br>"+statement+"<p>";
                        ok=false;
                    }
                }
            }

            // create temporary table in local database
            try {
                // temporary table might have already been created if local database is also
                // containing data that should be queried (local database would then also be a part
                // of dbList and temporary table would have been created in the above loop),
                // so add IF NOT EXISTS to the CREATE statement
                int pos = statement.toLowerCase().indexOf("table")+5;
                statement = statement.substring(0, pos) + " IF NOT EXISTS "
                            + statement.substring(pos);
                // execute statement to create local temporary table
                tempDB.execute(statement);
            } catch (SQLException e) {
                // error
                output+="<p>Error while creating temporary table in local database for request.";
                output+="<p>"+e.getSQLState()+"<p>"+e.getMessage()+"<br>"+statement+"<p>";
                ok=false;
            }
            
            return ok;
        }
        
        /** 
         * Fill temporary table in local database with data from relevant sources. The source
         * tables that are queried to fill the temporary table have three virtual columns that
         * may be used as normal SQL columns: The values in the column "exporterID" of the original
         * source tables are automatically translated into the real source ID of the exporter of
         * each flow. The column "exporterIP" is automatically filled with the corresponding IP address
         * of this exporter. (Both points are achieved by using the "exporter"-table of each source
         * database.) The third virtual column is called "databaseID" and is automatically filled with
         * the number of the source database each row comes from in the list <code>dbList<\code>
         * which must have been given to the constructor. The column SQL-types are "INTEGER(10) UNSIGNED"
         * for "exporterID" and "exporterIP" and for "databaseID" the column type must be able to hold numbers.
         *
         * @param  tmpname     Name of temporary table.
         * @param  statement   SQL-SELECT-String with placeholder #srctable# for
         *                     extracting data from relevant (and possibly remote) database tables.
         *                     (Placeholders for special WHERE-clauses can be used.)
         * @return             True for success, false on error.
         *                     Temporary table will be filled with data if successful.
         */
        private boolean fillTempTable(String tmpname, String statement) {
            boolean ok = true; // return value
            ArrayList dbThreads = new ArrayList(); // list of FillTempTable-objects for each db
            ArrayList dbThreadObjects = new ArrayList(); // list of corresponding Thread-objects
            int i; // counter variable
            // query-strings to replace placeholders in given SQL-query
            String srcIPQuery = "";
            String dstIPQuery = "";
            String srcPortQuery = "";
            String dstPortQuery = "";
            String protoQuery = "";
            String tosQuery = "";
            String exporterIDQuery = "";
            String timeQuery = "";
           
            // add simple WHERE-clause to statement if there is none
            if (statement.toLowerCase().indexOf("where ")==-1) {
                int wherePos = statement.toLowerCase().indexOf("group by");
                if (wherePos==-1) wherePos = statement.toLowerCase().indexOf("order by");
                if (wherePos==-1) wherePos = statement.length();
                statement = statement.substring(0,wherePos)+" WHERE 1=1 "
                    +statement.substring(wherePos);
            }
            // replace #params#-placeholder with standard query that checks all parameters
            statement=statement.replaceAll("#params#", "#srcIP# AND #dstIP# AND #srcPort# AND #dstPort# AND #proto# AND #dstTos# AND #exporterID# AND #timeQuery#");
            // change statement to automatically translate exporterID-values of source tables
            // into the real source IDs and IPs of an exporter that the user is looking for
            // (these real values are stored in table "exporter" of each source database)
            String helpstr = statement.toLowerCase();
            helpstr = helpstr.substring(helpstr.indexOf("from"), helpstr.indexOf("where"));
            if (helpstr.indexOf("exporter")==-1) {
              statement=statement.replaceAll("#srctable#", "#srctable#,exporter");
            }
            statement=statement.replaceAll("#exporterID#", "#expID#");
            if ((statement.toLowerCase().indexOf("exporterid")>-1) && (statement.indexOf("#expID#")==-1)) {
                // always include clause for "replacing" the exporterID with the real exporter source ID
                int wherePos = statement.toLowerCase().indexOf("where ")+6;
                statement = statement.substring(0,wherePos)+"#expID# AND "
                        +statement.substring(wherePos);
            } 
            if (statement.toLowerCase().indexOf("as exporterid")==-1) 
                statement=statement.replaceAll("exporterID", "exporter.sourceID");
            else {
                if (statement.toLowerCase().indexOf("exporterid")<statement.toLowerCase().indexOf("as exporterid")) {
                    statement=statement.replaceFirst("exporterID", "exporter.sourceID");
                }
            }
            statement=statement.replaceAll("#expID#", "#exporterID#");
            statement=statement.replaceAll("#srcIP#", "#srcX#");
            if (statement.toLowerCase().indexOf("as srcip")==-1) 
                statement=statement.replaceAll("srcIP", "#srctable#.srcIP");
            else {
                if (statement.toLowerCase().indexOf("srcip")<statement.toLowerCase().indexOf("as srcip")) {
                    statement=statement.replaceFirst("srcIP", "#srctable#.srcIP");
                }
            }
            statement=statement.replaceAll("#srcX#", "#srcIP#");
            statement=statement.replaceAll("exporterIP", "exporter.srcIP");
            // first phase: find all relevant tables in all databases
            // create thread for each database
            for (i=0; i<dbList.size(); i++) {
                FillTempTable f;
                if (useAlternate) {
                    f = new FillTempTableAlt((Statement)dbList.get(i), i, (DBInfo)dbInfoList.get(i), tempDB, dbLocalInfo, tmpname, startTime, endTime);
                } else {    
                    f = new FillTempTable((Statement)dbList.get(i), i, tempDB, tmpname, startTime, endTime);
                }
                dbThreads.add(f);
                Thread t = new Thread(f);
                t.setName("DB-Thread "+(i+1));
                dbThreadObjects.add(t);
                t.start();
            }
            // wait for threads to finish
            for (i=0; i<dbThreads.size(); i++) {
                try { 
                    FillTempTable f = (FillTempTable)dbThreads.get(i);
                    Thread t = (Thread)dbThreadObjects.get(i);
                    t.join();
                    // check flags: is TOS-data missing in any table? 
                    //              are ports aggregated in any table?
                    if (f.isTosMissing()) tosMissing = true;
                    if (f.isPortsAggregated()) portsAggregated = true;
                    // check flag: was thread successful or was there an error?
                    if (f.isSuccess()==false) ok = false;
                    // get eventual messages of this thread and add them to output
                    output += f.getOutput();
                } catch (InterruptedException e) {}
            }
            // exit on error
            if (!ok) return ok;
            // first phase is finished
            
            // second phase: get data from each database and store it in temp. table
            // fill in values for placeholders in SQL-query
            srcIPQuery = createIPQuery(srcIP, "#srctable#.srcIP");
            if (srcIPQuery.equalsIgnoreCase("")) srcIPQuery = "1=1";
            statement = statement.replaceAll("#srcIP#", srcIPQuery);
            dstIPQuery = createIPQuery(dstIP, "dstIP");
            if (dstIPQuery.equalsIgnoreCase("")) dstIPQuery = "1=1";
            statement = statement.replaceAll("#dstIP#", dstIPQuery);
            srcPortQuery = createPortQuery(srcPort, "srcPort", portsAggregated);
            if (srcPortQuery.equalsIgnoreCase("")) srcPortQuery = "1=1";
            statement = statement.replaceAll("#srcPort#", srcPortQuery);
            dstPortQuery = createPortQuery(dstPort, "dstPort", portsAggregated);
            if (dstPortQuery.equalsIgnoreCase("")) dstPortQuery = "1=1";
            statement = statement.replaceAll("#dstPort#", dstPortQuery);
            protoQuery = createProtoQuery(proto, "proto");
            if (protoQuery.equalsIgnoreCase("")) protoQuery = "1=1";
            statement = statement.replaceAll("#proto#", protoQuery);
            if (!tosMissing) {
		tosQuery = createTosQuery(tos, "dstTos");
                if (tosQuery.equalsIgnoreCase("")) tosQuery = "1=1";
            } else {
                // there is table with no tos-data, so use true-condition instead of tos-query
                tosQuery = "1=1";
            }
            statement = statement.replaceAll("#dstTos#", tosQuery);
            exporterIDQuery = createExporterIDQuery(exporterID, "exporter.sourceID");
            if (exporterIDQuery.equalsIgnoreCase("")) exporterIDQuery = "1=1";
            // always include clause for "replacing" the exporterID with the real exporter source ID
            exporterIDQuery = "exporter.id=#srctable#.exporterID AND "+ exporterIDQuery;
            statement = statement.replaceAll("#exporterID#", exporterIDQuery);
            timeQuery = createTimeQuery(startTime, endTime);
            statement = statement.replaceAll("#timeQuery#", timeQuery);
            // replace "dstTos" in SELECT ... with 0 if tos-data is missing in any table
            if (tosMissing) {
                statement = statement.replaceFirst("dstTos", "0 AS dstTos");
                statement = statement.replaceFirst("0 AS dstTos\\)", "0)");
            }
            // send updated SQL-query to each database-thread and start their second phase
            dbThreadObjects.clear();
            for (i=0; i<dbThreads.size(); i++) {
                FillTempTable f = (FillTempTable)dbThreads.get(i);
                f.setQueryStr(statement);
                Thread t = new Thread(f);
                t.setName("DB-Thread "+(i+1));
                dbThreadObjects.add(t);
                t.start();
            }
            // wait for threads to finish
            for (i=0; i<dbThreads.size(); i++) {
                try { 
                    FillTempTable f = (FillTempTable)dbThreads.get(i);
                    Thread t = (Thread)dbThreadObjects.get(i);
                    t.join();
                    // check flag: is everything okay?
                    if (!(f.isSuccess())) ok = false;
                    // get eventual messages of this thread and add them to output
                    output += f.getOutput();
                } catch (InterruptedException e) {}
            }
                        
            return ok;
        }
        
        /** 
         * Queries (local) database with temporary tables and returns ResultSet with results.
         * (May also be used to execute SQL statements that don't produce results. Null
         * will be returned then.)
         *
         * @param  statement   SQL-String for extracting data from temporary table.
         *                     (Placeholders for special WHERE-clauses may be used.)
         * @return             ResultSet with results of query, null on error or if there are no results
         *                     because the query was no SELECT-query.
         * @see    ResultSet
         */
        public ResultSet queryTempDB(String statement) {
            // result of query
            ResultSet result = null;
            // query-strings to replace placeholders in given SQL-query
            String srcIPQuery = "";
            String dstIPQuery = "";
            String srcPortQuery = "";
            String dstPortQuery = "";
            String protoQuery = "";
            String tosQuery = "";
            String exporterIDQuery = "";
            String timeQuery = "";

            // replace #params#-placeholder with standard query that checks all parameters
            statement=statement.replaceAll("#params#", "#srcIP# AND #dstIP# AND #srcPort# AND #dstPort# AND #proto# AND #dstTos# AND #exporterID# AND #timeQuery#");
            // fill in values for placeholders in SQL-query
            srcIPQuery = createIPQuery(srcIP, "srcIP");
            if (srcIPQuery.equalsIgnoreCase("")) srcIPQuery = "1=1";
            statement = statement.replaceAll("#srcIP#", srcIPQuery);
            dstIPQuery = createIPQuery(dstIP, "dstIP");
            if (dstIPQuery.equalsIgnoreCase("")) dstIPQuery = "1=1";
            statement = statement.replaceAll("#dstIP#", dstIPQuery);
            srcPortQuery = createPortQuery(srcPort, "srcPort", portsAggregated);
            if (srcPortQuery.equalsIgnoreCase("")) srcPortQuery = "1=1";
            statement = statement.replaceAll("#srcPort#", srcPortQuery);
            dstPortQuery = createPortQuery(dstPort, "dstPort", portsAggregated);
            if (dstPortQuery.equalsIgnoreCase("")) dstPortQuery = "1=1";
            statement = statement.replaceAll("#dstPort#", dstPortQuery);
            protoQuery = createProtoQuery(proto, "proto");
            if (protoQuery.equalsIgnoreCase("")) protoQuery = "1=1";
            statement = statement.replaceAll("#proto#", protoQuery);
            if (!tosMissing) {
		tosQuery = createTosQuery(tos, "dstTos");
                if (tosQuery.equalsIgnoreCase("")) tosQuery = "1=1";
            } else {
                // there is table with no tos-data, so use true-condition instead of tos-query
                tosQuery = "1=1";
            }
            statement = statement.replaceAll("#dstTos#", tosQuery);
            exporterIDQuery = createExporterIDQuery(exporterID, "exporterID");
            if (exporterIDQuery.equalsIgnoreCase("")) exporterIDQuery = "1=1";
            statement = statement.replaceAll("#exporterID#", exporterIDQuery);
            timeQuery = createTimeQuery(startTime, endTime);
            statement = statement.replaceAll("#timeQuery#", timeQuery);
            
            // query local database
            try {
                // check if statement is a SELECT or something else
                if (statement.toLowerCase().startsWith("select")) {
                    result = tempDB.executeQuery(statement);
                } else {
                    tempDB.execute(statement);
                    result = null;
                }
            } catch (SQLException e) {
                output += "Error while querying temporary table.";
                output += "<p>"+e.getMessage()+"<br>"+statement+"<p>";        
                result = null;
            }
            
            return result;
        }

        /** 
         * Drops table from local database.
         *
         * @param  tmpname     Name of table.
         */
        public void dropTable(String tmpname) {
            try {
		tempDB.execute("DROP TABLE IF EXISTS "+tmpname);
            } catch (SQLException e) {
                output += "Error while dropping table from temporary database. Error was ignored.";
                output += "<p>"+e.getMessage()+"<p>";        
            }
        }

        /**
         * Creates a temporary table in the local database and fills it with data that is extracted
         * by an SQL SELECT statement from available source tables.<p>
         * The source tables that are queried to fill the temporary table have three virtual columns
         * that may be used as normal SQL columns: The values in the column "exporterID" of the original
         * source tables are automatically translated into the real source ID of the exporter of
         * each flow. The column "exporterIP" is automatically filled with the corresponding IP address
         * of this exporter. (Both points are achieved by using the "exporter"-table of each source
         * database.) The third virtual column is called "databaseID" and is automatically filled with
         * the number of the source database each row comes from in the list <code>dbList<\code>
         * which must have been given to the constructor. The column SQL-types are "INTEGER(10) UNSIGNED"
         * for "exporterID" and "exporterIP" and for "databaseID" the column type must be able to
         * hold numbers.<p>
         * Identical entries can be removed if that is wanted, but this will make the entire query
         * very much slower because all relevant data from the source databases must be transferred
         * into a local table to make it possible to check for identical entries, not only the data
         * that is needed for the query itself.<p>
         * If no removal of identical entries is wanted, this method sends the given SELECT
         * statement directly to the source tables, so that data may get "pre-grouped" and narrowed
         * down remotely. The results are collected in the local table that gets created
         * by the given CREATE statement.<p>
         * If removal of identical entries is wanted, this method retrieves all relevant data from the
         * source tables into a standard temporary table in the local database and then removes the
         * identical entries from it. The given SELECT statement is then run on the temporary table and
         * its results are stored in the table that is created by the given CREATE statement.<p>
         * <i>If MySQLDump-version for distributed query and no removal of identical entries was selected,
         * the resulting table won't be an SQL TEMPORARY table, no matter what was stated in the CREATE
         * string, so the table must be dropped after using it!</i>
         *
         * @param   tableName       Name of the resulting table.
         * @param   createStr       SQL CREATE string to create the resulting table. If this
         *                          string is null, a standard table with all available columns
         *                          will be created.
         * @param   selectStr       SQL SELECT string with placeholder #srctable# for extracting data from
         *                          relevant (and possibly remote) database tables. If this
         *                          string is null, a standard query is run to retrieve all available
         *                          data that fits the parameters set by <code>setParams()</code>.
         *                          (Placeholders for special WHERE-clauses can be used.)
         * @param   remDoubles      True if identical entries shall be removed.
         * @param   checkAll        True if entries are only considered identical when their exporter
         *                          ID and their byte and packet values are the same.
         * @param   remTimeDiv      Entries are only considered identical if they lie within the same
         *                          time block of size <code>remTimeDiv</code> (in seconds).
         *
         * @return                  True on success, false on error.
         */
        public boolean fillTable(String tableName, String createStr, String selectStr, boolean remDoubles, boolean checkAll, int remTimeDiv) {
            String createTmp = "";
            String fillTmp = "";
            String tmpname = getUniqueName("tmp");
            if (remDoubles || (createStr==null) || (selectStr==null)) {
                // create standard temporary table
                // and retrieve all relevant data
                if (!remDoubles) tmpname = tableName;
                if (remDoubles || (createStr==null)) createTmp = "CREATE TEMPORARY TABLE "+tmpname+" (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
				"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, " +
				"proto TINYINT UNSIGNED, dstTos TINYINT UNSIGNED, " +
				"pkts BIGINT(20), bytes BIGINT(20) UNSIGNED, " +
				"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10)," +
				"exporterID INTEGER(10) UNSIGNED, exporterIP INTEGER(10) UNSIGNED," +
                                "databaseID SMALLINT(5) UNSIGNED)";
                else createTmp = createStr;
                if (remDoubles || (selectStr==null)) fillTmp = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes," +
                                 " firstSwitched, lastSwitched, exporterID, exporterIP, databaseID FROM #srctable# WHERE " +
                                 "#srcIP# AND #dstIP# AND #srcPort# AND #dstPort# AND #proto# AND #dstTos# AND #exporterID# AND #timeQuery#";
                else fillTmp = selectStr;
            } else {
                // no removal of identical entries, so run given statements directly
                tmpname = tableName;
                createTmp = createStr;
                fillTmp = selectStr;
            }

            // create the table and fill it
            if (createTempTable(tmpname, createTmp)==false) {
                // an error has occured
                dropTable(tmpname);
                output += "Error creating temporary table(s)! <p>";
                return false;
            }
            if (fillTempTable(tmpname, fillTmp)==false) {
                // an error has occured
                dropTable(tmpname);
                output += "Error filling temporary table(s)! <p>";
                return false;
            }
            
            if (useAlternate) try {
                // MySQLDump version needed to create tables in remote databases,
                // delete them (but don't delete the local table!)
                String destURL = tempDB.getConnection().getMetaData().getURL();
                for (int i=0; i<dbList.size(); i++) {
                    String sourceURL = ((Statement)dbList.get(i)).getConnection().getMetaData().getURL();
                    if (sourceURL.compareTo(destURL)!=0)
                        ((Statement)dbList.get(i)).execute("DROP TABLE IF EXISTS "+tmpname);
                }
            } catch (SQLException e) {
                dropTable(tmpname);
                output += "Error deleting remote temporary table(s)! <p>";
                return false;
            }
            
            if (remDoubles) {
                // remove identical entries
                
                // create the resulting table
                queryTempDB(createStr);
                
                // create internal select-statement that removes the identical entries
                String removal = "(SELECT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes," +
                                 " MIN(firstSwitched) AS firstSwitched, MAX(lastSwitched) AS lastSwitched, exporterID, exporterIP, databaseID FROM " + tmpname +
                            " GROUP BY srcIP,dstIP,srcPort,dstPort,proto,dstTos";
                removal += ",firstSwitched DIV "+remTimeDiv;
                if (checkAll) removal += ",exporterID,pkts,bytes";
                if (!checkAll) {
                    removal = removal.replaceAll("bytes", "MAX(bytes) AS bytes");
                    removal = removal.replaceAll("pkts", "MAX(pkts) AS pkts");
                }
                removal += ") AS tmp";

                // build the query that fills the resulting table
                selectStr = selectStr.replaceAll("#srctable#",removal);
                String fill = "INSERT INTO "+tableName+" "+selectStr;
                
                // run the query
                queryTempDB(fill);
                
                // drop the original table with all data
                dropTable(tmpname);
            }
            
            return true;
        }
        
        /**
         * Copies data from given standard table (with all available data columns) in the
         * local database into new table and removes identical entries in the process.
         * The resulting new table will have no identical data left while the old
         * table remains as it was.
         *
         * @param   oldTmpTable     Name of the old table with all source data.
         * @param   newTmpTable     Name of the new temporary table that will be created and
         *                          filled with unique data.
         * @param   remTimeDiv      Size of the time blocks (in seconds) in which the time stamps
         *                          of two entries must lie together to be considered identical.
         *                          (Setting values higher than one second may account for differences
         *                          in the time stamps given to the same traffic packets by different
         *                          exporters.)
         * @param   checkAll        Must the exporter ID, packets and bytes be identical as well (true)? On false, the
         *                          they may differ.
         * @param   tempFlag        True if the new table should be an SQL TEMPORARY table, false otherwise.
         */
        public void removeIdenticalData(String oldTmpTable, String newTmpTable, int remTimeDiv, boolean checkAll, boolean tempFlag) {
             boolean alterFlag = false;
             
             // check if old and new table name are identical
             if (oldTmpTable.equalsIgnoreCase(newTmpTable)) {
                // rename the old table, drop it later
                alterFlag = true;
                queryTempDB("ALTER TABLE "+oldTmpTable+" RENAME TO "+oldTmpTable+"old");
                oldTmpTable+="old";
             }

             // remove identical entries by grouping the temporary table into a new temp. table
             String createTmp = "CREATE "+(tempFlag?"TEMPORARY":"")+" TABLE "+newTmpTable+" SELECT srcIP, dstIP, srcPort, dstPort, proto, dstTos, pkts, bytes," +
                                 " MIN(firstSwitched) AS firstSwitched" + 
                                 ", MAX(lastSwitched) AS lastSwitched, exporterID, exporterIP, databaseID FROM " + oldTmpTable +
                                 " GROUP BY srcIP,dstIP,srcPort,dstPort,proto,dstTos";
             createTmp += ",firstSwitched DIV "+remTimeDiv;
             if (checkAll) createTmp += ",exporterID,pkts,bytes";
             if (!checkAll) {
                 createTmp = createTmp.replaceAll("bytes", "MAX(bytes) AS bytes");
                 createTmp = createTmp.replaceAll("pkts", "MAX(pkts) AS pkts");
             }
             queryTempDB(createTmp);
             
             if (alterFlag) {
                 // drop the renamed old table
                 dropTable(oldTmpTable);
             }
        }
        
        /**
         * Takes IP-address in simple long integer form and returns corresponding
         * address in the form XXX.XXX.XXX.XXX or the corresponding DNS string.
         *
         * @param addr      IP-address as an unformatted long integer.
         * @param resolve   Flag to signal if IP-address should be resolved
         *                  to DNS name (true) or not (false).
         * @return          String with IP-address in the form XXX.XXX.XXX.XXX or
         *                  the corresponding DNS name.
         */
        public String createIPOutput(long addr, boolean resolve) {
		
		InetAddress ip;
		ByteBuffer buf = ByteBuffer.allocate(4);
		
		// read ip address into byte buffer
                buf.putInt((int)addr);
		
		// create InetAddress-object from buffer
                try {
			ip = InetAddress.getByAddress(buf.array());
		} catch (UnknownHostException e) {
			return Long.toString(addr);
		}
                
                // return DNS name or IP-address in the form XXX.XXX.XXX.XXX
		if (resolve) 
			return ip.getCanonicalHostName();
		else
			return ip.getHostAddress();
	}
	
        /**
         * Takes port number as integer and returns string with clear text name
         * of this port on the current system or the number as string.
         *
         * @param port      Port number.
         * @param resolve   Try to find clear text name for port (true) or just
         *                  return port number as string (false).
         * @return          Port number as string or clear text name of port.
         */
        public String createPortOutput(int port, boolean resolve) {
		
		String output = "";
		
		if (port == 0)
			return "0";
		
		if (tcpServices != null) {
			
			if ((port < 1024) && resolve) {
				// try to resolve port number into clear text
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
	
        /**
         * Takes protocol number as integer and returns string with clear text name
         * of this protocol on the current system or the number as string.
         *
         * @param proto     Protocol number.
         * @param resolve   Try to find clear text name for protocol (true) or just
         *                  return protocol number as string (false).
         * @return          Protocol number as string or clear text name of protocol.
         */
	public String createProtoOutput(short proto, boolean resolve) {
		
		String output = "";
		
		if (protocols != null) {
			
			if (proto < 256 && resolve) {
				// try to find clear text name for protocol
                                if ((output=protocols[proto]) != null) {} 
				else
					output += proto;
			} else
				output += proto;
		} else
			output += proto;
		
		return output;
	}
	
        /**
         * Takes time in milliseconds since 1970 and returns clear text version.
         *
         * @param time      Time in milliseconds since 1970.
         * @return          String with time and date as clear text.
         */
        public String createTimeOutput(long time) {
        	Date date = new Date();
		
		date.setTime(time);
		
		return date.toString();
	}
	
        /**
         * Takes IP-address in string-form (as entered by user as parameter) and
         * database table column name and builds an SQL WHERE-clause that checks if
         * database entries match given parameter.
         *
         * @param addr      String with IP-address (as entered by user as parameter).
         * @param colName   Name of database table column that the WHERE-clause relates to.
         * @return          String with SQL-WHERE-clause (without the "WHERE"-keyword).
         */
	private String createIPQuery(String addr, String colName) {
		
		InetAddress ip = null;
		short subnetMask = 32;
		String[] splitString;
		long ipAddr = 0;
		long lowerBound = 0;
		long upperBound = 0;
		
		if (addr==null || colName==null || addr.equalsIgnoreCase("") || addr.equalsIgnoreCase("*"))
			return "";
		
                // check if only part of an IP address has been given
                // and if so, transform it into subnet-query
                if (addr.indexOf('/')==-1) {    
                    // count the number of dots in given address
                    int dotPos = -1;
                    int i = -1;
                    int dotCount = 0;
                    while ((dotPos=addr.indexOf('.', dotPos+1))>i) {
                        i = dotPos;
                        dotCount++;
                    }
                    // fill up missing part of address
                    for (i=dotCount; i<3; i++) {
                        addr += ".0";
                    }
                    // add number of fixed bits for subnet-query
                    if (dotCount<3) addr += "/"+((dotCount+1)*8);
                }
                
                // transform address into numeric value and build appropriate SQL query clause
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

        /**
         * Takes port in string-form (as entered by user as parameter) and
         * database table column name and builds an SQL WHERE-clause that checks if
         * database entries match given parameter.
         *
         * @param port       String with port (as entered by user as parameter).
         * @param colName    Name of database table column that the WHERE-clause relates to.
         * @param aggregated Signals if ports are aggregated in database table (true) or not (false).
         * @return           String with SQL-WHERE-clause (without the "WHERE"-keyword).
         */
	private String createPortQuery(String port, String colName, boolean aggregated) {
		
		String[] splitString;
		int[] portNumArray;
		int lowerBound = 0;
		int upperBound = 0;
		int portNum;
		
		if (port==null || colName==null || port.equalsIgnoreCase("")  || port.equalsIgnoreCase("*"))
			return "";
		
		splitString=port.split("-");
                if ((port.indexOf(",")==-1) && (splitString.length > 1)) {
			
                        // boundaries for port were given
			if (splitString.length == 2) {
                                // check if boundaries are correct numbers
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
					// not correct numbers, maybe single port name with "-" in it
					if (portMap != null) {
						portNumArray = (int[])portMap.get(port.toLowerCase());
						if (portNumArray != null)
							// yes, single port name was given
                                                        // use SQL = syntax
							return colName + "=" + portNumArray[0];
						else {
							// no port with given name was found
							if (colName.equalsIgnoreCase("srcPort"))
								output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
							if (colName.equalsIgnoreCase("dstPort"))
								output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
							return "";
						}
					} else {
						// no list of port names exists
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
				
				// correct boundaries were given, use SQL BETWEEN syntax
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				if (colName.equalsIgnoreCase("srcPort"))
					output += "<p>Entered Source Port was invalid and therefore omitted.</p><br>";
				if (colName.equalsIgnoreCase("dstPort"))
					output += "<p>Entered Destination Port was invalid and therefore omitted.</p><br>";
				return "";
			}
		
		} else {
			
                        // port must be in given value list, separated by comma
                        splitString=port.split(",");
                        String ret = "";
                        
                        // check if each given value is a number or a port name
			for (int i=0; i<splitString.length; i++) {
                            try {
				portNum = Integer.parseInt(splitString[i]);
                            } catch (NumberFormatException e) {
				// port name -> try to get its numeric value
				if (portMap != null) {
					portNumArray = (int[])portMap.get(splitString[i].toLowerCase());
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
			
                            ret += ","+portNum;
                        }
			// remove first comma
                        ret = ret.substring(1);
                        
			if (splitString.length>0)  {
                            // multiple values, use SQL IN syntax
                            return colName + " IN (" + ret + ")";
                        } else {
                            // single value, use SQL = syntax
                            return colName + "=" + ret;
                        }
		}
	}
	
        /**
         * Takes protocol in string-form (as entered by user as parameter) and
         * database table column name and builds an SQL WHERE-clause that checks if
         * database entries match given parameter.
         *
         * @param proto     String with protocol (as entered by user as parameter).
         * @param colName   Name of database table column that the WHERE-clause relates to.
         * @return          String with SQL-WHERE-clause (without the "WHERE"-keyword).
         */
	private String createProtoQuery(String proto, String colName) {
		
		String[] splitString;
		short[] protoNumArray;
		short lowerBound = 0;
                short upperBound = 0;
		short protoNum;
		
		if (proto==null || colName==null || proto.equalsIgnoreCase("")  || proto.equalsIgnoreCase("*"))
			return "";
		
		splitString = proto.split("-");
                if ((proto.indexOf(",")==-1) && (splitString.length > 1)) {

                        // boundaries for protocol were given
			if (splitString.length == 2) {
                                // check if boundaries are correct numbers
				try {
					lowerBound = Short.parseShort(splitString[0]);
					if (lowerBound > 256) {
						output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}
					upperBound = Short.parseShort(splitString[1]);
					if (upperBound > 256) {
						output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}
				} catch (NumberFormatException e) {
					// not correct numbers, maybe single protocol name with "-" in it
					if (protocolMap != null) {
						protoNumArray = (short[])protocolMap.get(proto.toLowerCase());
						if (protoNumArray != null)
							// yes, single protocol name was given
                                                        // use SQL = syntax
                                                        return colName + "=" + protoNumArray[0];
						else {
							// no protocol with given name was found
                                                        output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
							return "";
						}
					} else {
						// no list of protocol names exists
                                                output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
						return "";
					}
				}
				
				// boundaries were given, use SQL BETWEEN syntax
                                return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				output += "<p>Entered Protocol was invalid and therefore omitted.</p><br>";
				return "";
			}
		
		} else {
			
                        // protocol must be in given value list, separated by comma
                        splitString=proto.split(",");
                        String ret = "";
                        
                        // check if each given value is a number or a protocol name
			for (int i=0; i<splitString.length; i++) {
                            try {
				protoNum = Short.parseShort(splitString[i]);
                            } catch (NumberFormatException e) {
				// protocol name -> try to get its numeric value
                                if (protocolMap != null) {
					protoNumArray = (short[])protocolMap.get(splitString[i].toLowerCase());
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
                            ret += ","+protoNum;
                        }
			// remove first comma
                        ret = ret.substring(1);
                        
			if (splitString.length>0)  {
                            // multiple values, use SQL IN syntax
                            return colName + " IN (" + ret + ")";
                        } else {
                            // single value, use SQL = syntax
                            return colName + "=" + ret;
                        }
		}
	}
	
        /**
         * Takes tos in string-form (as entered by user as parameter) and
         * database table column name and builds an SQL WHERE-clause that checks if
         * database entries match given parameter.
         *
         * @param tos       String with tos (as entered by user as parameter).
         * @param colName   Name of database table column that the WHERE-clause relates to.
         * @return          String with SQL-WHERE-clause (without the "WHERE"-keyword).
         */
	private String createTosQuery(String tos, String colName) {
		
		String[] splitString;
		short lowerBound;
		short upperBound;
		short byteNum;
		
		if (tos==null || colName==null || tos.equalsIgnoreCase("") || tos.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=tos.split("-")).length > 1) {
			
			// boundaries for tos were given
			if (splitString.length == 2) {
                                // check if boundaries are correct numbers
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
					return "";
				}
				
				return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
				return "";
			}
		
		} else {
			
                        // tos must be in given value list, separated by comma
                        splitString=tos.split(",");
                        
                        // check if each given value is a number
			for (int i=0; i<splitString.length; i++) {
                            try {
				byteNum = Short.parseShort(splitString[i]);
                            } catch (NumberFormatException e) {
				output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
				return "";
                            }
			
                            if (byteNum > 255) {
				output += "<p>Entered ToS was invalid and therefore omitted.</p><br>";
				return "";
                            }
                        }
			
			if (splitString.length>0)  {
                            // multiple values, use SQL IN syntax
                            while (tos.endsWith(",")) tos = tos.substring(0, tos.length()-1);
                            return colName + " IN (" + tos + ")";
                        } else {
                            // single value, use SQL = syntax
                            return colName + "=" + tos;
                        }
		}
	}

        /**
         * Takes exporter ID in string-form (as entered by user as parameter) and
         * database table column name and builds an SQL WHERE-clause that checks
         * if database entries match given parameter.
         *
         * @param exporterID    String with exporter ID (as entered by user as parameter).
         * @param colName       Name of database table column that the WHERE-clause relates to.
         * @return              String with SQL-WHERE-clause (without the "WHERE"-keyword).
         */
	private String createExporterIDQuery(String exporterID, String colName) {
		
		String[] splitString;
		int lowerBound;
		int upperBound;
		int expNum;
		
		if (exporterID==null || colName==null || exporterID.equalsIgnoreCase("") || exporterID.equalsIgnoreCase("*"))
			return "";
		
		if ((splitString=exporterID.split("-")).length > 1) {
			
			// boundaries for exporter id were given
                        if (splitString.length == 2) {
                                // check if boundaries are correct numbers
				try {
					lowerBound = Integer.parseInt(splitString[0]);
					if (lowerBound > 65535) {
						output += "<p>Entered exporter ID was invalid and therefore omitted.</p><br>";
						return "";
					}
				
					upperBound = Integer.parseInt(splitString[1]);
					if (upperBound > 65535) {
						output += "<p>Entered exporter ID was invalid and therefore omitted.</p><br>";
						return "";
					}
					
				} catch (NumberFormatException e) {
					output += "<p>Entered exporter Id was invalid and therefore omitted.</p><br>";
					return "";
				}
				
				// use SQL BETWEEN syntax
                                return colName + " BETWEEN " + lowerBound + " AND " + upperBound;
				
			} else {
				output += "<p>Entered exporter ID was invalid and therefore omitted.</p><br>";
				return "";
			}
		
		} else {
                    
                        // exporter id must be in given value list, separated by comma
                        splitString=exporterID.split(",");
                        
                        // check if each given value is a number
			for (int i=0; i<splitString.length; i++) {
                            try {
				expNum = Integer.parseInt(splitString[i]);
                            } catch (NumberFormatException e) {
				output += "<p>Entered exporter ID was invalid and therefore omitted.</p><br>";
				return "";
                            }
			
                            if (expNum > 65535) {
				output += "<p>Entered exporter ID was invalid and therefore omitted.</p><br>";
				return "";
                            }
                        }
			
			if (splitString.length>0)  {
                            // multiple values, use SQL IN syntax
                            while (exporterID.endsWith(",")) exporterID = exporterID.substring(0, exporterID.length()-1);
                            return colName + " IN (" + exporterID + ")";
                        } else {
                            // single value, use SQL = syntax
                            return colName + "=" + exporterID;
                        }
		}
	}
	
        /**
         * Builds an SQL-WHERE-clause for the given time bounds of the query.
         *
         * @param startTime Start time for query in milliseconds since 1970.
         * @param endTime   End time for query in milliseconds since 1970.
         * @return          String with SQL-WHERE-clause (without the "WHERE"-keyword).
         */
	private String createTimeQuery(long startTime, long endTime) {
		
        	// create WHERE-clause and return it
                if (startTime < endTime) {
                    return "firstSwitched BETWEEN " + startTime/1000 + " AND " + (endTime/1000-1); //endTime not included 
			//" AND lastSwitched BETWEEN " + startTime/1000 + " AND " + endTime/1000 + " ";
		} else {
                    output += "<p>Entered time range was invalid and therefore omitted.</p><br>";
                    return "";
		}
		
	}
}
