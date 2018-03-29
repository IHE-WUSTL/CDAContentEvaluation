package edu.wustl.mir.erl.IHETools.CDAContentEvaluation.view;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Map;


import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.model.SelectItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.icesoft.faces.component.paneltabset.TabChangeEvent;
import com.icesoft.faces.context.FileResource;
import com.icesoft.faces.context.Resource;

import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.Assertion;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.Command;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.DocumentValidator;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDADocStore;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.CDAUserExport;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.Exporter;
import edu.wustl.mir.erl.IHETools.Util.CDABuilderRow;
import edu.wustl.mir.erl.IHETools.Util.DBUtil;
import edu.wustl.mir.erl.IHETools.Util.DataBaseConnection;
import edu.wustl.mir.erl.IHETools.Util.FacesUtil;
import edu.wustl.mir.erl.IHETools.Util.Query;
import edu.wustl.mir.erl.IHETools.Util.Util;
import edu.wustl.mir.erl.IHETools.Util.Valid;
import edu.wustl.mir.erl.IHETools.Util.Zip;

/**
 * Handles JSF and computation for CDAContentEvaluation.xhtmls
 * @author rmoult01
 *
 */
@ManagedBean
@SessionScoped
public class SessionBean implements Serializable, ValueChangeListener {
	private static final long serialVersionUID = 1L;

	/***************************************************************
	 * General Properties
	 **************************************************************/
	ApplicationBean applicationBean = ApplicationBean.getInstance();

	private static Logger log = Util.getLog();

	private static final String nl = System.getProperty("line.separator");
	private static final String tab = "\t";
	private static final String EMPTY = "";

	/***************************************************************
	 * Constructor
	 **************************************************************/

	public SessionBean() {
		evaluateContentInit();
		selectedTab = EVALUATE_CONTENT_TAB;
		log.info("SessionBean initialized");

	}


	/***************************************************************
	 ************** log in/out, CAS validation *************
	 **************************************************************/

	private String casUserId = "";
	private String casUserName = "";
	private boolean monitor = false;
	public boolean isMonitor() { return monitor;}

	public String getRole() {
		if (monitor) return "monitor ";
		else return "user ";
	}

	/**
	 * Action method for  log in command button
	 * @return loggedIn view
	 */
	public String logIn() {
		if (applicationBean.profileIs(ApplicationBean.Profiles.DEV)) {
			casUserId = "moulton";
			casUserName = "Ralph Moulton";
			monitor = true;
		}
		// ---------------------------------------------
		return "/loggedIn/CDAContentEvaluation.xhtml?faces-redirect=true";
	}

	/**
	 * Determine if user is logged in via CAS. Called from loggedIn view.
	 * @param cse - ignored.
	 */
	public void validateCAS(ComponentSystemEvent cse) {
		try {
			log.trace("validateCAS() called");
			//---------------------------------- already logged in
			if (!casUserId.isEmpty()) return;

			Principal p = FacesUtil.getHttpServletRequest().getUserPrincipal();

			//------------------------- Must have CAS AttributePrincipal
			if (p == null)
				throw new Exception("UserPrincipal is null");
			if (!(p instanceof AttributePrincipal)) 
				throw new Exception("UserPrincipal is class: " + 
						p.getClass().getName() + ", name: " + p.getName());
			AttributePrincipal ap = (AttributePrincipal) p;

			@SuppressWarnings("unchecked")
			Map<String, String> attrs = (Map<String,String>)ap.getAttributes();
			if (attrs == null)
				throw new Exception("Principal attributes are null");
			if (attrs.isEmpty())
				throw new Exception("Principal attributes are empty");

			//------------------------------------  monitors have extra status	
			monitor = false;
			String roles = attrs.get("role_name");
			if (roles == null) roles = "";
			roles = roles.toLowerCase();
			if (roles.contains("monitor_role")) monitor = true;

			//----------------------- get user id and name
			casUserId = attrs.get("username");
			if (casUserId == null) casUserId = ap.getName();
			String fn = attrs.get("firstname");
			if (fn == null) fn = "xxx";
			String ln = attrs.get("lastname");
			if (ln == null) ln = "xxx";
			casUserName = fn + " " + ln;
			log.info(getRole() + casUserName + " logged in");
			return;
		} catch (Exception e) {
			log.warn(getRole() +  "log in error: " + e.getMessage());
			try {FacesUtil.goToPage(Util.getHomeURL());}
			catch (Exception i) {
				log.warn("goToPage error: " + i.getMessage());
			}
		}
	} // EO validateCAS method


	/********************************************************************
	 ************************ panelTabSet Controls **********************
	 *******************************************************************/

	public static final int EVALUATE_CONTENT_TAB = 0;
	private static final int RETRIEVE_SAMPLE_DOCUMENTS_TAB = 1;
	private static final int RETRIEVE_GOLD_DOCUMENTS_TAB = 2;
	private static final int STORE_SAMPLE_DOCUMENTS_TAB = 3;
	private static final int DOCUMENT_BUILDER_TAB = 4;
	private int selectedTab;

	public int getSelectedTab() {
		return selectedTab;
	}
	public void setSelectedTab(int tab) {
		selectedTab = tab;
	}

	public void tabChangeListener(TabChangeEvent tabChangeEvent) {
		switch (selectedTab) {
		case EVALUATE_CONTENT_TAB:
			break;
		case RETRIEVE_SAMPLE_DOCUMENTS_TAB:
			sampleInit();
			break;
		case RETRIEVE_GOLD_DOCUMENTS_TAB:
			goldInit();
			break;
		case STORE_SAMPLE_DOCUMENTS_TAB:
			storeInit();
			break;
		case DOCUMENT_BUILDER_TAB:
			builderInit();
			break;					
		default:
			log.warn("invalid tab value: " + selectedTab);
		}
	}

