/*
 * Created on 19.10.2004
 */

package de.japes.net.nasty.collector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;

import de.japes.parser.nasty.ConfigParser;
import de.japes.parser.nasty.ParseException;

/**
 * @author unrza88
 */
public class Aggregator extends Thread {
	
	Connection con = null;
	Statement s = null;
	ResultSet result = null;
	long numSets = 0;
	String tableName = null;
	
	private HashSet knownTables = new HashSet();
	
	public Aggregator() {
		
		if (!ConfigParser.isInitialized()) {
			
			try {
				new ConfigParser(new FileInputStream(new File("./collector.cfg")));
			} catch (FileNotFoundException e) {
				System.err.println("Config file not found.");
				System.exit(-1);
			}
			
			try {
				ConfigParser.Start();
			} catch(ParseException e) {
				System.err.println("Syntax error in config file:");
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		}
		
	}
	
	public void run() {
		
		con = openDatabase();
		
		if (con == null) {
			System.err.println("Connection to database couldn't be established.");
			return;
		}
		
		Calendar currTime = new GregorianCalendar();
		int day = 0;
		
		System.out.println("StartTime Aggregator: " + currTime.getTime());
		
		//use last midnight as reference point
		currTime.set(Calendar.HOUR, 0);
		currTime.set(Calendar.MINUTE, 0);
		currTime.set(Calendar.SECOND, 0);
		currTime.set(Calendar.MILLISECOND, 0);
		currTime.set(Calendar.AM_PM, Calendar.AM);
		
		try {
			s = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		
			result = s.executeQuery("SHOW TABLES LIKE 'h\\_%'");
			
			while (result.next()) {
				
				tableName = result.getString(1);
				
				aggregateTables(s, tableName, currTime);
			
			}
			
			Iterator it = knownTables.iterator();
			
			while (it.hasNext()) {
				removeDuplicates(s, (String)it.next());
				it.remove();
			}
			
			result = s.executeQuery("SHOW TABLES LIKE 'd\\_%'");
			
			if (currTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				
				currTime.setLenient(true);
				
				do {
					currTime.roll(Calendar.DATE, -1);
				} while (currTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);
				
			}
			
			while(result.next()) {
				
				tableName = result.getString(1);
				
				aggregateTablesOnlyPortsGreater1024(s, tableName, currTime);
			}
			
		} catch (SQLException e) {
			System.err.println("Error while aggregating tables.");
			System.err.println(e.getMessage());
		}
		
		System.out.println("EndTime Aggregator: " + new GregorianCalendar().getTime());
	}
	
	private Connection openDatabase() {
		
		String url = "jdbc:mysql://" + ConfigParser.getDBServer() + "/" + ConfigParser.getDBName();
		Connection con = null;
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			System.err.println("Failed to load MySQL driver.");
			return null;
		}
		
		try {
			con = DriverManager.getConnection(url, ConfigParser.getDBUser(), ConfigParser.getDBPass()); 
			s = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		} catch (SQLException e) {
			System.err.println("Failed to connect to database.");
			return null;
		}
		
		return con;
	}
	
