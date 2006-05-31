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
 * Created on 22.09.2004
 */

package de.japes.parser;

/**
 * @author unrza88
 */

import java.util.HashMap;
import java.io.*;

public class Tester {

	public static void main(String[] args) {
		
		HashMap hash;
		String[] protos = null;
		try {
			//new ServicesParser(new FileInputStream(new File("/etc/services")));
			new ProtocolsParser(new FileInputStream(new File("/etc/protocols")));
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
		}
		
		try {
			ProtocolsParser.Start();
		} catch (ParseException e) {
			System.err.println("Parse Exception");
		}
		
		//hash = ServicesParser.getPortHash();
		hash = ProtocolsParser.getProtoHash();
		protos = ProtocolsParser.getProtocols();
		System.out.println(ProtocolsParser.isInitialized());
		
		//System.out.println("Name: http, Port: " + ((int[])hash.get("http"))[0]);
		System.out.println("Name: udp, Port: " + ((int[])hash.get("udp"))[0]);
	}
}
