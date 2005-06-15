/*
 * Created on 31.08.2004
 */

package de.japes.beans.nasty;

/**
 * @author unrza88
 */

import java.io.Serializable;
import org.jCharts.Chart;

public class ServletResults implements Serializable {

	private String queryResult;
	private String srcIP;
	private String dstIP;
	private String srcPort;
	private String dstPort;
	private String proto;
	private String tos;
	private String order;
	private String sort;
	private int numRows;
	private int currentOffset;
	private String nextButton;
	private Chart chart;
	
	/**
	 * @return Returns the queryResult.
	 */
	public String getQueryResult() {
		return queryResult;
	}
	/**
	 * @param queryResult The queryResult to set.
	 */
	public void setQueryResult(String queryResult) {
		this.queryResult = queryResult;
	}
	/**
	 * @return Returns the dstIP.
	 */
	public String getDstIP() {
		return dstIP;
	}
	/**
	 * @param dstIP The dstIP to set.
	 */
	public void setDstIP(String dstIP) {
		this.dstIP = dstIP;
	}
	/**
	 * @return Returns the dstPort.
	 */
	public String getDstPort() {
		return dstPort;
	}
	/**
	 * @param dstPort The dstPort to set.
	 */
	public void setDstPort(String dstPort) {
		this.dstPort = dstPort;
	}
	/**
	 * @return Returns the order.
	 */
	public String getOrder() {
		return order;
	}
	/**
	 * @param order The order to set.
	 */
	public void setOrder(String order) {
		this.order = order;
	}
	/**
	 * @return Returns the proto.
	 */
	public String getProto() {
		return proto;
	}
	/**
	 * @param proto The proto to set.
	 */
	public void setProto(String proto) {
		this.proto = proto;
	}
	/**
	 * @return Returns the sort.
	 */
	public String getSort() {
		return sort;
	}
	/**
	 * @param sort The sort to set.
	 */
	public void setSort(String sort) {
		this.sort = sort;
	}
	/**
	 * @return Returns the srcIP.
	 */
	public String getSrcIP() {
		return srcIP;
	}
	/**
	 * @param srcIP The srcIP to set.
	 */
	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}
	/**
	 * @return Returns the srcPort.
	 */
	public String getSrcPort() {
		return srcPort;
	}
	/**
	 * @param srcPort The srcPort to set.
	 */
	public void setSrcPort(String srcPort) {
		this.srcPort = srcPort;
	}
	/**
	 * @return Returns the tos.
	 */
	public String getTos() {
		return tos;
	}
	/**
	 * @param tos The tos to set.
	 */
	public void setTos(String tos) {
		this.tos = tos;
	}
	/**
	 * @return Returns the currentOffset.
	 */
	public int getCurrentOffset() {
		return currentOffset;
	}
	/**
	 * @param currentOffset The currentOffset to set.
	 */
	public void setCurrentOffset(int currentOffset) {
		this.currentOffset = currentOffset;
	}
	/**
	 * @return Returns the numRows.
	 */
	public int getNumRows() {
		return numRows;
	}
	/**
	 * @param numRows The numRows to set.
	 */
	public void setNumRows(int numRows) {
		this.numRows = numRows;
	}
	/**
	 * @return Returns the nextButton.
	 */
	public String getNextButton() {
		return nextButton;
	}
	/**
	 * @param nextButton The nextButton to set.
	 */
	public void setNextButton(String nextButton) {
		this.nextButton = nextButton;
	}
	
}