	/********************************************************************
	 ************************ Evaluate content tab **********************
	 *******************************************************************/

	public static final String EMPTY_DOC = "Paste document to test here" + nl + 
			"or upload file using the 'Browse...' or 'Choose File' button" +
			nl + "(which one depends on your browser)" +
			nl + "then the 'load document file' button";

	private DocumentBuilderFactory dbf = null;
	private DocumentBuilder db = null;

	private String lastDocumentType = "";
	private String documentType = null;
	private DocumentValidator docValidator = null;
	private String testDoc = "";
	private String testDocEvaluation = "";

	private boolean mepExpanded;
	private boolean mrpExpanded;
	private boolean gmpExpanded;
	private boolean tapExpanded;

	/**
	 * one time init routine for content evaluation tab
	 */
	private void evaluateContentInit() {
		try {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			db = dbf.newDocumentBuilder();
			documentType = applicationBean.getDocuments()[0].getLabel();
			processDocumentTypeChange();
		} catch (ParserConfigurationException e) {
			log.warn("Create document builder error: " + e.getMessage());
		}
	}

	/**
	 * change listener attached to the document type selector.
	 * Will reset the screen.
	 * @param vce
	 */
	public void documentTypeChange(ValueChangeEvent vce) {
		documentType = (String) vce.getNewValue();
		processDocumentTypeChange();
		FacesUtil.refreshPage();
	}

	/**
	 * Invoked when user changes the document type selected for evaluation and
	 * on session initialization.  Resets the screen and loads the appropriate 
	 * document type evaluator.
	 */
	private void processDocumentTypeChange() {
		if (lastDocumentType.equals(documentType)) return;
		lastDocumentType = documentType;
		docValidator = applicationBean.getValidator(documentType);
		mepExpanded = true;
		mrpExpanded = false;
		gmpExpanded = false;
		tapExpanded = false;
		testDoc = EMPTY_DOC;
		testDocEvaluation = nl + "Waiting to evaluate document";

	} // EO processDocumentTypeChanged method

	public void evaluateContentReset() {
		lastDocumentType = "";
		processDocumentTypeChange();
	}

	public void loadFile(FileEntryEvent fee) {
		File file = ((FileEntry)fee.getComponent()).getResults().getFiles().get(0).getFile();
		try {
			testDoc = FileUtils.readFileToString(file);	
		} catch (IOException io) {
			testDoc = "IO error trying to upload file: " + io.getMessage();
			return;
		}
		Document tDoc = null;		
		try (ByteArrayInputStream is = new ByteArrayInputStream(testDoc.getBytes())) {
			tDoc = db.parse(is);
			String s = Util.prettyPrintXML(tDoc);
			if (s != null) testDoc = s;
		} catch (Exception e) {
		}
	} // EO loadFile method

	/**
	 * Invoked when the Evaluate Document command button is clicked.
	 * Evaluates the Document and generates error and examine output.
	 */
	public void evaluateDocument() {
		Document testDocDoc = null;
		Node testNode = null;
		testDocEvaluation = "";

		//----------- Set panel expansion status
		mepExpanded = true;
		mrpExpanded = true;
		gmpExpanded = false;
		tapExpanded = false;

		//------------------------ Is there a document to test?
		if (StringUtils.isBlank(testDoc) || testDoc.equalsIgnoreCase(EMPTY_DOC)) {
			add("ERROR: No document to evaluate.");
			return;
		}

		//-------------------------- Parse test document
		try (ByteArrayInputStream is = new ByteArrayInputStream(testDoc.getBytes())) {
			testDocDoc = db.parse(is);
		} catch (Exception e) {
			add("ERROR: Test Document not valid xml: " + e.getMessage());
			return;
		}

		//-------------------- Pass all assertions
		for (Assertion a : docValidator.getAssertions()) {
			Command cmd = a.getCmd(); 

			/* "@" comments are copied to output. 
			 * Other comments require no action
			 */
			if (cmd == Command.COMMENT) {
				if (a.getAssertion().startsWith("@")) {
					add(a.getAssertion());
				}
				continue;
			}

			//------------- Get node from test document
			try {
				testNode = (Node) a.getxPathExpression().evaluate(testDocDoc, XPathConstants.NODE);
			} catch (XPathExpressionException e) {
				add("ERROR: Node not found for assertion: " + a.getAssertion() );
				continue;
			}

			if (cmd == Command.PRESENT) {
				add("SUCCESS: Node found for assertion: " + a.getAssertion());
				continue;
			}

			String expected = a.getNode().getTextContent();
			String found = testNode.getTextContent();

			if (cmd == Command.EQ) {
				if (expected.equals(found)) {
					add("SUCCESS: Node values equal for assertion: " + a.getAssertion());
				} else {
					add("ERROR: Node values not equal for assertion: " + a.getAssertion());
				}
				add("   Expected: " + expected + " found: " + found);
				continue;
			}

			if (cmd == Command.SIMILAR) {
				if (expected.equals(found)) {
					add("SUCCESS: Node values equal for assertion: " + a.getAssertion());
				} else {
					add("WARNING: Review - node values should be similar for assertion: " + a.getAssertion());
				}
				add("   Expected: " + expected + " found: " + found);
				continue;
			}

			log.warn("evaluateDocument() saw Command: " + cmd.toString());


		} // EO pass all assertions

	} // EO EvaluateDocument method 

	private void add(String msgLine) {
		if (!testDocEvaluation.endsWith(nl)) testDocEvaluation += nl;
		testDocEvaluation += msgLine;
	}


