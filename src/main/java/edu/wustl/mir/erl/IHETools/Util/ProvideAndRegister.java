package edu.wustl.mir.erl.IHETools.Util;

import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;

import java.nio.file.Files;
import java.nio.charset.Charset;

import org.apache.log4j.*;
import org.openhealthtools.ihe.xds.*;
import org.openhealthtools.*;
import org.openhealthtools.ihe.atna.auditor.XDSSourceAuditor;

import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDAUserExport;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDADocStore;

public class ProvideAndRegister  implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger log = null;
	private org.openhealthtools.ihe.xds.source.B_Source bSource = null;
	private static final String testRepositoryURI = "http://localhost:9180/xdstools2/sim/3c5d1444-c1ea-41de-8242-ee98511592b7/rep/prb";
	private static final String organizationBaseX = "1.3.6.1.4.1.21367.1.2.3.4";


	private static final String SAMPLE_WORK = "ZipWorkingDirectory";
	private static final String IHE_ITI_DESIGNATOR = "1.3.6.1.4.1.19376.1.2.3";
	private static final String FIXED_DEMOGRAPHICS = 
"    <patientName> \n" +
"      <familyName>Murphy</familyName> \n" +
"      <givenName>J</givenName> \n" +
"    </patientName> \n" +
"    <patientDateOfBirth>19691128</patientDateOfBirth> \n" +
"    <patientSex>F</patientSex> \n" +
"    <patientAddress> \n" +
"      <streetAddress>1234 Some St. USA</streetAddress> \n" +
"    </patientAddress>";
		
	public ProvideAndRegister() {
		log = Util.getLog();
	}
	
	public void export(CDAUserExport exportRecord, CDADocStore documentRecord,
			String docEntryTemplate, String submissionSetTemplate) throws Exception {
		System.out.println("ProvideAndRegister::export: " + exportRecord.toString());
		System.out.println("ProvideAndRegister::export: " + documentRecord.toString());
		
		File docFolder = this.createTemporaryFolder("pnr-export");
		System.out.println("Temp folder: " + docFolder.getAbsolutePath());
		String documentEntry = modifyDocumentEntry(docEntryTemplate, documentRecord);
		String submissionSet = modifySubmissionSet(submissionSetTemplate, documentRecord);
		writeTextFile(docFolder, "document.xml",      documentRecord.getDocument());
		writeTextFile(docFolder, "docEntryTemplate.xml",      docEntryTemplate);
		writeTextFile(docFolder, "submissionSetTemplate.xml", submissionSetTemplate);
		writeTextFile(docFolder, "docEntry.xml",              documentEntry);
		writeTextFile(docFolder, "submissionSet.xml",         submissionSet);

		setupForSubmission(exportRecord.getPnrEndpoint());
		executeProvideAndRegister(docFolder);
	}

	private File createTemporaryFolder(String seed) throws Exception{
		File f = null;
		f = Files.createTempDirectory(seed).toFile();
		return f;
	}
	
	private void writeTextFile(File folder, String name, String contents) throws Exception {
		File f = new File(folder, name);
		Charset charset = Charset.forName("UTF-8");
		BufferedWriter w = Files.newBufferedWriter(f.toPath(), charset);
		w.write(contents, 0, contents.length());
		w.close();
	}
	
	private String modifyDocumentEntry(String inputDocEntry, CDADocStore documentRecord) {
		String[] inputExpressions = {
				"<CLASSCODE />",
				"<FORMATCODE />",
				"<PATIENTID />",
				"<SOURCEPATIENTID />",
				"<SOURCEPATIENTINFO />",
				"<TYPECODE />"};
		String[] outputValues = new String[6];
		outputValues[0] = constructClassCode(documentRecord);
		outputValues[1] = constructFormatCode(documentRecord);
		outputValues[2] = constructPatientId(documentRecord, "patientId", "");
		outputValues[3] = constructSourcePatientId(documentRecord, "");
		outputValues[4] = constructSourcePatientInfo(documentRecord, "");
		outputValues[5] = constructTypeCode(documentRecord);
		
		String outputDocEntry = replaceElements(inputDocEntry, inputExpressions, outputValues);
		return outputDocEntry;
	}
	
	
	private String modifySubmissionSet(String inputSubmissionSet, CDADocStore documentRecord) {
		String[] inputExpressions = {
				"<CONTENTTYPECODE />",
				"<PATIENTID />"};
		String[] outputValues = new String[2];
		outputValues[0] = constructContentTypeCode(documentRecord);
		outputValues[1] = constructPatientId(documentRecord, "patientId", "");
		
		String outputSubmissionSet = replaceElements(inputSubmissionSet, inputExpressions, outputValues);
		return outputSubmissionSet;
	}
	
	private String constructClassCode(CDADocStore documentRecord) {
		return	"<classCode>\n" +
				"    <code>" + documentRecord.getClassCode() + "</code>\n" +
				"    <displayName><LocalizedString value=\"" + documentRecord.getClassCodeDisplay() + "\"/></displayName>\n" +
				"    <schemeName>" + documentRecord.getClassCodeDesignator() + "</schemeName>\n" +
				"  </classCode>\n";
	}
	
	private String constructFormatCode(CDADocStore documentRecord) {
		return	"<formatCode>\n" +
				"    <code>" + documentRecord.getFormatCode() + "</code>\n" +
				"    <displayName><LocalizedString value=\"" + documentRecord.getFormatCode() + "\"/></displayName>\n" +
				"    <schemeName>" + IHE_ITI_DESIGNATOR + "</schemeName>\n" +
				"  </formatCode>\n";
	}
	
	private String constructPatientId(CDADocStore documentRecord, String tag, String prefix) {
		String[] idTokens = documentRecord.getPatientID().split("\\^");
		String[] assigningAuthorityTokens = idTokens[3].split("&");
		return	prefix + "<" + tag + ">\n" +
				prefix + "    <idNumber>" + idTokens[0] + "</idNumber>\n" +
				prefix + "    <assigningAuthorityUniversalId>" + assigningAuthorityTokens[1] + "</assigningAuthorityUniversalId>\n" +
				prefix + "    <assigningAuthorityUniversalIdType>" + assigningAuthorityTokens[2] + "</assigningAuthorityUniversalIdType>\n" +
				prefix + "  </" + tag + ">\n";
	}
	
	private String constructSourcePatientId(CDADocStore documentRecord, String prefix) {
		return constructPatientId(documentRecord, "sourcePatientId", prefix);
	}
		
	private String constructSourcePatientInfo(CDADocStore documentRecord, String prefix) {
		String idString = constructPatientId(documentRecord, "patientIdentifier", "  ");
		return	prefix + "<sourcePatientInfo> \n" +
				prefix + idString + "\n" +
				prefix + FIXED_DEMOGRAPHICS + "\n" +
				prefix + "  </sourcePatientInfo>\n";
	}
	
	private String constructTypeCode(CDADocStore documentRecord) {
		return	"<typeCode>\n" +
				"    <code>" + documentRecord.getTypeCode() + "</code>\n" +
				"    <displayName><LocalizedString value=\"" + documentRecord.getTypeCodeDisplay() + "\"/></displayName>\n" +
				"    <schemeName>" + documentRecord.getTypeCodeDesignator() + "</schemeName>\n" +
				"  </typeCode>\n";
	}
	
	private String constructContentTypeCode(CDADocStore documentRecord) {
		return	"<contentTypeCode>\n" +
				"    <code>" + documentRecord.getTypeCode() + "</code>\n" +
				"    <displayName><LocalizedString value=\"" + documentRecord.getTypeCodeDisplay() + "\"/></displayName>\n" +
				"    <schemeName>" + documentRecord.getTypeCodeDesignator() + "</schemeName>\n" +
				"  </contentTypeCode>\n";
	}
	
	private String replaceElements(String inputString, String[] inputExpressions, String[] outputValues) {
		String outputString = new String(inputString);
		for (int i = 0; i < inputExpressions.length; i++) {
			outputString = outputString.replaceFirst(inputExpressions[i], outputValues[i]);
		}
		return outputString;
	}
	
	/*Forms a time stamp for logging of the form YYYY/MM/DD hh:mm:ss using current system time*/
	private static String formTimestamp(){
		long time = System.currentTimeMillis();
		GregorianCalendar c = new GregorianCalendar();
		c.setTimeInMillis(time);
		StringBuffer timeStamp = new StringBuffer();
		timeStamp.append(c.get(GregorianCalendar.YEAR));
		timeStamp.append("/");
		timeStamp.append(c.get(GregorianCalendar.MONTH) + 1);
		timeStamp.append("/");
		timeStamp.append( c.get(GregorianCalendar.DAY_OF_MONTH));
		timeStamp.append(" ");
		timeStamp.append( c.get(GregorianCalendar.HOUR_OF_DAY));
		timeStamp.append(":");
		timeStamp.append(c.get(GregorianCalendar.MINUTE));
		timeStamp.append(":");
		timeStamp.append(c.get(GregorianCalendar.SECOND));
		return timeStamp.toString();
	}
	
	
	/**
	 * Forms a HL7 v2.5 DTM time stamp for logging of the form YYYYMMDDHHMMSS
	 *  using current system time*/
