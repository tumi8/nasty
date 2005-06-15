/*
 * Created on 29.09.2004
 */

package de.japes.text;

/**
 * @author unrza88
 * 
 * partly taken from http://www.lowagie.com/iText/examples/Chap0101.java
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;


public class PdfCreator {

	public String exportToPdf(byte[] imgArray) {
		
		String filename;
		Document document = new Document();
		Image img = null;
		
        
		Calendar cal = Calendar.getInstance();
		
		filename = 	"chart_" + cal.get(Calendar.YEAR) + cal.get(Calendar.MONTH) + 
					cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY) +
					cal.get(Calendar.MINUTE) + ".pdf";
		
		try {
			img = Image.getInstance(imgArray);
		} catch(MalformedURLException e) {
			return e.getMessage();
		} catch(IOException e) {
			return e.getMessage();
		} catch(BadElementException e) {
			return e.getMessage();
		}
		
		img.setAlignment(Image.ALIGN_CENTER);
		
        try {
            
            // step 2:
            // we create a writer that listens to the document
            // and directs a PDF-stream to a file
            
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            
            // step 3: we open the document
            document.open();
            
            // step 4: we add a paragraph to the document
            document.add(img);
            
        }
        catch(DocumentException de) {
            return de.getMessage();
        }
        catch(IOException ioe) {
        	return ioe.getMessage();
        }
        
        // step 5: we close the document
        document.close();
  
		return filename;
	}
}
