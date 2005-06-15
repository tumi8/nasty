/*
 * Created on 15.09.2004
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
import org.jCharts.properties.LegendProperties;
import org.jCharts.properties.PieChart2DProperties;

/**
 * @author unrza88
 */
public class ChartOutputCreator extends OutputCreator {
	
	private static final String contentType = "text/html";
	private boolean ipPie = false;
	private boolean appPieSrc = false;
	private boolean appPieDst = false;
	private boolean traffic = false;
	private boolean trafficSrc = false;
	private boolean trafficDst = false;
	private boolean ignoreHighPorts = false;

    protected int width = 640;
    protected int height = 480;
    
    
	public ChartOutputCreator() {
	
	}

	public String createOutput(HttpServletRequest request, HttpServletResponse response, Statement s) {
		
		double 		sum = 0;
		double 		tmp = 0;
		double[] 	dataSet = null;
		
		String 		legend = "";
		String 		chart = "";
		String[] 	labels = null;
	
		ResultSet 	result = null;
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(2);
		
		appPieSrc 	= false;
		appPieDst 	= false;
		ipPie 		= false;
		traffic 	= false;
		trafficSrc	= false;
		trafficDst	= false;
		ignoreHighPorts = false;
		
		String[] checkValues = request.getParameterValues("checks");
		
		int i = 0;
		
		while (checkValues != null && i < checkValues.length) {

			if (checkValues[i].equalsIgnoreCase("ignoreHighPorts"))
				ignoreHighPorts = true;
			
			i++;
		}
		
		output = "";
		
		output += "Start: " + new GregorianCalendar().getTime() + "\n";
		
		if ((chart=request.getParameter("chartSelect")).equalsIgnoreCase("Application Protocol Distribution (Source Ports)"))
			appPieSrc = true;
		else if (chart.equalsIgnoreCase("Application Protocol Distribution (Destination Ports)"))
			appPieDst = true;
		else if (chart.equalsIgnoreCase("IP Protocol Distribution"))
			ipPie = true;
		else if (chart.equalsIgnoreCase("Traffic Over Time"))
			traffic = true;
		else if (chart.equalsIgnoreCase("Traffic Over Time (Source Ports)"))
			trafficSrc = true;
		else if (chart.equalsIgnoreCase("Traffic Over Time (Destination Ports)"))
			trafficDst = true;
		
		if (appPieSrc || appPieDst) {
			
			try {
				
				s.execute("CREATE TEMPORARY TABLE appTmp (port SMALLINT(5) UNSIGNED, bytes BIGINT(20) UNSIGNED)");
				
			} catch (SQLException e) {
				
				output += "<p>Error creating temporary table.</p>";
				output += e.getMessage();
				return output;
			}
		} else if (ipPie) {
			
			try {
				
				s.execute("CREATE TEMPORARY TABLE ipTmp (proto TINYINT(3) UNSIGNED, bytes BIGINT(20) UNSIGNED)");
				
			} catch (SQLException e) {
				
				output += "<p>Error creating temporary table.</p>";
				output += e.getMessage();
				return output;
			}
		} else if (traffic) {
			
			try {
				
				s.execute("CREATE TEMPORARY TABLE trafficTmp (firstSwitched INTEGER(10) UNSIGNED, bytes BIGINT(20) UNSIGNED)");
				
			} catch (SQLException e) {
				
				output += "<p>Error creating temporary table.</p>";
				output += e.getMessage();
				return output;
			}
		} else if (trafficSrc || trafficDst) {
			
			try {
				
				s.execute("CREATE TEMPORARY TABLE trafficTmp (port SMALLINT(5) UNSIGNED, firstSwitched INTEGER(10) UNSIGNED, bytes BIGINT(20) UNSIGNED)");
				
			} catch (SQLException e) {
				
				output += "<p>Error creating temporary table.</p>";
				output += e.getMessage();
				return output;
			}
		}
		
		String query = "";
		
		if (!traffic && !trafficSrc && !trafficDst) {
			
			query = createSQLQuery(request, s);
		
			if (query == "") {
				output += "No valid query could be produced";
				return output;
			}
		
			try {
				result = s.executeQuery(query);
			
			} catch (SQLException e) {
			
				output += "<p>Error using DB connection.</p>";
				output += e.getMessage();
				return output;
			} catch (OutOfMemoryError e) {
				output += "Query is too general and used up all the memory.";
				return output;
			}
		} else {
			
			fillTemporaryTable(request, s);
		}
		
		try {
			
			if (request.getParameter("chartSelect").equalsIgnoreCase("IP Protocol Distribution") ||
				request.getParameter("chartSelect").equalsIgnoreCase("Application Protocol Distribution (Source Ports)") ||
				request.getParameter("chartSelect").equalsIgnoreCase("Application Protocol Distribution (Destination Ports)"))
				
				legend = generatePie(request, result);
			
			else if (request.getParameter("chartSelect").equalsIgnoreCase("Traffic Over Time") ||
					 request.getParameter("chartSelect").equalsIgnoreCase("Traffic Over Time (Source Ports)") ||
					 request.getParameter("chartSelect").equalsIgnoreCase("Traffic Over Time (Destination Ports)"))
				
				legend = generateAreaChart(request, s);
			
		} catch (ChartDataException e) {
			return e.getMessage();
			
		} catch (SQLException e) {
			return e.getMessage();
		
		} finally {
			try {
				if(appPieSrc || appPieDst)
					s.execute("DROP TABLE appTmp");
				else if(ipPie)
					s.execute("DROP TABLE ipTmp");
				else if(traffic || trafficSrc || trafficDst)
					s.execute("DROP TABLE trafficTmp");
			} catch (SQLException e) {
				return e.getMessage();
			}
		}
		
		//if (legend == "null")
			//return "<p>No results.</p>";
		
		output += "End: " + new GregorianCalendar().getTime() + "\n";
		
		return 	output + "\n" + "<table><tr><td><IMG src=\"/nasty/ChartsDeliverer\"></td><td>" + legend + "</td></tr></table>" +
				"<br><a href=\"/nasty/PdfDeliverer\">Get chart as PDF</a>";
	}
	
