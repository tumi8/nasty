<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>


 <html>
 <head>
  <title>
   Nasty - Network Analysis And Statistics Yielding
  </title>
  <style type="text/css">
    <!--
    body {
            background-color: #eeeeee;
    }

    td, p, ul, ol {
            color: black;
            font-size: 10px;
            font-family: Verdana, "Lucida Sans", Arial, Geneva, Helvetica, Helv, "Myriad Web", Syntax, sans-serif;
            text-align: justify;
    }
    h1
{
	font-family: "Trebuchet MS", Verdana, "Lucida Sans", Arial, Geneva, Helvetica, Helv, "Myriad Web", Syntax, sans-serif;
        color: #454545;
	font-size: 200%;
	font-weight: bold;
}
    -->
  </style>
 </head>

 <body>

  <!-- body table -->
  <table align="center" valign="top" border="0" cellpadding="2" cellspacing="0" width="950">
   <!-- top header -->
   <tr>
    <td valign="center" width="25%">
     <!-- title table -->
     <table align="center" bgcolor="#000000" border="0" cellpadding="1" cellspacing="0" width="100%">
      <tr>
       <td valign="center">
        <table bgcolor="#ffffff" border="0" cellpadding="8" cellspacing="0" width="100%">
         <tr>
          <td valign="center">
           <h1>&nbsp;Nasty - Network Analysis And Statistics Yielding</h1>
          </td>
         </tr>
        </table>
       </td>
      </tr>
     </table>
     <!-- end title table -->
    </td>
   </tr>
   <!-- end top header -->
 
   <tr>
    <!-- top column -->
    <td valign="top" width="25%">
     <!-- sidebox -->
     <table bgcolor="#000000" border="0" cellpadding="0" cellspacing="0" width="100%">
      <tr>
       <td>
        <table border="0" cellpadding="3" cellspacing="1" width="100%">
         <tr>
          <td bgcolor="#cccccc">
           &nbsp;<b>Login</b>
          </td>
         </tr>
         <tr>
          <td bgcolor="#ffffff">
          <p>You first have to login to access the requested page! <br><br>
  	
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

		
          </td>
         </tr>
        </table>
       </td>
      </tr>
     </table>
     <!-- end sidebox -->

    </td>
    <!-- end top column -->
	</tr>

  </table>
  <!-- end body table -->
 </body>
</html>
</html>
