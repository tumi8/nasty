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
