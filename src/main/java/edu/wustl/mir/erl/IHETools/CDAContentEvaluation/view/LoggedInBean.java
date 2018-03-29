package edu.wustl.mir.erl.IHETools.CDAContentEvaluation.view;

import java.io.Serializable;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDADocStore;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDAUserExport;
import edu.wustl.mir.erl.IHETools.Util.DBUtil;
import edu.wustl.mir.erl.IHETools.Util.DataBaseConnection;
import edu.wustl.mir.erl.IHETools.Util.FacesUtil;
import edu.wustl.mir.erl.IHETools.Util.Query;
import edu.wustl.mir.erl.IHETools.Util.Util;
import edu.wustl.mir.erl.IHETools.Util.Valid;

public class LoggedInBean implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String EMPTY = "";
	private static final SelectItem[] EMPTY_SYSTEMS =
			new SelectItem[] {new SelectItem(EMPTY, "-- Select organization first --")};
	private static final String EMPTY_DOC = "Paste document to store here.";

	private ApplicationBean applicationBean = 
		(ApplicationBean) FacesUtil.getManagedBean("applicationBean");
	private static SessionBean sessionBean = 
		(SessionBean) FacesUtil.getManagedBean("sessionBean");
	private static Logger log = Util.getLog();

	private String organization;
	private String system;
	private SelectItem[] systems;
	private String patientID;
	private SelectItem[] patientIDs = null;
	private String notes;
	private boolean msgExpanded;
	private String documentType;
	private String formatCode;
	private String typeCode;
	private String classCode;
	private String testDoc;
	private boolean documentStored;
	
	private SelectItem[] filterFormatCodes = null;
	private SelectItem[] selectFormatCodes = null;
	private SelectItem[] filterClassCodes  = null;
	private SelectItem[] selectClassCodes  = null;
	private SelectItem[] filterTypeCodes   = null;
	private SelectItem[] selectTypeCodes   = null;
	
	private String provideAndRegisterEndpoint;

	public LoggedInBean(String doc) {
		reset();
		testDoc = doc;
		if (StringUtils.isEmpty(testDoc) || 
			testDoc.equalsIgnoreCase(SessionBean.EMPTY_DOC))
			testDoc = EMPTY_DOC;
	}
	public LoggedInBean() {
		this(EMPTY_DOC);
	}
	
	public void reset() {
		organization = EMPTY;
		systems = EMPTY_SYSTEMS;
		system = EMPTY;
		patientID    = EMPTY;
		documentType = EMPTY;
		formatCode   = EMPTY;
		typeCode     = EMPTY;
		classCode    = EMPTY;
		notes = EMPTY;
		msgExpanded = true;
		testDoc = EMPTY_DOC;
		documentStored = false;
		
		provideAndRegisterEndpoint = EMPTY;
		
		filterFormatCodes = applicationBean.getFilterFormatCodes();
		selectFormatCodes = applicationBean.getSelectFormatCodes();
		filterClassCodes  = applicationBean.getFilterClassCodes();
		selectClassCodes  = applicationBean.getSelectClassCodes();
		filterTypeCodes   = applicationBean.getFilterTypeCodes();
		selectTypeCodes   = applicationBean.getSelectTypeCodes();
	}

	// ----------------------------------------------------
	// Listener & action methods
	// ----------------------------------------------------

	public void organizationChange(ValueChangeEvent vce) {
		organization = (String) vce.getNewValue();
		if (organization == null) organization = EMPTY;
		systems = EMPTY_SYSTEMS;
		if (!organization.equalsIgnoreCase(EMPTY)) {
			systems = applicationBean.getSelectOrganizationSystems(organization);
		}
		if (systems.length == 2) system = (String) systems[1].getValue();
		else system = (String) systems[0].getValue();
		
		String selectedPatientID = applicationBean.getPatientIDFromSystemName(system);
		patientIDs = new SelectItem[1];
		patientIDs[0] = new SelectItem(selectedPatientID, selectedPatientID);
		patientID = (String)patientIDs[0].getValue();
		log.debug("LoggedInBean::organizationChange: org " + organization);
		log.debug("LoggedInBean::organizationChange: sys " + system);
		log.debug("LoggedInBean::organizationChange: pat " + patientID);
	}
	

	public void systemChange(ValueChangeEvent vce) {
		String selectedSystem = (String) vce.getNewValue();
		if (selectedSystem == null) {
			//system = EMPTY;
			log.debug("LoggedInBean::systemChange event with NULL system");
			return;
		}
		
		system = selectedSystem;
		String selectedPatientID = applicationBean.getPatientIDFromSystemName(system);
		log.debug("LoggedInBean::systemChange: sys " + system);
		log.debug("LoggedInBean::systemChange: pat " + selectedPatientID);
		
		patientIDs = new SelectItem[1];
		patientIDs[0] = new SelectItem(selectedPatientID, selectedPatientID);
		patientID = (String)patientIDs[0].getValue();
		log.debug("LoggedInBean::systemChange: pat " + selectedPatientID);

	}
	
	public void documentChange(ValueChangeEvent vce) {
		String selectedDocumentType = (String)vce.getNewValue();
		if (selectedDocumentType == null) {
			log.debug("LoggedInBean::documentChange event with NULL document");
			return;
		}
		log.debug("LoggedInBean::documentChange: " + selectedDocumentType);
		String formatCode    = applicationBean.getDocumentTypeToFormatCode(selectedDocumentType);
		if (formatCode.startsWith("*")) {
			selectFormatCodes = applicationBean.getSelectFormatCodes();
		} else {
			selectFormatCodes    = new SelectItem[1];
			selectFormatCodes[0] = new SelectItem(formatCode, formatCode);
			log.debug("LoggedInBean::documentChange: mapped formatCode: " + formatCode);
		}
	

		String classCode    = applicationBean.getDocumentTypeToClassCode(selectedDocumentType);
		if (classCode.startsWith("*")) {
			selectClassCodes = applicationBean.getSelectClassCodes();
		} else {
			selectClassCodes    = new SelectItem[1];
			selectClassCodes[0] = new SelectItem(classCode, classCode);
			log.debug("LoggedInBean::documentChange: mapped classCode: " + classCode);
		}
		
		String typeCode     = applicationBean.getDocumentTypeToTypeCode(selectedDocumentType);
		if (typeCode.startsWith("*")) {
			selectTypeCodes = applicationBean.getSelectTypeCodes();
		} else {
			selectTypeCodes     = new SelectItem[1];
			selectTypeCodes[0]  = new SelectItem(typeCode, typeCode);
			log.debug("LoggedInBean::documentChange: mapped typeCode: " + typeCode);
		}
	}
	
	public void dummy(ValueChangeEvent vce) {
		
	}
	
	public void storeDocument(ActionEvent ae) {
		Valid v = new Valid();
		v.startValidations();
		if (organization.equalsIgnoreCase(EMPTY)) 
			v.error("Organization Code not selected");
		if (system.equalsIgnoreCase(EMPTY))
			v.error("System Code not selected");
		if (patientID.equalsIgnoreCase(EMPTY))
			v.error("Patient ID not selected");
		if (documentType.equalsIgnoreCase(EMPTY))
			v.error("Document type not selected");
		if (testDoc.equalsIgnoreCase(EMPTY) ||
			testDoc.equalsIgnoreCase(EMPTY_DOC))
			v.error("Document not entered");
		if (formatCode.equalsIgnoreCase(EMPTY))
			v.error("formatCode is not entered");
		if (typeCode.equalsIgnoreCase(EMPTY))
			v.error("typeCode is not entered");
		if (classCode.equalsIgnoreCase(EMPTY))
			v.error("classCode is not entered");
		if (v.isErrors()) return;
		String[] typeCodeArray   = typeCode.split(", ");
		String[] classCodeArray  = classCode.split(", ");
		String[] formatCodeArray = formatCode.split(", ");
		String[] patientIDArray= patientID.split(", ");
		CDADocStore doc = new CDADocStore();
		doc.setOrganization(organization);
		doc.setSystem(system);
		doc.setPatientID(patientIDArray[0]);
		doc.setLastName(patientIDArray[1]);
		doc.setDocumentType(documentType);
		doc.setFormatCode(formatCodeArray[0]);
		doc.setTypeCode(typeCodeArray[0]);
		doc.setTypeCodeDesignator(typeCodeArray[1]);
		doc.setTypeCodeDisplay(typeCodeArray[2]);
		doc.setClassCode(classCodeArray[0]);
		doc.setClassCodeDesignator(classCodeArray[1]);
		doc.setClassCodeDisplay(classCodeArray[2]);
		doc.setNotes(notes);
		doc.setDocument(testDoc);
		
		try {
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			int documentId = doc.insert(dbc);
			
			String pnrURI = applicationBean.getProvideAndRegisterURI("Connectathon XDS.b Registry/Repository");
			if (pnrURI != null) {
				String casUserId = sessionBean.getCasUserId();
				if (casUserId == null) { // Should not happen
					casUserId = "none";
				}
				CDAUserExport export = new CDAUserExport();
				export.setUserId(casUserId);
				export.setDocumentId("" + documentId);
				export.setStatus("unsent");
				export.setExportType("XDS.b");
				export.setPnrEndpoint(pnrURI);
				export.setToAddress("");
				export.setSubject("");
				export.setBody("");
				export.setConfidentialityCode("N");
				export.setLanguageCode("en-us");
				export.setMimeType("mime type");
				export.insert(dbc);
			}
		} catch (Exception e) {
			String em = "Error adding document to archive: " + e.getMessage();
			log.warn(em);
			FacesUtil.addErrorMessage(em);
			return;
		}
		documentStored = true;
		log.debug("EO storeDocument() reached");
		return;
	}
	
	public void returnToStore() {
		reset();
	}
	public void returnToEvaluate() {
		reset();
		sessionBean.setSelectedTab(SessionBean.EVALUATE_CONTENT_TAB);
	}
	
	//------------- Style shows non-entered items with pink background
	private static final String EMPTY_STYLE = "background-color:pink";
	private static final String REG_STYLE = "background-color:white";
	private String getStyle(String value) {
		if (value.equalsIgnoreCase(EMPTY)) return EMPTY_STYLE;
		else return REG_STYLE;
	}
	public String getOrganizationStyle() { return getStyle(organization); }
	public String getSystemStyle() { return getStyle(system); }
	public String getPatientIDStyle() { return getStyle(patientID); }
	public String getDocumentTypeStyle() { return getStyle(documentType); }
	public String getDocumentStyle() {
		String a = testDoc;
		if (a.equalsIgnoreCase(EMPTY_DOC)) a = EMPTY;
		return "width:100%; height: 400px; " + getStyle(a);
	}
	public String getFormatCodeStyle() { return getStyle(formatCode); }
	public String getTypeCodeStyle()   { return getStyle(typeCode); }
	public String getClassCodeStyle()  { return getStyle(classCode); }
	public String getProvideAndRegisterEndpointStyle() { 
		System.out.println("LoggedInBean::getProvideAndRegisterEndpointStyle");
		return getStyle(provideAndRegisterEndpoint); }
	

	// ----------------------------------------------------
	// Getters & Setters
	// ----------------------------------------------------

	public boolean isOneSystem() {
		return systems.length == 2;
	}

	public String getTestDoc() {
		return testDoc;
	}
	public void setTestDoc(String testDoc) {
		this.testDoc = testDoc;
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
	
	public SelectItem[] getPatientIDs() {
		if (patientIDs != null) {
			return patientIDs;
		}
		return applicationBean.getSelectPatientIDs();
	}

	public SelectItem[] getSystems() {
		return systems;
	}

	public void setSystems(SelectItem[] systems) {
		this.systems = systems;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public boolean isMsgExpanded() {
		return msgExpanded;
	}

	public void setMsgExpanded(boolean msgExpanded) {
		this.msgExpanded = msgExpanded;
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

	public String getClassCode() {
		return classCode;
	}
	public void setClassCode(String classCode) {
		this.classCode = classCode;
	}

	public boolean isDocumentStored() {
		return documentStored;
	}

	public String getProvideAndRegisterEndpoint() {
		System.out.println("provideAndRegisterEndpoint " + provideAndRegisterEndpoint);
		return provideAndRegisterEndpoint;
	}
	public void setProvideAndRegisterEndpoint(String provideAndRegisterEndpoint) {
		this.provideAndRegisterEndpoint = provideAndRegisterEndpoint;
	}

	public SelectItem[] getFilterFormatCodes() {
		return filterFormatCodes;
	}
	public SelectItem[] getSelectFormatCodes() {
		log.debug("Returning selectFormatCodes");
		return selectFormatCodes;
	}

	public SelectItem[] getFilterTypeCodes() {
		return filterTypeCodes;
	}
	public SelectItem[] getSelectTypeCodes() {
		log.debug("Returning selectTypeCodes");
		return selectTypeCodes;
	}

	public SelectItem[] getFilterClassCodes() {
		return filterClassCodes;
	}
	public SelectItem[] getSelectClassCodes() {
		log.debug("Returning selectClassCodes");
		return selectClassCodes;
	}
	
} // EO LoggedInBean class
