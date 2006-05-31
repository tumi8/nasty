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
 * Title:   ChartOutputCreator
 * Project: NASTY
 *
 * @author  David Halsband, Thomas Schurtz, unrza88
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
import java.awt.Stroke;
import java.awt.Shape;
/**
 * Queries given databases according to the parameters set by nasty's <code>index.jsp</code>
 * and produces specific graphical charts and their legends from the results.
 * <p>
 * The charts are stored as an object named <code>chart</code> in the servlet context and are
 * shown by invoking the servlet <code>ChartsDeliverer</code>.
 * 
 * @see ChartsDeliverer
 */
public class ChartOutputCreator extends OutputCreator {
	
    /** MIME-content of results is <code>text/html</code> */
    private static final String contentType = "text/html";
    /** True if IP protocol distribution chart was selected. */
    private boolean ipPie = false;
    /** True if application protocol distribution (source ports) chart was selected. */
    private boolean appPieSrc = false;
    /** True if application protocol distribution (destination ports) chart was selected. */
    private boolean appPieDst = false;
    /** True if traffic over time chart was selected. */
    private boolean traffic = false;
    /** True if traffic over time (source ports) chart was selected. */
    private boolean trafficSrc = false;
    /** True if traffic over time (destination ports) chart was selected. */
    private boolean trafficDst = false;
    /** True if traffic over time (exporters) chart was selected. */
    private boolean trafficExp = false;
    /** True if packet analysis chart was selected. */
    private boolean pktAna = false;
    /** True if size analysis chart was selected. */
    private boolean sizeAna = false;
    /** True if duration analysis chart was selected. */
    private boolean durAna = false;
    /** Width of the chart. */
    protected int width = 640;
    /** Height of the chart. */
    protected int height = 480;
    /** Name of temporary table for database query. */
    private String tmpname="";

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
    public ChartOutputCreator(HttpServletRequest request, HttpServletResponse response, Statement s, ArrayList dbList) {
        super(request, response, s, dbList);
    }

    /**
     * Checks which chart the user has selected and calls the corresponding method to produce it.
     * (The chart will be stored as the parameter <code>chart</code> in the servlet context as an
     * object by these methods. The legend for the chart however will be returned as an HTML-string.) 
     * 
     * @return  String that holds the invocation command to call the <code>ChartsDeliverer</code>
     *          servlet which shows the chart that was produced and stored in the servlet context.
     *          The string also holds the HTML-legend for the chart and/or eventual messages such
     *          as error messages.
     */
    public String createOutput() {

        String chart = "";  // name of selected chart
        String legend = ""; // chart legend

        // find out which chart was selected?
        chart=request.getParameter("chartSelect");
        if (chart.equalsIgnoreCase("Application Protocol Distribution (Source Ports)")){
            appPieSrc = true;
        }else if (chart.equalsIgnoreCase("Application Protocol Distribution (Destination Ports)")){
            appPieDst = true;
        }else if (chart.equalsIgnoreCase("IP Protocol Distribution")){
            ipPie = true;
        }else if (chart.equalsIgnoreCase("Traffic Over Time")){
            traffic = true;
        }else if (chart.equalsIgnoreCase("Traffic Over Time (Source Ports)")){
            trafficSrc = true;
        }else if (chart.equalsIgnoreCase("Traffic Over Time (Destination Ports)")){
            trafficDst = true;
        }else if (chart.equalsIgnoreCase("Traffic Over Time (Exporters)")){
            trafficExp = true;
        }else if (chart.equalsIgnoreCase("Packet Analysis")){
            pktAna = true;
        }else if (chart.equalsIgnoreCase("Size Analysis")){
            sizeAna = true;
        }else if (chart.equalsIgnoreCase("Duration Analysis")){
            durAna = true;
        }
        
        // get a unique name for the temporary table for the database query
        tmpname = dq.getUniqueName("chartTmp");
        // call the appropriate method to produce the selected chart and its legend
        try {
            if (ipPie || appPieSrc || appPieDst) {
                legend = generateProtoAndPortPie();
            }
            else if (traffic || trafficSrc || trafficDst ) {
                legend = generateTrafficAreaChart();
            }
            else if (trafficExp ) {
                legend = generateTrafficExpAreaChart();
            }
            else if (pktAna || sizeAna || durAna){
                legend = generateAreaAnalysis();
            }
        } catch (ChartDataException e) {
            return e.getMessage();
        } catch (SQLException e) {
            return e.getMessage();

        // drop the temporary table that was created in each method
        } finally {
            dq.dropTable(tmpname);
        }
		
        // return results of query
        // (created chart has been stored as an object in the session context and is painted
        // by invoking the ChartsDeliverer-servlet)
        return 	output + "\n" + "<table><tr><td><IMG src=\"/nasty/ChartsDeliverer\"></td><td>" + legend + "</td></tr></table>" +
        "<br><a href=\"/nasty/PdfDeliverer\">Get chart as PDF</a><p>" + dq.getOutput();
    }
	