	/********************************************************************
	 ****************** Retrieve CDA sample documents tab ****************
	 *******************************************************************/

	private boolean sampleFilterPanelExpanded;
	private boolean sampleListPanelExpanded;
	private boolean sampleDisplayPanelExpanded;

	private String sfOrganization = EMPTY;

	private SelectItem[] sfSystems;
	private String sfSystem = EMPTY;
	public boolean isSfSystemsDisabled() {
		return StringUtils.isBlank(sfOrganization);
	}

	private String sfDocType = EMPTY;

	private int sfMaxRows;

	private CDADocStore[] samples;
	private String sortColumnName, oldSortColumnName;
	private boolean ascending,oldAscending;

	private String selectedDoc;
	private Integer[] SDID;


	private void sampleInit() {
		sampleListPanelExpanded = false;
		sampleReset();
	}

	public String sampleReset() {
		sampleFilterPanelExpanded = true;
		sampleDisplayPanelExpanded = false;
		sfOrganization = EMPTY;
		sfoChangeListener(null);
		sfDocType = EMPTY;
		sfMaxRows = 10;
		samples = new CDADocStore[0];
		selectedDoc = "";
		exportSelection="empty";
		emailSelected = false;
		XDSBSelected=false;
		SDID=null;
		toAddress=null;
		subject=null;
		body=null;
		patientId=null;
		sourcePatientId=null;
		classCode=null;
		confidentialityCode=null;
		formatCode=null;
		healthCareFacilityTypeCode=null;
		homeCommunityId=null;
		languageCode=null;
		mimeType=null;
		practiceSettingCode=null;
		typeCode=null;
		if (sampleZip != null) sampleZip.close();
		return null;
	}


	public void sampleDeleteDocument() {
		try (DataBaseConnection conn = 
				DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			for (CDADocStore sDoc : samples) {
				if (sDoc.isSelected()) {
					new Query(DBUtil.CDA_DOC_STORE_DELETE)
					.set("id", sDoc.getId())
					.dbUpdate(conn);
				}
			}
			sampleReset();
		} catch (Exception e) {
			String em = "Delete CDA document(s) error " + e.getMessage();
			log.warn(em);			
		}
	}

	public void sfoChangeListener(ValueChangeEvent vce) {
		sfOrganization = (vce == null) ? EMPTY : (String) vce.getNewValue();
		sfSystem = EMPTY;
		if (StringUtils.isBlank(sfOrganization)) {
			sfSystems = new SelectItem[1];
			sfSystems[0] = new SelectItem(EMPTY, "Any");
			return;
		}
		sfSystems = applicationBean.getFilterOrganizationSystems(sfOrganization);
		FacesUtil.refreshPage();
		return;
	}

	public void clearSfOganization() {
		sfOrganization = EMPTY;
		sfoChangeListener(null);
	}

	public void clearSfSystem() {
		sfSystem = EMPTY;
	}

	public void clearSfDocType() {
		sfDocType = EMPTY;
	}

	public void clearSfMaxRows() {
		sfMaxRows = 10;
	}

	public void sfQuery() {
		ResultSet result = null;
		Valid v = new Valid();
		v.startValidations();
		if (sfMaxRows <= 0) 
			v.error("Max rows must be positive inteter");
		if (v.isErrors()) return;
		StringBuilder query = 
				new StringBuilder("select * from cda_doc_store");
		boolean firstFilter = true;
		if (!StringUtils.isBlank(sfOrganization)) {
			query.append((firstFilter == true) ? " WHERE" : " AND");
			query.append(" organization = '")
			.append(sfOrganization)
			.append("'");
			firstFilter = false;
		}
		if (!StringUtils.isBlank(sfSystem)) {
			query.append((firstFilter == true) ? " WHERE" : " AND");
			query.append(" system = '")
			.append(sfSystem)
			.append("'");
			firstFilter = false;
		}
		if (!StringUtils.isBlank(sfDocType)) {
			query.append((firstFilter == true) ? " WHERE" : " AND");
			query.append(" document_type = '")
			.append(sfDocType)
			.append("'");
			firstFilter = false;
		}
		query.append(" LIMIT ")
		.append(sfMaxRows)
		.append(";");

		samples = new CDADocStore[0];

		try (DataBaseConnection dbc = 
				DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			result = new Query(query).dbQuery(dbc);
			samples = CDADocStore.load(result);
			if (samples.length == 0) {
				v.error("No documents match these filter settings");
				return;
			}
			sampleListPanelExpanded = true;
		} catch (Exception e) {
			String em = "Query error: " + e.getMessage();
			log.warn(em);
			v.error(em);
			return;
		}
	}

	public boolean isSampleSelectAllDisabled() {
		return samples.length == sampleSelectedCount();
	}
	public boolean isSampleDeselectAllDisabled() {
		return sampleSelectedCount() == 0;
	}
	public boolean isSampleDisplayDocumentDisabled() {
		return sampleSelectedCount() != 1;
	}
	public boolean isSampleDownloadDocumentDisabled() {
		return sampleSelectedCount() == 0;
	}

	public String sampleSelectAll() {
		for (int i = 0; i < samples.length; i ++)
			samples[i].setSelected(true);
		return null;
	}

	public String sampleDeselectAll() {
		for (int i = 0; i < samples.length; i ++)
			samples[i].setSelected(false);
		return null;
	}

	private int sampleSelectedCount() {
		int selectedCount = 0;
		for (int i = 0; i < samples.length; i ++)
			if (samples[i].isSelected()) selectedCount++;
		return selectedCount;
	}

