<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<html>
	<style type="text/css">
	<!--
	 a:link { color:yellow; }
	 a:visited { color:orange; }
	 a:hover { color:orange; }
	 a:active { color:red; }
	    -->
	</style>

	<body bgcolor="green" text="yellow">

		<h1 align=center>Network Analyzer</h1>

		<table width="100%">
		     <colgroup>
		             <col width="2*">
		             <col width="1*">
		             <col width="32*">
	             </colgroup>
		
		     <tr><td valign="top">
		     	     <a href="./search.jsp">Search</a><br><br>
		     	     <a href="./graphs.jsp">Charts</a></td>
		     <td></td>
		
		     <td>
			<form method="POST" action="/nasty/ChartsGenerator">

		     <c:if test="${param.submitted}" />
		     	<table>
		     		<tr><td>SrcIP:</td>
		     		    <td><input type="text" name="srcIP" value="<c:out value='${param.srcIP}' />"></td>
		     		    <td>DstIP:</td>
		     		    <td><input type="text" name="dstIP" value="<c:out value='${param.dstIP}' />"></td></tr>
		     		<tr><td>SrcPort:</td>
		     		    <td><input type="text" name="srcPort" value="<c:out value='${param.srcPort}' />"></td>
		     		    <td>DstPort:</td>
		     		    <td><input type="text" name="dstPort" value="<c:out value='${param.dstPort}' />"></td></tr>
		     		<tr><td>Protocol:</td>
		     		    <td><input type="text" name="proto" value="<c:out value='${param.proto}' />"></td>
		     		    <td>ToS:</td>
		     		    <td><input type="text" name="tos" value="<c:out value='${param.prot}' />"></td></tr>
				 <tr><td></td><td><select name="graph" size="1">
				    		<option value="protocol_pie">Protocol Distribution</option>
						<option>something</option></td></tr>
		     	</table><br>

		     	<input type="hidden" name="submitted" value="true">

		     	<input type="submit" value="Search">
		     	
		     </form>
		</td></tr></table>
		<hr>

		<c:if test="${param.submitted}" >
			<h3>Results:</h3>

			<img src="/nasty/ChartsDeliverer">
		</c:if>
	</body>
</html>
