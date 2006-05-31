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
 * Title:   StatusOutputCreator
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */

package de.japes.servlets.nasty;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jCharts.types.ChartType;
import org.jCharts.axisChart.AxisChart;
import org.jCharts.chartData.AxisChartDataSet;
import org.jCharts.chartData.DataSeries;
import org.jCharts.chartData.interfaces.IAxisDataSeries;
import org.jCharts.chartData.ChartDataException;
import org.jCharts.chartData.PieChartDataSet;
import org.jCharts.properties.util.ChartFont;
import org.jCharts.nonAxisChart.PieChart2D;
import org.jCharts.properties.*;
import org.jCharts.axisChart.customRenderers.axisValue.renderers.*;

/**
 * Queries given databases according to the parameters set by nasty's <code>index.jsp</code>
 * and produces eight specific graphical charts and their legends from the results.
 * <p>
 * The charts are stored as objects named <code>status1</code> - <code>status8</code> in the 
 * servlet context and are shown by invoking the servlet <code>ChartsDeliverer</code>.
 * 
 * @see ChartsDeliverer
 */
public class StatusOutputCreator extends OutputCreator {
	
    /** MIME-content of results is <code>text/html</code> */
    private static final String contentType = "text/html";
    /** Netmask for determining subnet size. */
    private short netmask = 24;
    /** Width of the chart. */
    private int width = 900;
    /** Height of the chart. */
    private int height = 400;
    /** Name of temporary table for database query without removed identical entries. */
    private String tmpTableAll="";
    /** Name of temporary table for database query with removed identical entries. */
    private String tmpTableReduced="";

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
    public StatusOutputCreator(HttpServletRequest request, HttpServletResponse response, Statement s, ArrayList dbList) {
        super(request, response, s, dbList);
    }