	/**
	 * Action method for Display Selected document command button
	 * @return null
	 */
	public String sampleDisplayDocument() {
		selectedDoc = "";
		for (int i = 0; i < samples.length; i ++)
			if (samples[i].isSelected()) {
				selectedDoc = samples[i].getDocument();
				break;
			}
		sampleListPanelExpanded = false;
		sampleDisplayPanelExpanded = true;
		return null;
	}

	private Zip sampleZip = null;
	private static final String SAMPLE_WORK = "INI:ZipWorkingDirectory";
	private static final String SAMPLE_BOILER = 
			"INI:SampleBoilerplateDirectory";
	private static final String ZIP_FILE_NAME = "sampleCDADoc.zip";
	private String sampleZipPfn = "";
	/**
	 * Action method for "Download Selected document(s)" command button
	 * Creates zip file with Readme.txt, documentList.txt, and the selected
	 * document#.xml files 
	 * @return null
	 */
	public String sampleDownloadDocument() {
		sampleZipPfn = "";
		Valid v = new Valid();
		v.startValidations();
		try {
			//--- New sample Zip instance for each Session
			if (sampleZip == null) {
				sampleZip = new Zip();
				sampleZip.setWorkingDirectory(SAMPLE_WORK, false);
				sampleZip.setBoilerDirectory(SAMPLE_BOILER);
			}
			sampleZip.startZip();
			int msgCounter = 0;
			StringBuffer list = new StringBuffer();
			for (CDADocStore sample : samples) {
				if (!sample.isSelected()) continue;
				msgCounter++;
				String msgFileName = "msg" + msgCounter + ".xml";
				list.append(sample.getOrganization())
				.append(tab)
				.append(sample.getSystem())
				.append(tab)
				.append(sample.getDocumentType())
				.append(tab)
				.append(msgFileName)
				.append(nl);
				sampleZip.addStringAsFile(sample.getDocument(), msgFileName);
			}
			sampleZip.addStringAsFile(list.toString(), "documentList.txt");
			sampleZipPfn = sampleZip.zip(ZIP_FILE_NAME);
		} catch (Exception e) {
			String em = "Error creating zip file: " + e.getMessage();
			log.warn(em);
			v.error(em);
		}
		return null;
	}

	public boolean isNoSampleZipFile() {
		return sampleZipPfn.isEmpty();
	}
	public boolean isSampleZipFile() {
		return !sampleZipPfn.isEmpty();
	}
	public Resource getSampleZipResource() {
		if (sampleZipPfn.isEmpty()) return null;
		return new FileResource(new File(sampleZipPfn));
	}



	/*******************************************************************
	 ******************** Retrieve gold documents tab *******************
	 *******************************************************************/

	private boolean goldListPanelExpanded;
	private boolean goldDocPanelExpanded;
	private boolean goldAssertPanelExpanded;

	private DocumentValidator[] goldDocs;

	private String goldSelectedDoc;
	private String goldSelectedAsserts;

	private String[] goldDocPath;

	private void goldInit() {
		goldDocs = applicationBean.getValidators();
		goldReset();

	}
	private void goldReset() {
		for (DocumentValidator mv: goldDocs) {
			mv.setSelected(false);
		}
		emailSelected = false;
		XDSBSelected = false;
		goldListPanelExpanded = true;
		goldDocPanelExpanded = false;
		goldSelectedDoc = "";
		goldAssertPanelExpanded = false;
		goldSelectedAsserts = "";
		goldZipPfn = "";
		exportSelection="empty";
		toAddress = null;
		subject = null;
		body = null;
		patientId = null;
		sourcePatientId = null;
		classCode = null;
		confidentialityCode = null;
		formatCode = null;
		healthCareFacilityTypeCode = null;
		homeCommunityId = null;
		languageCode = null;
		mimeType = null;
		practiceSettingCode = null;
		typeCode = null;
		goldDocPath = null;

	}

	private int goldSelectedCount() {
		int count = 0;
		for (DocumentValidator mv: goldDocs) {
			if (mv.isSelected()) count++;
		}
		return count;
	}

	public boolean isGoldSelectAllDisabled() {
		return goldDocs.length == goldSelectedCount();
	}
	public boolean isGoldDeselectAllDisabled() {
		return goldSelectedCount() == 0;
	}
	public boolean isGoldDisplayDisabled() {
		return goldSelectedCount() != 1;
	}

	public String goldSelectAll() {
		for (int i = 0; i < goldDocs.length; i ++)
			goldDocs[i].setSelected(true);
		return null;
	}

	public String goldDeselectAll() {
		for (int i = 0; i < goldDocs.length; i ++)
			goldDocs[i].setSelected(false);
		return null;
	}

	/** 
	 * Action method for Display selected gold document command button
	 * @return null
	 */
	public String goldDisplayDocument() {
		goldSelectedDoc = "";
		for (DocumentValidator mv: goldDocs) {
			if (mv.isSelected()) goldSelectedDoc = mv.getGoldDoc();
		}
		goldAssertPanelExpanded = false;
		goldDocPanelExpanded = true;
		return null;
	}

	public String goldDisplayAsserts() {
		goldSelectedAsserts = "";
		for (DocumentValidator mv: goldDocs) {
			if (mv.isSelected()) goldSelectedAsserts = mv.getAssertMsg();
		}
		goldDocPanelExpanded = false;
		goldAssertPanelExpanded = true;
		return null;
	}

	public String getGoldZipFileName() {
		return GOLD_ZIP_FILE_NAME;
	}


