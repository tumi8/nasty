<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<c:set var="remTimeDiv" value="${param.remTimeDiv}" />
<c:if test="${param.remTimeDiv == null}">
	<c:set var="remTimeDiv" value="1" />
</c:if>
<c:if test="${param.remTimeDiv == ''}">
	<c:set var="remTimeDiv" value="1" />
</c:if>
<c:set var="grpSrcIPDiv" value="${param.grpSrcIPDiv}" />
<c:if test="${param.grpSrcIPDiv == null}">
	<c:set var="grpSrcIPDiv" value="32" />
</c:if>
<c:if test="${param.grpSrcIPDiv == ''}">
	<c:set var="grpSrcIPDiv" value="32" />
</c:if>
<c:set var="grpDstIPDiv" value="${param.grpDstIPDiv}" />
<c:if test="${param.grpDstIPDiv == null}">
	<c:set var="grpDstIPDiv" value="32" />
</c:if>
<c:if test="${param.grpDstIPDiv == ''}">
	<c:set var="grpDstIPDiv" value="32" />
</c:if>
<c:set var="grpBytesDiv" value="${param.grpBytesDiv}" />
<c:if test="${param.grpBytesDiv == null}">
	<c:set var="grpBytesDiv" value="1" />
</c:if>
<c:if test="${param.grpBytesDiv == ''}">
	<c:set var="grpBytesDiv" value="1" />
</c:if>
<c:set var="grpTimeDiv" value="${param.grpTimeDiv}" />
<c:if test="${param.grpTimeDiv == null}">
	<c:set var="grpTimeDiv" value="1" />
</c:if>
<c:if test="${param.grpTimeDiv == ''}">
	<c:set var="grpTimeDiv" value="1" />
</c:if>

<c:forEach items="${paramValues.checks}" var="current">
	<c:choose>
		<c:when test="${current == 'resolveIP'}">
			<c:set var="resolveIP" value="true" />
		</c:when>
		<c:when test="${current == 'showSrcIP'}">
			<c:set var="showSrcIP" value="true" />
		</c:when>
		<c:when test="${current == 'showDstIP'}">
			<c:set var="showDstIP" value="true" />
		</c:when>
		<c:when test="${current == 'showSrcPort'}">
			<c:set var="showSrcPort" value="true" />
		</c:when>
		<c:when test="${current == 'showDstPort'}">
			<c:set var="showDstPort" value="true" />
		</c:when>
		<c:when test="${current == 'showProto'}">
			<c:set var="showProto" value="true" />
		</c:when>
		<c:when test="${current == 'showTos'}">
			<c:set var="showTos" value="true" />
		</c:when>
		<c:when test="${current == 'showPackets'}">
			<c:set var="showPackets" value="true" />
		</c:when>
		<c:when test="${current == 'showBytes'}">
			<c:set var="showBytes" value="true" />
		</c:when>
		<c:when test="${current == 'showFirstSwitched'}">
			<c:set var="showFirstSwitched" value="true" />
		</c:when>
		<c:when test="${current == 'showLastSwitched'}">
			<c:set var="showLastSwitched" value="true" />
		</c:when>
		<c:when test="${current == 'showDuration'}">
			<c:set var="showDuration" value="true" />
		</c:when>
		<c:when test="${current == 'showExporter'}">
			<c:set var="showExporter" value="true" />
		</c:when>
		<c:when test="${current == 'showDatabase'}">
			<c:set var="showDatabase" value="true" />
		</c:when>
		<c:when test="${current == 'restrictTime'}">
			<c:set var="restrictTime" value="true" />
		</c:when>
		<c:when test="${current == 'ignoreHighPorts'}">
			<c:set var="ignoreHighPorts" value="true" />
		</c:when>
		<c:when test="${current == 'remDoubles'}">
			<c:set var="remDoubles" value="true" />
		</c:when>
		<c:when test="${current == 'remExporterID'}">
			<c:set var="remDoubles" value="true" />
			<c:set var="remExporterID" value="true" />
		</c:when>
	</c:choose>
</c:forEach>