    /**
     * Loads raw data from source tables into temporary table and optionally creates a second
     * temporary table with removed identical data. Then specific methods are called to produce
     * eight standard charts which show an overview of the status of the network.
     * (The charts will be stored as the parameter <code>status1</code> - <code>status8</code> in
     * the servlet context as an object by these methods.) 
     * 
     * @return  HTML-String that holds the invocation commands to call the <code>ChartsDeliverer</code>
     *          servlet which shows the charts that were produced and stored in the servlet context.
     *          The string also holds eventual messages such as error messages.
     */
    public String createOutput() {

        // ************* prepare the temporary tables ***************
               
        // ************ PERFORMANCE WATCH ***************
        long perfWatch;
        // **********************************************

        // get a name for the temporary table
        tmpTableAll = dq.getUniqueName("statusTmp");
        
        if (remDoubles) {  
            // retrieve all data into one table and copy it without identical entries into second table
            tmpTableReduced = dq.getUniqueName("statusTmpNoDoubles");
            if (dq.fillTable(tmpTableAll, null, null, false, false, 1)==false) {
                output += dq.getOutput();
                return "Error creating and filling temporary table! <p>"+output;
            }
            // ************ PERFORMANCE WATCH ***************
            perfWatch = System.currentTimeMillis();
            // **********************************************
            dq.removeIdenticalData(tmpTableAll, tmpTableReduced, remTimeDiv, remExporterID, true);
        } else {
            // no removal of identical entries, just copy the name of the temp. table
            // and only retrieve necessary data
            tmpTableReduced = tmpTableAll;
            String createTmp = "CREATE TEMPORARY TABLE "+tmpTableAll+" (srcIP INTEGER(10) UNSIGNED, dstIP INTEGER(10) UNSIGNED," +
				"srcPort SMALLINT(5) UNSIGNED, dstPort SMALLINT(5) UNSIGNED, " +
				"proto TINYINT UNSIGNED, " +
				"bytes BIGINT(20) UNSIGNED, " +
				"firstSwitched INTEGER(10) UNSIGNED, " +
                                "databaseID SMALLINT(5) UNSIGNED)";
            String fillTmp = "SELECT SQL_BIG_RESULT srcIP, dstIP, srcPort, dstPort, proto, bytes," +
                                 " firstSwitched, databaseID FROM #srctable# WHERE " +
                                 "#params#";
            if (dq.fillTable(tmpTableAll, createTmp, fillTmp, false, false, 1)==false) {
                output += dq.getOutput();
                return "Error creating and filling temporary table! <p>"+output;
            }
            // ************ PERFORMANCE WATCH ***************
            perfWatch = System.currentTimeMillis();
            // **********************************************
        }

        // ************* build the eight standard charts *******************
        for (int i=1; i<=8; i++) request.getSession().getServletContext().removeAttribute("status"+i);
        try {
            generateTrafficCharts("status1","status2");
            for (short whatPie=1; whatPie<=3; whatPie++)
                generatePortAndProtoCharts(whatPie,"status"+(whatPie+2));
            for (short whatChart=1; whatChart<=3; whatChart++)
                generateTopNetsCharts(whatChart,"status"+(whatChart+5));
        } catch (Exception e) {
            output += "<p>Error creating charts.<p>" + e.getMessage() + "<p>";
            dq.dropTable(tmpTableAll);
            dq.dropTable(tmpTableReduced);
            return output + "<p>" + dq.getOutput();
        }

        // drop the temporary tables
        dq.dropTable(tmpTableAll);
        dq.dropTable(tmpTableReduced);
		
        // ************ finished, return the constructed output **************

        output += dq.getOutput();
        
        // ************ PERFORMANCE WATCH ***************
        output += "<p>Time to rearrange data and query temp. tables: " + (System.currentTimeMillis()-perfWatch) + " ms<p>";
        // **********************************************
        
        // return results of query
        // (created charts have been stored as objects in the session context and are painted
        // by invoking the ChartsDeliverer-servlet)
        return 	"<b>Monitored Traffic from " + dq.createTimeOutput(startTime) + " to " + dq.createTimeOutput(endTime) + " </b><p>" +
                "<hr><table><tr><td><IMG src=\"/nasty/ChartsDeliverer?chartname=status1\"></td>" +
                "<td><IMG src=\"/nasty/ChartsDeliverer?chartname=status2\"></td></tr></table><p>" + 
                "<hr><table><tr><td><IMG src=\"/nasty/ChartsDeliverer?chartname=status3\"></td>" + 
                "<td><IMG src=\"/nasty/ChartsDeliverer?chartname=status4\"></td>" + 
                "<td><IMG src=\"/nasty/ChartsDeliverer?chartname=status5\"></td></tr></table><p>" + 
                "<hr><table><tr><td><IMG src=\"/nasty/ChartsDeliverer?chartname=status6\"></td>" + 
                "<td><IMG src=\"/nasty/ChartsDeliverer?chartname=status7\"></td></tr></table><p>" + 
                "<hr><table><tr><td><IMG src=\"/nasty/ChartsDeliverer?chartname=status8\"></td></tr></table><p>" + 
                "<hr><p>" + output;
    }
	