    /**
     * Generates pie-charts for IP protocol and source and destination port distribution.
     * The chart will be stored in the servlet context as parameter <code>chart</code>.
     *
     * @return  String that holds the legend of the chart in HTML-format.
     */
    private String generateProtoAndPortPie() 
    throws SQLException, ChartDataException {

        int numResults = 0;

        double sum = 0;
        double tmp = 0;
        double[] dataSet = null;

        ResultSet result;
        String statement;
        String legend;
        String sumString = "";
        String[] labels = null;
        Paint[] paints = null;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);

        Paint[] possiblePaints = new Paint[]{Color.blue, Color.red, Color.green,
                                    Color.yellow, Color.cyan, Color.black, Color.magenta,
                                    Color.pink, Color.orange, Color.white};
        PieChart2DProperties properties = new PieChart2DProperties();
        properties.setBorderStroke( new BasicStroke( 0f ) );

        LegendProperties legendProperties = new LegendProperties();
        legendProperties.setNumColumns( 2 );
        legendProperties.setPlacement( LegendProperties.BOTTOM );

        ChartProperties chartProperties = new ChartProperties();

        // prepare to create temporary table for the database query
        boolean ok;
        String createTmp = "";
        if (ipPie){
           createTmp = "CREATE TEMPORARY TABLE "+tmpname+" (proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED)";
        } else {
           createTmp = "CREATE TEMPORARY TABLE "+tmpname+" (port SMALLINT(5) UNSIGNED, bytes BIGINT(20) UNSIGNED)";
        }

        // fill temporary table by querying relevant source tables
        statement = "";
        if (appPieSrc) {
             statement = "SELECT SQL_BIG_RESULT srcPort, SUM(bytes) ";
        } else if(appPieDst) {
             statement = "SELECT SQL_BIG_RESULT dstPort, SUM(bytes) ";
        } else if (ipPie) {
             statement = "SELECT SQL_BIG_RESULT proto, SUM(bytes) ";
        }
        statement += "FROM #srctable# WHERE #params#";
        if (appPieSrc) {
            if (ignoreHighPorts) {
		statement += " AND srcPort BETWEEN 1 AND 1023 GROUP BY srcPort";
            } else
		statement += " GROUP BY srcPort";
        } else if (appPieDst) {
            if (ignoreHighPorts) {
                statement += " AND dstPort BETWEEN 1 AND 1023 GROUP BY dstPort";
            } else 
                statement += " GROUP BY dstPort";
        } else if (ipPie) {
            statement += " GROUP BY proto";
	}
        ok=dq.fillTable(tmpname, createTmp, statement, remDoubles, remExporterID, remTimeDiv);
        if (!ok) { return ""; }
        
        // query the temporary table
        if (appPieSrc || appPieDst) {
            statement = "SELECT port, SUM(bytes) AS sum FROM "+tmpname+" GROUP BY port ORDER BY sum DESC";
        } else if(ipPie) {
            statement = "SELECT proto, SUM(bytes) AS sum FROM "+tmpname+" GROUP BY proto ORDER BY sum DESC";
        }
        result = dq.queryTempDB(statement);
        if (result==null) { return ""; }
                
        // create chart from the results of the query
        try {
            while (result.next()) {
                if (result.getInt(1) < 1023)
                    numResults++;
            }
            result.beforeFirst();
                
            if (numResults == 0) {
                request.getSession().getServletContext().removeAttribute("chart");
                return "No matching data was found.";
            }
			
            dataSet = new double[numResults>10?10:numResults];
            labels = new String[numResults>10?10:numResults];
            paints = new Paint[numResults>10?10:numResults];
			
            Arrays.fill(dataSet, 0);
			
            result.beforeFirst();
			
            int i = 0;
            int currPort = 0;
			
            while (result.next()) {
				
                if (ignoreHighPorts) {
                    if (i<dataSet.length-1 || numResults == 10) {
                        if (appPieSrc || appPieDst)
                            labels[i] = dq.createPortOutput(result.getInt(1), true);
                        else if (ipPie)
                            labels[i] = dq.createProtoOutput(result.getShort(1), true);
                        else
                            labels[i] = result.getString(1);
                        dataSet[i] = result.getDouble(2);
                        sum += dataSet[i];
                    } else {
                        tmp = result.getDouble(2);
                        dataSet[dataSet.length-1] += tmp;
                        sum += tmp;
                    }
                    i++;
                } else {
                    if (appPieSrc || appPieDst) {
                        currPort = result.getInt(1);
                        if ((i<dataSet.length-1 || numResults == 10) && (currPort < 1024) && (currPort != 0)) {
                            labels[i] = dq.createPortOutput(result.getInt(1), true);
                            dataSet[i] = result.getDouble(2);
                            sum += dataSet[i];
                            i++;
                        } else {
                            tmp = result.getDouble(2);
                            dataSet[dataSet.length-1] += tmp;
                            sum += tmp;
                        }
                    } else if (ipPie) {
                        if (i<dataSet.length-1 || numResults == 10) {
                            labels[i] = dq.createProtoOutput(result.getShort(1), true);
                            dataSet[i] = result.getDouble(2);
                            sum += dataSet[i];
                            i++;
                        } else {
                            tmp = result.getDouble(2);
                            dataSet[dataSet.length-1] += tmp;
                            sum += tmp;
                        }
                    } else {
                        if (i<dataSet.length-1 || numResults == 10) {
                            labels[i] = result.getString(1);
                            dataSet[i] = result.getDouble(2);
                            sum += dataSet[i];
                            i++;
                        } else {
                            tmp = result.getDouble(2);
                            dataSet[dataSet.length-1] += tmp;
                            sum += tmp;
                        }
                    }
                }
            }
			
            if (numResults!=10)
                labels[dataSet.length-1] = "Others";
                
        } catch (SQLException e) {
            throw new SQLException();
        }
            
