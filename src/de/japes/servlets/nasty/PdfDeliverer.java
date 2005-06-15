/*
 * Created on 30.09.2004
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
 * @author unrza88
 */
public class PdfDeliverer extends HttpServlet {
	
	PdfCreator pdfCreator = new PdfCreator();
	
	public void doPost(HttpServletRequest request,
	   		   HttpServletResponse response)
		throws ServletException, IOException {
		
		doGet(request, response);
		
	}
	
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws ServletException, IOException {
		
		Chart generatedChart = (Chart)this.getServletContext().getAttribute("chart");
		
		if (generatedChart == null) {
			response.setContentType("text/html");
			PrintWriter writer = response.getWriter();
			writer.println("<html><body><p>Generated Chart is null");
			writer.println("</p></body></html>");
			return;
		}
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
				
		String filename = pdfCreator.exportToPdf(byteStream.toByteArray());
		
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
		
		new File(filename).delete();
	}

}