<c:forEach items="${paramValues.group}" var="current">
	<c:choose>
		<c:when test="${current == 'grpSrcIP'}">
			<c:set var="grpSrcIP" value="true" />
		</c:when>
		<c:when test="${current == 'grpDstIP'}">
			<c:set var="grpDstIP" value="true" />
		</c:when>
		<c:when test="${current == 'grpSrcPort'}">
			<c:set var="grpSrcPort" value="true" />
		</c:when>
		<c:when test="${current == 'grpDstPort'}">
			<c:set var="grpDstPort" value="true" />
		</c:when>
		<c:when test="${current == 'grpProto'}">
			<c:set var="grpProto" value="true" />
		</c:when>
		<c:when test="${current == 'grpTos'}">
			<c:set var="grpTos" value="true" />
		</c:when>
		<c:when test="${current == 'grpPackets'}">
			<c:set var="grpPackets" value="true" />
		</c:when>
		<c:when test="${current == 'grpBytes'}">
			<c:set var="grpBytes" value="true" />
		</c:when>
		<c:when test="${current == 'grpTime'}">
			<c:set var="grpTime" value="true" />
		</c:when>
		<c:when test="${current == 'grpDuration'}">
			<c:set var="grpDuration" value="true" />
		</c:when>
		<c:when test="${current == 'grpExporter'}">
			<c:set var="grpExporter" value="true" />
		</c:when>
		<c:when test="${current == 'grpDatabase'}">
			<c:set var="grpDatabase" value="true" />
		</c:when>
	</c:choose>
</c:forEach>