        for (int i=0; i<(numResults>10 ? dataSet.length:numResults); i++) {
            paints[i] = possiblePaints[i];
            labels[i] += " (" + (nf.format((double)(dataSet[i]/sum*100))) + "%)";
        }

        try {
            PieChartDataSet pds = new PieChartDataSet(request.getParameter("chartSelect"), dataSet, labels, paints, properties);
            PieChart2D pie = new PieChart2D(pds, legendProperties, chartProperties, this.width, this.height);
			
            // store the generated chart in the servlet context
            request.getSession().getServletContext().setAttribute("chart", pie);
			
        } catch (ChartDataException e) {
            throw new ChartDataException("Error generating pie.");
        }

        // create legend for the chart from the results of the query
        legend = "<table border='3' frame='box'><tr><th>Color</th><th>Meaning</th><th>Amount</th></tr>";
		
        for (int i=0; i < (numResults>10 ? dataSet.length:numResults); i++) {
            String red = Integer.toHexString(((Color)paints[i]).getRed()).toUpperCase();
            String green = Integer.toHexString(((Color)paints[i]).getGreen()).toUpperCase();
            String blue = Integer.toHexString(((Color)paints[i]).getBlue()).toUpperCase();
			
            if (red.length() < 2)
                red = "0" + red;
            if (green.length() < 2)
                green = "0" + green;
            if (blue.length() < 2)
                blue = "0" + blue;

            legend += "<tr><td bgcolor=" + red + green + blue + "></td>" +
                      "<td align=center>" + labels[i].split(" ")[0] + "</td><td align=\"right\">";
			
            if (outputUnit.equalsIgnoreCase("bytes")) {
                legend += dataSet[i];
                sumString = nf.format(sum);
            } else if(outputUnit.equalsIgnoreCase("kilo")) {
                legend += nf.format(dataSet[i]/1024) + " kB";
                sumString = nf.format(sum/1024) + "kB";
            } else if (outputUnit.equalsIgnoreCase("mega")) {
                legend += nf.format(dataSet[i]/1024/1024) + " MB";
                sumString = nf.format(sum/1024/1024) + " MB";
            } else
                legend += "Error";
            legend += " (" + nf.format((double)(dataSet[i]/sum*100)) + "%)</td></tr>";
        }
		
        legend += "<tr><td></td><td align=center>Sum</td><td align=\"right\">" + sumString + "</td></tr>"; 
        legend += "</table>";
		
