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
