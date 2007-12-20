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
 * Title:   FillTempTable
 * Project: NASTY
 *
 * @author  Thomas Schurtz
 * @version %I% %G%
 */
package de.japes.servlets.nasty;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.LinkedList;
import java.util.Iterator;


/**
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
public class FillTempTable implements Runnable {

  /** 
   * JDBC-Statement-object for (possibly remote) source database. 
   * @see Statement
   */
  protected Statement sourceDB;
  /** Identifier for source database. */
  protected int databaseID;
  /** 
   * JDBC-Statement-object for (preferrably local) destination database
   * holding the temporary table.
   * @see Statement
   */
  protected Statement destDB;
  /** Name of temporary table in local database. */
  protected String tmpTable;

  /** Flag to signal if source and destination database are the same. */
  protected boolean dbIdentical = false;
  /** Flag to signal if tos-data is missing in at least one source-table. */
  protected boolean tosMissing = false;
  /** Flag to signal if ports are aggregated in at least one source-table. */
  protected boolean portsAggregated = false;
  /** Flag to signal if task was successfully completed. */
  protected boolean success = true;
  /** String to add eventual output messages, for example error messages. */
  protected String output = "";

  /** Start time for determining relevant source-tables. */
  protected long startTime;
  /** End time for determining relevant source-tables. */
  protected long endTime;
  /** List of relevant source-tables. */
  protected LinkedList tables = null;
  
  /** Performance watch. */
  long i = 0;

  /**
   * SQL-statement to query relevant source tables
   * (placeholder <code>#srctable#</code> must be used).
   * <p>
   * If <code>queryStr</code> is empty -> Phase 1, else -> Phase 2.
   */
  protected String queryStr = ""; 

  
  
  /**
   * Class constructor.
   *
   * @param sourceDB    Statement-object for (possibly remote) source database.    
   * @param databaseID  Identifier for source database.
   * @param destDB      Statement-object for (preferrably local) database that holds
   *                    the temporary table to be filled.
   * @param tmpTable    Name of temporary table.
   * @param startTime   Start time of query, needed to determine relevant source tables.
   * @param endTime     End time of query, needed to determine relevant source tables.
   * @param output      String to add eventual output messages, for example error messages.
   */
  public FillTempTable(Statement sourceDB, int databaseID, Statement destDB, String tmpTable, long startTime, long endTime) {
    this.sourceDB = sourceDB;
    this.databaseID = databaseID;
    this.destDB = destDB;
    this.tmpTable = tmpTable;
    this.startTime = startTime;
    this.endTime = endTime;
  }

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
   * Returns success-flag which signals if task of this thread was successfully completed.
   *
   * @return    true if everything went okay, false otherwise.
   */
  public boolean isSuccess() { return success; }
  
  /** 
   * Returns output-string with eventual messages for the user, for example error messages.
   *
   * @return    Output-String.
   */
  public String getOutput() { return output; }

  /** 
   * Sets SQL-query to extract data from relevant source tables. Once an SQL-query has been
   * provided by the caller, the second phase of thread can be initiated in which
   * the temporary table gets filled with data from relevant source tables.
   *
   * @param queryStr    SQL-SELECT-statement that reads data from source tables. Placeholder
   *                    <code>#srctable#</code> has to be used and will be replaced in turn 
   *                    by the name of each relevant source table.
   */ 
  public void setQueryStr(String queryStr) { 
    if (queryStr!=null) this.queryStr = queryStr;
  }

  /**
   * Main thread function, determines phase of thread and executes it.
   * <p>
   * Determination is based on <code>queryStr</code>. If it is empty and no SQL-query-string has
   * been provided, thread will execute first phase and determine all relevant source tables.
   * Otherwise the given query will be run on all relevant tables, with the results being written
   * into the temporary table of the local database.
   */
  public void run() {
      output = "";
      
      if (queryStr.equals("")) {
        // thread phase 1: determine relevant source tables and check "tosMissing" and "portsAggregated"

        // ************Performance watch**********************
        //i = System.currentTimeMillis();
        // ***************************************************
        
        getTables();

        // ************Performance watch**********************
        //i = (System.currentTimeMillis()-i);
        //output += "Phase 1 of thread "+Thread.currentThread().getName()+" finished after "+i+" ms.<p>";
        i = System.currentTimeMillis();
        // ***************************************************
    }
    else {
        // thread phase 2: extract the data with now given query and store it into temp. table

        // ************Performance watch**********************
        //i = System.currentTimeMillis();
        // ***************************************************

        long rowcount = fillTables();

        // ************Performance watch**********************
        i = (System.currentTimeMillis()-i);
        output += Thread.currentThread().getName()+" finished after "+i+" ms, resulted in "+rowcount+" rows.<p>";
        // ***************************************************
    }
  }

  /**
   * Thread phase 1: Determine relevant source tables and check "tosMissing" and "portsAggregated".
   * <p>
   * Based on the given time bounds, this method goes through all available source tables and
   * determines which ones are relevant (meaning which ones contain data from within
   * the time bounds).
   * <p>
   * It is also checked if tos-data is missing and if ports are aggregated in any of the
   * relevant tables. If so, the appropriate flags <code>tosMissing</code> and 
   * <code>portsAggregated</code> are set.
   */
  public void getTables() {

    tables = new LinkedList();
    ResultSet result;
    String tableName;
    String year;
    String month;
    String day;
    String hour = "0";
    String half = "0";
    String statement = "";
    boolean hourTable = false;
    boolean dayTable = false;
    boolean weekTable = false;
		
    Calendar tableTime = new GregorianCalendar(TimeZone.getTimeZone("GMT+00:00"));
		
    // the SQL-statement to get the names of the available tables in source database
    statement = "SHOW TABLES LIKE '_\\_%'";
			
    try {
      // check if source- and destination database are the same
      // (second phase of thread would then not have to copy data between databases)
      String sourceURL = sourceDB.getConnection().getMetaData().getURL();
      String destURL = destDB.getConnection().getMetaData().getURL();
      if (sourceURL.compareTo(destURL)==0) dbIdentical = true;
              
      // execute query to get list of tables
      result = sourceDB.executeQuery(statement);
			
      // iterate through available tables and determine if they are relevant
      // (also set flags tosMissing and portsAggregated if necessary)
      while (result.next()) {
				
	tableName = result.getString(1);

        // extract time bounds of data in current table from its name
        String[] parts = tableName.split("_");
	year = parts[1].substring(0,4);
	month= parts[1].substring(4,6);
	day  = parts[1].substring(6);
				
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
				
	// compare time bounds of current table with time bounds of query and
        // insert the table into list of relevant tables if it contains relevant data
        if (hourTable) {
            if (tabTime <= endTime && tabTime+30*60*1000 >= startTime)  {
		tables.add(tableName);
            }
	} else if (dayTable) {
            if (tabTime <= endTime && tabTime+24*60*60*1000 >= startTime) {
                tosMissing = true;
		tables.add(tableName);
            }
	} else if (weekTable) {
            if (tabTime <= endTime && tabTime+7*24*60*60*1000 >= startTime) {
        	tosMissing = true;
               	portsAggregated = true;
		tables.add(tableName);
            }
	}
					
	hourTable = false;
	dayTable = false;
	weekTable = false;
				
      }
		
    } catch (SQLException e) {
	// error
        tables = null;
        success = false;
        output += "Error while determining relevant source tables!";
        output += "<p>"+e.getMessage()+"<p>";
    }
			
  }

  /**
   * Thread phase 2: Extract the data with now given query and store it into temporary table.
   * <p>
   * This method runs the SQL-query provided in <code>queryStr</code> on all relevant
   * source tables. For each source table, the placeholder <code>#srctable#</code> that
   * has to be used in the SQL-query-string is replaced by the actual name of the source table.
   * The results of each query are written into the temporary table of the local database.
   * Also added will be the database identifier (by replacing "databaseID" in the SELECT-statement
   * with the actual value).
   * <p>
   * (If any unique-index- or unique-key-violation occurs while inserting data into temporary
   * table, this method will catch the corresponding SQL-exception and continue unaffectedly,
   * with the duplicate data just not being inserted.)
   */
  public long fillTables() {
    // list of relevant source-tables
    Iterator it = tables.listIterator();
    // holds results of each queried source table
    ResultSet result;
    // buffer to build SQL-statement for inserting remote data into local temp. table
    // (StringBuffer is much faster than String)
    StringBuffer statement=new StringBuffer("");
    // name of current source table
    String srctable="";
    // number of columns in database table
    int columnCount;
    long rowcount = 0;

    // add the database identifier into the SELECT statement
    queryStr = queryStr.replaceAll("databaseID", ""+databaseID);
    
    // iterate through all relevant source-tables
    while (it.hasNext()) {
      try {

        // check if source and dest. database are the same
        if (dbIdentical) {

            // databases identical, read data directly via SQL into temporary table
            srctable = (String)it.next();
            rowcount += destDB.executeUpdate("INSERT IGNORE INTO "+tmpTable+" "+queryStr.replaceAll("#srctable#",srctable));

        } else {
            // databases not identical, transfer data "manually"
	    //
	    final int MAX_ROWS = 50000;
            // read data from source-table
            srctable = (String)it.next();
	    String curQueryStr = queryStr.replaceAll("#srctable#",srctable);
	    long pos = 0;
	    boolean firstValue;
	    statement = new StringBuffer();
	    do {
		// read only 50000 rows in one step to avoid heap space out of memory error
		result = sourceDB.executeQuery(curQueryStr+" LIMIT "+pos+","+MAX_ROWS);
		columnCount = result.getMetaData().getColumnCount();
		statement.append("INSERT IGNORE INTO "+tmpTable+" VALUES ");

		// insert data into local temporary table
		// compose SQL-INSERT statement from extracted remote data
		firstValue=true;
		while (result.next()) {
		    if (!firstValue) {
			statement.append(",");
		    } else firstValue=false;
		    statement.append("(");
		    int i;
		    for (i=1; i<columnCount; i++) {
			statement.append("'"+result.getString(i)+"',");
		    }
		    statement.append("'"+result.getString(i)+"')");
		}
		// execute INSERT-statement (but only if it contains any data)
		if (!firstValue)
		    rowcount +=  destDB.executeUpdate(statement.toString());
		// release results and statement buffer
		result.close();
		statement.setLength(0);
		pos += MAX_ROWS;
	    } while(!firstValue);
        }

      } catch (SQLException e) {
            // error, report to caller
            output += "Error while filling temporary database from source table "+srctable+"!";
            output += "<p>"+e.getMessage()+"<p>";        
            success = false;
	    return 0;
      }
      
    } // while (it.hasNext())
    return rowcount;
  }

  
}