    /**
     * Generates traffic over time chart as object in servlet context
     * (grouped by database) and a special traffic over time chart that shows the amount of
     * traffic identified as identical as another object in servlet context.
     *
     * @param   chartName1   Name under which the chart object with the traffic grouped by
     *                       database will be stored in the servlet context.
     * @param   chartName2   Name under which the chart object with the reduced traffic
     *                       will be stored in the servlet context.
     */
    private void generateTrafficCharts(String chartName1, String chartName2) 
    throws ChartDataException {
	// flags to signal what scale the time axis uses
        boolean minScale	= false;
	boolean halfHourScale 	= false;
	boolean dayScale	= false;
        // number of distinct points on the time axis of the chart
        int numDataPoints = 0;
        // size of the time blocks used in the chart
	long unit = 0;
        // the arrays that will hold the data for the charts
        double[][] 	data;
	double[][] 	data2;
	// array that will hold the names of each database for the chart label
        String[] legendLabels;
    	// object that helps to format the labels on the time scale
        Calendar tmpTime = new GregorianCalendar();
	// holds the results of the SQL queries needed to calculate the chart data
        ResultSet result = null;

        // ******** prepare the chart object **************
        
        // prepare the chart object by setting some attributes		
	NumberFormat nf = NumberFormat.getNumberInstance();
	nf.setMinimumIntegerDigits(2);
	AreaChartProperties areaChartProperties = new AreaChartProperties();
        LegendProperties legendProperties = new LegendProperties();
        AxisProperties axisProperties = new AxisProperties( false );
        axisProperties.setXAxisLabelsAreVertical(true);
        ChartProperties chartProperties = new ChartProperties();;
        String xAxisTitle = "Time";
	String yAxisTitle = "";
       
        // calculate the colors for each database
	Paint[] paints = new Paint[dq.getDBCount()]; 
        for (int i = 0; i<paints.length; i++) {
            paints[i] = Color.getHSBColor((float) i/paints.length,1,1);
        }
        
        // check the selected unit in which the amount of traffic should be shown
        int bytesDivisor = 1;
	if (outputUnit.equalsIgnoreCase("bytes")) {
        	yAxisTitle = "Bytes";
	} else if(outputUnit.equalsIgnoreCase("kilo")) {
		yAxisTitle = "Kilobytes";
		bytesDivisor = 1024;
	} else if (outputUnit.equalsIgnoreCase("mega")) {
		yAxisTitle = "Megabytes";
		bytesDivisor = 1024*1024;
	}
		
	// calculate number of time blocks to be used as data points on x-axis
	if (startTime > endTime) {
                throw new ChartDataException("End time is before start time.");
        }
	long duration = endTime - startTime;
        if (duration <= 12*60*60*1000l) {
                minScale = true;
                numDataPoints = (int)(duration/(60*1000l));
                unit = 60*1000;  //draw data every minute
        } else if (duration <= 2*24*60*60*1000l) {
                halfHourScale = true;
                numDataPoints = (int)(duration/(30*60*1000l));
                unit = 30*60*1000;
                numDataPoints++;
        } else {
                dayScale = true;
                numDataPoints = (int)(duration/(24*60*60*1000l));
                unit = 24*60*60*1000l;
                xAxisTitle = "Date";
                numDataPoints++;
        }
        
        // prepare label and data objects for the chart
        String[] xAxisLabels = new String[numDataPoints];
        Arrays.fill(xAxisLabels,"No data");
        data = new double[dq.getDBCount()][numDataPoints];
        for (int i=0; i<data.length; i++) { Arrays.fill(data[i],0); }
        data2 = new double[2][numDataPoints];
        for (int i=0; i<data2.length; i++) { Arrays.fill(data2[i],0); }
        legendLabels = new String[dq.getDBCount()];
        Arrays.fill(legendLabels,"No Data");
    	
	// ************ query the temporary tables to fill the data arrays of the charts ******
        try {
           
                // first: query the temporary table with all data (no doubles removed) to
                //        get traffic recorded in each database
                String statement = "SELECT databaseID, SUM(bytes) as amount,firstSwitched FROM "+tmpTableAll+
						" GROUP BY databaseID,firstSwitched DIV "+(unit/1000)+
                                                " ORDER BY databaseID,firstSwitched";
                result = dq.queryTempDB(statement);
                if (result == null) throw new ChartDataException("No data in given time span!");

                // prepare looping through the query results
                long currtime = 0;
        	long currXValue = startTime;
                boolean firstRun = true;
                int i = 0;
                int dbNum = 0;
                int currpoint = 0;
                while(result.next()) {
                        if (i==numDataPoints) { 
                            // finished one database, prepare for the next
                            i=0; currXValue = startTime; firstRun = false;
                        }
                        // retrieve database number of this row
                        dbNum = result.getInt(1);
                        // check the time stamp of this row
                        currtime = result.getLong(3)*1000;
                        // test if the time stamp falls within current block on the time axis
                        currpoint = ((int)(currtime/unit)) - ((int)(startTime/unit));
                        if (currpoint == i) {
                            // yes, there was traffic in this time block recorded by this database
                            data[dbNum][i] = (double)result.getLong(2)/bytesDivisor;
                        } else {
                            // no traffic in this time block for this database 
                            data[dbNum][i] = 0;
                            if (currpoint<numDataPoints) result.previous();
                        }
                        // set name of the database as label
                        legendLabels[dbNum]=getDBName(dbNum);
                        if (firstRun) {
                            // set labels for the time axis
                            if(minScale) {
				tmpTime.setTimeInMillis(currXValue);
        			xAxisLabels[i]=nf.format(tmpTime.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(tmpTime.get(Calendar.MINUTE));
                            } else if(halfHourScale) {
				tmpTime.setTimeInMillis(currXValue);
				xAxisLabels[i]=nf.format(tmpTime.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(tmpTime.get(Calendar.MINUTE));
                            } else if(dayScale) {
				tmpTime.setTimeInMillis(currXValue);
				xAxisLabels[i]=nf.format(tmpTime.get(Calendar.DATE)) + "." + nf.format(tmpTime.get(Calendar.MONTH)+1);
                            }
                        }
    			// increase counters		
    			currXValue += unit;
                        i++;
        	}
                result.close();

                // second: query the temporary table with the reduced data (no doubles) to
                //         build the chart that shows how much data has been removed as identical
                statement = "SELECT SUM(bytes) as amount,firstSwitched FROM "+tmpTableReduced+
						" GROUP BY firstSwitched DIV "+(unit/1000)+
                                                " ORDER BY firstSwitched";
		result = dq.queryTempDB(statement);
                if (result == null) throw new ChartDataException("No data in given time span!");

                // prepare looping through the query results
                currtime = 0;
                i = 0;
                while(result.next()) {
                        // check the time stamp of this row
                        currtime = result.getLong(2)*1000;
                        // test into which block on the time axis the current data belongs
                        i = ((int)(currtime/unit)) - ((int)(startTime/unit));
                        if (i<numDataPoints) data2[0][i] = (double)result.getLong(1)/bytesDivisor;
        	}
                result.close();
    			
    	} catch (Exception e) {
    		throw new ChartDataException("Error getting values from database:" + e.getMessage());
        }
    	
        // calculate the amount of data removed as identical
        long absoluteTraffic = 0;
        long reducedTraffic = 0;
        long sum = 0;
        for (int i=0; i<numDataPoints; i++) {
            sum = 0;
            for (int j=0; j<dq.getDBCount(); j++) { sum+=data[j][i]; }
            data2[1][i] = sum - data2[0][i];
            absoluteTraffic += sum;
            reducedTraffic += data2[1][i];
        }
        reducedTraffic = Math.round((double)(reducedTraffic)/(double)(absoluteTraffic)*100);
        
        // ********* create the chart objects ***************

        // create the chart that shows all traffic grouped by database
        IAxisDataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, "All Traffic recorded by each Database with selected Properties");
      	dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, legendLabels, paints, ChartType.AREA_STACKED, areaChartProperties ) );
    	AxisChart axisChart = new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, width/2+20, height );
        
        // create the chart that shows the amount of data removed as identical
        dataSeries = new DataSeries( xAxisLabels, xAxisTitle, null, "Illustration of Removal of identical data");
      	dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data2, new String[] {"Traffic after Removal","Removed identical Data ("+reducedTraffic+"%)"},
                new Paint[] {Color.getHSBColor((float)0.1,(float)1.0,(float)1.0),Color.LIGHT_GRAY}, ChartType.AREA_STACKED, areaChartProperties ) );
    	AxisChart axisChart2 = new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, width/2-20, height );
    	
        // store the generated charts in the servlet context
        request.getSession().getServletContext().setAttribute(chartName1, axisChart);
        request.getSession().getServletContext().setAttribute(chartName2, axisChart2);
		
    }
        	
    /**
     * Generates pie charts as objects in servlet context showing the distribution of
     * source and destination ports and of the ip protocols that were used.
     *
     * @param   whatPie     Selects if source port (1), destination port (2) 
     *                      or protocol (3) pie is demanded.
     * @param   chartName   Name under which the chart object will be stored in the servlet context.
     */
    private void generatePortAndProtoCharts(short whatPie, String chartName) 
    throws ChartDataException {
        // number of distinct ports/protocols in the temp. table
        int numResults = 0;
        // signals if high ports are included in temp. table
        boolean hasHighPorts = false;
        // what pie chart should be produced?
        boolean ipPie = (whatPie==1);
        boolean appPieSrc = (whatPie==2);
        boolean appPieDst = (whatPie==3);
        // helper variables to calculate pie data
        double tmp = 0;
        double sum = 0;
        // holds the labels of the pie slices (port/protocol numbers or names)
        String[] labels = null;
        // holds the colors of the pie slices
        Paint[] paints = null;
        // used to hold SQL statements
        String statement = "";
        // holds the results of the SQL queries that are made to get the data for the pie chart
        ResultSet result;
        // array of data for the pie chart
        double[] dataSet = null;

        // ******** prepare the chart object **************
        
        // prepare the chart object by setting some attributes		
	NumberFormat nf = NumberFormat.getNumberInstance();
	nf.setMinimumIntegerDigits(1);
        nf.setMaximumFractionDigits(2);
        Paint[] possiblePaints = new Paint[]{Color.blue, Color.red, Color.green,
                                    Color.yellow, Color.cyan, Color.black, Color.magenta,
                                    Color.pink, Color.orange, Color.gray};
        PieChart2DProperties properties = new PieChart2DProperties();
        properties.setBorderStroke( new BasicStroke( 0f ) );
        LegendProperties legendProperties = new LegendProperties();
        legendProperties.setNumColumns( 2 );
        legendProperties.setPlacement( LegendProperties.BOTTOM );
        ChartProperties chartProperties = new ChartProperties();
        String title = "IP Protocol Distribution";
        if (appPieSrc) title = "Source Port Distribution";
        if (appPieDst) title = "Destination Port Distribution";

	// ************ query the temporary tables to fill the data arrays of the charts ******
        
        // build the appropriate SQL statement and query the temp. table
        if (appPieSrc) {
            statement = "SELECT srcPort AS port, SUM(bytes) AS sum FROM "+tmpTableReduced
                        +(ignoreHighPorts?" WHERE srcPort<1024 AND srcPort>0":"") 
                        + " GROUP BY srcPort ORDER BY sum DESC";
        } else if(appPieDst) {
            statement = "SELECT dstPort AS port, SUM(bytes) AS sum FROM "+tmpTableReduced
                        +(ignoreHighPorts?" WHERE dstPort<1024 AND dstPort>0":"") 
                        + " GROUP BY dstPort ORDER BY sum DESC";
        } else if(ipPie) {
            statement = "SELECT proto, SUM(bytes) AS sum FROM "+tmpTableReduced+" GROUP BY proto ORDER BY sum DESC";
        }
        result = dq.queryTempDB(statement);
        if (result==null) { throw new ChartDataException("Error creating pie charts."); }
                
        // fill data array for the chart by looping through the results of the query
        try {
            // first: find out how many ports/protocols are in the temp. table
            //        (only the top ten are shown, rest will be aggregated as "others")
            while (result.next()) {
                if (ipPie || ((result.getInt(1)>0) && (result.getInt(1)<1024))) numResults++;
                else if (!ipPie && ((result.getInt(1)>=1024)||(result.getInt(1)==0))) hasHighPorts = true;
            }
            result.beforeFirst();
            // if high ports should not be ignored, they will be aggregated as "high ports"
            if (hasHighPorts) numResults++;
            if (numResults==0) {
                result.close();
                throw new ChartDataException("Error creating pie charts, no data.");
            }
			
            // prepare the data, label and paint arrays for the pie chart
            dataSet = new double[numResults>10?10:numResults];
            labels = new String[numResults>10?10:numResults];
            if (numResults>10) labels[9]="others";
            paints = new Paint[numResults>10?10:numResults];
            Arrays.fill(dataSet, 0);
			
            // prepare looping through the results
            int i = 0;
            int slicenum = 0;
            while (result.next()) {
                // check if current row belongs to "high ports" or if
                // it already falls under "others"
                if (!ipPie && ((result.getInt(1)>=1024)||(result.getInt(1)==0))) {
                    slicenum = numResults-1;
                    if (slicenum>=dataSet.length) slicenum=dataSet.length-1;
                    i--;
                } else {
                    slicenum = i;
                }
                // set label for the slice if it hasn'd been done already
                if (labels[slicenum]==null) {
                    if (!ipPie) {
                        if ((result.getInt(1)>0) && (result.getInt(1)<1024)) 
                            labels[slicenum] = dq.createPortOutput(result.getInt(1), true);
                        else
                            labels[slicenum] = "high ports";
                    }
                    else labels[slicenum] = dq.createProtoOutput(result.getShort(1), true);
                }
                // retrieve amount of bytes for current port and add it to its pie slice
                tmp = result.getDouble(2);
                dataSet[slicenum] += tmp;
                sum += tmp;
                // increase slice counter if "others" hasn't been reached already
                if (i<9) i++;
            }
            result.close();

        } catch (SQLException e) {
            throw new ChartDataException("Error creating pie charts.<p>"+e.getMessage());
        }
            
        // add percentage of all traffic to labels of each pie
        for (int i=0; i<(numResults>10 ? dataSet.length:numResults); i++) {
            paints[i] = possiblePaints[i];
            labels[i] += " (" + (nf.format((double)(dataSet[i]/sum*100))) + "%)";
        }

        // ********* create the chart object ***************

        PieChartDataSet pds = new PieChartDataSet(title, dataSet, labels, paints, properties);
        PieChart2D pie = new PieChart2D(pds, legendProperties, chartProperties, width/3, (int)(height/1.25));
			
        // store the generated chart in the servlet context
        request.getSession().getServletContext().setAttribute(chartName, pie);

    }

    /**
     * Generates bar charts as objects in servlet context showing the top source
     * and destination subnets as well as the top flows between subnets.
     *
     * @param   whatChart   Selects what chart should be produced: Top sources (1),
     *                      top destinations (2) or top flows (3).
     * @param   chartName   Name under which the chart object will be stored in the servlet context.
     */
    private void generateTopNetsCharts(short whatChart, String chartName) 
    throws ChartDataException {
        // check byte output format
        String xAxisTitle = "Bytes";
        int bytesDivisor = 1;
	if (outputUnit.equalsIgnoreCase("kilo")) {
		xAxisTitle = "Kilobytes";
		bytesDivisor = 1024;
	} else if (outputUnit.equalsIgnoreCase("mega")) {
		xAxisTitle = "Megabytes";
		bytesDivisor = 1024*1024;
	}

        // ****** query the temporary table *********

        ResultSet result = null;
        String statement = "";
        if (whatChart==1) {
            statement = "SELECT srcIP,SUM(bytes) AS amount FROM " + tmpTableReduced
                            + " GROUP BY srcIP DIV " + grpSrcIPDiv 
                            + " ORDER BY amount DESC LIMIT 0,10";
        } else if (whatChart==2) {
            statement = "SELECT dstIP,SUM(bytes) AS amount FROM " + tmpTableReduced
                            + " GROUP BY dstIP DIV " + grpDstIPDiv 
                            + " ORDER BY amount DESC LIMIT 0,10";
        } else {
            statement = "SELECT srcIP,dstIP,SUM(bytes) AS amount FROM " + tmpTableReduced
                            + " GROUP BY srcIP DIV " + grpSrcIPDiv + ",dstIP DIV " + grpDstIPDiv
                            + " ORDER BY amount DESC LIMIT 0,10";
        }
        result = dq.queryTempDB(statement);
        if (result==null) {
            throw new ChartDataException("Error creating bar charts.");
        }
        // read results
        String[] xAxisLabels;
        double[][] data;
        int numResults = 0;
        try {
            // count results
            while (result.next()) {
                numResults++;
            }
            result.beforeFirst();
            if (numResults==0) {
                result.close();
                throw new ChartDataException("No data for bar charts.");
            }
            
            // create sufficient arrays to store the results for the chart object
            xAxisLabels= new String[numResults+1];
            data= new double[1][numResults+1];

            // fill arrays from SQL results
            int i=0;
            while (result.next()) {
                if (whatChart==1) {
                    xAxisLabels[i] = dq.createIPOutput((result.getLong(1)/grpSrcIPDiv)*grpSrcIPDiv, resolveIP);
                } else if (whatChart==2) {
                    xAxisLabels[i] = dq.createIPOutput((result.getLong(1)/grpDstIPDiv)*grpDstIPDiv, resolveIP);
                } else {
                    xAxisLabels[i] = dq.createIPOutput((result.getLong(1)/grpSrcIPDiv)*grpSrcIPDiv, resolveIP)
                           + " - " + dq.createIPOutput((result.getLong(2)/grpDstIPDiv)*grpDstIPDiv, resolveIP);
                }
                data[0][i] = (double)(result.getLong("amount"))/(double)bytesDivisor;
                data[0][numResults] += result.getLong("amount");
                i++;
            }
            result.close();
            
            // get sum of entire traffic and calculate "other" value
            statement = "SELECT SUM(bytes) FROM "+tmpTableReduced;
            result = dq.queryTempDB(statement);
            if (result.next()) {
                data[0][numResults] = (double)(result.getLong(1)-data[0][numResults])/(double)bytesDivisor;
            } else {
                data[0][numResults] = 0;
            }
            xAxisLabels[numResults] = "Others";
            result.close();
        } catch (Exception e) {
            throw new ChartDataException("Error creating bar charts.<p>"+e.getMessage());
        }
        
        // ***** create the chart object *********
        String title = "Top Sources";
        if (whatChart==2) title = "Top Destinations";
        if (whatChart==3) title = "Top Origin-Destination-Pairs";
        DataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, null, title );
        Paint[] paints= new Paint[]{ Color.yellow };
        if (whatChart==2) paints[0] = Color.blue;
        if (whatChart==3) paints[0] = Color.green;
        BarChartProperties barChartProperties= new BarChartProperties();
        ValueLabelRenderer valueLabelRenderer = new ValueLabelRenderer( false, false, true, -2 );
        valueLabelRenderer.setValueLabelPosition( ValueLabelPosition.ON_TOP );
        valueLabelRenderer.useVerticalLabels( false );
        barChartProperties.addPostRenderEventListener( valueLabelRenderer );
        AxisChartDataSet axisChartDataSet= new AxisChartDataSet( data, null, paints, ChartType.BAR, barChartProperties );
        dataSeries.addIAxisPlotDataSet( axisChartDataSet );
        ChartProperties chartProperties= new ChartProperties();
        AxisProperties axisProperties= new AxisProperties( true );
        int chartWidth = (whatChart==3?width:width/2);
        AxisChart axisChart= new AxisChart( dataSeries, chartProperties, axisProperties, null, chartWidth, (int)(height/1.5) );
        // store the generated chart in the servlet context
        request.getSession().getServletContext().setAttribute(chartName, axisChart);
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
