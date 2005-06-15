/*
 * Created on 15.07.2004
 */

package de.japes.net.nasty.collector;

/**
 * @author unrza88
 */

import java.io.IOException;

public interface FlowsetPacket {
	
	public void readContents() throws IOException, FlowFormatException;
	
	public void setContent(byte[] buf);
	
	public void setSrcAddress(long addr);
}

class FlowFormatException extends Exception {
	
	public FlowFormatException() {}
	public FlowFormatException(String msg) {
		super(msg);
	}
}

class UnknownTemplateException extends Exception {
	public UnknownTemplateException () {}
	public UnknownTemplateException (String msg) {
		super(msg);
	}
}

class UnknownExporterException extends Exception {
	public UnknownExporterException () {}
	public UnknownExporterException (String msg) {
		super(msg);
	}
}