	private String generatePie(HttpServletRequest request, ResultSet result) 
		throws SQLException, ChartDataException {
	
		int numResults = 0;
		
		double 		sum = 0;
		double 		tmp = 0;
		double[] 	dataSet = null;
		
		String 		legend;
		String 		outputUnit = request.getParameter("unit");
		String 		sumString = "";
		String[] 	labels = null;
		Paint[] 	paints = null;
		
		
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
							labels[i] = createPortOutput(result.getInt(1), true);
						else if (ipPie)
							labels[i] = createProtoOutput(result.getShort(1), true);
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
							
							labels[i] = createPortOutput(result.getInt(1), true);
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
							
							labels[i] = createProtoOutput(result.getShort(1), true);
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
			
			request.getSession().getServletContext().setAttribute("chart", pie);
			
		} catch (ChartDataException e) {
			throw new ChartDataException("Error generating pie.");
		}
		
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
			
			legend += 	"<tr><td bgcolor=" + red + green + blue + "></td>" +
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
		
		return legend;
	}
	
	private String generateAreaChart(HttpServletRequest request, Statement s) 
		throws ChartDataException {
		
		boolean restrictTime 	= false;
		boolean minScale		= false;
		boolean halfHourScale 	= false;
		boolean dayScale		= false;
		
		short startYear 	= 0;
    	short startMonth 	= 0;
    	short startDay 		= 0;
    	short startHour		= 0;
    	short startMin		= 0;
    	
    	short endYear		= 0;
    	short endMonth		= 0;
    	short endDay		= 0;
    	short endHour		= 0;
    	short endMin        = 0;
    	
    	short numDataPoints = 0;
    	
		int bytesDivisor = 1;
		int port = 0;
		
		long start = 0, end = 0;
		long unit = 0;
		
		double[][] 	data;
		
		String title;
		String output="";
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
        
		String outputUnit = request.getParameter("unit");
		
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
		
		//check if time was restricted
		String[] checkValues = request.getParameterValues("checks");
		for (int i=0; i<checkValues.length; i++) {
			if (checkValues[i].equalsIgnoreCase("restrictTime")) {
				restrictTime = true;
				break;
			}
		}
		
		if (!restrictTime) {
			//generate a 24h chart
			
			Calendar cal = GregorianCalendar.getInstance();
    		cal.set(Calendar.MINUTE, 0);
    		cal.set(Calendar.SECOND, 0);
    		cal.set(Calendar.MILLISECOND, 0);		
    		end = cal.getTimeInMillis();

        	cal.add(Calendar.HOUR, -23);
        	start = cal.getTimeInMillis();
        	
        	title = "Traffic over the last 24 hours";
        	
		} else {
			
			startYear 		= Short.parseShort(request.getParameter("startYear"));
        	startMonth 		= (short)(Short.parseShort(request.getParameter("startMonth"))-1);
        	startDay 		= Short.parseShort(request.getParameter("startDay"));
        	startHour		= Short.parseShort(request.getParameter("startHour"));
        	startMin		= Short.parseShort(request.getParameter("startMin"));
        	
        	endYear		= Short.parseShort(request.getParameter("endYear"));
        	endMonth		= (short)(Short.parseShort(request.getParameter("endMonth"))-1);
        	endDay		= Short.parseShort(request.getParameter("endDay"));
        	endHour		= Short.parseShort(request.getParameter("endHour"));
        	endMin		= Short.parseShort(request.getParameter("endMin"));
        	
        	Calendar startTime 	= new GregorianCalendar();
        	startTime.set(startYear, startMonth, startDay, startHour, startMin);
        	startTime.set(Calendar.SECOND, 0);
        	startTime.set(Calendar.MILLISECOND, 0);
        	start = startTime.getTimeInMillis();
        	
        	Calendar endTime 	= new GregorianCalendar();
        	endTime.set(endYear, endMonth, endDay, endHour, endMin);
        	endTime.set(Calendar.SECOND, 0);
        	endTime.set(Calendar.MILLISECOND, 0);
        	end = endTime.getTimeInMillis();
        	
        	title = "Traffic from " + startTime.getTime() + " to " + endTime.getTime();
		}
		
		if (start > end) {
    		return "End time is before start time.";
    	}
		
		long duration = end - start;
		
		if (duration <= 12*60*60*1000l) {
    		minScale = true;
    		numDataPoints = (short)(duration/(60*1000l));
    		unit = 60*1000;  //draw data every minute
    	} else if (duration <= 2*24*60*60*1000l) {
    		halfHourScale = true;
    		numDataPoints = (short)(duration/(30*60*1000l));
    		unit = 30*60*1000;
    	} else {
    		dayScale = true;
    		numDataPoints = (short)(duration/(24*60*60*1000l));
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
			
		
    	long currXValue = start;
    	
    	Calendar tmpTime = new GregorianCalendar();
    	
    	for (int i=0; i<numDataPoints; i++) {
    		
    		try {
    			
    			if (traffic) {
    	    				
    				paints = new Paint[]{Color.blue};
    				
    				String statement = "SELECT SUM(bytes) FROM trafficTmp WHERE firstSwitched BETWEEN " + currXValue/1000 + " AND " + ((currXValue+unit)/1000-1);
    				
    				result = s.executeQuery(statement);
    				
    				result.first();
    				
    				data[0][i] = (double)result.getLong(1)/bytesDivisor;
    			
    			} else if(trafficSrc || trafficDst) {
    				
    				int[] favouriteServices = new int[5];
    				
    				paints = new Paint[]{Color.blue, Color.red, Color.green,
    						Color.yellow, Color.cyan, Color.gray};
    				
    				//first find out which services were seen most often
    				String statement =  "SELECT port, SUM(bytes) as amount FROM trafficTmp WHERE port<1024" +
										" GROUP BY port ORDER BY amount DESC LIMIT 0,5";
    				
    				result = s.executeQuery(statement);
    				
    				int position = 0;
    				
    				while(result.next()) {
    					
    					favouriteServices[position] = result.getInt(1);
    					
    					if (favouriteServices[position] == 0)
    						legendLabels[position]="Layer3-Traffic";
    					else
    						legendLabels[position]=createPortOutput(favouriteServices[position], true);
    					
    					position++;
    					
    				}
    				
    				legendLabels[5]="Other";
    				
    				//now get all the traffic and sort amount of bytes into categories
    				statement = "SELECT port, SUM(bytes) as amount FROM trafficTmp WHERE firstSwitched BETWEEN " + currXValue/1000 + " AND " + ((currXValue+unit)/1000-1) +
								" GROUP BY port";
    				
    				result = s.executeQuery(statement);
    				
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
    			
    		} catch (SQLException e) {
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
    	
    	request.getSession().getServletContext().setAttribute("chart", axisChart);
		
		return output;
	}
	/*private String generateAreaChart(HttpServletRequest request, ResultSet result) 
		throws ChartDataException {
		
		int 			numRows = 0;
		long 			firstSwitched;
		long 			bytes;
		long 			lowerBound = 0;
		long 			maxValue = 0;
		boolean 		first = true;
		int 			index = 0;
		int				bytesDivisor = 1;
		String 			debug = "";
		String 			outputUnit = request.getParameter("unit");
		
		AreaChartProperties areaChartProperties = new AreaChartProperties();
	    LegendProperties legendProperties = new LegendProperties();
	    AxisProperties axisProperties = new AxisProperties( false );
	    ChartProperties chartProperties = new ChartProperties();;
	    
        axisProperties.setXAxisLabelsAreVertical(true);
        
        LabelAxisProperties xAxisProperties= (LabelAxisProperties) axisProperties.getXAxisProperties();
        LabelAxisProperties yAxisProperties= (LabelAxisProperties) axisProperties.getYAxisProperties();


        ChartFont axisScaleFont = new ChartFont( new Font( "Georgia Negreta cursiva", Font.PLAIN, 13 ), Color.black );
        xAxisProperties.setScaleChartFont( axisScaleFont );
        yAxisProperties.setScaleChartFont( axisScaleFont );

        ChartFont axisTitleFont = new ChartFont( new Font( "Arial Narrow", Font.PLAIN, 14 ), Color.black );
        xAxisProperties.setTitleChartFont( axisTitleFont );
        yAxisProperties.setTitleChartFont( axisTitleFont );

        DataAxisProperties dataAxisProperties = (DataAxisProperties) yAxisProperties;

        dataAxisProperties.setNumItems(10);

        ChartFont titleFont = new ChartFont( new Font( "Georgia Negreta cursiva", Font.PLAIN, 14 ), Color.black );
        chartProperties.setTitleFont( titleFont );

        ValueLabelRenderer valueLabelRenderer = new ValueLabelRenderer(false, false, true, -1 );
        valueLabelRenderer.setValueLabelPosition( ValueLabelPosition.ON_TOP );
        valueLabelRenderer.useVerticalLabels( false );
        areaChartProperties.addPostRenderEventListener( valueLabelRenderer );
        
		String[] legendLabels = {"Traffic"};
		Paint[] paints = new Paint[]{Color.blue};
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
		
        
        long[] timeBounds = new long[2];
        
        //check if the time was restricted, if not create a 24h chart
        if (createTimeQuery(request, timeBounds) == "") {
        	
        	Calendar cal = GregorianCalendar.getInstance();
    		cal.add(Calendar.HOUR, -23);
    		cal.set(Calendar.MINUTE, 0);
    		cal.set(Calendar.SECOND, 0);
    		cal.set(Calendar.MILLISECOND, 0);
    		
        	String title = "Traffic over the last 24 hours";
        	double[][] 		data = new double[1][24];
        	
        	lowerBound = cal.getTimeInMillis();
        	
        	try {
    			
    			result.beforeFirst();
    			
    			while(result.next()) {
    				
    				numRows++;
    				
    				firstSwitched = 1000*result.getLong(1);
    				bytes = result.getLong(2);
    				
    				/*if (first) {
    					lowerBound = cal.getTimeInMillis();   					
    					first = false;
    				}*/
    				
    				/*if ((firstSwitched >= lowerBound) && (firstSwitched<(lowerBound+24*60*60*1000))) {
    					index = (int)((firstSwitched-lowerBound)/(60*60*1000));
    				
    					//debug += "Abstand zur lowerBound: " + (firstSwitched-lowerBound) + "\n";
    					//debug += index + "\n";
    					//debug += lowerBound + "\n";	
    					data[0][index] += bytes/bytesDivisor;
    				}
    			}
    		} catch (SQLException e) {
    			return e.getMessage();
    		}
    		
    		for (int i=0; i<data[0].length; i++)
    			output += "Bytes[" + i + "]: " + data[0][i] + "\n";
    		
    		if (numRows == 0)
    			return "null";
    		    		
    		int currHour = cal.get(Calendar.HOUR_OF_DAY);
			
        	String[] xAxisLabels = {String.valueOf(currHour),
        							String.valueOf((currHour+1)%24),
									String.valueOf((currHour+2)%24),
									String.valueOf((currHour+3)%24),
									String.valueOf((currHour+4)%24),
									String.valueOf((currHour+5)%24),
									String.valueOf((currHour+6)%24),
									String.valueOf((currHour+7)%24),
									String.valueOf((currHour+8)%24),
									String.valueOf((currHour+9)%24),
									String.valueOf((currHour+10)%24),
									String.valueOf((currHour+11)%24),
									String.valueOf((currHour+12)%24),
									String.valueOf((currHour+13)%24),
									String.valueOf((currHour+14)%24),
									String.valueOf((currHour+15)%24),
									String.valueOf((currHour+16)%24),
									String.valueOf((currHour+17)%24),
									String.valueOf((currHour+18)%24),
									String.valueOf((currHour+19)%24),
									String.valueOf((currHour+20)%24),
									String.valueOf((currHour+21)%24),
									String.valueOf((currHour+22)%24),
									String.valueOf((currHour+23)%24)};
    		//String[] xAxisLabels = {"0","1","2","3","4","5","6","7","8","9","10","11","12",
    			//					"13","14","15","16","17","18","19","20","21","22","23"};
	
		
        	IAxisDataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, title );
        
        	try {
        		dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, legendLabels, paints, ChartType.AREA, areaChartProperties ) );
        	} catch (ChartDataException e) {
        		throw new ChartDataException("Error generating pie.");
        	}
		
        	AxisChart axisChart = new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, width, height );
        
        	request.getSession().getServletContext().setAttribute("chart", axisChart);
        	
        	/*try {
        		JPEGEncoder.encode(axisChart, 1.0f, new FileOutputStream(new File("axisChart.jpg")));
        	} catch (IOException e) {
        		return debug + " IOException:" + e.getMessage();
        	} catch (PropertyException e) {
        		return debug + " ProperyException:" + e.getMessage();
        	}*/
        /*} else {
        	
        	short startYear 	= Short.parseShort(request.getParameter("startYear"));
        	short startMonth 	= (short)(Short.parseShort(request.getParameter("startMonth"))-1);
        	short startDay 		= Short.parseShort(request.getParameter("startDay"));
        	short startHour		= Short.parseShort(request.getParameter("startHour"));
        	short startMin		= Short.parseShort(request.getParameter("startMin"));
        	
        	short endYear		= Short.parseShort(request.getParameter("endYear"));
        	short endMonth		= (short)(Short.parseShort(request.getParameter("endMonth"))-1);
        	short endDay		= Short.parseShort(request.getParameter("endDay"));
        	short endHour		= Short.parseShort(request.getParameter("endHour"));
        	short endMin		= Short.parseShort(request.getParameter("endMin"));
        	
        	long unit = 0;
        	short numDataPoints = 0;
        	boolean dayScale = false;
        	boolean halfHourScale = false;
        	boolean minScale = false;
        	
        	String title = "Traffic over some time";
        	
        	Calendar startTime 	= new GregorianCalendar();
        	
        	startTime.set(startYear, startMonth, startDay, startHour, startMin);
        	startTime.set(Calendar.MILLISECOND, 0);
        	
        	Calendar endTime 	= new GregorianCalendar();
        	
        	endTime.set(endYear, endMonth, endDay, endHour, endMin);
        	endTime.set(Calendar.MILLISECOND, 0);
        	
        	long start, end;
        	
        	if ((start=startTime.getTimeInMillis()) > (end=endTime.getTimeInMillis())) {
        		return "End time is before start time.";
        	}
        	
        	long duration = end - start;
        	
        	//duration less than one hour
        	if (duration <= 60*60*1000l) {
        		minScale = true;
        		numDataPoints = (short)(duration/(60*1000l));
        		unit = 60*1000;  //draw data every minute
        	} else if (duration <= 7*24*60*60*1000l) {
        		halfHourScale = true;
        		numDataPoints = (short)(duration/(30*60*1000l));
        		unit = 30*60*1000;
        	} else {
        		dayScale = true;
        		numDataPoints = (short)(duration/(24*60*60*1000l));
        		unit = 24*60*60*1000l;
        		xAxisTitle = "Date";
        	} 
        	
        	String[] xAxisLabels = new String[numDataPoints];
        	double[][] 	data = new double[1][numDataPoints];
        	
        	Calendar tmpTime = new GregorianCalendar();
        	tmpTime.setTimeInMillis(startTime.getTimeInMillis());
        	tmpTime.roll(Calendar.DAY_OF_YEAR, -1);
        	
        	for (int i=0; i<numDataPoints; i++) {
        		if (halfHourScale)
        			xAxisLabels[i] = String.valueOf((startTime.get(Calendar.HOUR)+i)%24);
        		else if (dayScale) {
        			tmpTime.roll(Calendar.DAY_OF_YEAR, 1);
        			xAxisLabels[i] = String.valueOf(tmpTime.getTime());
        		} else if (minScale)
        			xAxisLabels[i] = String.valueOf((startTime.get(Calendar.MINUTE)+i)%60);
        	}
        	
        	try {
    			
    			result.beforeFirst();
    			
    			while(result.next()) {
    				
    				numRows++;
    				
    				firstSwitched = 1000*result.getLong(1);
    				bytes = result.getLong(2)/bytesDivisor;
    	
    				/*if (first) {
    					lowerBound = cal.getTimeInMillis();   					
    					first = false;
    				}*/
    				
    				/*if ((firstSwitched >= start) && (firstSwitched<end)) {
    					index = (int)((firstSwitched-start)/unit);
    				
    					//debug += "Abstand zur lowerBound: " + (firstSwitched-lowerBound) + "\n";
    					//debug += index + "\n";
    					//debug += lowerBound + "\n";	
    					data[0][index] += bytes;
    					
    					if (data[0][index] > maxValue)
    						maxValue = (long)data[0][index];
    				}
    			}
    		} catch (SQLException e) {
    			return e.getMessage();
    		} //catch (ArrayIndexOutOfBoundsException e) {
    			//return "Index: " + index + " data[0].length: " + data[0].length;
    		//}
    		
    		if (numRows == 0)
    			return "null";
    		
    		debug += maxValue;
    		
    		int logarithm = (int)(Math.log(maxValue)/Math.log(10));
    		
    		dataAxisProperties.setRoundToNearest( logarithm-2 );
    			
        	IAxisDataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, title );
            
        	try {
        		dataSeries.addIAxisPlotDataSet( new AxisChartDataSet( data, legendLabels, paints, ChartType.AREA, areaChartProperties ) );
        	} catch (ChartDataException e) {
        		throw new ChartDataException("Error generating pie.");
        	}
        	
        	AxisChart axisChart = new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, width, height );
        	
        	request.getSession().getServletContext().setAttribute("chart", axisChart);
        }
        
		return debug;
	}*/
	
	private String createSQLQuery(HttpServletRequest request, Statement s) {
		
		String selectedChart = "";
		boolean ipProtDistr = false;
		boolean applProtDistr = false;
		
		String statement = "";
		String srcIPQuery = "";
		String dstIPQuery = "";
		String srcPortQuery = "";
		String dstPortQuery = "";
		String protoQuery = "";
		String tosQuery = "";
		String timeQuery = "";
		boolean notFirst = false;
		String order = "";
		
		long[] timeBounds = new long[2];
	
		boolean portsAggregated = false;
		boolean tosMissing = false;
		
		timeQuery = createTimeQuery(request, timeBounds);

		LinkedList tables = getTables(s, timeBounds);
		
		Iterator it = tables.listIterator();
	
		while (it.hasNext()) {
		
			String currTable = (String)it.next();
		
			if (currTable.charAt(0) == 'w') {
				tosMissing = true;
				portsAggregated = true;
			} else if (currTable.charAt(0) == 'd') {
				tosMissing = true;
			}
		}
	
		srcIPQuery = createIPQuery(request.getParameter("srcIP"), "srcIP");
		
		dstIPQuery = createIPQuery(request.getParameter("dstIP"), "dstIP");
		
		srcPortQuery = createPortQuery(request.getParameter("srcPort"), "srcPort", portsAggregated);
		
		dstPortQuery = createPortQuery(request.getParameter("dstPort"), "dstPort", portsAggregated);
		
		protoQuery = createProtoQuery(request.getParameter("proto"), "proto");
		
		if (!tosMissing) {
			tosQuery = createTosQuery(request.getParameter("tos"), "dstTos");
		}
		
		it = tables.listIterator();
		
		while (it.hasNext()) {
			
			if (appPieSrc) {
				statement = "INSERT INTO appTmp (";
				statement += "SELECT SQL_BIG_RESULT srcPort, SUM(bytes) FROM ";
			} else if(appPieDst) {
				statement = "INSERT INTO appTmp (";
				statement += "SELECT SQL_BIG_RESULT dstPort, SUM(bytes) FROM ";
			} else if (ipPie) {
				statement = "INSERT INTO ipTmp (";
				statement += "SELECT SQL_BIG_RESULT proto, SUM(bytes) FROM ";
			} else if (traffic) {
				statement = "INSERT INTO trafficTmp (";
				statement += "SELECT SQL_BIG_RESULT firstSwitched, SUM(bytes) FROM ";
			}
			
			statement += (String)it.next();
			
			if (srcIPQuery != "") {
				statement += " WHERE " + srcIPQuery;
				notFirst=true;
			} 
			if (dstIPQuery != "") {
				if (notFirst)
					statement += " AND ";
				else 
					statement += " WHERE ";
				statement += dstIPQuery;
				notFirst=true;
			}
			if (srcPortQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += srcPortQuery;
				notFirst=true;
			}
			if (dstPortQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += dstPortQuery;
				notFirst=true;
			}
			if (protoQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += protoQuery;
				notFirst=true;
			}
			
			if (tosQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += "WHERE ";
				statement += tosQuery;
				notFirst=true;
			}
			
			if (timeQuery != "") {
				if(notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += timeQuery;
				notFirst=true;
			}
			
			if (appPieSrc) {
				
				if (ignoreHighPorts) {
					if (notFirst)
						statement += " AND ";
					else
						statement += " WHERE ";
				
					statement += " srcPort BETWEEN 1 AND 1023 GROUP BY srcPort";
				} else
					statement += " GROUP BY srcPort";
				
			} else if (appPieDst) {
				
				if (ignoreHighPorts) {
					if (notFirst)
						statement += " AND ";
					else
						statement += " WHERE ";
	
					statement += " dstPort BETWEEN 1 AND 1023 GROUP BY dstPort";
				} else 
					statement += " GROUP BY dstPort";
			
			} else if (ipPie) {
				statement += " GROUP BY proto";
			} else if (traffic) {
				statement += " GROUP BY firstSwitched";
			}
			
			statement += ")";
	
			try {
				s.execute(statement);
			} catch (SQLException e) {
				//output += "Couldn't insert data into temporary table.";
				output += "Error filling temporary table (createSQLQuery): " + e.getMessage() + "\n";
				
			}
			
			notFirst=false;
		}
		
		if (statement == "") {
			output += "No data for given time range available.\n";
			return "";
		}
		
		if (appPieSrc || appPieDst) {
			
			statement = "SELECT port, SUM(bytes) AS sum FROM appTmp GROUP BY port ORDER BY sum DESC";
		} else if(ipPie) {
			statement = "SELECT proto, SUM(bytes) AS sum FROM ipTmp GROUP BY proto ORDER BY sum DESC";
		} else if(traffic) {
			statement = "SELECT firstSwitched, bytes FROM trafficTmp";
		}
		
		return statement;
		
		
		/*
		} else if (selectedChart.equalsIgnoreCase("Traffic Over Time")) {
			query = "SELECT firstSwitched, bytes FROM flow ";
			query += createWhereClause(request);
			query += " ORDER BY firstSwitched";
		}
		
		return query;*/	
	}
	
	private void fillTemporaryTable(HttpServletRequest request, Statement s) {
		
		String selectedChart = "";
		boolean ipProtDistr = false;
		boolean applProtDistr = false;
		
		String statement = "";
		String srcIPQuery = "";
		String dstIPQuery = "";
		String srcPortQuery = "";
		String dstPortQuery = "";
		String protoQuery = "";
		String tosQuery = "";
		String timeQuery = "";
		boolean notFirst = false;
		String order = "";
		
		long[] timeBounds = new long[2];
	
		boolean portsAggregated = false;
		boolean tosMissing = false;
		
		timeQuery = createTimeQuery(request, timeBounds);

		LinkedList tables = getTables(s, timeBounds);
		
		Iterator it = tables.listIterator();
	
		while (it.hasNext()) {
		
			String currTable = (String)it.next();
		
			if (currTable.charAt(0) == 'w') {
			
				portsAggregated = true;
			} else if (currTable.charAt(0) == 'd') {
				tosMissing = true;
			}
		}
	
		srcIPQuery = createIPQuery(request.getParameter("srcIP"), "srcIP");
		
		dstIPQuery = createIPQuery(request.getParameter("dstIP"), "dstIP");
		
		srcPortQuery = createPortQuery(request.getParameter("srcPort"), "srcPort", portsAggregated);
		
		dstPortQuery = createPortQuery(request.getParameter("dstPort"), "dstPort", portsAggregated);
		
		protoQuery = createProtoQuery(request.getParameter("proto"), "proto");
		
		if (!tosMissing) {
			tosQuery = createTosQuery(request.getParameter("tos"), "dstTos");
		}
		
		it = tables.listIterator();
		
		while (it.hasNext()) {
			
			if (appPieSrc) {
				statement = "INSERT INTO appTmp (";
				statement += "SELECT SQL_BIG_RESULT srcPort, SUM(bytes) FROM ";
			} else if(appPieDst) {
				statement = "INSERT INTO appTmp (";
				statement += "SELECT SQL_BIG_RESULT dstPort, SUM(bytes) FROM ";
			} else if (ipPie) {
				statement = "INSERT INTO ipTmp (";
				statement += "SELECT SQL_BIG_RESULT proto, SUM(bytes) FROM ";
			} else if (traffic) {
				statement = "INSERT INTO trafficTmp (";
				statement += "SELECT SQL_BIG_RESULT firstSwitched, SUM(bytes) FROM ";
			} else if (trafficSrc) {
				statement = "INSERT INTO trafficTmp (";
				statement += "SELECT SQL_BIG_RESULT srcPort, firstSwitched, SUM(bytes) FROM ";
			} else if (trafficDst) {
				statement = "INSERT INTO trafficTmp (";
				statement += "SELECT SQL_BIG_RESULT dstPort, firstSwitched, SUM(bytes) FROM ";
			}
			
			statement += (String)it.next();
			
			if (srcIPQuery != "") {
				statement += " WHERE " + srcIPQuery;
				notFirst=true;
			} 
			if (dstIPQuery != "") {
				if (notFirst)
					statement += " AND ";
				else 
					statement += " WHERE ";
				statement += dstIPQuery;
				notFirst=true;
			}
			if (srcPortQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += srcPortQuery;
				notFirst=true;
			}
			if (dstPortQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += dstPortQuery;
				notFirst=true;
			}
			if (protoQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += protoQuery;
				notFirst=true;
			}
			
			if (tosQuery != "") {
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += tosQuery;
				notFirst=true;
			}
			
			if (timeQuery != "") {
				if(notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
				statement += timeQuery;
				notFirst=true;
			}
			
			if (appPieSrc) {
				
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";

				//statement += " srcPort BETWEEN 1 AND 1024 GROUP BY srcPort";
				statement += "  GROUP BY srcPort";
				
			} else if (appPieDst) {
				
				if (notFirst)
					statement += " AND ";
				else
					statement += " WHERE ";
	
				//statement += " dstPort BETWEEN 1 AND 1024 GROUP BY dstPort";
				statement += "  GROUP BY dstPort";
			
			} else if (ipPie) {
				statement += " GROUP BY proto";
			} else if (traffic) {
				statement += " GROUP BY firstSwitched";
			} else if (trafficSrc) {
				
				if (ignoreHighPorts) {
					if (notFirst)
						statement += " AND ";
					else
						statement += " WHERE ";
	
					statement += " srcPort BETWEEN 1 AND 1023 GROUP BY firstSwitched, srcPort";
				} else 
					statement += " GROUP BY firstSwitched, srcPort";
			} else if (trafficDst) {
				
				if (ignoreHighPorts) {
					if (notFirst)
						statement += " AND ";
					else
						statement += " WHERE ";
	
					statement += " dstPort BETWEEN 1 AND 1023 GROUP BY firstSwitched, dstPort";
				} else 
					statement += " GROUP BY firstSwitched, dstPort";
			}
			
			statement += ")";
	
			try {
				s.execute(statement);
			} catch (SQLException e) {
				//output += "Couldn't insert data into temporary table.";
				output += "Error filling temporary table: " + e.getMessage() + "\n";
				
			}
			
			notFirst=false;
		}
		
		if (statement == "") {
			output += "No data for given time range available.\n";
			return;
		}
		
		return;
		
	}
	
	/**
	 * @return Returns the contentType.
	 */
	public String getContentType() {
		return contentType;
	}
}
