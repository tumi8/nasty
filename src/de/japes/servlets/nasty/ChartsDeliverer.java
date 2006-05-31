/**
 * Title:   ChartsDeliverer
 * Project: NASTY
 *
 * @author  unrza88
 * @version %I% %G%
 */

package de.japes.servlets.nasty;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jCharts.*;
import org.jCharts.chartData.*;
import org.jCharts.encoders.*;
import org.jCharts.properties.*;

/**
 * Servlet that extracts a chart from the servlet context, encodes it into jpeg
 * and returns it.
 * <p>
 * The chart was created by <code>ChartOutputCreator</code> as an
 * <code>org.jCharts.Chart</code>-object.
 */
public class ChartsDeliverer extends HttpServlet {
	
	/**
         * This method would be invoked by the servlet container when the servlet
         * was called via HTTP-POST. Since HTTP-GET is normally used to retrieve the chart, this
         * method is not really necessary, but it is standard practice to implement it.
         * All it does is call <code>doGet</code>.
         *
         * @param request   Input parameters of the servlet.
         * @param response  Output parameters of the servlet.
         */
	public void doPost(HttpServletRequest request,
	   		   HttpServletResponse response)
		throws ServletException, IOException {
		
		doGet(request, response);
		
	}
	
	/**
         * This method is invoked by the servlet container when a chart should be
         * retrieved. It looks into the servlet context, extracts the chart-object,
         * encodes it into jpeg and returns it.
         *
         * @param request   Input parameters of the servlet.
         * @param response  Output parameters of the servlet, the jpeg-chart will be put there.
         */
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws ServletException, IOException {
		
		PrintWriter out = null;
                
                // check which chart object
                String chartname = request.getParameter("chartname");
                if (chartname==null) chartname = "chart";
		
		// retrieve the chart-object from the servlet context
                Chart chart = (Chart)this.getServletContext().getAttribute(chartname);
		
		if (chart == null) {
			// no chart was found in the context
                        out = response.getWriter();
			out.println("Chart is null");
			return;
		}
		
		try {
                        // encode the chart into jpeg and add it to the servlet response
			ServletEncoderHelper.encodeJPEG(chart, 1.0f, response);
		} catch (ChartDataException e) {
			out.println("Error encoding chart");
		} catch (PropertyException e) {
			out.println("Something's wrong with the chart's properties");
		}
	
	}
}
