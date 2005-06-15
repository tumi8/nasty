<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<html>

  <style type="text/css">
  <!--

  	body {
	        font-family: Arial;
	        font-weight: bold;
	        background-color: #FFFFCC;
	        color: #006600;
	}

	table {
	        font-weight: bold;
	}

	input {
	        color: #006600;
	}

	select {
	        color: #006600;
	}

	a:link { color:green; }
	a:visited { color:orange; }
	a:hover { color:orange; }
	a:active { color:red; }
  -->
  </style>

  <head>
    <title>nasty - Network Analysis And Statistics Yielding</title>
  </head>

  <body>

    <h1>nasty</h1>

    	<p>You have to login to access the requested page! <br><br>
  	
		<form method="POST" action='<%= response.encodeURL("j_security_check") %>'>
	    		<table>
				<tr><td>Username:</td>
				    <td><input type="text" name="j_username"></td></tr>
	    			<tr><td>Password:</td>
				    <td><input type="password" name="j_password"></td></tr>
			</table><br>
	    		<input type="submit" value="Login">
	    		<input type="reset" value="Reset">
		</form>

	</p>

  </body>
</html>