        // return the legend of the chart
        return legend;
    }
	
    /**
     * Generates charts to show traffic over time (as a whole and if selected by the user
     * divided by source or destination port). No legend for the chart is produced.
     * The chart will be stored in the servlet context as parameter <code>chart</code>.
     *
     * @return  String that holds eventual messages such as error messages.
     */
    private String generateTrafficAreaChart() 
    throws ChartDataException {
		
		boolean minScale	= false;
		boolean halfHourScale 	= false;
		boolean dayScale	= false;
		
                int numDataPoints = 0;
    	
		int bytesDivisor = 1;
		int port = 0;
		
		long unit = 0;
		
		double[][] 	data;
		
		String title;
		String[] legendLabels;
		
		Paint[] paints = null;
		
                ResultSet result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumIntegerDigits(2);
		

                //****************set some chart attributes*****************
		
		AreaChartProperties areaChartProperties = new AreaChartProperties();
                LegendProperties legendProperties = new LegendProperties();
	    
	    
                AxisProperties axisProperties = new AxisProperties( false );
                axisProperties.setXAxisLabelsAreVertical(true);
       
                ChartFont axisScaleFont = new ChartFont( new Font( "Georgia Negreta cursiva", Font.PLAIN, 13 ), Color.black );
        
                LabelAxisProperties xAxisProperties= (LabelAxisProperties) axisProperties.getXAxisProperties();
                xAxisProperties.setScaleChartFont( axisScaleFont );
        
                LabelAxisProperties yAxisProperties= (LabelAxisProperties) axisProperties.getYAxisProperties();
                yAxisProperties.setScaleChartFont( axisScaleFont );

                ChartFont axisTitleFont = new ChartFont( new Font( "Arial Narrow", Font.PLAIN, 14 ), Color.black );
                xAxisProperties.setTitleChartFont( axisTitleFont );
                yAxisProperties.setTitleChartFont( axisTitleFont );

                DataAxisProperties dataAxisProperties = (DataAxisProperties) yAxisProperties;
                dataAxisProperties.setNumItems(10);

                ChartFont titleFont = new ChartFont( new Font( "Georgia Negreta cursiva", Font.PLAIN, 14 ), Color.black );
                ChartProperties chartProperties = new ChartProperties();;
                chartProperties.setTitleFont( titleFont );
        
		String xAxisTitle = "Time";
		String yAxisTitle = "";
        
		if (outputUnit.equalsIgnoreCase("bytes")) {
			yAxisTitle = "Bytes";
		} else if(outputUnit.equalsIgnoreCase("kilo")) {
			yAxisTitle = "Kilobytes";
			bytesDivisor = 1024;
		} else if (outputUnit.equalsIgnoreCase("mega")) {
			yAxisTitle = "Megabytes";
			bytesDivisor = 1024*1024;
		}
		
		//************************************************************
		
                title = "Traffic from " + dq.createTimeOutput(startTime) + " to " + dq.createTimeOutput(endTime);
                
                
		if (startTime > endTime) {
                    return "End time is before start time.";
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
                } else {
                    dayScale = true;
                    numDataPoints = (int)(duration/(24*60*60*1000l)) + 1;
                    unit = 24*60*60*1000l;
                    xAxisTitle = "Date";
                }
		
		String[] xAxisLabels = new String[numDataPoints];
    	
		if (traffic) {
                    data = new double[1][numDataPoints];
                    legendLabels = new String[]{"Overall Traffic"};
        	
		} else if(trafficSrc || trafficDst) {
                    data = new double[6][numDataPoints];
                    legendLabels = new String[6];
		} else {	//at the moment just to have the variables initialized in any case
                    data = new double[1][numDataPoints];
                    legendLabels = new String[]{"Overall Traffic"};
		}
			
                // create and fill temporary table
		boolean ok=true;
                if (traffic) {
                    ok=dq.fillTable(tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (firstSwitched INTEGER(10) UNSIGNED, bytes BIGINT(20) UNSIGNED)",
                           "SELECT SQL_BIG_RESULT firstSwitched, SUM(bytes) FROM #srctable# WHERE " +
                           "#params# GROUP BY firstSwitched DIV "+(unit/1000), remDoubles, remExporterID, remTimeDiv);
                } else if (trafficSrc) {
                    ok=dq.fillTable(tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (port SMALLINT(5) UNSIGNED, firstSwitched INTEGER(10) UNSIGNED, bytes BIGINT(20) UNSIGNED)",
                           "SELECT SQL_BIG_RESULT srcPort, firstSwitched, SUM(bytes) FROM #srctable# WHERE " +
                           "#params# GROUP BY firstSwitched DIV "+(unit/1000)+", srcPort", remDoubles, remExporterID, remTimeDiv);
                } else if (trafficDst) {
                    ok=dq.fillTable(tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (port SMALLINT(5) UNSIGNED, firstSwitched INTEGER(10) UNSIGNED, bytes BIGINT(20) UNSIGNED)",
                           "SELECT SQL_BIG_RESULT dstPort, firstSwitched, SUM(bytes) FROM #srctable# WHERE " +
                           "#params# GROUP BY firstSwitched DIV "+(unit/1000)+", dstPort", remDoubles, remExporterID, remTimeDiv);
                }
                if (!ok) { return ""; }
                
                // generate the chart by querying the temporary table
		
    	long currXValue = startTime;
    	
    	Calendar tmpTime = new GregorianCalendar();
    	
    	for (int i=0; i<numDataPoints; i++) {
    		
    		try {
    			
    			if (traffic) {
    	    				
    				paints = new Paint[]{Color.blue};
    				
    				String statement = "SELECT SUM(bytes) FROM "+tmpname+" WHERE firstSwitched BETWEEN " + currXValue/1000 + " AND " + ((currXValue+unit)/1000-1);
    				
    				result = dq.queryTempDB(statement);
    				
    				result.first();
    				
    				data[0][i] = (double)result.getLong(1)/bytesDivisor;
    			
                        } else if(trafficSrc || trafficDst) {
    				
    				int[] favouriteServices = new int[5];
    				
    				paints = new Paint[]{Color.blue, Color.red, Color.green,
    						Color.yellow, Color.cyan, Color.gray};
    				
    				//first find out which services were seen most often
    				String statement =  "SELECT port, SUM(bytes) as amount FROM "+tmpname+" WHERE port<1024" +
										" GROUP BY port ORDER BY amount DESC LIMIT 0,5";
    				
    				result = dq.queryTempDB(statement);
    				
    				int position = 0;
    				
    				while(result.next()) {
    					
    					favouriteServices[position] = result.getInt(1);
    					
    					if (favouriteServices[position] == 0)
    						legendLabels[position]="Layer3-Traffic";
    					else
    						legendLabels[position]=dq.createPortOutput(favouriteServices[position], true);
    					
    					position++;
    					
    				}
    				
    				legendLabels[5]="Other";
    				
    				//now get all the traffic and sort amount of bytes into categories
    				statement = "SELECT port, SUM(bytes) as amount FROM "+tmpname+" WHERE firstSwitched BETWEEN " + currXValue/1000 + " AND " + ((currXValue+unit)/1000-1) +
								" GROUP BY port";
    				
    				result = dq.queryTempDB(statement);
    				
    				while(result.next()) {
    						
    					port = result.getInt(1);
    						
    					if (port == favouriteServices[0])
    						data[0][i] = (double)result.getLong(2)/bytesDivisor;
    					
    					else if(port == favouriteServices[1])
    						data[1][i] = (double)result.getLong(2)/bytesDivisor;
    					
    					else if(port == favouriteServices[2])
    						data[2][i] = (double)result.getLong(2)/bytesDivisor;
    					
    					else if(port == favouriteServices[3])
    						data[3][i] = (double)result.getLong(2)/bytesDivisor;
    					
    					else if(port == favouriteServices[4])
    						data[4][i] = (double)result.getLong(2)/bytesDivisor;
    					
    					else
    						data[5][i] += (double)result.getLong(2)/bytesDivisor;
    				}
    				 
    			} else { //normally never reached
					paints = new Paint[]{Color.blue, Color.red, Color.green,
    						Color.yellow, Color.cyan, Color.black};
    			}
    			
    			if(minScale) {
    				//if((currXValue%(5*60*1000))==0) {
    					tmpTime.setTimeInMillis(currXValue);
    					xAxisLabels[i]=nf.format(tmpTime.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(tmpTime.get(Calendar.MINUTE));
    				//} else
    				//	xAxisLabels[i]= " ";
    			} else if(halfHourScale) {
    				//if((currXValue%(2*60*60*1000))==0) {
    					tmpTime.setTimeInMillis(currXValue);
    					xAxisLabels[i]=nf.format(tmpTime.get(Calendar.HOUR_OF_DAY)) + ":" + nf.format(tmpTime.get(Calendar.MINUTE));
    				//} else
    				//	xAxisLabels[i]= " ";
    			} else if(dayScale) {
    				//if((currXValue%(5*24*60*60*1000))==0) {
    					tmpTime.setTimeInMillis(currXValue);
    					xAxisLabels[i]=nf.format(tmpTime.get(Calendar.DATE)) + "." + nf.format(tmpTime.get(Calendar.MONTH)+1);
    				//} else
    				//	xAxisLabels[i]= " ";
    			}
    					
    			currXValue += unit;
    			
    		} catch (Exception e) {
    			return "Error getting values from database:" + e.getMessage();
                }
    	}
    	
    	if (paints==null) {
    		return "Paint array not initialized.";
    	}
    		
    	IAxisDataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, title );
        
    	try {
    		if (traffic)
    			dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, legendLabels, paints, ChartType.AREA, areaChartProperties ) );
    		else if(trafficSrc || trafficDst)
    			dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, legendLabels, paints, ChartType.AREA_STACKED, areaChartProperties ) );
                
    	} catch (ChartDataException e) {
    		output += e.getMessage();
    		throw new ChartDataException("Error generating pie.");
    	}
    	
    	AxisChart axisChart = new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, width, height );
    	
    	// store the generated chart in the servlet context
        request.getSession().getServletContext().setAttribute("chart", axisChart);
		
        // return with eventual messages
        return output;
    }
    /**
     * Generates chart to show traffic over time divided by exporters. No legend for the chart is
     * produced. The chart will be stored in the servlet context as parameter <code>chart</code>.
     *
     * @return  String that holds eventual messages such as error messages.
     */
    private String generateTrafficExpAreaChart() 
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
        LegendProperties legendProperties = new LegendProperties();
        AxisProperties axisProperties = new AxisProperties( false );
        axisProperties.setXAxisLabelsAreVertical(true);
        ChartProperties chartProperties = new ChartProperties();;
        String xAxisTitle = "Time";
	String yAxisTitle = "";
       
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
        String[] xAxisLabels = new String[numDataPoints];
        Arrays.fill(xAxisLabels,"No data");
        Paint[] paints = null;
        Stroke[] strokes = null;
        Shape[] shapes = null;
        
        // ********** create and fill the temporary table *******
        if (dq.fillTable (tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (exporterID INTEGER(10) UNSIGNED, exporterIP INTEGER(10) UNSIGNED," + 
           "bytes BIGINT(20) UNSIGNED, firstSwitched INTEGER(10) UNSIGNED)",
           "SELECT SQL_BIG_RESULT exporterID, exporterIP, SUM(bytes) AS bytes, firstSwitched FROM #srctable# " +
           "WHERE #params# " + 
           "GROUP BY exporterID, firstSwitched DIV "+(unit/1000)+" ORDER BY exporterID,firstSwitched",
           remDoubles, remExporterID, remTimeDiv)==false) {
            throw new ChartDataException("<p>Error creating and filling temporary table!<p>"+dq.getOutput());
        }

        try {
                // read list of exporters from temporary table
                ArrayList expIDList = new ArrayList();
                ArrayList expIPList = new ArrayList();
                String queryTmp = "SELECT exporterID, exporterIP FROM "+tmpname+" GROUP BY exporterID ORDER BY exporterID";
                result = dq.queryTempDB(queryTmp);
                while (result.next()) {
                    expIDList.add(new Long(result.getLong("exporterID")));
                    expIPList.add(new Long(result.getLong("exporterIP")));
                }
                int expCount = expIDList.size();
        
                // prepare label and data objects for the chart
                data = new double[expCount][numDataPoints];
                for (int i=0; i<data.length; i++) { Arrays.fill(data[i],0); }
                legendLabels = new String[expCount];
                Arrays.fill(legendLabels,"No Data");
                
                // calculate the colors and strokes for each database
                paints = new Paint[expCount]; 
                strokes = new Stroke[expCount]; 
                shapes = new Shape[expCount]; 
                for (int i = 0; i<paints.length; i++) {
                    strokes[i] = LineChartProperties.DEFAULT_LINE_STROKE;
                    shapes[i] = null;
                    paints[i] = Color.getHSBColor((float) i/paints.length,1,1);
                }
        
                // ************ query the temporary tables to fill the data arrays of the charts ******
           
                // first: query the temporary table with all data (no doubles removed) to
                //        get traffic recorded by each exporter
                queryTmp = "SELECT exporterID, exporterIP, SUM(bytes) as amount,firstSwitched FROM "+tmpname+
						" GROUP BY exporterID,firstSwitched DIV "+(unit/1000)+
                                                " ORDER BY exporterID,firstSwitched";
                result = dq.queryTempDB(queryTmp);
                if (result == null) throw new ChartDataException("No data in given time span!");

                // prepare looping through the query results
                long currtime = 0;
        	long currXValue = startTime;
                boolean firstRun = true;
                int i = 0;
                long expID = 0;
                long expIP = 0;
                int currpoint = 0;
                while(result.next()) {
                        if (i==numDataPoints) { 
                            // finished one database, prepare for the next
                            i=0; currXValue = startTime; firstRun = false;
                        }
                        // retrieve exporter number of this row
                        expID = result.getLong(1);
                        // retrieve exporter IP of this row
                        expIP = result.getLong(2);
                        // check the time stamp of this row
                        currtime = result.getLong(4)*1000;
                        // test if the time stamp falls within current block on the time axis
                        currpoint = ((int)(currtime/unit)) - ((int)(startTime/unit));
                        if (currpoint == i) {
                            // yes, there was traffic in this time block recorded by this exporter
                            data[expIDList.indexOf(new Long(expID))][i] = (double)result.getLong(3)/bytesDivisor;
                        } else {
                            // no traffic in this time block for this database 
                            data[expIDList.indexOf(new Long(expID))][i] = 0;
                            if (currpoint<numDataPoints) result.previous();
                        }
                        // set name of the database as label
                        legendLabels[expIDList.indexOf(new Long(expID))]=expID+" ("+dq.createIPOutput(expIP, false)+")";
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

    	} catch (Exception e) {
    		throw new ChartDataException("Error getting values from database:" + e.getMessage());
        }
    	
        // ********* create the chart objects ***************

        // create the chart that shows all traffic grouped by database
	LineChartProperties lineChartProperties = new LineChartProperties(strokes, shapes);
        IAxisDataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, request.getParameter("chartSelect"));
      	dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, legendLabels, paints, ChartType.LINE, lineChartProperties ) );
    	AxisChart axisChart = new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, width, height );
        
        // store the generated charts in the servlet context
        request.getSession().getServletContext().setAttribute("chart", axisChart);
		
        // return with eventual messages
        return output;
    }
        
    /**
     * Generates area-charts for packet, size and duration analysis. No legend for the chart
     * is produced.
     * The chart will be stored in the servlet context as parameter <code>chart</code>.
     *
     * @return  String that holds eventual messages such as error messages.
     */
    private String generateAreaAnalysis() 
    throws ChartDataException {
		
		boolean minScale	= false;
		boolean halfHourScale 	= false;
		boolean dayScale	= false;
		
                int numDataPoints = 0;
    	
		int j = 0;                
                
		long unit = 0;
		
		double[][] 	data;
		
		String title="";
		String[] legendLabels;
		
		Paint[] paints = null;
		
		ResultSet result = null;
                ResultSet resultmax = null;
		
                // create and fill temporary table
                boolean ok=true;
                if (pktAna) {
                    ok=dq.fillTable (tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (pkts INTEGER(10) UNSIGNED, amount INTEGER(10) UNSIGNED)", "SELECT SQL_BIG_RESULT pkts, count(*) FROM #srctable# WHERE " +
                           "#params# GROUP BY pkts", remDoubles, remExporterID, remTimeDiv);
                    /*dq.createTempTable(tmpname, "CREATE TEMPORARY TABLE "+tmpname+" (pkts INTEGER(10) UNSIGNED, amount INTEGER(10) UNSIGNED)");
                    dq.fillTempTable (tmpname,"SELECT SQL_BIG_RESULT pkts, count(*) FROM #srctable# WHERE " +
                           "#params# GROUP BY pkts");*/
                } else if (sizeAna) {
                    /*dq.createTempTable(tmpname, "CREATE TEMPORARY TABLE "+tmpname+" (bytes INTEGER(10) UNSIGNED, amount INTEGER(10) UNSIGNED)");
                    dq.fillTempTable (tmpname,"SELECT SQL_BIG_RESULT (bytes-(bytes%100)) as kbytes, count(*) FROM #srctable# WHERE " +
                           "#params# GROUP BY kbytes");
                    */
                    ok=dq.fillTable (tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (bytes INTEGER(10) UNSIGNED, amount INTEGER(10) UNSIGNED)","SELECT SQL_BIG_RESULT (bytes/1024) as kbytes, count(*) FROM #srctable# WHERE " +
                           "#params# GROUP BY kbytes", remDoubles, remExporterID, remTimeDiv);
                } else if (durAna){
                    ok=dq.fillTable (tmpname,"CREATE TEMPORARY TABLE "+tmpname+" (duration INTEGER(10) UNSIGNED, amount INTEGER(10) UNSIGNED)", "SELECT SQL_BIG_RESULT (lastSwitched-firstSwitched+1) as duration, count(*) FROM #srctable# WHERE " +
                           "#params# GROUP BY duration", remDoubles, remExporterID, remTimeDiv);            
                }   
                if (!ok) { return ""; }

                
                // generate the chart by querying the temporary table
                
                //****************set some chart attributes*****************
		
		AreaChartProperties areaChartProperties = new AreaChartProperties();
                LegendProperties legendProperties = new LegendProperties();
	    
	    
                AxisProperties axisProperties = new AxisProperties( false );
                axisProperties.setXAxisLabelsAreVertical(true);
       
                ChartFont axisScaleFont = new ChartFont( new Font( "Georgia Negreta cursiva", Font.PLAIN, 13 ), Color.black );
        
                LabelAxisProperties xAxisProperties= (LabelAxisProperties) axisProperties.getXAxisProperties();
                xAxisProperties.setScaleChartFont( axisScaleFont );
        
                LabelAxisProperties yAxisProperties= (LabelAxisProperties) axisProperties.getYAxisProperties();
                yAxisProperties.setScaleChartFont( axisScaleFont );

                ChartFont axisTitleFont = new ChartFont( new Font( "Arial Narrow", Font.PLAIN, 14 ), Color.black );
                xAxisProperties.setTitleChartFont( axisTitleFont );
                yAxisProperties.setTitleChartFont( axisTitleFont );

                DataAxisProperties dataAxisProperties = (DataAxisProperties) yAxisProperties;
                //number of items to displayed on yAxis               
                dataAxisProperties.setNumItems(10);

                ChartFont titleFont = new ChartFont( new Font( "Georgia Negreta cursiva", Font.PLAIN, 14 ), Color.black );
                ChartProperties chartProperties = new ChartProperties();;
                chartProperties.setTitleFont( titleFont );
                
                String xAxisTitle = null;
                String yAxisTitle = null;
                
                // set the titles of the x and y axis
                if (pktAna){
                    xAxisTitle = "number of packets per flow";
                    yAxisTitle = "amount";
                }else if (sizeAna){
                    xAxisTitle = "size in kilobytes per flow";
                    yAxisTitle = "amount";
                }else if (durAna){
                    xAxisTitle = "duration in seconds per flow";
                    yAxisTitle = "amount";
                }
              
		
		//************************************************************
		
                // Create a title with start and end time
           
		title = request.getParameter("chartSelect");
                
                if (startTime > endTime) {
                    return "End time is before start time.";
                }
		
		long duration = endTime - startTime;
		
                
                // Get the maximum value to limit the x-axis
                try {
                    if (pktAna){
                        resultmax = dq.queryTempDB ("SELECT max(pkts) as maxx, max(amount) as maxy FROM "+tmpname);
                    }
                    else if (sizeAna){
                        resultmax = dq.queryTempDB ("SELECT max(bytes) as maxx, max(amount) as maxy FROM "+tmpname);
                    }
                    else if (durAna){
                        dq.queryTempDB ("DELETE FROM "+tmpname+" WHERE duration < 0");
                        resultmax = dq.queryTempDB ("SELECT max(duration) as maxx, max(amount) as maxy FROM "+tmpname);
                    }
                    while (resultmax.next()){
                        output += "maximum x= " + resultmax.getLong("maxx") + " maximum y= " + resultmax.getLong("maxy");
                        // number of points to be displayed on x-axis; add 2 to have 1 zero-value at the end -> more beautiful
                        numDataPoints = resultmax.getInt("maxx") + 2;
                    }
                }catch (SQLException e) {
                    return "Error getting values from database:" + e.getMessage();
                }catch (NullPointerException e) {
                    return "Error getting values from database:" + e.getMessage();
                }

                unit = 1;

                String[] xAxisLabels = new String[numDataPoints];
                data = new double[1][numDataPoints];
                paints = new Paint[]{Color.red};
                
                long currXValue = 0;
                double test;
               
                try {
                    if (pktAna) {
                        // get the results from the database
                        result = dq.queryTempDB ("SELECT pkts, sum(amount) AS amount FROM "+tmpname+" GROUP by pkts ORDER BY amount DESC");
                        // assign values of resultSet to the data points
                        while (result.next()){
                            j = result.getInt("pkts");
                            data [0][j] = result.getDouble("amount");
                        }
                    }
                    else if (sizeAna) {
                        // get the results from the database
                        result = dq.queryTempDB ("SELECT bytes, sum(amount) AS amount FROM "+tmpname+" GROUP by bytes ORDER BY amount DESC");
                        // assign values of resultSet to the data points
                        while (result.next()){
                            j = result.getInt("bytes");
                            data [0][j] = result.getDouble("amount");
                        }
                    }
                    else if (durAna) {
                        // get the results from the database
                        result = dq.queryTempDB ("SELECT duration, sum(amount) AS amount FROM "+tmpname+" GROUP by duration ORDER BY amount DESC");    
                        // assign values of resultSet to the data points
                        while (result.next()){
                            j = result.getInt("duration");
            //                if ( j>=0 && j<=1000 ){
            //                    test = result.getDouble("amount");
            //                    if ( test>=0 && test<=1000 ){
                                    data [0][j] = result.getDouble("amount");
            //                    }
            //                }
                        }
                    }
                    
                    // set last value to zero to make it more beautiful
                    data [0][j+1] = 0;
                    
                }
                catch (SQLException e) {
                    return "Error getting values from database:" + e.getMessage();
                } 
                catch (OutOfMemoryError e) {
                    return "Query is too general and used up all the memory." + e.getMessage();
                }
                catch (NullPointerException e) {
                    return "Error getting values from database:" + e.getMessage();
                }
               
               for (int i=0; i<numDataPoints; i++) { 
                xAxisLabels[i]= Long.toString(currXValue);
                currXValue += unit;
               }
   	
    	if (paints==null) {
    		return "Paint array not initialized.";
    	}
    	
        // Create the dataSeries
    	IAxisDataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, title );
        
        
        // Add the DataSet
    	try {
            dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, null, paints, ChartType.AREA, areaChartProperties ) );
    	} catch (ChartDataException e) {
            output += e.getMessage();
            throw new ChartDataException("Error generating Area Chart.");
    	}
    	
        // Create the AxisChart
    	AxisChart axisChart = new AxisChart( dataSeries, chartProperties, axisProperties, null, width, height );
    	
        
    	// store the generated chart in the servlet context
        request.getSession().getServletContext().setAttribute("chart", axisChart);
	
        // return with eventual messages
        //output += "";
        //output += " 1=" + data[0][0] + ", 2=" + data [0][1] + ", 3=" + data[0][2] + ", 4=" + data[0][3] + ", 5=" + data[0][4]+ ", 6=" + data[0][5];
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
