/*
 * Created on 09.09.2004
 */

package de.japes.servlets.nasty;

/**
 * @author unrza88
 */

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jCharts.*;
import org.jCharts.chartData.*;
import org.jCharts.encoders.*;
import org.jCharts.properties.*;


public class ChartsDeliverer extends HttpServlet {
	
	public void doPost(HttpServletRequest request,
	   		   HttpServletResponse response)
		throws ServletException, IOException {
		
		doGet(request, response);
		
	}
	
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws ServletException, IOException {
		
		PrintWriter out = null;
		
		Chart chart = (Chart)this.getServletContext().getAttribute("chart");
		
		if (chart == null) {
			out = response.getWriter();
			out.println("Chart is null");
			return;
		}
		
		try {
			ServletEncoderHelper.encodeJPEG(chart, 1.0f, response);
		} catch (ChartDataException e) {
			out.println("Error encoding chart");
		} catch (PropertyException e) {
			out.println("Something's wrong with the chart's properties");
		}
	
	}
}