	private Zip goldZip = null;
	private static final String GOLD_WORK = "INI:ZipWorkingDirectory";
	private static final String GOLD_BOILER = 
			"INI:GoldBoilerplateDirectory";
	private static final String GOLD_ZIP_FILE_NAME = "goldCDADoc.zip";
	private String goldZipPfn = "";
	/**
	 * Action method for "Download Selected document(s)" command button
	 * Creates zip file with Readme.txt, DocumentList.txt, and the selected
	 * Document#.xml files 
	 * @return null
	 */
	public String goldDownloadDocument() {
		goldZipPfn = "";
		Valid v = new Valid();
		v.startValidations();
		try {
			//--- New sample Zip instance for each Session
			if (goldZip == null) {
				goldZip = new Zip();
				goldZip.setWorkingDirectory(GOLD_WORK, false);
				goldZip.setBoilerDirectory(GOLD_BOILER);
			}
			goldZip.startZip();
			for (DocumentValidator gold : goldDocs) {
				if (!gold.isSelected()) continue;
				String mt = gold.getDocumentName();
				goldZip.addStringAsFile(gold.getGoldDoc(), 
						mt + ".gold-document.xml");
				goldZip.addStringAsFile(gold.getAssertMsg(), 
						mt + ".assertions.txt");
			}
			goldZipPfn = goldZip.zip(GOLD_ZIP_FILE_NAME);
		} catch (Exception e) {
			String em = "Error creating zip file: " + e.getMessage();
			log.warn(em);
			v.error(em);
		}
		return null;
	}

	public boolean isNoGoldZipFile() {
		return goldZipPfn.isEmpty();
	}
	public boolean isGoldZipFile() {
		return !goldZipPfn.isEmpty();
	}
	public Resource getGoldZipResource() {
		if (goldZipPfn.isEmpty()) return null;
		return new FileResource(new File(goldZipPfn));
	}

	public String goldZipReset() {
		goldReset();
		return null;
	}


	/*******************************************************************
	 ***************************** Export ******************************
	 *******************************************************************/

	//user input variables

	private String userId = "";
	private String toAddress;
	private String subject;
	private String body;
	private String patientId;
	private String sourcePatientId;
	private String classCode;
	private String confidentialityCode;
	private String formatCode;
	private String healthCareFacilityTypeCode;
	private String homeCommunityId;
	private String languageCode;
	private String mimeType;
	private String practiceSettingCode;
	private String typeCode;
	private String exportSelection;


	private boolean emailSelected = false;
	private boolean XDSBSelected = false;
	
	private String provideAndRegisterEndpoint;

	/**
	 * Determines if export method is Email or XDSB
	 */
	public void checkSelection(ValueChangeEvent event){
		exportSelection = (String) event.getNewValue();
		emailSelected = false;
		XDSBSelected  = false;
		if (exportSelection.equals("Email"))
			emailSelected = true;
		else if (exportSelection.equals("XDS.b"))
			XDSBSelected = true;
		FacesUtil.refreshPage();
		
	}

	/**
	 * If a gold standard document is to be exported, obtains path to document.
	 */
	public void goldExport(ActionEvent event) {
		System.out.println("SessionBean::goldExport");
		if (exportRequiredFields("gold"))
			return;
		if (exportSelection.equalsIgnoreCase("Download")){
			goldDownloadDocument();
		}
		if (exportSelection.equalsIgnoreCase("Email") || exportSelection.equalsIgnoreCase("XDS.b")){
			goldDocPath = new String[goldSelectedCount()];
			int counter=0;
			for (DocumentValidator gold: goldDocs) {
				if (gold.isSelected()){
					Path path = Paths.get("Documents", gold.getDocumentName(), gold.getDocumentName() + ".xml");
					goldDocPath[counter] = path.toString();
					counter++;
				}
			}
			exportDoc(goldDocPath.length, "gold", goldDocPath, null);
			goldReset();
		}
	}

	/**
	 * If a sample document is to be exported, obtains ID for database retrieval.
	 */
	public void sampleExport(ActionEvent event) {
		System.out.println("SessionBean::sampleExport");
		if (exportRequiredFields("sample"))
			return;
		System.out.println("Satisfied required fields");

		if (exportSelection.equalsIgnoreCase("Download")){
			sampleDownloadDocument();
		}
		System.out.println("Export Selection: " + exportSelection);
		if (exportSelection.equalsIgnoreCase("Email") || exportSelection.equalsIgnoreCase("XDS.b")){
			SDID = new Integer[sampleSelectedCount()];
			int counter=0;
			for (CDADocStore sample: samples) {
				if (sample.isSelected()){
					SDID[counter] = sample.getId();
					counter++;
				}
			}
			exportUploadedSamples(SDID);
			//exportDoc(SDID.length, "sample", null, SDID);
			sampleReset();
		}
	}
	
	public void exportUploadedSamples(Integer[] SDID) {
		int documentCount = SDID.length;
		int idx = 0;
		for (idx = 0; idx < documentCount; idx++) {
			System.out.println("Index: " + idx + ", Document ID: " + SDID[idx] + " " + provideAndRegisterEndpoint);
			Valid v = new Valid();
			String pnrURI = applicationBean.getProvideAndRegisterURI(provideAndRegisterEndpoint);
			v.startValidations();
			CDAUserExport export = new CDAUserExport();
			export.setUserId(casUserId);
			export.setDocumentId(SDID[idx].toString());
//			export.setDocumentType(docType);
//			export.setDocPath(docPath);
//			export.setSDID(SDID);
			export.setStatus("unsent");
			export.setExportType(exportSelection);
			export.setPnrEndpoint(pnrURI);
			export.setToAddress(StringUtils.trimToNull(toAddress));
			export.setSubject(StringUtils.trimToNull(subject));
			export.setBody(StringUtils.trimToNull(body));
//			export.setPatientId(StringUtils.trimToNull(patientId));
//			export.setSourcePatientId(StringUtils.trimToNull(sourcePatientId));
//			export.setClassCode(StringUtils.trimToNull(classCode));
			export.setConfidentialityCode(StringUtils.trimToNull(confidentialityCode));
//			export.setFormatCode(StringUtils.trimToNull(formatCode));
//			export.setHealthCareFacilityTypeCode(StringUtils.trimToNull(healthCareFacilityTypeCode));
//			export.setHomeCommunityId(StringUtils.trimToNull(homeCommunityId));
			export.setLanguageCode(StringUtils.trimToNull(languageCode));
			export.setMimeType(StringUtils.trimToNull(mimeType));
//			export.setPracticeSettingCode(StringUtils.trimToNull(practiceSettingCode));
//			export.setTypeCode(StringUtils.trimToNull(typeCode));
			try (DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
				export.insert(dbc);
			} catch (Exception e) {
				log.error(e.getMessage());
				v.error("Could not insert document into database");
			}		
		}
	}

