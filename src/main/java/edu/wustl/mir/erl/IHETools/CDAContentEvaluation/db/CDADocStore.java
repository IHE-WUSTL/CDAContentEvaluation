package edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


import edu.wustl.mir.erl.IHETools.Util.DBUtil;
import edu.wustl.mir.erl.IHETools.Util.DataBaseConnection;
import edu.wustl.mir.erl.IHETools.Util.Query;

/**
 * Implementation for cda_document_store table. Includes:<ul>
 * <li/>Object representation of table row.
 * <li/>Constructor and methods to build objects from JDBC
 * {@link java.sql.ResultSet ResultSet}
 * <li/>Insert method.
 * <li/>Comparator for sorting arrays of Company objects
 * <li/>toString method useful for inserting object in log.</ul>
 * @author rmoult01
 */
public class CDADocStore implements Serializable {
	private static final long serialVersionUID = 1L;

	private int id = 0;
	private String organization = "";
	private String system = "";
	private String patientID = "";
	private String lastName  = "";
	private String documentType = "";
	private String formatCode = "";
	private String typeCode = "";
	private String typeCodeDesignator = "";
	private String typeCodeDisplay = "";
	private String classCode = "";
	private String classCodeDesignator = "";
	private String classCodeDisplay = "";
	private String notes = "";
	private String document = "";

	private boolean selected = false;

	public CDADocStore() {}

	/**
	 * Constructor builds Company object from the current row of the passed
	 * ResultSet.
	 * @param result ResultSet. Must be positioned at row for which Company
	 * object is desired.
	 * @throws SQLException on error.
	 */
	public CDADocStore(ResultSet result) throws SQLException {
		id                 = result.getInt("id");
		organization       = result.getString("organization");
		system             = result.getString("system");
		patientID          = result.getString("patient_id");
		lastName           = result.getString("last_name");
		documentType       = result.getString("document_type");
		formatCode         = result.getString("format_code");
		typeCode           = result.getString("type_code");
		typeCodeDesignator = result.getString("type_code_designator");
		typeCodeDisplay    = result.getString("type_code_display");
		classCode          = result.getString("class_code");
		classCodeDesignator= result.getString("class_code_designator");
		classCodeDisplay   = result.getString("class_code_display");
		notes              = result.getString("notes");
		document           = result.getString("document");
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getSystem() {
		return system;
	}
	public void setSystem(String system) {
		this.system = system;
	}

	public String getPatientID() {
		return patientID;
	}
	public void setPatientID(String patientID) {
		this.patientID = patientID;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	
	public String getFormatCode() {
		return formatCode;
	}
	public void setFormatCode(String formatCode) {
		this.formatCode = formatCode;
	}
	
	public String getTypeCode() {
		return typeCode;
	}
	public void setTypeCode(String typeCode) {
		this.typeCode = typeCode;
	}
	
	public String getTypeCodeDesignator() {
		return typeCodeDesignator;
	}
	public void setTypeCodeDesignator(String typeCodeDesignator) {
		this.typeCodeDesignator = typeCodeDesignator;
	}
	
	public String getTypeCodeDisplay() {
		return typeCodeDisplay;
	}
	public void setTypeCodeDisplay(String typeCodeDisplay) {
		this.typeCodeDisplay = typeCodeDisplay;
	}

	
	public String getClassCode() {
		return classCode;
	}
	public void setClassCode(String classCode) {
		this.classCode = classCode;
	}
	
	public String getClassCodeDesignator() {
		return classCodeDesignator;
	}
	public void setClassCodeDesignator(String classCodeDesignator) {
		this.classCodeDesignator = classCodeDesignator;
	}
	
	public String getClassCodeDisplay() {
		return classCodeDisplay;
	}
	public void setClassCodeDisplay(String classCodeDisplay) {
		this.classCodeDisplay = classCodeDisplay;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String toString() {
		return "[CDADocStore: id=" + id +
				" organization=" + organization +
				" system=" + system +
				" document type=" + documentType +
				" notes=" + notes +
				" document=" + document +
				"]";
	}

	/**
	 * Loads the rows in a JDBC ResultSet into CDADocStore objects and 
	 * returns them as an array.
	 * @param result ResultSet, assumed to contain zero or more rows from the
	 * cda_document_store table, using their default column names. The ResultSet 
	 * is assumed to be positioned beforeFirst.
	 * @return Array of CDADocStore objects
	 * @throws SQLException on error
	 */
	public static CDADocStore[] load(ResultSet result) throws SQLException {
		List<CDADocStore> CDADocStores = new ArrayList<CDADocStore>();
		while (result.next()) {
			CDADocStores.add(new CDADocStore(result));
		}
		return CDADocStores.toArray(new CDADocStore[0]);
	}

	   /**
	    * Inserts this {@link edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDADocStore CDADocStore} object into the
	    * cda_doc_store table, using the passed 
	    * {@link edu.wustl.mir.erl.IHETools.Util.DataBaseConnection DataBaseConnection} 
	    * @param dbc DataBaseConnection. if null or not for admin db one is created.
	    * @return int id assigned by the database to the new row.
	    * @throws Exception on error.
	    */
	   public int insert(DataBaseConnection dbc) throws Exception {
	      if (dbc == null || !dbc.getDbName().equalsIgnoreCase(DBUtil.DBNAME))
	         dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
	      new Query(DBUtil.CDA_DOC_STORE_INSERT)
	           .set("organization",        organization)
	           .set("system",              system)
	           .set("patientID",           patientID)
	           .set("lastName",            lastName)
	           .set("documentType",        documentType)
	           .set("formatCode",          formatCode)
	           .set("typeCode",            typeCode)
	           .set("typeCodeDesignator",  typeCodeDesignator)
	           .set("typeCodeDisplay",     typeCodeDisplay)
	           .set("classCode",           classCode)
	           .set("classCodeDesignator", classCodeDesignator)
	           .set("classCodeDisplay",    classCodeDisplay)
	           .set("notes",               notes)
	           .set("document",            document)
	           .dbUpdate(dbc);
	      id = DBUtil.getCdaMsgStoreLid(dbc);
	      return id;
	   }
	   
	   
	   
	   
	   /**
	    * Comparator class
	    */
	   public static class Comp implements Comparator<CDADocStore> {

	      private String col;
	      private boolean asc;
	      public Comp(String columnName, boolean ascending) {
	         col = columnName;
	         asc = ascending;
	      }

	      @Override
	      public int compare(CDADocStore one, CDADocStore two) {
	    	  if (col == null) return 0;
	    	  if (col.equalsIgnoreCase("organization")) {
	    		  return asc ?
	    				  one.getOrganization().compareToIgnoreCase(two.getOrganization()) :
	    					  two.getOrganization().compareToIgnoreCase(one.getOrganization()) ;
	    	  } else if (col.equalsIgnoreCase("system")) {
	    		  return asc ?
	    				  one.getSystem().compareToIgnoreCase(two.getSystem()) :
	    					  two.getSystem().compareToIgnoreCase(one.getSystem()) ;
	    	  } else if (col.equalsIgnoreCase("documentType")) {
	    		  return asc ?
	    				  one.getDocumentType().compareToIgnoreCase(two.getDocumentType()) :
	    					  two.getDocumentType().compareToIgnoreCase(one.getDocumentType()) ;
	    	  } else return 0;
	      }

	   } // EO CDADocStore Comparator inner class

} // EO CDADocStore class
