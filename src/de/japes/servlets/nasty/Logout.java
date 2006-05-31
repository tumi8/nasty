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
 * Title:   Logout
 * Project: NASTY
 *
 * @author  Thomas Schurtz, unrza88
 * @version %I% %G%
 */
package de.japes.servlets.nasty;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * This servlet is called when a user presses the logout-button of nasty's <code>index.jsp</code>.
 * It terminates the user's session and displays a logout confirmation page.
 */
public class Logout extends HttpServlet {
	
	/**
         * This method is invoked by the servlet container when the logout-button
         * of <code>index.jsp</code> is pressed. It displays the logout confirmation
         * page and terminates the user's session.
         *
         * @param request   Input parameters of the servlet.
         * @param response  Output parameters of the servlet.
         */
	public void doPost(HttpServletRequest request,
			   HttpServletResponse response)
		throws ServletException, IOException {
		
            // display logout confirmation page
            this.getServletContext().getRequestDispatcher("/logout.html").forward(request, response);
            // terminate user's session
            request.getSession().invalidate();
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
