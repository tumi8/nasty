/**
 * Title:   PdfDeliverer
 * Project: NASTY
 *
 * @author  unrza88
 * @version %I% %G%
 */

package de.japes.servlets.nasty;

import de.japes.text.PdfCreator;
import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jCharts.Chart;
import org.jCharts.chartData.ChartDataException;
import org.jCharts.encoders.JPEGEncoder;
import org.jCharts.properties.PropertyException;

/**
 * Servlet that extracts a chart from the servlet context, encodes it into pdf
 * and returns it.
 * <p>
 * The chart was created by <code>ChartOutputCreator</code> as an
 * <code>org.jCharts.Chart</code>-object.
 */
public class PdfDeliverer extends HttpServlet {
	
	/** Object that creates pdf content. */
        PdfCreator pdfCreator = new PdfCreator();
	
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
         * encodes it into pdf and returns it.
         *
         * @param request   Input parameters of the servlet.
         * @param response  Output parameters of the servlet, the jpeg-chart will be put there.
         */
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws ServletException, IOException {
		
		// retrieve the chart-object from the servlet context
		Chart generatedChart = (Chart)this.getServletContext().getAttribute("chart");
		
		if (generatedChart == null) {
			// no chart was found in the context
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body><p>Generated Chart is null");
			writer.println("</p></body></html>");
			return;
		}

                // encode chart into jpeg first and write it into a stream
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		try {
			JPEGEncoder.encode(generatedChart, 1.0f, byteStream);
		} catch(PropertyException e) {
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body><p>Error creating PDF.");
			writer.println(e.getMessage());
			writer.println("</p></body></html>");
		} catch(ChartDataException e) {
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body><p>Error creating PDF.");
			writer.println(e.getMessage());
			writer.println("</p></body></html>");
		}
				
		// convert stream to pdf
                String filename = pdfCreator.exportToPdf(byteStream.toByteArray());
		
		// write pdf-stream into servlet response
                OutputStream out = response.getOutputStream();
		
		DataOutputStream dataOut = new DataOutputStream(out);
		
		FileInputStream fileIn = new FileInputStream(filename);
		
		response.setContentType("application/pdf");
		
		byte[] buf = new byte[1024];
		
		while(fileIn.read(buf)>0) {
			dataOut.write(buf);
		}
		
		dataOut.close();
		out.close();
		fileIn.close();
		
		// delete the stream
                new File(filename).delete();
	}

}
