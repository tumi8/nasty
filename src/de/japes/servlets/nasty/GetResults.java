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
 * Title:   GetResults
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */
package de.japes.servlets.nasty;

import de.japes.beans.nasty.*;
import java.io.*;
import java.sql.*;
import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.util.ArrayList;

/**
 * Servlet that queries databases filled by nasty-collectors according to
 * parameters selected in nasty's <code>index.jsp</code> and returns the output
 * in the desired format to <code>index.jsp</code>.
 */
public class GetResults extends HttpServlet {
	/** Bean that is used to transmit return parameters back to the JSP. */
	private ServletResults servResp = new ServletResults();
        /** Output of the query. */
        private String output = "";
	
	/**
         * This method is invoked by the servlet container when the search-button
         * of <code>index.jsp</code> is pressed. It checks which databases should
         * be queried, prepares the connections to these databases and calls the
         * appropriate <code>OutputCreator</code> to produce the results
         * of the selected query type.
         *
         * @param request   Input parameters of the servlet.
         * @param response  Output parameters of the servlet.
         * @see OutputCreator
         */
        public void doPost(HttpServletRequest request,
					   HttpServletResponse response)
		throws ServletException, IOException {
		
                // check if search-button was not pressed and instead a switch to another
                // query type occured
                if (request.getParameter("search")==null) {
                    if (request.getParameter("status")!=null) {
                        this.getServletContext().getRequestDispatcher("/index.jsp?switch=true").forward(request, response);
                    }
                    if (request.getParameter("html")!=null) {
                        this.getServletContext().getRequestDispatcher("/index2.jsp?switch=true").forward(request, response);
                    }
                    if (request.getParameter("perl")!=null) {
                        this.getServletContext().getRequestDispatcher("/index3.jsp?switch=true").forward(request, response);
                    }
                    if (request.getParameter("htmlexp")!=null) {
                        this.getServletContext().getRequestDispatcher("/index4.jsp?switch=true").forward(request, response);
                    }
                    if (request.getParameter("chart")!=null) {
                        this.getServletContext().getRequestDispatcher("/index5.jsp?switch=true").forward(request, response);
                    }
                    return;
                }
                String format = request.getParameter("outputFormat");
                if (format==null) format = "";
                String indexJspName = "";
                if (format.equalsIgnoreCase("status")) {
                    indexJspName = "/index.jsp";
                } else if (format.equalsIgnoreCase("html")) {
                    indexJspName = "/index2.jsp";
                } else if (format.equalsIgnoreCase("perl")) {
                    indexJspName = "/index3.jsp";
                } else if (format.equalsIgnoreCase("htmlexp")) {
                    indexJspName = "/index4.jsp";
                } else if (format.equalsIgnoreCase("chart")) {
                    indexJspName = "/index5.jsp";
                } else {
                     return;
                }
            
                // local database for combining results in a temporary table
                DataSource ds = null;
                // number of databases to be queried
                int dbNum = 0;
                // list of databases that can be queried
                ArrayList dbDataSourceList = new ArrayList();
                ArrayList dbList = new ArrayList();

                // **** added for mysqldump-version of program ********
                // information on how to contact the database servers with mysqldump
                // (host, user, password and name of the database)
                // list of such information for the databases that can be queried
                ArrayList dbInfoList = new ArrayList();
                // information for local database that holds the temporary table
                DBInfo dbLocalInfo = new DBInfo();
                // ****************************************************
		
                output = "";
		
		// retrieve databases to be used
                try {
               
			Context initCtx = new InitialContext();
                        Context envCtx = (Context) initCtx.lookup("java:comp/env");	
			
			/* Debug Code for testing the context.
			   
			       NamingEnumeration enum = envCtx.listBindings("jdbc");
			       while (enum.hasMore()) {
				   Binding binding = (Binding) enum.next();
				   output += "Name: " + binding.getName() + "<br>";
				   output += "Type: " + binding.getClassName() + "<br>";
				   output += "Value: " + binding.getObject() + "<p>";
			       }
*/

                        // lookup local database to be used for combining results
                        ds = (DataSource)envCtx.lookup("jdbc/LocalDB");
                        
                        // ********* added for mysqldump-version of program*********
                        // get information on the local database
                        dbLocalInfo.setHost(request.getSession().getServletContext().getInitParameter("dbHostLocal"));
                        dbLocalInfo.setUser(request.getSession().getServletContext().getInitParameter("dbUserLocal"));
                        dbLocalInfo.setPassword(request.getSession().getServletContext().getInitParameter("dbPasswordLocal"));
                        dbLocalInfo.setName(request.getSession().getServletContext().getInitParameter("dbNameLocal"));
                        if (request.getSession().getServletContext().getInitParameter("dbUseSSHLocal")!=null) {
                            dbLocalInfo.setUseSSH((request.getSession().getServletContext().getInitParameter("dbUseSSHLocal").compareToIgnoreCase("true")==0));
                        }
                        // **********************************************************

                        // get list of databases to be queried
                        String[] dbNames = request.getParameterValues("dbSelects");
                        // lookup number of databases to be queried
                        dbNum = dbNames.length;
                        // lookup selected databases and add them to the list
                        for (int i=0; i<dbNum; i++) {
                            dbDataSourceList.add((DataSource)envCtx.lookup("jdbc/QueryDB"+dbNames[i]));
 
                            // ****** added for mysqldump-version of program*********
                            // get information on selected database
                            DBInfo dbInfo = new DBInfo();
                            dbInfo.setHost(request.getSession().getServletContext().getInitParameter("dbHost"+dbNames[i]));
                            dbInfo.setUser(request.getSession().getServletContext().getInitParameter("dbUser"+dbNames[i]));
                            dbInfo.setPassword(request.getSession().getServletContext().getInitParameter("dbPassword"+dbNames[i]));
                            dbInfo.setName(request.getSession().getServletContext().getInitParameter("dbName"+dbNames[i]));
                            if (request.getSession().getServletContext().getInitParameter("dbUseSSH"+dbNames[i])!=null) {
                                dbInfo.setUseSSH((request.getSession().getServletContext().getInitParameter("dbUseSSH"+dbNames[i]).compareToIgnoreCase("true")==0));
                            }
                            dbInfoList.add(dbInfo); 
                            // ******************************************************
                        }
		
		} catch (Exception e) {
			// error: compose error message and return to JSP
                        response.setContentType("text/html");
                        if (request.getParameterValues("dbSelects")==null) {
                            output += "<p> No database selected! </p>";
                        } else {
                            output += "<p>Error while trying to find DB connections.</p>";
                            output += e.getMessage() + ": " + e.getCause() + "<p>";
                        }
			servResp.setQueryResult(output);
			request.setAttribute("result", servResp);
			this.getServletContext().getRequestDispatcher(indexJspName).forward(request, response);
			return;
		}

        	// establish database connections
                Connection conn=null; 
		Statement s=null; 
		try {
                    // get Statement-object for local database
                    conn = ds.getConnection();
                    s = conn.createStatement();
                } catch (SQLException e) {
		    // fatal error, local database is needed for storing temporary tables, exit	
                    output += "<p>Error opening DB connection to local database.</p>";
                    output += e.getMessage() + ": " + e.getCause() + "<p>";
                    servResp.setQueryResult(output);
                    request.setAttribute("result", servResp);
                    this.getServletContext().getRequestDispatcher(indexJspName).forward(request, response);
                    return;
		} 
                // get Statement-objects for each database that should be queried
                for (int i=0; i<dbNum; i++) {
                    try {
                        conn = ((DataSource)dbDataSourceList.get(i)).getConnection();
                        dbList.add(conn.createStatement());
                    } catch (SQLException e) {
                        // non fatal error, just use other databases
			output += "<p>Error opening connection to "+(i+1)+". selected database.</p>";
			output += e.getMessage() + ": " + e.getCause() + "<p>";
                    }
		} 
                        

                // check selected output format and create corresponding OutputCreator
                OutputCreator outputCreator;
                if (format.equalsIgnoreCase("html")) {
                     outputCreator = new HTMLOutputCreator(request, response, s, dbList);
                } else if (format.equalsIgnoreCase("htmlexp")) {
                     outputCreator = new HTMLExpOutputCreator(request, response, s, dbList);
                } else if (format.equalsIgnoreCase("perl")) {
                     outputCreator = new PerlOutputCreator(request, response, s, dbList);
                } else if (format.equalsIgnoreCase("chart")) {
                     outputCreator = new ChartOutputCreator(request, response, s, dbList);
                } else if (format.equalsIgnoreCase("status")) {
                     outputCreator = new StatusOutputCreator(request, response, s, dbList);
                } else {
                     return;
                }

                // Java- or mysqldump-distributed-query?
                if ((request.getParameter("dqSelect")!=null) && (request.getParameter("dqSelect").equalsIgnoreCase("useMysqldumpDQ"))) {
                    outputCreator.useMysqldumpVersion(dbInfoList, dbLocalInfo);
                }
			
                try {

                    // ********* perform query **********
                    output += outputCreator.createOutput();
                    // **********************************
                    
                    // close Statement-objects
                    s.close();
                    for (int i=0; i<dbList.size(); i++) {
                        ((Statement) dbList.get(i)).close();
                    } 
                } catch (SQLException e) {
		    // error	
                    output += "<p>Error using DB connection.</p>";
                    output += e.getMessage() + ": " + e.getCause() + "</p>";
                } 
		
                // transfer results to browser
                response.setContentType(outputCreator.getContentType());
                servResp.setQueryResult(output);
                request.setAttribute("result", servResp);
                
                this.getServletContext().getRequestDispatcher(indexJspName).forward(request, response);
		
		return;
	}
	
	/**
         * This method would be invoked by the servlet container when the servlet
         * was called via HTTP-GET. Since <code>index.jsp</code> uses HTTP-POST, this
         * method is not really necessary, but it is standard practice to implement it.
         * All it does is call <code>doPost</code>.
         *
         * @param request   Input parameters of the servlet.
         * @param response  Output parameters of the servlet.
         */
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws IOException, ServletException {
		
		doPost(request, response);
	}
	
}
