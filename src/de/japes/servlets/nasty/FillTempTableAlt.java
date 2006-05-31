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
 * Title:   FillTempTableAlt
 * Project: NASTY
 *
 * @author  Thomas Schurtz
 * @version %I% %G%
 */
package de.japes.servlets.nasty;

import java.sql.Statement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.*;


/**
 *  Alternate version of <code>FillTempTable</code> that uses external tool mysqldump
 *  for transferring data from remote database tables to temporary table in local database.
 *  
 *  Needs CREATE-rights on databases. CREATE TEMPORARY TABLES-rights would not be enough because
 *  MySQL-TEMPORARY-tables are connection specific and mysqldump uses different connection.
 *
 *  Needs temporarily created tables (but no MySQL-TEMPORARY-tables) in all remote databases 
 *  as opposed to normal java-internal version because data is collected there first 
 *  and then transferred "en bloc". (mysqldump can only copy complete tables, not query-results.)
 *
 *  Needs detailed information on source and destination database: Host, user, password, name of
 *  database as this data cannot be extracted from given JDBC-connection objects but needs to be
 *  sent to mysqldump-tool.
 *
 *  Threadable class that queries the remote collector-database <code>sourceDB</code> 
 *  and stores the results in local temporary table <code>tmpTable</code> of <code>destDB</code>.
 *  <p>
 *  Thread works in two phases: <p>
 *  1. Determine relevant source tables and check if "tosMissing" and "portsAggregated".
 *  <p>
 *  Caller then composes SQL-query <code>queryStr</code> based on "tosMissing" and "portsAggregated"
 *  from all threads to extract data from relevant tables. (Placeholder <code>#srctable#</code> 
 *  must be used in query, it will be replaced by the name of each source table.)
 *  Caller then restarts threads (second phase).
 *  <p>
 *  2. Extract the data with now given query and store it into local <code>tmpTable</code>.
 */
public class FillTempTableAlt extends FillTempTable implements Runnable {

  /** Array of data for source database: host, user, password, name of database*/
  protected DBInfo sourceInfo;
  /** Array of data for destination database: host, user, password, name of database*/
  protected DBInfo destInfo;

 
  /**
   * Class constructor.
   *
   * @param sourceDB    Statement-object for (possibly remote) source database.
   * @param databaseID  Identifier for source database.
   * @param sourceInfo  Array of data for source database (host, user, password, db-name).    
   * @param destDB      Statement-object for (preferrably local) database that holds
   *                    the temporary table to be filled.
   * @param destInfo    Array of data for destination database (host, user, password, db-name).    
   * @param tmpTable    Name of temporary table.
   * @param startTime   Start time for query, needed to determine relevant source tables.
   * @param endTime     End time for query, needed to determine relevant source tables.
   * @param output      String to add eventual output messages, for example error messages.
   */
  public FillTempTableAlt(Statement sourceDB, int databaseID, DBInfo sourceInfo, Statement destDB, DBInfo destInfo,
          String tmpTable, long startTime, long endTime) {
    super(sourceDB,databaseID,destDB,tmpTable, startTime, endTime);
    this.sourceInfo = sourceInfo;
    this.destInfo = destInfo;
  }


  /**
   * Thread phase 2: Extract the data with now given query and store it into temporary table.
   * (Alternate version with mysqldump!)
   * <p>
   * This method runs the SQL-query provided in <code>queryStr</code> on all relevant
   * source tables. For each source table, the placeholder <code>#srctable#</code> that
   * has to be used in the SQL-query-string is replaced by the actual name of the source table.
   * The results of each query are first written into the temporary table of the remote database
   * and then transferred into local temporary table by mysqldump.
   * Also added will be the database identifier (by replacing "databaseID" in the SELECT-statement
   * with the actual value).
   */
  public void fillTables() {
    Iterator it = tables.listIterator();    // list of relevant source-tables
    String srctable="";                     // holds name of current source-table
		
    // add the database identifier into the SELECT statement
    queryStr = queryStr.replaceAll("databaseID", ""+databaseID);

    // iterate through all relevant source-tables
    while (it.hasNext()) {
      try {
         // extract data from source table and store it into remote temporary database
         srctable = (String)it.next();
         sourceDB.executeUpdate("INSERT IGNORE INTO "+tmpTable+" "+queryStr.replaceAll("#srctable#",srctable));
      } catch (SQLException e) {
         // error, report to caller
         output += "Error while filling temporary database from source table "+srctable+"!";
         output += "<p>"+e.getMessage()+"<p>";        
         success = false;
      }
    }

    // copy data from remote temporary table into local temporary table via mysqldump
    // (needs not be done if remote and local databases are the same)
    if (!dbIdentical) try {
        // different syntax for calling command line in windows and linux
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt=Runtime.getRuntime();
        String[] command = {"","",""};
        if (os.indexOf("windows")>-1) {
            command[0] = "cmd.exe";
            command[1] = "/c";
        } else {
            command[0] = "/bin/sh";
            command[1] = "-c";
        }
        // build mysqldump-command
        // check if port was given
        String sourceHost = "";
        String sourcePort = "";
        if (sourceInfo.getHost().indexOf(":")>-1) {
            sourceHost = sourceInfo.getHost().split(":")[0];
            sourcePort = "--port="+sourceInfo.getHost().split(":")[1];
        } else sourceHost = sourceInfo.getHost();
        String destHost = "";
        String destPort = "";
        if (destInfo.getHost().indexOf(":")>-1) {
            destHost = destInfo.getHost().split(":")[0];
            destPort = "--port="+destInfo.getHost().split(":")[1];
        } else destHost = destInfo.getHost();
        

        if (sourceInfo.isUseSSH()) {
            // Version mit SSH
            command[2] = "ssh -q "+sourceHost+" mysqldump "+sourcePort+" --user="+sourceInfo.getUser()
                +" --password="+sourceInfo.getPassword()+" -t --compact --lock-tables=FALSE "+sourceInfo.getName()
                +" "+tmpTable;
        } else {
            // Version ohne SSH
            command[2] = "mysqldump --host="+sourceHost+" "+sourcePort+" --user="+sourceInfo.getUser()
                +" --password="+sourceInfo.getPassword()+" -t --compact --compress --lock-tables=FALSE "+sourceInfo.getName()
                +" "+tmpTable;
        }
        if (destInfo.isUseSSH()) {
            // Version mit SSH
            command[2] += " | ssh -q "+destHost+" mysql "+destPort+" --user="+destInfo.getUser()
                +" --password="+destInfo.getPassword()+" -C "+destInfo.getName();
        
        } else {
            // Version ohne SSH
            command[2] += " | mysql --host="+destHost+" "+destPort+" --user="+destInfo.getUser()
                +" --password="+destInfo.getPassword()+" "+destInfo.getName();
        }
        
        
        // execute command and wait for it to finish
        Process proc=rt.exec(command);
        /*BufferedInputStream bis = new BufferedInputStream(proc.getErrorStream());
        int i=0;
        while ((i = bis.read())!=-1) { 
            output+=(char)i;
        }*/
        Thread.yield();
        proc.waitFor();
    } catch (Exception e) {
        // error, report to caller
        output += "Error while filling temporary database from source table "+srctable+" with mysqldump!";
        output += "<p>"+e.getMessage()+"<p>";        
        success = false;
    }
  }
  
}
