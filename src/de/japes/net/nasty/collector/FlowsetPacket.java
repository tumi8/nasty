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
 * Created on 15.07.2004
 */

package de.japes.net.nasty.collector;

/**
 * @author unrza88
 */

import java.io.IOException;

public interface FlowsetPacket {
	
	public void readContents() throws IOException, FlowFormatException;
	
	public void setContent(byte[] buf);
	
	public void setSrcAddress(long addr);
}

class FlowFormatException extends Exception {
	
	public FlowFormatException() {}
	public FlowFormatException(String msg) {
		super(msg);
	}
}

class UnknownTemplateException extends Exception {
	public UnknownTemplateException () {}
	public UnknownTemplateException (String msg) {
		super(msg);
	}
}

class UnknownExporterException extends Exception {
	public UnknownExporterException () {}
	public UnknownExporterException (String msg) {
		super(msg);
	}
}