<html>
 <head>
  <title>
   Nasty - Network Analysis And Statistics Yielding
  </title>
  <link href="style.css" rel="stylesheet" type="text/css" />
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

		<!-- form -->
		
            <form method="POST" action="/nasty/GetResults">
 
	<tr>
    <!-- query type column -->
    <td valign="top" width="75%">
     <table border="0" cellpadding="0" cellspacing="0" width="100%">
      <tr>
       <td valign="top" width="100%">

        <!-- story -->
        
        <table align="center" bgcolor="#000000" border="0" cellPadding="0" cellSpacing="0" width="100%">
         <tr>
          <td>
           <table border="0" cellpadding="3" cellspacing="1" width="100%" align=center>
            <tr>
             <td bgcolor="#cccccc">
              &nbsp;<big>Select different Query to perform:</big>
             </td>
            </tr>
            <tr>
             <td bgcolor="#ffffff">
                    <br>
                        <input type="button" name="status" value="Network Status Overview" disabled>
                        <input type="submit" name="html" value="Table">
		     	<input type="submit" name="perl" value="Perl Table">
		     	<input type="submit" name="htmlexp" value="Table with Flow-Exporter-Distrib.">
		     	<input type="submit" name="chart" value="Chart">

             </td>
            </tr>
           </table>
          </td>
         </tr>
        </table>
        <!-- end story -->
        <br />
        

       </td>
      </tr>
     </table>
    </td>
    <!-- end query type column -->
   </tr>

   
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
           &nbsp;<big>Query to perform: <b>Network Status Overview</b></big>
          </td>
         </tr>
         <tr>
          <td bgcolor="#ffffff">

		     	<input type="hidden" name="outputFormat" value="status">
		     	<input type="hidden" name="chartSelect" value="<c:out value='${param.chartSelect}' />">
			<p>
			<b>Restrict Data:</b>
                        <p>
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
		     		    <td><input type="text" name="tos" value="<c:out value='${param.tos}' />"></td>
		     		    <td>ExporterID:</td>
		     		    <td><input type="text" name="exporterID" value="<c:out value='${param.exporterID}' />"></td></tr></table>
                        <table>
                            <tr><td>
                            <c:choose>
                                <c:when test="${(!param.submitted || restrictTime)}">
                                        <input type="checkbox" name="checks" value="restrictTime" checked> 
                                </c:when>
                                <c:otherwise>
                                        <input type="checkbox" name="checks" value="restrictTime">						
                                        <%--<input type="checkbox" name="checks" value="restrictTime">--%>
                                </c:otherwise>
                             </c:choose>
                                Time between:</td>
                                <td><select name="startDay" size="1">
                                        <c:forEach begin="1" end="31" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.startDay==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="startMonth" size="1">
                                        <c:forEach begin="1" end="12" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.startMonth==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="startYear" size="1">
                                        <c:forEach begin="2005" end="2008" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.startYear==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="startHour" size="1">
                                        <c:forEach begin="0" end="23" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.startHour==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="startMin" size="1">
                                        <c:forEach begin="0" end="59" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.startMin==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                and</td>
                                <td>
                                <select name="endDay" size="1">
                                        <c:forEach begin="1" end="31" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.endDay==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="endMonth" size="1">
                                        <c:forEach begin="1" end="12" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.endMonth==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="endYear" size="1">
                                        <c:forEach begin="2005" end="2008" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.endYear==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="endHour" size="1">
                                        <c:forEach begin="0" end="23" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.endHour==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                <select name="endMin" size="1">
                                        <c:forEach begin="0" end="59" var="current">
                                                <c:choose>
                                                        <c:when test="${param.submitted && param.endMin==current}">
                                                                <option selected><c:out value="${current}" /></option>
                                                        </c:when>
                                                        <c:otherwise>
                                                                <option><c:out value="${current}" /></option>
                                                        </c:otherwise>
                                                </c:choose>
                                        </c:forEach>
                                </select>
                                </td>
                        </tr>
		     	</table>
			<p>Collector Databases to query:<br>

                        <%
                          int dbNum=Integer.valueOf(application.getInitParameter("dbNum")).intValue();
                          for (int i=1; i<dbNum+1; i++) {
                             String dbName=application.getInitParameter("dbDescription"+i); 
                             boolean checked = false;
                             String[] dbChecks = request.getParameterValues("dbSelects");
                             if (dbChecks!=null) {
                               for (int j=0; j<dbChecks.length; j++) {
                                 if (dbChecks[j].compareTo(""+i)==0) checked=true;
                               }
                             } else checked=true; 
                             if (checked) {
                               out.println("<input type=\"checkbox\" name=\"dbSelects\" value=\""+i+"\" checked>"+dbName);
                             } else {
                               out.println("<input type=\"checkbox\" name=\"dbSelects\" value=\""+i+"\">"+dbName);
                             }
                          }
                        %>
                        
		     	<p>
 
			Remove Records with identical Data in:<i> (May slow down query substantially!)</i><br>
			<c:choose>
				<c:when test="${(param.submitted && remDoubles)}">
					<input type="checkbox" name="checks" value="remDoubles" checked>SrcIP,DstIP,SrcPort,DstPort,Protocol,ToS,Time Stamps
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="remDoubles">SrcIP,DstIP,SrcPort,DstPort,Protocol,ToS,Time Stamps
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && remExporterID)}">
					<input type="checkbox" name="checks" value="remExporterID" checked> + ExporterID,Packets,Bytes
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="remExporterID"> + ExporterID,Packets,Bytes
				</c:otherwise>
			</c:choose>
                        <br>(Time Stamps must be within the same 
		     	<input type="text" name="remTimeDiv" size=5 value="<c:out value='${remTimeDiv}' />" />
		     	-Sec.-Blocks)
		     	
		     	<p>
                        <hr>

                        <c:choose>
				<c:when test="${(!param.submitted || grpSrcIP)}">
					<input type="hidden" name="group" value="grpSrcIP">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpDstIP)}">
					<input type="hidden" name="group" value="grpDstIP">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpSrcPort)}">
					<input type="hidden" name="group" value="grpSrcPort">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpDstPort)}">
					<input type="hidden" name="group" value="grpDstPort">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpProto)}">
					<input type="hidden" name="group" value="grpProto">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpTos)}">
					<input type="hidden" name="group" value="grpTos">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpTime)}">
					<input type="hidden" name="group" value="grpTime">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpPackets)}">
					<input type="hidden" name="group" value="grpPackets">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpBytes)}">
					<input type="hidden" name="group" value="grpBytes">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpDuration)}">
					<input type="hidden" name="group" value="grpDuration">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpExporter)}">
					<input type="hidden" name="group" value="grpExporter">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || grpDatabase)}">
					<input type="hidden" name="group" value="grpDatabase">
				</c:when>
			</c:choose>
                        <b>Aggregate Data:</b>
                        <p>
                        SrcIP Netmask:
		     	<input type="text" name="grpSrcIPDiv" size=2 value="<c:out value='${grpSrcIPDiv}' />" />
		     	DstIP Netmask:
		     	<input type="text" name="grpDstIPDiv" size=2 value="<c:out value='${grpDstIPDiv}' />" />

		     	<input type="hidden" name="grpBytesDiv" value="<c:out value='${param.grpBytesDiv}' />">
		     	<input type="hidden" name="grpTimeDiv" value="<c:out value='${param.grpTimeDiv}' />">
			
			<c:choose>
				<c:when test="${param.order==null}">
					<input type="hidden" name="order" value="none">
				</c:when>
				<c:otherwise>
                		     	<input type="hidden" name="order" value="<c:out value='${param.order}' />">
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${param.sort==null}">
					<input type="hidden" name="sort" value="increase">
				</c:when>
				<c:otherwise>
                		     	<input type="hidden" name="sort" value="<c:out value='${param.sort}' />">
				</c:otherwise>
			</c:choose>
                        
                        <p>
		     	
			<hr>
			<b>Additional Options:</b>

			<c:choose>
				<c:when test="${(!param.submitted || showSrcIP)}">
					<input type="hidden" name="checks" value="showSrcIP">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showDstIP)}">
					<input type="hidden" name="checks" value="showDstIP">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showSrcPort)}">
					<input type="hidden" name="checks" value="showSrcPort">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showDstPort)}">
					<input type="hidden" name="checks" value="showDstPort">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showProto)}">
					<input type="hidden" name="checks" value="showProto">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showTos)}">
					<input type="hidden" name="checks" value="showTos">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showPackets)}">
					<input type="hidden" name="checks" value="showPackets">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showBytes)}">
					<input type="hidden" name="checks" value="showBytes">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showFirstSwitched)}">
					<input type="hidden" name="checks" value="showFirstSwitched">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showLastSwitched)}">
					<input type="hidden" name="checks" value="showLastSwitched">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showDuration)}">
					<input type="hidden" name="checks" value="showDuration">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showExporter)}">
					<input type="hidden" name="checks" value="showExporter">
				</c:when>
			</c:choose>
			<c:choose>
				<c:when test="${(!param.submitted || showDatabase)}">
					<input type="hidden" name="checks" value="showDatabase">
				</c:when>
			</c:choose>
			
                        <p>Output Unit:<br>
			<c:choose>
				<c:when test="${(param.submitted && param.unit=='bytes') || !param.submitted}">
					<input type="radio" name="unit" value="bytes" checked>Bytes
				</c:when>
				<c:otherwise>
					<input type="radio" name="unit" value="bytes">Bytes
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && param.unit=='kilo')}">
					<input type="radio" name="unit" value="kilo" checked>kB
				</c:when>
				<c:otherwise>
					<input type="radio" name="unit" value="kilo">kB
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && param.unit=='mega')}">
					<input type="radio" name="unit" value="mega" checked>MB
				</c:when>
				<c:otherwise>
					<input type="radio" name="unit" value="mega">MB
				</c:otherwise>
			</c:choose>
			</p>
			<p>
			<c:choose>
				<c:when test="${(param.submitted && resolveIP)}">
					<input type="checkbox" name="checks" value="resolveIP" checked>resolve IP addresses?
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="resolveIP">resolve IP addresses?
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && ignoreHighPorts)}">
					<input type="checkbox" name="checks" value="ignoreHighPorts" checked>ignore Ports &gt; 1023 in Src and Dst Port charts?
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="ignoreHighPorts">ignore Ports &gt; 1023 in Src and Dst Port charts?
				</c:otherwise>
			</c:choose>
                        <p>Method for Distributed Query:<br>
                	<c:choose>
				<c:when test="${(param.submitted && param.dqSelect=='useJavaDQ') || !param.submitted}">
					<input type="radio" name="dqSelect" value="useJavaDQ" checked>Java only
				</c:when>
				<c:otherwise>
					<input type="radio" name="dqSelect" value="useJavaDQ">Java only
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && param.dqSelect=='useMysqldumpDQ')}">
					<input type="radio" name="dqSelect" value="useMysqldumpDQ" checked>Mysqldump
				</c:when>
				<c:otherwise>
					<input type="radio" name="dqSelect" value="useMysqldumpDQ">Mysqldump
				</c:otherwise>
			</c:choose>
		     	</p>
		     	<hr>
		     	<input type="hidden" name="submitted" value="true">

		     	<input type="hidden" name="offset" value="0">
		     	<input type="hidden" name="outputLength" value="50">
				
			<table><tr><td>	
		     	<input type="submit" name="search" value="Execute Query">
		     	
		     </form></td>
		     <td>
		     <form method="POST" action="/nasty/Logout">
		     	<input type="submit" value="Logout">
		     </form></td>
		     </tr></table>

			<!-- end form -->
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
	<tr>
    <!-- results column -->
    <td valign="top" width="75%">
     <table border="0" cellpadding="0" cellspacing="0" width="100%">
      <tr>
       <td valign="top" width="100%">



	<c:if test="${(param.submitted && (!param.switch))}" >
        <!-- story -->
        
        <table align="center" bgcolor="#000000" border="0" cellPadding="0" cellSpacing="0" width="100%">
         <tr>
          <td>
           <table border="0" cellpadding="3" cellspacing="1" width="100%" align=center>
            <tr>
             <td bgcolor="#cccccc">
              &nbsp;<big>Results:</big>
             </td>
            </tr>
            <tr>
             <td bgcolor="#ffffff">
<!-- results -->

                <jsp:useBean id="result" class="de.japes.beans.nasty.ServletResults" scope="request" />
		<jsp:getProperty name="result" property="queryResult" />

<!-- end results -->
             </td>
            </tr>
           </table>
          </td>
         </tr>
        </table>
        <!-- end story -->
	</c:if>
        <br />
        

       </td>
      </tr>
     </table>
    </td>
    <!-- end results column -->
   </tr>

  </table>
  <!-- end body table -->
 </body>
</html>
