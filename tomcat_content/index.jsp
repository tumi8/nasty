<%@ page contentType="text/html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

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
		<c:when test="${current == 'restrictTime'}">
			<c:set var="restrictTime" value="true" />
		</c:when>
		<c:when test="${current == 'ignoreHighPorts'}">
			<c:set var="ignoreHighPorts" value="true" />
		</c:when>
	</c:choose>
</c:forEach>
	
<html>
	<link rel="stylesheet" type="text/css" href="nasty.css">

	<head>
	    <title>nasty - Network Analysis And Statistics Yielding</title>
        </head>
	      
	<body>

		<h1 align=center>nasty</h1>

			<form method="POST" action="/nasty/GetResults">

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
		     		    <td><input type="text" name="tos" value="<c:out value='${param.prot}' />"></td></tr></table>
				    <table>
				    <tr><td>
				    <c:choose>
				    	<c:when test="${(param.submitted && restrictTime)}">
						<input type="checkbox" name="checks" value="restrictTime" checked> 
					</c:when>
					<c:otherwise>
						<input type="checkbox" name="checks" value="restrictTime">
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
						<c:forEach begin="2004" end="2006" var="current">
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
						<c:forEach begin="2004" end="2006" var="current">
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
					</td></tr>
		     	</table>

			<p>Output Columns:<br>
			<c:choose>
				<c:when test="${(param.submitted && !showSrcIP)}">
					<input type="checkbox" name="checks" value="showSrcIP">SrcIP
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showSrcIP" checked>SrcIP
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showDstIP)}">
					<input type="checkbox" name="checks" value="showDstIP">DstIP
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showDstIP" checked>DstIP
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showSrcPort)}">
					<input type="checkbox" name="checks" value="showSrcPort">SrcPort
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showSrcPort" checked>SrcPort
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showDstPort)}">
					<input type="checkbox" name="checks" value="showDstPort">DstPort
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showDstPort" checked>DstPort
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showProto)}">
					<input type="checkbox" name="checks" value="showProto">Protocol
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showProto" checked>Protocol
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showTos)}">
					<input type="checkbox" name="checks" value="showTos">ToS
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showTos" checked>ToS
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showPackets)}">
					<input type="checkbox" name="checks" value="showPackets">Packets
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showPackets" checked>Packets
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showBytes)}">
					<input type="checkbox" name="checks" value="showBytes">Bytes
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showBytes" checked>Bytes
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showFirstSwitched)}">
					<input type="checkbox" name="checks" value="showFirstSwitched">FirstSwitched
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showFirstSwitched" checked>FirstSwitched
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showLastSwitched)}">
					<input type="checkbox" name="checks" value="showLastSwitched">LastSwitched
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showLastSwitched" checked>LastSwitched
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showDuration)}">
					<input type="checkbox" name="checks" value="showDuration">Duration
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showDuration" checked>Duration
				</c:otherwise>
			</c:choose>
			<c:choose>
				<c:when test="${(param.submitted && !showExporter)}">
					<input type="checkbox" name="checks" value="showExporter">ExporterID
				</c:when>
				<c:otherwise>
					<input type="checkbox" name="checks" value="showExporter" checked>ExporterID
				</c:otherwise>
			</c:choose>
			

		     	<p>Order by:<br>
		     	<c:choose>
		     		<c:when test="${(param.submitted && param.order=='none') || !param.submitted}">
		     			<input type="radio" name="order" value="none" checked>none
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="none">none
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='srcIP'}">	
		     			<input type="radio" name="order" value="srcIP" checked>Source IP
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="srcIP">Source IP
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='dstIP'}">
		     			<input type="radio" name="order" value="dstIP" checked>Destination IP
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="dstIP">Destination IP
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='srcPort'}">
		     	       		<input type="radio" name="order" value="srcPort" checked>Source Port
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="srcPort">Source Port
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='dstPort'}">
		     			<input type="radio" name="order" value="dstPort" checked>Destination Port
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="dstPort">Destination Port
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='proto'}">
		     			<input type="radio" name="order" value="proto" checked>Protocol
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="proto">Protocol
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='pkts'}">
		     	       		<input type="radio" name="order" value="pkts" checked>Packets
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="pkts">Packets
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='bytes'}">
		     	       		<input type="radio" name="order" value="bytes" checked>Bytes
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="bytes">Bytes
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='firstSwitched'}">
		     	       		<input type="radio" name="order" value="firstSwitched" checked>First Switched
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="firstSwitched">FirstSwitched
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='lastSwitched'}">
		     	       		<input type="radio" name="order" value="lastSwitched" checked>Last Switched
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="lastSwitched">Last Switched
		     		</c:otherwise>
		     	</c:choose>
		     	<c:choose>
		     		<c:when test="${param.submitted && param.order=='duration'}">
		     	       		<input type="radio" name="order" value="duration" checked>Duration<br>
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="order" value="duration">Duration<br>
		     		</c:otherwise>
		     	</c:choose>
		     	</p>
		     	<p>Sort Order:<br>
		     	<c:choose>
		     		<c:when test="${(param.submitted && param.sort=='increase') || !param.submitted}"> 
		     			<input type="radio" name="sort" value="increase" checked>increasing
		     			<input type="radio" name="sort" value="decrease">decreasing
		     		</c:when>
		     		<c:otherwise>
		     			<input type="radio" name="sort" value="increase">increasing
		     			<input type="radio" name="sort" value="decrease" checked>decreasing
		     		</c:otherwise>
		     	</c:choose>
		     	</p>
		     	<p>Output Format:<br>
		     	<c:choose>
		     		<c:when test="${(param.submitted && param.outputFormat=='html') || !param.submitted}">
		     			<input type="radio" name="outputFormat" value="html" checked>HTML					
		     		</c:when>
				<c:otherwise>
					<input type="radio" name="outputFormat" value="html">HTML
				</c:otherwise>
			</c:choose>
		     	<c:choose>
		     		<c:when test="${(param.submitted && param.outputFormat=='transit') || !param.submitted}">
		     			<input type="radio" name="outputFormat" value="html" checked>HTML (only transit traffic)					
		     		</c:when>
				<c:otherwise>
					<input type="radio" name="outputFormat" value="html">HTML (only transit traffic)
				</c:otherwise>
			</c:choose>
			<c:choose>
		     		<c:when test="${(param.submitted && param.outputFormat=='perl')}">
		     			<input type="radio" name="outputFormat" value="perl" checked>Perl Array
				</c:when>
				<c:otherwise>
		     			<input type="radio" name="outputFormat" value="perl">Perl Array
				</c:otherwise>
			</c:choose>
			<c:choose>
		     		<c:when test="${(param.submitted && param.outputFormat=='chart')}">
		     			<input type="radio" name="outputFormat" value="chart" checked>Chart
				</c:when>
				<c:otherwise>
		     			<input type="radio" name="outputFormat" value="chart">Chart
				</c:otherwise>
			</c:choose>
			<c:set var="charts" value="IP Protocol Distribution,Application Protocol Distribution (Source Ports),Application Protocol Distribution (Destination Ports),Traffic Over Time,Traffic Over Time (Source Ports),Traffic Over Time (Destination Ports)" />
			<select name="chartSelect" size="1">
				<c:forEach items="${charts}" var="current">
					<c:choose>
						<c:when test="${param.submitted && param.chartSelect==current}">
							<option selected><c:out value="${current}" /></option>
						</c:when>
						<c:otherwise>
							<option><c:out value="${current}" /></option>
						</c:otherwise>
					</c:choose>
				</c:forEach>
			</select>
			</p>
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
		     	</p>
		     	<input type="hidden" name="submitted" value="true">

		     	<input type="hidden" name="offset" value="0">
		     	<input type="hidden" name="outputLength" value="50">
			<table><tr><td>	
		     	<input type="submit" value="Search">
		     	
		     </form></td>
		     <td>
		     <form method="POST" action="/nasty/Logout">
		     	<input type="submit" value="Logout">
		     </form></td>
		     </tr></table>
		<hr>

		<c:if test="${param.submitted}" >
			<h3>Results:</h3>

			<jsp:useBean id="result" class="de.japes.beans.nasty.ServletResults" scope="request" />
			<jsp:getProperty name="result" property="queryResult" />
		</c:if>
	</body>
</html>
