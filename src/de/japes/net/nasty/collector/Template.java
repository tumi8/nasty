package de.japes.net.nasty.collector;
/*
 * Created on 20.07.2004
 */

/**
 * @author unrza88
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Date;


public class Template {

	public static HashMap knownExporters = new HashMap();
	private int templateID;
	private int fieldCount;
	private Date lastReceived;
	private int dataFlowsetLength = 0;
	private LinkedList templateFields = new LinkedList();
	
	public Template(DataInputStream in, long sourceID, long sourceAddress, Statement s) throws IOException {
		
		Identifier 	tmpId;
		LinkedList 	tmpExporterList;
		Exporter	tmpExporter;
		boolean		exporterFound=false;
		
		try {
			templateID = in.readUnsignedShort();
			fieldCount = in.readUnsignedShort();
			
			for (int i=fieldCount; i>0; i--) {
				TemplateDescr actDescr = new TemplateDescr();
				actDescr.fieldType = in.readUnsignedShort();
				actDescr.fieldLength = in.readUnsignedShort();
				dataFlowsetLength += actDescr.fieldLength;
				
				templateFields.add(actDescr);
				
			}
		} catch (IOException e) {
			System.err.println("Error reading stream from within template constructor.");
			throw e;
		}
		
		if (knownExporters.containsKey(tmpId=new Identifier(sourceID))) {
			
			//if (((tmpExporter=(Exporter)knownTemplates.get(tmpId))).exporterTemplates.containsKey(new Identifier(templateID))) {
				//maybe I should check here whether the already known template equals the received one
			//	System.out.println("Exporter schon drin.");
			//}
			//else {
				tmpExporterList=(LinkedList)knownExporters.get(tmpId);
				
				int i=0;
				
				while (i < tmpExporterList.size()) {
					
					if ((tmpExporter=(Exporter)tmpExporterList.get(i)).exporterAddress == sourceAddress) {
						
						this.lastReceived = new Date();
						tmpExporter.exporterTemplates.put(new Identifier(templateID), this);
						exporterFound = true;
						break;
					}
					i++;
				}
					
				if (!exporterFound) {

					this.lastReceived = new Date();
					tmpExporterList.add(tmpExporter=new Exporter(sourceID, sourceAddress));
					tmpExporter.exportToDB(s);
					tmpExporter.exporterTemplates.put(new Identifier(templateID), this);
					
				}
			//}
		} else {
			
			tmpExporterList = new LinkedList();
			tmpExporterList.add(tmpExporter=new Exporter(sourceID, sourceAddress));
			tmpExporter.exportToDB(s);
			((Exporter)tmpExporterList.getLast()).exporterTemplates.put(new Identifier(templateID), this);
			
			knownExporters.put(new Identifier(sourceID), tmpExporterList);
		}
	}
	
	public int getFieldCount() {
		return fieldCount;
	}
	
	public int getTemplateID() {
		return templateID;
	}
	
	public LinkedList getTemplateFields() {
		return templateFields;
	}
	
	public int getDataFlowsetLength() {
		return dataFlowsetLength;
	}
}
	
class Exporter {
	long exporterID;
	long exporterAddress;
	int dbID = 0;
	
	HashMap exporterTemplates = new HashMap();
	
	public Exporter(long sourceID, long sourceAddress) {
		exporterID = sourceID;
		exporterAddress = sourceAddress;
	}
	
	public void exportToDB(Statement s) {
		
		ResultSet result;
		
		try {
			s.executeUpdate("INSERT INTO exporter (sourceID, srcIP) " + 
					"VALUES(" + exporterID + ", " + exporterAddress + ")", Statement.RETURN_GENERATED_KEYS);
			
			result = s.getGeneratedKeys();
			
			result.next();
			dbID = result.getInt(1);
			
		} catch (SQLException e) {
			
			if (e.getErrorCode() == 1062) {	//exporter already in database
				
				try {
					result = s.executeQuery("SELECT id from exporter WHERE sourceID="+ exporterID + 
											" AND srcIP=" + exporterAddress);
	
					result.next();
					dbID = result.getInt(1);
					
				} catch (SQLException e2) {
					System.err.println("Error getting exporters database ID");
					System.err.println(e2.getMessage());
				}
				
			} else {
				System.err.println("Error adding exporter to database");
				System.err.println(e.getMessage());
			}
		}
	}
}