	/**
	 * Exports document. Inserts into database.
	 */
	public void exportDoc(int numDocs, String docType, String[] docPath, Integer[] SDID){
/*		Valid v = new Valid();
		v.startValidations();
		CDAUserExport doc = new CDAUserExport();
		doc.setNumDocs(numDocs);
		doc.setUserId(userId);										
		doc.setDocumentType(docType);
		doc.setDocPath(docPath);
		doc.setSDID(SDID);
		doc.setStatus("unsent");
		doc.setExportType(exportSelection);
		doc.setToAddress(StringUtils.trimToNull(toAddress));
		doc.setSubject(StringUtils.trimToNull(subject));
		doc.setBody(StringUtils.trimToNull(body));
		doc.setPatientId(StringUtils.trimToNull(patientId));
		doc.setSourcePatientId(StringUtils.trimToNull(sourcePatientId));
		doc.setClassCode(StringUtils.trimToNull(classCode));
		doc.setConfidentialityCode(StringUtils.trimToNull(confidentialityCode));
		doc.setFormatCode(StringUtils.trimToNull(formatCode));
		doc.setHealthCareFacilityTypeCode(StringUtils.trimToNull(healthCareFacilityTypeCode));
		doc.setHomeCommunityId(StringUtils.trimToNull(homeCommunityId));
		doc.setLanguageCode(StringUtils.trimToNull(languageCode));
		doc.setMimeType(StringUtils.trimToNull(mimeType));
		doc.setPracticeSettingCode(StringUtils.trimToNull(practiceSettingCode));
		doc.setTypeCode(StringUtils.trimToNull(typeCode));

		try (DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			doc.insert(dbc);
		} catch (Exception e) {
			log.error(e.getMessage());
			v.error("Could not insert document into database");
		}
*/
		//Exporter exporter = new Exporter();
		//Exporter is intended to autonomously search the database for unsent documents and send them, although it is currently inoperable.
	}

	/**
	 * Returns true if a user input error is present.
	 */
	public boolean exportRequiredFields(String docType){
		Valid v = new Valid();
		v.startValidations();
		//No Location Selected
		if (exportSelection.equalsIgnoreCase("empty")){
			v.error("Export Method Not Selected");
		}
		//No Document Selected
		if (docType.equals("gold")){
			if (goldSelectedCount()==0){
				v.error("No Document(s) Selected");
			}
		}else if (docType.equals("sample")){
			if (sampleSelectedCount()==0){
				v.error("No Document(s) Selected");
			}
		}
		return v.isErrors();
	}

	/*******************************************************************
	 ******************** store sample documents tab *******************
	 *******************************************************************/
	LoggedInBean loggedInBean;

	public void storeInit() {
		loggedInBean = new LoggedInBean(testDoc);
	}
	public LoggedInBean getMb() { return loggedInBean; }

	/*******************************************************************
	 *************************** CDA Document Builder tab **************
	 *******************************************************************/
	private String document="";
	private String documentBuilderSelection = "empty";
	private ArrayList<CDABuilderRow> cdaBuilderRows;

	public void builderInit(){
		documentBuilderSelection = "empty";
		cdaBuilderRows = new ArrayList<CDABuilderRow>();
		builderReset();
	}

	public void builderReset(){
		document="";
		cdaBuilderRows.clear();
	}

	public boolean isTemplateSelected(){
		if (documentBuilderSelection.equals("empty")){
			return false;
		}
		return true;
	}

	public void documentBuilderSelected(ValueChangeEvent event){
		Valid v = new Valid();
		v.startValidations();
		builderReset();
		if (event.getNewValue().equals("empty")){
			return;
		}
		documentBuilderSelection = (String) event.getNewValue();
		try(DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			ResultSet rs = new Query(DBUtil.CDA_BUILDER_KEYNAMES_RETRIEVE).set("document", event.getNewValue().toString()).dbQuery(dbc);
			while (rs.next()){
				String keyname = rs.getString("keyname");
				buildTable(keyname);
				FacesUtil.refreshPage();
			}
		}catch(Exception e){
			log.warn(e.getMessage());
			v.error("Could not load sections for selected document");
		}
	}

	public void processValueChange(ValueChangeEvent evt){
		for (CDABuilderRow row:cdaBuilderRows){
			document+=row.getXML();
		}
	}