//	private static String formDTM(){
//		SimpleDateFormat date = new SimpleDateFormat("yyyyMMddHHmmss");
//		return date.format(new Date());
//	}
	
	/**
	 * Forms a HL7 v2.5 DTM time stamp for logging of the form YYYYMMDDHHMMSS
	 *  using current system time in GMT time zone */
	private static String formGMT_DTM(){
		String timeInGMT = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		// first, set up time in current time zone (where program is running
		sdf.setTimeZone(TimeZone.getDefault());
		String tm = sdf.format(new Date());
		
		// convert (though there is probably is some circular logic here, oh well
		Date specifiedTime;
		//System.out.println("Specified time is: " + tm);
		//System.out.println("time zone is:GMT" + offset);
		try {
			// switch timezone
			specifiedTime = sdf.parse(tm);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			timeInGMT = sdf.format(specifiedTime);
			//System.out.println("Specified time post conversion: "+ tm);
			//System.exit(0);
		} catch (ParseException e) {
			// FIXME just skip the conversion, bad time stamp, hence bad
			// CDA!
			// Maybe this should be more robust?? An Exception?
		}
		return timeInGMT;
	}

	private void setupForSubmission(String repositoryURI) throws Exception {
		log.debug("ProvideAndRegister::setupForSubmission URI: " + repositoryURI);
		java.net.URI uri = new java.net.URI(repositoryURI);
		bSource = new org.openhealthtools.ihe.xds.source.B_Source(uri);
		XDSSourceAuditor auditor = XDSSourceAuditor.getAuditor();
		auditor.getConfig().setAuditorEnabled(false);
		log.debug(formTimestamp());
	}
	
	private void executeProvideAndRegister(File docFolder) throws Exception {
		log.debug(("ProvideAndRegister::executeProvideAndRegister"));
		org.openhealthtools.ihe.xds.source.SubmitTransactionData txnData =
				new org.openhealthtools.ihe.xds.source.SubmitTransactionData();
		log.debug("Adding input document, and metadata.");
		String folderPath = docFolder.getPath();
		org.openhealthtools.ihe.xds.document.XDSDocument clinicalDocument =
				new org.openhealthtools.ihe.xds.document.XDSDocumentFromFile(org.openhealthtools.ihe.xds.document.DocumentDescriptor.CDA_R2, folderPath + "/document.xml");
		File docEntryFile = new File (folderPath + "/docEntry.xml");
        FileInputStream fis = new FileInputStream(docEntryFile);
        String docEntryUUID = txnData.loadDocumentWithMetadata(clinicalDocument, fis);
        fis.close();
        
        // say that you are assigned an organizational oid of TestConfiguration.ORGANIZATIONAL_OID
        // added length limit for NIST registry of 64
        txnData.getDocumentEntry(docEntryUUID).setUniqueId(org.openhealthtools.ihe.utils.OID.createOIDGivenRoot(organizationBaseX, 64));
        log.debug("Done setting documentEntry metadata for: " +txnData.getDocumentEntry(docEntryUUID).toString());
        
        
        // add submission set metadata
        log.debug("Applying Submission Set Metadata to the Submission.");
        File submissionSetFile = new File(folderPath + "/submissionSet.xml");
        fis = new FileInputStream(submissionSetFile);
        txnData.loadSubmissionSet(fis);
        fis.close();

        // say that you are assigned an organizational oid of TestConfiguration.ORGANIZATIONAL_OID
        // added length limit for NIST registry of 64
        txnData.getSubmissionSet().setUniqueId(org.openhealthtools.ihe.utils.OID.createOIDGivenRoot(organizationBaseX,64));
        // set submission time
        txnData.getSubmissionSet().setSubmissionTime(formGMT_DTM());

        // set submission set source id
        txnData.getSubmissionSet().setSourceId(organizationBaseX);
        
        log.debug("Submitting Document.");
        org.openhealthtools.ihe.xds.response.XDSResponseType response = bSource.submit(txnData);
        log.debug("Response status: " + response.getStatus().getName());
        if(response.getErrorList() != null){
                if(response.getErrorList().getError() != null){
                        log.debug("Returned " + response.getErrorList().getError().size() + " errors.");
                }
        }
        log.debug("Submission complete");
        
	}
	
}