	private void aggregateTables(Statement s, String table, Calendar currTime) {
		
		String currTable;
		String year;
		String month;
		String day;
		String hour;
		String half;
		String aggregationTable="";
		
		boolean isHourTable = false;
		
		ResultSet result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMinimumIntegerDigits(2);
		
		String[] parts = table.split("_");
		year = parts[1].substring(0,4);
		month= parts[1].substring(4,6);
		day	 = parts[1].substring(6);
		
		if (parts[0].equalsIgnoreCase("h")) {
			hour = parts[2];
			half = parts[3];
			isHourTable = true;
		} else { 
			hour = "0";
			half = "0";
		}
		
		Calendar tableTime = new GregorianCalendar();
		
		if (isHourTable) {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day), Integer.parseInt(hour),
					Integer.parseInt(half)==0?0:30, 0);
			
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getDaysDetailedData()*24*60*60*1000+30*60*1000)) {
				return;
			}
			
			aggregationTable = 	"d_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
			
		} else {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day));
			
			tableTime.set(Calendar.HOUR, 0);
			tableTime.set(Calendar.MINUTE, 0);
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getWeeksDailyData()*7*24*60*60*1000+1*24*60*60*1000))
				return;
			
			if (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				tableTime.setLenient(true);
			
				do {
					tableTime.roll(Calendar.DATE, -1);
				} while (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);
			}
			
			aggregationTable = 	"w_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		}
		
		try {
			if (!knownTables.contains(aggregationTable)) {
				s.executeUpdate("CREATE TABLE IF NOT EXISTS " + aggregationTable + "_tmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
						" srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " +
						" firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
				
				knownTables.add(aggregationTable);
			}
		} catch (SQLException e) {
			System.err.println("Couldn't create table " + aggregationTable + "_tmp.");
			System.err.println(e.getMessage());
		}
		
		try {
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, COUNT(*) FROM " +
					table + " GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID)");

			s.executeUpdate("LOCK TABLES " + table + " WRITE");
			s.executeUpdate("DROP TABLE " + table);
			
		} catch (SQLException e) {
			System.err.println("Error while aggregating table " + table);
			System.err.println(e.getMessage());
		}
	}
	
	private void aggregateTablesOnlyPortsGreater1024(Statement s, String table, Calendar currTime) {
		
		String currTable;
		String year;
		String month;
		String day;
		String hour;
		String half;
		String aggregationTable="";
		
		boolean isHourTable = false;
		
		ResultSet result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMinimumIntegerDigits(2);
		
		String[] parts = table.split("_");
		year = parts[1].substring(0,4);
		month= parts[1].substring(4,6);
		day	 = parts[1].substring(6);
		
		if (parts[0].equalsIgnoreCase("h")) {
			hour = parts[2];
			half = parts[3];
			isHourTable = true;
		} else { 
			hour = "0";
			half = "0";
		}
			
		Calendar tableTime = new GregorianCalendar();
		
		if (isHourTable) {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day), Integer.parseInt(hour),
					Integer.parseInt(half)==0?0:30, 0);
			
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getDaysDetailedData()*24*60*60*1000+30*60*1000))
				return;
			
			aggregationTable = 	"d_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		
		} else {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day));
			
			tableTime.set(Calendar.HOUR, 0);
			tableTime.set(Calendar.MINUTE, 0);
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getWeeksDailyData()*7*24*60*60*1000+1*24*60*60*1000))
				return;
			
			if (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				tableTime.setLenient(true);
			
				do {
					tableTime.roll(Calendar.DATE, -1);
				} while (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);
			}
			
			aggregationTable = 	"w_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		}
		
		try {
			if (!knownTables.contains(aggregationTable)) {
				s.executeUpdate("CREATE TABLE IF NOT EXISTS " + aggregationTable + "_tmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
						" srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " +
						" firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
				
				knownTables.add(aggregationTable);
			}
		} catch (SQLException e) {
			System.err.println("Couldn't create table " + aggregationTable + "_tmp.");
			System.err.println(e.getMessage());
		}
		
		try {
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) FROM " +
					table + " WHERE srcPort<1024 AND dstPort<1024 GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, 0, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) FROM " +
					table + " WHERE srcPort<1024  AND dstPort>=1024 GROUP BY srcIP, dstIP, srcPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP, 0, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) FROM " +
					table + " WHERE srcPort>=1024  AND dstPort<1024 GROUP BY srcIP, dstIP, dstPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP, 0, 0, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows) FROM " +
					table + " WHERE srcPort>=1024  AND dstPort>=1024 GROUP BY srcIP, dstIP, proto, exporterID)");

			
			s.executeUpdate("LOCK TABLES " + table + " WRITE");
			s.executeUpdate("DROP TABLE " + table);
			
		} catch (SQLException e) {
			System.err.println("Error while aggregating table " + table);
			System.err.println(e.getMessage());
		}
	}
	
	private void aggregateTablesByDstSubnet24AndPortsGreater1024(Statement s, String table, Calendar currTime) {
		
		String currTable;
		String year;
		String month;
		String day;
		String hour;
		String half;
		String aggregationTable="";
		
		boolean isHourTable = false;
		
		ResultSet result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMinimumIntegerDigits(2);
		
		String[] parts = table.split("_");
		year = parts[1].substring(0,4);
		month= parts[1].substring(4,6);
		day	 = parts[1].substring(6);
		
		if (parts[0].equalsIgnoreCase("h")) {
			hour = parts[2];
			half = parts[3];
			isHourTable = true;
		} else { 
			hour = "0";
			half = "0";
		}
			
		
		
		Calendar tableTime = new GregorianCalendar();
		
		if (isHourTable) {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day), Integer.parseInt(hour),
					Integer.parseInt(half)==0?0:30, 0);
			
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getDaysDetailedData()*24*60*60*1000+30*60*1000))
				return;
			
			aggregationTable = 	"d_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		} else {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day));
			
			tableTime.set(Calendar.HOUR, 0);
			tableTime.set(Calendar.MINUTE, 0);
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getWeeksDailyData()*7*24*60*60*1000+1*24*60*60*1000))
				return;
			
			if (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				tableTime.setLenient(true);
			
				do {
					tableTime.roll(Calendar.DATE, -1);
				} while (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);
			}
			
			aggregationTable = 	"w_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		}
		
		try {
			if (!knownTables.contains(aggregationTable)) {
				s.executeUpdate("CREATE TABLE IF NOT EXISTS " + aggregationTable + "_tmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
						" srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " +
						" firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
				
				knownTables.add(aggregationTable);
			}
		} catch (SQLException e) {
			System.err.println("Couldn't create table " + aggregationTable + "_tmp.");
			System.err.println(e.getMessage());
		}
		
		try {
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP&0xffffff00 as dstSubnet, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, COUNT(*) FROM " +
					table + " WHERE srcPort<1024 AND dstPort<1024 GROUP BY srcIP, dstSubnet, srcPort, dstPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP&0xffffff00 as dstSubnet, srcPort, 0, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID , COUNT(*) FROM " +
					table + " WHERE srcPort<1024 AND dstPort>=1024 GROUP BY srcIP, dstSubnet, srcPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP&0xffffff00 as dstSubnet, 0, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID , COUNT(*) FROM " +
					table + " WHERE srcPort>=1024 AND dstPort<1024 GROUP BY srcIP, dstSubnet, dstPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP, dstIP&0xffffff00 as dstSubnet, 0, 0, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID , COUNT(*) FROM " +
					table + " WHERE srcPort>=1024 AND dstPort>=1024 GROUP BY srcIP, dstSubnet, proto, exporterID)");

			
			s.executeUpdate("LOCK TABLES " + table + " WRITE");
			s.executeUpdate("DROP TABLE " + table);
			
		} catch (SQLException e) {
			System.err.println("Error while aggregating table " + table);
			System.err.println(e.getMessage());
		}
	}
	
	private void aggregateTablesBySrcAndDstSubnet24AndPortsGreater1024(Statement s, String table, Calendar currTime) {
		
		String currTable;
		String year;
		String month;
		String day;
		String hour;
		String half;
		String aggregationTable="";
		
		boolean isHourTable = false;
		
		ResultSet result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMinimumIntegerDigits(2);
		
		String[] parts = table.split("_");
		year = parts[1].substring(0,4);
		month= parts[1].substring(4,6);
		day	 = parts[1].substring(6);
		
		if (parts[0].equalsIgnoreCase("h")) {
			hour = parts[2];
			half = parts[3];
			isHourTable = true;
		} else { 
			hour = "0";
			half = "0";
		}
			
		
		
		Calendar tableTime = new GregorianCalendar();
		
		if (isHourTable) {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day), Integer.parseInt(hour),
					Integer.parseInt(half)==0?0:30, 0);
			
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getDaysDetailedData()*24*60*60*1000+30*60*1000))
				return;
			
			aggregationTable = 	"d_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		} else {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day));
			
			tableTime.set(Calendar.HOUR, 0);
			tableTime.set(Calendar.MINUTE, 0);
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getWeeksDailyData()*7*24*60*60*1000+1*24*60*60*1000))
				return;
			
			if (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				tableTime.setLenient(true);
			
				do {
					tableTime.roll(Calendar.DATE, -1);
				} while (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);
			}
			
			aggregationTable = 	"w_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		}
		
		try {
			if (!knownTables.contains(aggregationTable)) {
				s.executeUpdate("CREATE TABLE IF NOT EXISTS " + aggregationTable + "_tmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
						" srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " +
						" firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
				
				knownTables.add(aggregationTable);
			}
		} catch (SQLException e) {
			System.err.println("Couldn't create table " + aggregationTable + "_tmp.");
			System.err.println(e.getMessage());
		}
		
		try {
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP&0xffffff00 as srcSubnet, dstIP&0xffffff00 as dstSubnet, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, COUNT(*) FROM " +
					table + " WHERE srcPort<1024 AND dstPort<1024 GROUP BY srcSubnet, dstSubnet, srcPort, dstPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP&0xffffff00 as srcSubnet, dstIP&0xffffff00 as dstSubnet, srcPort, 0, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID , COUNT(*) FROM " +
					table + " WHERE srcPort<1024 AND dstPort>=1024 GROUP BY srcSubnet, dstSubnet, srcPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP&0xffffff00 as srcSubnet, dstIP&0xffffff00 as dstSubnet, 0, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID , COUNT(*) FROM " +
					table + " WHERE srcPort>=1024 AND dstPort<1024 GROUP BY srcSubnet, dstSubnet, dstPort, proto, exporterID)");
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP&0xffffff00 as srcSubnet, dstIP&0xffffff00 as dstSubnet, 0, 0, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID , COUNT(*) FROM " +
					table + " WHERE srcPort>=1024 AND dstPort>=1024 GROUP BY srcSubnet, dstSubnet, proto, exporterID)");

			
			s.executeUpdate("LOCK TABLES " + table + " WRITE");
			s.executeUpdate("DROP TABLE " + table);
			
		} catch (SQLException e) {
			System.err.println("Error while aggregating table " + table);
			System.err.println(e.getMessage());
		}
	}
	
	private void aggregateTablesSubnet24AndAnyPorts(Statement s, String table, Calendar currTime) {
		
		String currTable;
		String year;
		String month;
		String day;
		String hour;
		String half;
		String aggregationTable="";
		
		boolean isHourTable = false;
		
		ResultSet result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setMinimumIntegerDigits(2);
		
		String[] parts = table.split("_");
		year = parts[1].substring(0,4);
		month= parts[1].substring(4,6);
		day	 = parts[1].substring(6);
		
		if (parts[0].equalsIgnoreCase("h")) {
			hour = parts[2];
			half = parts[3];
			isHourTable = true;
		} else { 
			hour = "0";
			half = "0";
		}
			
		
		
		Calendar tableTime = new GregorianCalendar();
		
		if (isHourTable) {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day), Integer.parseInt(hour),
					Integer.parseInt(half)==0?0:30, 0);
			
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getDaysDetailedData()*24*60*60*1000+30*60*1000))
				return;
			
			aggregationTable = 	"d_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		} else {
			
			tableTime.set(Integer.parseInt(year), Integer.parseInt(month)-1, 
					Integer.parseInt(day));
			
			tableTime.set(Calendar.HOUR, 0);
			tableTime.set(Calendar.MINUTE, 0);
			tableTime.set(Calendar.MILLISECOND, 0);
			
			// check if current table is older than specified time in config file
			if ((currTime.getTimeInMillis()-tableTime.getTimeInMillis()) <= (ConfigParser.getWeeksDailyData()*7*24*60*60*1000+1*24*60*60*1000))
				return;
			
			if (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				tableTime.setLenient(true);
			
				do {
					tableTime.roll(Calendar.DATE, -1);
				} while (tableTime.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY);
			}
			
			aggregationTable = 	"w_" + tableTime.get(Calendar.YEAR) + nf.format(tableTime.get(Calendar.MONTH)+1) +
			nf.format(tableTime.get(Calendar.DATE));
		}
		
		try {
			if (!knownTables.contains(aggregationTable)) {
				s.executeUpdate("CREATE TABLE IF NOT EXISTS " + aggregationTable + "_tmp (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
						" proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " +
						" firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
				
				knownTables.add(aggregationTable);
			}
		} catch (SQLException e) {
			System.err.println("Couldn't create table " + aggregationTable + "_tmp.");
			System.err.println(e.getMessage());
		}
		
		try {
			s.executeUpdate("INSERT INTO " + aggregationTable + "_tmp" +
					" (SELECT SQL_BIG_RESULT srcIP&0xffffff00 as srcSubnet, dstIP&0xffffff00 as dstSubnet, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, COUNT(*) FROM " +
					table + " GROUP BY srcSubnet, dstSubnet, proto, exporterID)");

			
			s.executeUpdate("LOCK TABLES " + table + " WRITE");
			s.executeUpdate("DROP TABLE " + table);
			
		} catch (SQLException e) {
			System.err.println("Error while aggregating table " + table);
			System.err.println(e.getMessage());
		}
	}
	
	private void removeDuplicates(Statement s, String table) {
		
		ResultSet duplGroups, duplEntries ;
		long srcIP, dstIP;
		int srcPort;
		short proto;
		long exporterID;
		long bytes;
		long pkts;
		
		try {
			s.executeUpdate("CREATE TABLE " + table + " (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
					" srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " +
					" firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
			
			s.executeUpdate("INSERT INTO " + table + " (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows)" +
					" FROM " + table + "_tmp GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID)");
			
			s.executeUpdate("DROP TABLE " + table + "_tmp");
			
		} catch (SQLException e) {
			System.err.println("Error ");
			System.err.println(e.getMessage());
		}
		
	}
	
	private void removeDuplicatesOnlyPortsGreater1024(Statement s, String table) {
		
		ResultSet duplGroups, duplEntries ;
		long srcIP, dstIP;
		int srcPort;
		short proto;
		long exporterID;
		long bytes;
		long pkts;
		
		try {
			
			s.execute("CREATE TABLE " + table + " (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED, " +
					"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT(3) UNSIGNED, " +
					" bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, firstSwitched INTEGER(10) UNSIGNED, " +
					" lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
			
			s.execute("INSERT INTO " + table + " (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows)" +
					" FROM " + table + "_tmp GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID)");
			
			s.execute("DROP TABLE " + table + "_tmp");
			
		} catch (SQLException e) {
			System.err.println("Error ");
			System.err.println(e.getMessage());
		}
		
	}
	
	private void removeDuplicatesWithSubnetsAndPortsGreater1024(Statement s, String table) {
		
		ResultSet duplGroups, duplEntries ;
		long srcIP, dstIP;
		int srcPort;
		short proto;
		long exporterID;
		long bytes;
		long pkts;
		
		try {
			
			s.executeUpdate("CREATE TABLE " + table + " (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED, " +
					"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " + 
					"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
			
			s.executeUpdate("INSERT INTO " + table + " (SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows)" +
					" FROM " + table + "_tmp GROUP BY srcIP, dstIP, srcPort, dstPort, proto, exporterID)");
			
			s.executeUpdate("DROP TABLE " + table + "_tmp");
			
		} catch (SQLException e) {
			System.err.println("Error ");
			System.err.println(e.getMessage());
		}
		
	}
	
	private void removeDuplicatesWithSubnetsAndAnyPorts(Statement s, String table) {
		
		ResultSet duplGroups, duplEntries ;
		long srcIP, dstIP;
		int srcPort;
		short proto;
		long exporterID;
		long bytes;
		long pkts;
		
		try {
			
			s.executeUpdate("CREATE TABLE " + table + " (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED, " +
					"proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED, pkts BIGINT(20) UNSIGNED, " + 
					"firstSwitched INTEGER(10) UNSIGNED, lastSwitched INTEGER(10) UNSIGNED, exporterID SMALLINT(5) UNSIGNED, aggFlows SMALLINT(5) UNSIGNED)");
			
			s.executeUpdate("INSERT INTO " + table + " (SELECT SQL_BIG_RESULT srcIP, dstIP, proto, SUM(bytes), SUM(pkts), MIN(firstSwitched), MAX(lastSwitched), exporterID, SUM(aggFlows)" +
					" FROM " + table + "_tmp GROUP BY srcIP, dstIP, proto, exporterID)");
			
			s.executeUpdate("DROP TABLE " + table + "_tmp");
			
		} catch (SQLException e) {
			System.err.println("Error ");
			System.err.println(e.getMessage());
		}	
	}
}