	/**
	 * Display table of sections for selected document.
	 */
	public void buildTable(String keyname) throws Exception{
		Valid v = new Valid();
		v.startValidations();
		try(DataBaseConnection dbc= DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){

			String templateId;
			String section;
			String opt;

			ResultSet sections = new Query(DBUtil.CDA_BUILDER_TEMPLATEIDS_RETRIEVE)
			.set("keyname", keyname).dbQuery(dbc);

			while (sections.next()){
				templateId = sections.getString("templateId");
				section = sections.getString("section");
				opt = sections.getString("opt");

				//retrieve the selections
				ResultSet selections = new Query(DBUtil.CDA_BUILDER_SELECTIONS_RETRIEVE)
				.set("templateId", templateId).dbQuery(dbc);
				ArrayList<SelectItem> selectOptions = new ArrayList<SelectItem>();
				selectOptions.add(new SelectItem("empty", "--Select Section Type--"));
				while (selections.next()){
					selectOptions.add(new SelectItem(selections.getString("type")));
				}
				cdaBuilderRows.add(new CDABuilderRow(section, opt, templateId, selectOptions));
			}
		}

	}

	/**
	 * Concatenate XML sections for display.
	 */
	public void checkDocument(){
		Valid v = new Valid();
		v.startValidations();
		Boolean isErrors = true;
		for (CDABuilderRow br:cdaBuilderRows){
			if (StringUtils.trimToNull(br.getXML())!=null)
				isErrors = false;
		}
		if (isErrors){
			v.error("At least one section must be added");
		}else{
			buildDocument();
		}
	}


