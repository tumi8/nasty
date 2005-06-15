/*
 * Created on 24.09.2004
 */

package de.japes.servlets.nasty;

/**
 * @author unrza88
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;

public class Logout extends HttpServlet {
	
	public void doPost(HttpServletRequest request,
			   HttpServletResponse response)
		throws ServletException, IOException {
		
		request.getSession().invalidate();
	}
	
	public void doGet(HttpServletRequest request,
			   HttpServletResponse response) 
		throws IOException, ServletException {
		
		doPost(request, response);
	}
}
