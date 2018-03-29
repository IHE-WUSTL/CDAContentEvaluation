package edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


import edu.wustl.mir.erl.IHETools.Util.DBUtil;
import edu.wustl.mir.erl.IHETools.Util.DataBaseConnection;
import edu.wustl.mir.erl.IHETools.Util.Query;


public class CDAUserExport implements Serializable{
	private static final long serialVersionUID = 1L;
	
//	private int numDocs;
	private int id = 0;
	private String userId = "";
	private String documentId = "";
//	private String documentType = "";
//	private Object [] docPath;
//	private Object [] SDID;
//	private Object [] sampleDocs;
	private String status = "";
	private String exportType = "";
	private String pnrEndpoint = "";
	private String toAddress;
	private String subject;
	private String body;
//	private String patientId;
//	private String sourcePatientId;
//	private String classCode;
	private String confidentialityCode;
//	private String formatCode;
//	private String healthCareFacilityTypeCode;
//	private String homeCommunityId;
	private String languageCode;
	private String mimeType;
//	private String practiceSettingCode;
//	private String typeCode;

	public CDAUserExport(){}

	
	public int insert(DataBaseConnection dbc) throws Exception {
		new Query(DBUtil.CDA_EXPORT_INFO_INSERT)
//		.set("numDocs", numDocs)
		.set("documentId", documentId)
		.set("userId", userId)
//		.set("documentType", documentType)
		.set("status", status)
		.set("exportType",  exportType)
		.set("pnrEndpoint", pnrEndpoint)
		.set("toAddress", toAddress)
		.set("subject", subject)
		.set("body", body)
//		.set("patientId", patientId)
//		.set("sourcePatientId", sourcePatientId)
//		.set("classCode", classCode)
		.set("confidentialityCode", confidentialityCode)
//		.set("formatCode", formatCode)
//		.set("healthCareFacilityTypeCode", healthCareFacilityTypeCode)
//		.set("homeCommunityId", homeCommunityId)
		.set("languageCode", languageCode)
		.set("mimeType", mimeType)
//		.set("practiceSettingCode", practiceSettingCode)
//		.set("typeCode", typeCode)
		.dbUpdate(dbc);
		id = DBUtil.getCdaExportInfoLid(dbc);

/*
		for(int x=0; x<numDocs; x++){
			if (documentType.equals("gold")){
				new Query(DBUtil.CDA_EXPORT_FILES_INSERT)
				.set("infoId", id)
				.set("docPath", docPath[x])
				.set("SDID", null)
				.dbUpdate(dbc);
			}
			if (documentType.equals("sample")){
				new Query(DBUtil.CDA_EXPORT_FILES_INSERT)
				.set("infoId", id)
				.set("SDID", SDID[x])
				.set("docPath", null)
				.dbUpdate(dbc);
			}
		}
*/
		return id;
	}










	/*************************************************************
	 * ****************** GETTERS AND SETTERS ********************
	 *************************************************************/

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getToAddress() {
		return toAddress;
	}
	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}

/*
	public String getPatientId() {
		return patientId;
	}
	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}
	public String getSourcePatientId() {
		return sourcePatientId;
	}
	public void setSourcePatientId(String sourcePatientId) {
		this.sourcePatientId = sourcePatientId;
	}
	public String getClassCode() {
		return classCode;
	}
	public void setClassCode(String classCode) {
		this.classCode = classCode;
	}
*/
	public String getConfidentialityCode() {
		return confidentialityCode;
	}
	public void setConfidentialityCode(String confidentialityCode) {
		this.confidentialityCode = confidentialityCode;
	}

/*
	public String getFormatCode() {
		return formatCode;
	}
	public void setFormatCode(String formatCode) {
		this.formatCode = formatCode;
	}
	public String getHealthCareFacilityTypeCode() {
		return healthCareFacilityTypeCode;
	}
	public void setHealthCareFacilityTypeCode(String healthCareFacilityTypeCode) {
		this.healthCareFacilityTypeCode = healthCareFacilityTypeCode;
	}
	public String getHomeCommunityId() {
		return homeCommunityId;
	}
	public void setHomeCommunityId(String homeCommunityId) {
		this.homeCommunityId = homeCommunityId;
	}
*/
	public String getLanguageCode() {
		return languageCode;
	}
	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
/*
	public String getPracticeSettingCode() {
		return practiceSettingCode;
	}
	public void setPracticeSettingCode(String practiceSettingCode) {
		this.practiceSettingCode = practiceSettingCode;
	}
	public String getTypeCode() {
		return typeCode;
	}
	public void setTypeCode(String typeCode) {
		this.typeCode = typeCode;
	}
*/
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getExportType() {
		return exportType;
	}
	public void setExportType(String exportType) {
		this.exportType = exportType;
	}
	public String getSubject() {
		return subject;
	}

	public void setPnrEndpoint(String pnrEndpoint) {
		this.pnrEndpoint = pnrEndpoint;
	}
	public String getPnrEndpoint() {
		return pnrEndpoint;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
/*
	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public Object[] getDocPath() {
		return docPath;
	}
	public void setDocPath(Object[] docPath) {
		this.docPath = docPath;
	}

	public Object[] getSDID() {
		return SDID;
	}

	public void setSDID(Object[] sDID) {
		SDID = sDID;
	}

	public int getNumDocs() {
		return numDocs;
	}

	public void setNumDocs(int numDocs) {
		this.numDocs = numDocs;
	}

	public Object[] getSampleDocs() {
		return sampleDocs;
	}

	public void setSampleDocs(Object[] sampleDocs) {
		this.sampleDocs = sampleDocs;
	}
*/
	@Override
	public String toString() {
		String out= "CDAUserExport [id=" + id + ", userId="
				+ userId
				+ ", status=" + status + ", exportType=" + exportType
				+ ", pnrEndpoint=" + pnrEndpoint
				+ ", toAddress=" + toAddress + ", subject=" + subject 
				+ ", body=" + body
				+ ", confidentialityCode=" + confidentialityCode
				+ ", languageCode="
				+ languageCode + ", mimeType=" + mimeType
				+ "]";
		return out; 
	}














}