	public void buildDocument(){
		log.debug("Start build message");
		Valid v = new Valid();
		v.startValidations();
		document = "";
		Boolean	addEnd = buildHeader();
		document+= "<component><structuredBody>";
		for (CDABuilderRow row:cdaBuilderRows){
			if (row.getXML()!=""){
				String section = "<component>" + row.getXML() + "</component>";
				document+=section;
			}
		}
		document+= "</structuredBody></component>";
		if(addEnd)
			addEnd();
		InputSource is = new InputSource(new StringReader(document));
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document secDoc = db.parse(is);	
			document = Util.prettyPrintXML(secDoc);
		}catch(ParserConfigurationException e){
			log.warn(e.getMessage());
			v.error("Could not instantiate DocumentBuilder");
		}catch(SAXException e){
			log.warn(e.getMessage());
			v.error("Could not parse code");
		}catch(IOException e){
			log.warn(e.getMessage());
			v.error("Could not parse code");
		}
	}

	/**
	 * Obtains document header from file.
	 */
	public boolean buildHeader(){
		Valid v = new Valid();
		v.startValidations();
		try(DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			ResultSet rs = new Query(DBUtil.CDA_BUILDER_HEADER_RETRIEVE).set("document", documentBuilderSelection).dbQuery(dbc);
			while (rs.next()){
				String headPath = rs.getString("header");
				if (headPath==null)
					return false;
				Path header = Util.getRunDirectory().resolve(Paths.get(headPath));
				BufferedReader br = new BufferedReader(new FileReader(header.toFile()));
				StringBuffer sb = new StringBuffer();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
				br.close();
				String head = sb.toString();
				//head = StringUtils.substringAfter(head, ">");
				document+=head;
			}
			return true;
		}catch(Exception e){
			log.warn("Could not find document header");
			v.error("Document header not found");
			return false;
		}
	}

	/**
	 * Inserts ending of XML. Necessary when header is added.
	 */
	public void addEnd(){
		String end = "</ClinicalDocument>";
		document+=end;
	}

	public boolean refreshButton(){
		for (CDABuilderRow br:cdaBuilderRows){
			if (StringUtils.trimToNull(br.getXML())!=null)
				return false;
		}
		return true;
	}

	/*******************************************************************
	 ******************** getters and setters *******************
	 *******************************************************************/

	public String getDocumentType() {
		return documentType;
	}


	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public boolean isMepExpanded() {
		return mepExpanded;
	}

	public void setMepExpanded(boolean mepExpanded) {
		this.mepExpanded = mepExpanded;
	}

	public boolean isMrpExpanded() {
		return mrpExpanded;
	}

	public void setMrpExpanded(boolean mrpExpanded) {
		this.mrpExpanded = mrpExpanded;
	}

	public boolean isGmpExpanded() {
		return gmpExpanded;
	}

	public void setGmpExpanded(boolean gmpExpanded) {
		this.gmpExpanded = gmpExpanded;
	}

	public boolean isTapExpanded() {
		return tapExpanded;
	}

	public void setTapExpanded(boolean tapExpanded) {
		this.tapExpanded = tapExpanded;
	}

	public DocumentValidator getDocValidator() {
		return docValidator;
	}

	public void setDocValidator(DocumentValidator docValidator) {
		this.docValidator = docValidator;
	}

	public String getTestDoc() {
		return testDoc;
	}

	public void setTestDoc(String testDoc) {
		this.testDoc = testDoc;
	}

	public String getTestDocEvaluation() {
		return testDocEvaluation;
	}

	public void setTestDocEvaluation(String testDocEvaluation) {
		this.testDocEvaluation = testDocEvaluation;
	}

	public String getCasUserId() {
		return casUserId;
	}

	public void setCasUserId(String casUserId) {
		this.casUserId = casUserId;
	}

	public String getCasUserName() {
		return casUserName;
	}

	public void setCasUserName(String casUserName) {
		this.casUserName = casUserName;
	}

	public boolean isLoggedIn() {
		return !casUserId.isEmpty();
	}
	public boolean isSampleFilterPanelExpanded() {
		return sampleFilterPanelExpanded;
	}
	public void setSampleFilterPanelExpanded(boolean sampleFilterPanelExpanded) {
		this.sampleFilterPanelExpanded = sampleFilterPanelExpanded;
	}
	public String getSfOrganization() {
		return sfOrganization;
	}
	public void setSfOrganization(String sfOrganization) {
		this.sfOrganization = sfOrganization;
	}
	public SelectItem[] getSfSystems() {
		return sfSystems;
	}
	public void setSfSystems(SelectItem[] sfSystems) {
		this.sfSystems = sfSystems;
	}
	public String getSfSystem() {
		return sfSystem;
	}
	public void setSfSystem(String sfSystem) {
		this.sfSystem = sfSystem;
	}
	public String getSfDocType() {
		return sfDocType;
	}
	public void setSfDocType(String sfDocType) {
		this.sfDocType = sfDocType;
	}
	public boolean isSampleListPanelExpanded() {
		return sampleListPanelExpanded;
	}
	public void setSampleListPanelExpanded(boolean sampleListPanelExpanded) {
		this.sampleListPanelExpanded = sampleListPanelExpanded;
	}

	public boolean isSampleDisplayPanelExpanded() {
		return sampleDisplayPanelExpanded;
	}
	public void setSampleDisplayPanelExpanded(boolean sampleDisplayPanelExpanded) {
		this.sampleDisplayPanelExpanded = sampleDisplayPanelExpanded;
	}
	public int getSfMaxRows() {
		return sfMaxRows;
	}
	public void setSfMaxRows(int sfMaxRows) {
		this.sfMaxRows = sfMaxRows;
	}
	public CDADocStore[] getSamples() {
		return samples;
	}
	public void setSamples(CDADocStore[] samples) {
		this.samples = samples;
	}
	public String getSortColumnName() {
		return sortColumnName;
	}
	public void setSortColumnName(String sortColumnName) {
		this.sortColumnName = sortColumnName;
	}
	public String getOldSortColumnName() {
		return oldSortColumnName;
	}
	public void setOldSortColumnName(String oldSortColumnName) {
		this.oldSortColumnName = oldSortColumnName;
	}
	public boolean isAscending() {
		return ascending;
	}
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}
	public boolean isOldAscending() {
		return oldAscending;
	}
	public void setOldAscending(boolean oldAscending) {
		this.oldAscending = oldAscending;
	}
	public String getSelectedDoc() {
		return selectedDoc;
	}
	public void setSelectedDoc(String selectedDoc) {
		this.selectedDoc = selectedDoc;
	}
	public String getSampleZipFileName() {
		return ZIP_FILE_NAME;
	}
	public boolean isGoldListPanelExpanded() {
		return goldListPanelExpanded;
	}
	public void setGoldListPanelExpanded(boolean goldListPanelExpanded) {
		this.goldListPanelExpanded = goldListPanelExpanded;
	}
	public boolean isGoldDocPanelExpanded() {
		return goldDocPanelExpanded;
	}
	public void setGoldDocPanelExpanded(boolean goldDocPanelExpanded) {
		this.goldDocPanelExpanded = goldDocPanelExpanded;
	}
	public boolean isGoldAssertPanelExpanded() {
		return goldAssertPanelExpanded;
	}
	public void setGoldAssertPanelExpanded(boolean goldAssertPanelExpanded) {
		this.goldAssertPanelExpanded = goldAssertPanelExpanded;
	}
	public DocumentValidator[] getGoldDocs() {
		return goldDocs;
	}
	public void setGoldDocs(DocumentValidator[] goldDocs) {
		this.goldDocs = goldDocs;
	}
	public String getGoldSelectedDoc() {
		return goldSelectedDoc;
	}
	public String getGoldSelectedAsserts() {
		return goldSelectedAsserts;
	}

	public String[] getGoldDocPath() {
		return goldDocPath;
	}

	public void setGoldDocPath(String[] goldDocPath) {
		this.goldDocPath = goldDocPath;
	}

	public String getDocumentBuilderSelection() {
		return documentBuilderSelection;
	}

	public void setDocumentBuilderSelection(String documentBuilderSelection) {
		this.documentBuilderSelection = documentBuilderSelection;
	}

	public ArrayList<CDABuilderRow> getCdaBuilderRows() {
		return cdaBuilderRows;
	}

	public void setCdaBuilderRows(ArrayList<CDABuilderRow> cdaBuilderRows) {
		this.cdaBuilderRows = cdaBuilderRows;
	}

	public Integer[] getSDID() {
		return SDID;
	}

	public void setSDID(Integer[] sDID) {
		SDID = sDID;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getToAddress() {
		return toAddress;
	}

	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}

	public String getSubject() {
		return subject;
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

	public String getConfidentialityCode() {
		return confidentialityCode;
	}

	public void setConfidentialityCode(String confidentialityCode) {
		this.confidentialityCode = confidentialityCode;
	}

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

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

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

	public String getExportSelection() {
		return exportSelection;
	}

	public void setExportSelection(String exportSelection) {
		this.exportSelection = exportSelection;
	}

	public boolean isEmailSelected() {
		return emailSelected;
	}

	public void setEmailSelected(boolean emailSelected) {
		this.emailSelected = emailSelected;
	}

	public boolean isXDSBSelected() {
		return XDSBSelected;
	}

	public void setXDSBSelected(boolean xDSBSelected) {
		XDSBSelected = xDSBSelected;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getDocument() {
		return document;
	}
	public void setDocument(String document) {
		this.document = document;
	}

	public String getProvideAndRegisterEndpoint() {
		return provideAndRegisterEndpoint;
	}
	public void setProvideAndRegisterEndpoint(String provideAndRegisterEndpoint) {
		this.provideAndRegisterEndpoint = provideAndRegisterEndpoint;
		System.out.println("SessionBean::setProvideAndRegisterEndpoint: " + provideAndRegisterEndpoint);
	}

	public void dummy(ValueChangeEvent vce) {
		String x = vce.toString();
		System.out.println("SessionBean::dummy " + x);
	}




} // EO SessionBean class
