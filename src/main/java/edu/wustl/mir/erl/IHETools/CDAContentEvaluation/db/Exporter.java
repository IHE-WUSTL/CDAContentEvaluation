package edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.wustl.mir.erl.IHETools.Util.DBUtil;
import edu.wustl.mir.erl.IHETools.Util.DataBaseConnection;
import edu.wustl.mir.erl.IHETools.Util.ProvideAndRegister;
import edu.wustl.mir.erl.IHETools.Util.Query;
import edu.wustl.mir.erl.IHETools.Util.Util;
import edu.wustl.mir.erl.IHETools.Util.Valid;
import edu.wustl.mir.erl.IHETools.Util.Zip;


public class Exporter  implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Path runDirectory = null;
	
	private String zip;
	private String zipPfn;
	private Zip zipper;
	private String docString;
	private String docXML;

	private static final String WORK = "INI:ZipWorkingDirectory";
	private static final String GOLD_BOILER = 
			"INI:GoldBoilerplateDirectory";
	private static final String SAMPLE_BOILER = 
			"INI:SampleBoilerplateDirectory";
	private static final String GOLD_ZIP_FILE_NAME = "goldCDADoc.zip";
	private static final String SAMPLE_ZIP_FILE_NAME = "sampleCDADoc.zip";
	private final String DOC_ENTRY_PFN     = "templates/docEntry.xml";
	private final String SUBMISSION_SET_PFN= "templates/submissionSet.xml";

	private static final String tab = "\t";
	private static final String nl = System.getProperty("line.separator");
	
	private String docEntryTemplate = null;
	private String submissionSetTemplate = null;


	private DocumentBuilderFactory dbf;
	private DocumentBuilder db;
	private Document docDoc = null;
	private static Logger logger = null;

	public Exporter() {
		logger = Util.getLog();
	}
	
	public void executeOnce() {
		ArrayList<CDAUserExport> documentList = retrieveExportEntries("unsent");
		logger.debug("Number of unsent documents: " + documentList.size());
		for (CDAUserExport exportRecord:documentList) {
			logger.debug(exportRecord.toString());
			try {
				CDADocStore documentRecord = retrieveDocument(exportRecord.getDocumentId());
				initializeFileTemplates();
				ProvideAndRegister provideAndRegister = new ProvideAndRegister();
				provideAndRegister.export(exportRecord, documentRecord, docEntryTemplate, submissionSetTemplate);
				DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
				new Query(DBUtil.CDA_EXPORT_INFO_UPDATE_STATUS)
						.set("status", "sent")
						.set("id", exportRecord.getId())
						.dbUpdate(dbc);
				dbc.close();
				logger.debug("Export complete for record with id: " + exportRecord.getId());
				String patientId = documentRecord.getPatientID();
				patientId = computeSearchPatientId(patientId);
				logger.debug("Patient ID for search: " + patientId);
			} catch (Exception e) {
				logger.error("Could not complete export, export record ID: " + exportRecord.getId());
				logger.error(exportRecord.toString());
				e.printStackTrace();
			}
		}
	}
	
	
	private void initializeFileTemplates() throws Exception {
		if (docEntryTemplate == null) {
			runDirectory = Util.getRunDirectory();
			docEntryTemplate      = new String(Files.readAllBytes(runDirectory.resolve(DOC_ENTRY_PFN)));
			submissionSetTemplate = new String(Files.readAllBytes(runDirectory.resolve(SUBMISSION_SET_PFN)));
		}
	}

	/**
	 * Retrieves any unsent documents from the cda_user_exports table in the database.
	 * @param type: the kind of document (gold or sample)
	 * @return ResultSet of documents.
	 */
	public ArrayList<CDAUserExport> retrieveExportEntries(String status) {
		try {
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			ResultSet rs = new Query(DBUtil.CDA_EXPORT_INFO_RETRIEVE_STATUS)
			.set("status", status)
			.dbQuery(dbc);
			ArrayList<CDAUserExport> rtn = queueBuilder(rs);
			rs.close();
			dbc.close();
			return rtn;
		} catch(Exception e) {
			System.out.println("Could not connect to database");
			return null;
		}
	}

	/**
	 * Retrieves any unsent documents from the cda_user_exports table in the database.
	 * @param type: the kind of document (gold or sample)
	 * @return ResultSet of documents.
	 */
	public ResultSet retrieveExportInfo(String documentType, String status, String exportType){
		try{
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			ResultSet rs = new Query(DBUtil.CDA_EXPORT_INFO_RETRIEVE)
			.set("documentType", documentType)
			.set("status", status)
			.set("exportType", exportType)
			.dbQuery(dbc);
			dbc.close();
			return rs;
		}catch(Exception e){
			System.out.println("Could not connect to database");
			return null;
		}
	}

	public ResultSet retrieveExportFiles(int infoId){
		try {
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			ResultSet rs = new Query(DBUtil.CDA_EXPORT_FILES_RETRIEVE)
			.set("infoId", infoId)
			.dbQuery(dbc);
			dbc.close();
			return rs;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public ResultSet retrieveExportSamples(int id){
		try{
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			ResultSet rs = new Query(DBUtil.CDA_EXPORT_SAMPLES_RETRIEVE)
			.set("id", id)
			.dbQuery(dbc);
			dbc.close();
			return rs;			
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	CDADocStore retrieveDocument(String documentId) {
		try{
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			ResultSet rs = new Query(DBUtil.CDA_EXPORT_SAMPLES_RETRIEVE)
			.set("id", documentId)
			.dbQuery(dbc);
			CDADocStore d = null;
			while (rs.next()) {
				d = new CDADocStore(rs);
			}
			rs.close();
			dbc.close();
			return d;			
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Creates an ArrayList of CDAUserExport objects.
	 * @param rs: the input ResultSet
	 * @return ArrayList of CDAUserExport objects
	 */
	public ArrayList<CDAUserExport> queueBuilder(ResultSet info){
		ArrayList<CDAUserExport> documentList = new ArrayList<CDAUserExport>();
	
		if (info!=null){
			try {
				while (info.next()){
					CDAUserExport doc = new CDAUserExport();
					doc.setId(info.getInt("id"));
					doc.setUserId(info.getString("user_id"));
					doc.setDocumentId(info.getString("document_id"));
//					doc.setDocumentType(info.getString("documentType"));
					doc.setStatus(info.getString("status"));
					doc.setExportType(info.getString("export_type"));
					doc.setPnrEndpoint(info.getString("pnr_endpoint"));
					doc.setToAddress(info.getString("to_address"));
					doc.setSubject(info.getString("subject"));
					doc.setBody(info.getString("body"));
//					doc.setPatientId(info.getString("patientId"));
//					doc.setSourcePatientId(info.getString("sourcePatientId"));
//					doc.setClassCode(info.getString("classCode"));
					doc.setConfidentialityCode(info.getString("confidentiality_code"));
//					doc.setFormatCode(info.getString("formatCode"));
//					doc.setHealthCareFacilityTypeCode(info.getString("healthCareFacilityTypeCode"));
//					doc.setHomeCommunityId(info.getString("homeCommunityId"));
					doc.setLanguageCode(info.getString("language_code"));
					doc.setMimeType(info.getString("mime_type"));
//					doc.setPracticeSettingCode(info.getString("practiceSettingCode"));
//					doc.setTypeCode(info.getString("typeCode"));
//					doc.setNumDocs(info.getInt("numDocs"));

					documentList.add(doc);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return documentList;
		} else
			return null;
	}

	/*
	public ArrayList<CDAUserExport> queueBuilderDeprecated(ResultSet info){
		ArrayList<CDAUserExport> documentList = new ArrayList<CDAUserExport>();
	
		if (info!=null){
			try {
				while (info.next()){
					CDAUserExport doc = new CDAUserExport();
					doc.setId(info.getInt("id"));
					doc.setUserId(info.getString("userId"));										
					doc.setDocumentType(info.getString("documentType"));
					doc.setStatus(info.getString("status"));
					doc.setExportType(info.getString("exportType"));
					doc.setToAddress(info.getString("toAddress"));
					doc.setSubject(info.getString("subject"));
					doc.setBody(info.getString("body"));
					doc.setPatientId(info.getString("patientId"));
					doc.setSourcePatientId(info.getString("sourcePatientId"));
					doc.setClassCode(info.getString("classCode"));
					doc.setConfidentialityCode(info.getString("confidentialityCode"));
					doc.setFormatCode(info.getString("formatCode"));
					doc.setHealthCareFacilityTypeCode(info.getString("healthCareFacilityTypeCode"));
					doc.setHomeCommunityId(info.getString("homeCommunityId"));
					doc.setLanguageCode(info.getString("languageCode"));
					doc.setMimeType(info.getString("mimeType"));
					doc.setPracticeSettingCode(info.getString("practiceSettingCode"));
					doc.setTypeCode(info.getString("typeCode"));
					doc.setNumDocs(info.getInt("numDocs"));

					ResultSet files = retrieveExportFiles(doc.getId());

					if (doc.getDocumentType().equals("gold")){
						ArrayList<String> paths = new ArrayList<String>();
						while(files.next()){
							paths.add(files.getString("docPath"));
						}
						doc.setDocPath(paths.toArray());
					}
					if (doc.getDocumentType().equals("sample")){
						ArrayList<CDADocStore> sampleDocs = new ArrayList<CDADocStore>();
						while(files.next()){
							CDADocStore[] docs = CDADocStore.load(retrieveExportSamples(files.getInt("SDID")));
							for (CDADocStore document:docs){
								sampleDocs.add(document);
							}
						}
						doc.setSampleDocs(sampleDocs.toArray());
					}
					documentList.add(doc);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return documentList;
		} else
			return null;
	}
*/
	public File zipExport(CDAUserExport doc) {
/*
		Valid v = new Valid();
		v.startValidations();
		try {
			//--- New sample Zip instance for each Session
			if (zipper == null) {
				zipper = new Zip();
				zipper.setWorkingDirectory(WORK, false);
				if (doc.getDocumentType().equals("gold"))
					zipper.setBoilerDirectory(GOLD_BOILER);
				else if (doc.getDocumentType().equals("sample"))
					zipper.setBoilerDirectory(SAMPLE_BOILER);
			}
			zipper.startZip();
			if (doc.getDocumentType().equals("gold")){

				for (Object docPath:doc.getDocPath()){
					Path path = Paths.get((String) docPath);
					path = Util.getRunDirectory().resolve(path);
					String docName = path.getFileName().toString();
					try{
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						byte[] docBytes = Files.readAllBytes(path);
						docString = docBytes.toString();
						docDoc = db.parse(new ByteArrayInputStream(docBytes));
						// --- pretty print doc if we can
						docXML = Util.prettyPrintXML(docDoc);
						if (docXML == null)
							docXML = docString;
					} catch (Exception ep) {
						throw new Exception("Document File "
								+ docName
								+ " has parsing error(s) " + ep.getMessage());
					}
					zipper.addStringAsFile(docXML, 
							docName);
				}
				zipPfn = zipper.zip(GOLD_ZIP_FILE_NAME);
			}
			if (doc.getDocumentType().equals("sample")){
				int msgCounter = 0;
				StringBuffer list = new StringBuffer();
				for (Object sample:doc.getSampleDocs()){
					msgCounter++;
					String msgFileName = "msg" + msgCounter + ".xml";
					list.append(((CDADocStore) sample).getOrganization())
					.append(tab)
					.append(((CDADocStore) sample).getSystem())
					.append(tab)
					.append(((CDADocStore) sample).getDocumentType())
					.append(tab)
					.append(msgFileName)
					.append(nl);
					zipper.addStringAsFile(((CDADocStore) sample).getDocument(), msgFileName);
				}
				zipper.addStringAsFile(list.toString(), "documentList.txt");
				zipPfn = zipper.zip(SAMPLE_ZIP_FILE_NAME);
			}
			return new File(zipPfn);

		} catch (Exception e) {
			String em = "Error creating zip file: " + e.getMessage();
			v.error(em);
		}
*/
		return null;

	}
	public void exportEmail(String documentType, String status){
		ResultSet info = retrieveExportInfo(documentType, status, "Email");
		ArrayList<CDAUserExport> docs = queueBuilder(info);
		try{
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			for (CDAUserExport doc:docs){
				new Query(DBUtil.CDA_EXPORT_INFO_UPDATE_STATUS)
				.set("status", "sent")
				.set("id", doc.getId())
				.dbUpdate(dbc);
				dbc.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	public void exportXDSB(String documentType, String status){
		ResultSet rs = retrieveExportInfo(documentType, status, "XDS.b");
		ArrayList<CDAUserExport> docs = queueBuilder(rs);
		try{
			DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);
			for (CDAUserExport doc:docs){
				new Query(DBUtil.CDA_EXPORT_INFO_UPDATE_STATUS)
				.set("status", "sent")
				.set("id", doc.getId())
				.dbUpdate(dbc);
				dbc.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String computeSearchPatientId(String patientId) {
		String rtnString = null;
		
		String[] idTokens = patientId.split("\\^");
		String[] assigningAuthorityTokens = idTokens[3].split("&");
		rtnString = idTokens[0] + "^^^"
				+ "&" + assigningAuthorityTokens[1] + "&" + assigningAuthorityTokens[2]; 

		return rtnString;
	}
}
