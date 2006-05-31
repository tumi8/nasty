/*
 * Created on 31.08.2004
 */

package de.japes.beans.nasty;

/**
 * @author unrza88
 */

import java.io.Serializable;

public class ServletResults implements Serializable {

	private String queryResult;
	
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
	
}
