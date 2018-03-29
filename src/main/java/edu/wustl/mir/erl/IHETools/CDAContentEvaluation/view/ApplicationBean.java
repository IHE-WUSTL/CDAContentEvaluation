package edu.wustl.mir.erl.IHETools.CDAContentEvaluation.view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.model.SelectItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.Assertion;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.Command;
import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.DocumentValidator;
import edu.wustl.mir.erl.IHETools.Util.CDABuilderXMLDBInsert;
import edu.wustl.mir.erl.IHETools.Util.XDSBCodesDBInsert;
import edu.wustl.mir.erl.IHETools.Util.DBUtil;
import edu.wustl.mir.erl.IHETools.Util.DataBaseConnection;
import edu.wustl.mir.erl.IHETools.Util.CDABuilderExcelDBInsert;
import edu.wustl.mir.erl.IHETools.Util.ExporterThread;
import edu.wustl.mir.erl.IHETools.Util.FacesUtil;
import edu.wustl.mir.erl.IHETools.Util.Query;
import edu.wustl.mir.erl.IHETools.Util.Util;

@ManagedBean(eager = true)
@ApplicationScoped
public class ApplicationBean implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String EMPTY = "";
	private static final String nl = System.getProperty("line.separator");
	private static ApplicationBean instance = null;

	private static final String APPLICATION_NAME = "CDAContentEvaluation";

	public enum Profiles {
		DEV, NA, EU
	}

	private static Profiles profile = null;

	public boolean isWiki() {
		return !Util.getWiki().isEmpty();
	}

	public String getWikiURL() {
		return Util.getWiki();
	}

	private static String casHostName = "";
	private static boolean CASAvailable = false;
	private static String casLogoutURL = "";

	private static Path runDirectory;
	private static Logger log = null;

	private SelectItem[] documents;
	private Map<String, DocumentValidator> validators = new HashMap<>();
	private ArrayList<SelectItem> documentBuilderTypes;

	// ----------------------------------- Filter for visible directories
	private DirectoryStream.Filter<Path> filterDir = new DirectoryStream.Filter<Path>() {
		public boolean accept(Path file) throws IOException {
			if (Files.isDirectory(file)
					&& !file.getFileName().toString().startsWith("."))
				return true;
			return false;
		}
	};

	public ApplicationBean() {

		try {
			Util.initializeApplication(APPLICATION_NAME);
			runDirectory = Util.getRunDirectory();
			log = Util.getLog();

			log.debug("Initializing ApplicationBean");

			try {
				profile = Profiles.valueOf(Util.getProfileString()
						.toUpperCase());
			} catch (IllegalArgumentException | NullPointerException ex) {
				throw new Exception("Unsupported profile "
						+ Util.getProfileString());
			}

			/*
			 * Checks that CAS login host is accessible on port 80. If so, turns
			 * on CAS login and gets CAS logout URL
			 */
			if (profile != Profiles.DEV) {
				String casHostname = Util.getParameterString("CASHostName",
						"ihe.wustl.edu");
				try (Socket s = new Socket(casHostname, 80)) {
					String hostIp = s.getLocalAddress().getHostAddress();
					CASAvailable = true;
					log.info("CAS login available");
					casLogoutURL = "http://" + casHostname
							+ "/cas/logout?service=http%3A%2F%2F" + hostIp
							+ "%2F" + Util.getApplicationContext() + "%2F";
				} catch (Exception e) {
					log.warn("CAS login not available: " + e.getMessage());
				}
			}

			initializeDocuments();
			initializePatientIDs();
			initializeDocTypes();
			initializeFormatCodes();
			initializeTypeCodes();
			initializeClassCodes();

			initializeXDSMetadata();

			initializeContentCreators();
			initializeProvideAndRegisterEndpoints();
			initializeDB();
			initializeExport();
			initializeBuilderMenu();
			initializeExporterThread();
			log.info(Util.getApplicationContext()
					+ " ApplicationBean initialized");
			instance = this;
		} catch (Exception e) {
			log.error("Couldn't instatiate ApplicationBean: " + e.getMessage());
			System.exit(1);
		}
	} // EO constructor

	/**
	 * Validates connections to application database.<ul>
	 * <li/>Creates application database user if it does not exist.
	 * <li/>If the application database does not exist, creates it and all tables. Note:
	 * if the database exists, all tables in the database are assumed to exist.<ul/>
	 * 
	 * @throws Exception on error
	 */
	private void initializeDB() throws Exception {
		String dbName = DBUtil.DBNAME;
		DataBaseConnection dbc = DataBaseConnection
				.getDataBaseConnection(dbName);

		if (!DBUtil.userExists(dbc))
			DBUtil.createUser(dbc, true);

		if (!DBUtil.databaseExists(dbc)) {
			DBUtil.createDatabase(dbc);
			new Query(DBUtil.CREATE_CDA_DOC_STORE_TABLE).dbUpdates(dbc);
			new Query(DBUtil.CREATE_CDA_EXPORT_INFO_TABLE).dbUpdates(dbc);
			new Query(DBUtil.CREATE_CDA_EXPORT_FILES_TABLE).dbUpdates(dbc);
			new Query(DBUtil.CREATE_CDA_BUILDER_DOCUMENTS_TABLE).dbUpdates(dbc);
			new Query(DBUtil.CREATE_CDA_BUILDER_TEMPLATEIDS_TABLE).dbUpdates(dbc);
			new Query(DBUtil.CREATE_CDA_BUILDER_SELECTIONS_TABLE).dbUpdates(dbc);
			new Query(DBUtil.CREATE_CDA_XDSB_TABLE).dbUpdates(dbc);
			initializeBuilderDB();
			initializeXDSBDB();
		}

		Util.dbClose(dbc);
		log.info("Startup DB check OK");
	}

	/**
	 * Validates CDA gold standard documents and assertion files as much as
	 * possible, and loads them.
	 * 
	 * @throws Exception
	 *             on error
	 */
	private void initializeDocuments() throws Exception {

		int documentsCounter = 0;
		int validDocumentsCounter = 0;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		XPath xpath = XPathFactory.newInstance().newXPath();

		List<SelectItem> selectItems = new ArrayList<>();

		// ----------------------- Validate Document Directory
		Path documentDir = runDirectory.resolve(Util.getParameterString(
				"DocumentDir", "Documents"));
		String pre = "CDA test documents directory ";
		Util.isValidPfn(pre, documentDir, true, "r");
		log.debug(pre + documentDir.getFileName().toString() + " validated");

		// ----------------------- Validate Assertions Directory
		Path assertionDir = runDirectory.resolve(Util.getParameterString(
				"AssertionDir", "Assertions"));
		pre = "CDA assertions directory";
		Util.isValidPfn(pre, assertionDir, true, "r");
		log.debug(pre + assertionDir.getFileName().toString() + " validated");

		// --- Process directories under the Document Directory
		log.trace("Validating CDA document test directories.");
		try (DirectoryStream<Path> docDirs = Files.newDirectoryStream(
				documentDir, filterDir)) {
			for (Path docDir : docDirs) {
				documentsCounter++;
				String docString = null;
				Document docDoc = null;
				Path asr = null;

				String docName = docDir.getFileName().toString();
				log.debug("**** Document: " + docName);

				DocumentValidator docValidator = new DocumentValidator();
				docValidator.setDocumentName(docName);
				boolean assertionFileErrorsFound = false;

				// --------------- is directory accessible?
				try {
					Util.isValidPfn("CDA Document dir", docDir, true, "r");

					// ------- get one and only one document file
					Path doc = Util.getOneAndOnlyFile("Document File", docDir,
							"*.xml");
					// -------- parse the document file
					try {
						byte[] docBytes = Files.readAllBytes(doc);
						docString = docBytes.toString();
						docDoc = db.parse(new ByteArrayInputStream(docBytes));
						// --- pretty print doc if we can
						String goldDoc = Util.prettyPrintXML(docDoc);
						if (goldDoc == null)
							goldDoc = docString;
						docValidator.setGoldDoc(nl + goldDoc);
					} catch (Exception ep) {
						throw new Exception("Document File "
								+ doc.getFileName().toString()
								+ " has parsing error(s) " + ep.getMessage());
					}

					// ---- get one and only one assertions file
					asr = Util.getOneAndOnlyFile("Assertions File", docDir,
							"*.txt");
					if (!Files.isReadable(asr))
						throw new Exception("Assertions file "
								+ asr.getFileName().toString()
								+ " is not readable");

					// ---------- load the file
					List<String> lines = Files.readAllLines(asr,
							Charset.defaultCharset());

					// ------- process text lines in file
					for (String line : lines) {

						// --------- remove leading & trailing whitespace
						line = line.trim();

						// ---- comments starting with semicolon are internal
						// (ignored).
						if (line.startsWith(";"))
							continue;

						// --- comments starting with # & blank lines are passed
						// on.
						if (line.startsWith("#") || line.startsWith("@")
								|| line.isEmpty()) {
							docValidator.addAssertion(new Assertion(line));
							continue;
						}

						// ------------------------ split command lines on tab
						String[] fields = line.split("\\t");
						String cmd = fields[0].trim().toUpperCase();

						// ------------ Regular commands, case insensitive
						if (!Util.isOneOf(cmd, "EQ", "PRESENT", "SIMILAR",
								"INSERT")) {
							log.warn("Invalid command code [" + cmd
									+ "] in assertion file: "
									+ asr.getFileName() + " for document "
									+ docDir.getFileName());
							assertionFileErrorsFound = true;
							continue;
						}
						// ---------- need to have argument
						if (fields.length < 2) {
							log.warn("No arguments in assertion [" + line
									+ "] in assertion file: "
									+ asr.getFileName() + " for document "
									+ docDir.getFileName());
							assertionFileErrorsFound = true;
							continue;
						}

						// ------------ commands other than insert
						XPathExpression xpe = null;
						Node node = null;
						String em = " in assertion file: " + asr.getFileName()
								+ " for document " + docDir.getFileName();
						if (!cmd.equals("INSERT")) {

							// -------- validate xpath expression can compile
							try {
								xpe = xpath.compile(fields[1]);
							} catch (XPathExpressionException
									| NullPointerException xce) {
								log.warn("XPath compile error in assertion ["
										+ line + "]" + em + " - "
										+ xce.getMessage());
								assertionFileErrorsFound = true;
								continue;
							}
							// --- Validate that gold msg has referenced node
							try {
								node = (Node) xpe.evaluate(docDoc,
										XPathConstants.NODE);
								if (node == null)
									throw new Exception("node not in gold doc");
							} catch (Exception xee) {
								log.warn("XPath evaluation error in assertion ["
										+ line
										+ "]"
										+ em
										+ " - "
										+ xee.getMessage());
								assertionFileErrorsFound = true;
								continue;
							}
							String comment = "";
							if (fields.length > 2)
								comment = fields[2];
							Assertion a = new Assertion(line,
									Command.valueOf(cmd), xpe, node, fields[1],
									comment);
							docValidator.addAssertion(a);
							continue;

						} // EO cmd other than insert

						// ----------------- Insert command
						if (cmd.equals("INSERT")) {
							List<String> ilines = null;
							Path iFilePath = null;
							// -- validate insert file existence, readable,
							// read.
							try {
								if (!fields[1].endsWith(".txt"))
									fields[1] += ".txt";
								iFilePath = assertionDir.resolve(fields[1]);
								Util.isValidPfn("Insert file", iFilePath,
										false, "r");
								ilines = Files.readAllLines(iFilePath,
										Charset.defaultCharset());
							} catch (Exception ife) {
								log.warn("INSERT command error in assertion ["
										+ line + "]" + em + " - "
										+ ife.getMessage());
								assertionFileErrorsFound = true;
								continue;
							}
							// --------- process lines in insert file
							for (String iline : ilines) {
								iline = iline.trim();

								// ---- comment lines / blank lines
								if (iline.startsWith(";"))
									continue;
								if (iline.startsWith("#")
										|| iline.startsWith("@")
										|| iline.isEmpty()) {
									docValidator.addAssertion(new Assertion(
											iline));
									continue;
								}

								String[] ifields = iline.split("\\t");
								String icmd = ifields[0].trim().toUpperCase();

								em = " in insert file: "
										+ iFilePath.getFileName() + em;
								// -- Regular commands, case insensitive
								if (!Util.isOneOf(icmd, "EQ", "EQUALS",
										"PRESENT", "SIMILAR")) {
									log.warn("Invalid command code [" + icmd
											+ "] " + em);
									assertionFileErrorsFound = true;
									continue;
								}
								// ---------- need to have argument
								if (ifields.length < 2) {
									log.warn("No xpath in assertion [" + iline
											+ "] " + em);
									assertionFileErrorsFound = true;
									continue;

								}// ---- validate xpath expression can compile
								try {
									xpe = xpath.compile(ifields[1]);
								} catch (XPathExpressionException
										| NullPointerException xce) {
									log.warn("XPath compile error in assertion ["
											+ line
											+ "]"
											+ em
											+ " - "
											+ xce.getMessage());
									assertionFileErrorsFound = true;
									continue;
								}
								// Validate gold msg has referenced node
								try {
									node = (Node) xpe.evaluate(docDoc,
											XPathConstants.NODE);
								} catch (XPathExpressionException
										| IllegalArgumentException
										| NullPointerException xee) {
									log.warn("XPath evaluation error in assertion ["
											+ line
											+ "]"
											+ em
											+ " - "
											+ xee.getMessage());
									assertionFileErrorsFound = true;
									continue;
								}
								String comment = "";
								if (ifields.length > 2)
									comment = ifields[2];
								Assertion a = new Assertion(iline,
										Command.valueOf(icmd), xpe, node,
										ifields[1], comment);
								docValidator.addAssertion(a);
								continue;

							} // EO prs insert file lines

						} // EO process insert command

					} // EO pass assertion file lines
					if (assertionFileErrorsFound == true)
						throw new Exception("assertion errors found");

					validDocumentsCounter++;
					docValidator.init();
					selectItems.add(new SelectItem(docValidator
							.getDocumentName()));
					validators
							.put(docValidator.getDocumentName(), docValidator);

				} catch (Exception e) {
					log.warn("Skipping [" + docDir + "] " + e.getMessage());
				}

			} // EO iterate directories under document directory.
			log.info("CDA documents " + documentsCounter + nl + " valid "
					+ validDocumentsCounter);
			if (validDocumentsCounter == 0)
				throw new Exception("no valid documents found");
			if (documentsCounter > validDocumentsCounter) {
				String startWithErrors = Util.getParameterString(
						"RunAppWithSomeInvalidDocuments", "NO");
				if (startWithErrors.equalsIgnoreCase("NO"))
					throw new Exception(documentsCounter
							- validDocumentsCounter
							+ " invalid documents found.");
			}
			this.documents = selectItems.toArray(new SelectItem[0]);
			log.info("documents initialized");

		} // Try block around iterate directories

	} // EO initializeDocuments method

	private final String DOC_TYPES_PFN = "documentTypes.txt";
	private final String PATIENT_IDS_PFN = "ConnectathonPatientDemographicsNA2014.txt";
	private final String FORMAT_CODES_PFN = "formatCodes.txt";
	private final String TYPE_CODES_PFN = "typeCodes.txt";
	private final String CLASS_CODES_PFN = "classCodes.txt";
	private final String PNR_ENDPOINTS_PFN = "pnrEndpoints.txt";

	private SelectItem[] filterDocTypes = null;
	private SelectItem[] selectDocTypes = null;
	private SelectItem[] filterPatientIDs = null;
	private SelectItem[] selectPatientIDs = null;
	private SelectItem[] filterFormatCodes = null;
	private SelectItem[] selectFormatCodes = null;
	private SelectItem[] filterTypeCodes = null;
	private SelectItem[] selectTypeCodes = null;
	private SelectItem[] filterClassCodes = null;
	private SelectItem[] selectClassCodes = null;
	private SelectItem[] filterProvideAndRegisterEndpoints = null;
	private SelectItem[] selectProvideAndRegisterEndpoints = null;

	private Map<String, String> provideAndRegisterMap = null;
	private Map<String, String> mapSystemNameToPatientID = null;
	private Map<String, String> mapDocumentTypeToFormatCode = null;
	private Map<String, String> mapDocumentTypeToClassCode = null;
	private Map<String, String> mapDocumentTypeToTypeCode = null;

	private void initializePatientIDs() throws Exception {
		List<SelectItem> patids = new ArrayList<>();
		mapSystemNameToPatientID = new HashMap<String, String>();
		Path patidPath = runDirectory.resolve(PATIENT_IDS_PFN);
		Util.isValidPfn("patientID", patidPath, false, "r");
		List<String> lines = Files.readAllLines(patidPath,
				Charset.defaultCharset());
		for (String line : lines) {
			int pound = line.indexOf("#");
			if (pound >= 0)
				line = line.substring(0, pound);
			line = line.trim();
			if (line.isEmpty())
				continue;
			String[] tokens = line.split("\\t");
			if (!(tokens[3].equals("NA2014")))
				continue;
			line = tokens[0] + ", " + tokens[1];
			patids.add(new SelectItem(line, line));
			mapSystemNameToPatientID.put(tokens[1], line);
		}
		if (patids.isEmpty())
			throw new Exception("no Patient IDs");
		filterPatientIDs = new SelectItem[patids.size() + 1];
		filterPatientIDs[0] = new SelectItem(EMPTY, "Any");
		selectPatientIDs = new SelectItem[patids.size() + 1];
		selectPatientIDs[0] = new SelectItem(EMPTY, "-- Select Patient ID --");
		for (int i = 0; i < patids.size(); i++) {
			filterPatientIDs[i + 1] = patids.get(i);
			selectPatientIDs[i + 1] = patids.get(i);
		}
	}

	private void initializeDocTypes() throws Exception {
		List<SelectItem> dts = new ArrayList<>();
		Path dtsPath = runDirectory.resolve(DOC_TYPES_PFN);
		Util.isValidPfn("document type names", dtsPath, false, "r");
		List<String> lines = Files.readAllLines(dtsPath,
				Charset.defaultCharset());
		for (String line : lines) {
			int pound = line.indexOf("#");
			if (pound >= 0)
				line = line.substring(0, pound);
			line = line.trim();
			if (line.isEmpty())
				continue;
			dts.add(new SelectItem(line, line));
		}
		if (dts.isEmpty())
			throw new Exception("no document type names");
		filterDocTypes = new SelectItem[dts.size() + 1];
		filterDocTypes[0] = new SelectItem(EMPTY, "Any");
		selectDocTypes = new SelectItem[dts.size() + 1];
		selectDocTypes[0] = new SelectItem(EMPTY, "-- Select Document Type --");
		for (int i = 0; i < dts.size(); i++) {
			filterDocTypes[i + 1] = dts.get(i);
			selectDocTypes[i + 1] = dts.get(i);
		}
	}

	private void initializeFormatCodes() throws Exception {
		List<SelectItem> fcs = new ArrayList<>();
		Path fcsPath = runDirectory.resolve(FORMAT_CODES_PFN);
		Util.isValidPfn("formatCode", fcsPath, false, "r");
		List<String> lines = Files.readAllLines(fcsPath,
				Charset.defaultCharset());
		for (String line : lines) {
			int pound = line.indexOf("#");
			if (pound >= 0)
				line = line.substring(0, pound);
			line = line.trim();
			if (line.isEmpty())
				continue;
			fcs.add(new SelectItem(line, line));
		}
		if (fcs.isEmpty())
			throw new Exception("no formatCodes");
		filterFormatCodes = new SelectItem[fcs.size() + 1];
		filterFormatCodes[0] = new SelectItem(EMPTY, "Any");
		selectFormatCodes = new SelectItem[fcs.size() + 1];
		selectFormatCodes[0] = new SelectItem(EMPTY, "-- Select formatCode --");
		for (int i = 0; i < fcs.size(); i++) {
			filterFormatCodes[i + 1] = fcs.get(i);
			selectFormatCodes[i + 1] = fcs.get(i);
		}
	}

	private void initializeTypeCodes() throws Exception {
		List<SelectItem> tcs = new ArrayList<>();
		Path tcsPath = runDirectory.resolve(TYPE_CODES_PFN);
		Util.isValidPfn("typeCode", tcsPath, false, "r");
		List<String> lines = Files.readAllLines(tcsPath,
				Charset.defaultCharset());
		for (String line : lines) {
			int pound = line.indexOf("#");
			if (pound >= 0)
				line = line.substring(0, pound);
			line = line.trim();
			if (line.isEmpty())
				continue;
			tcs.add(new SelectItem(line, line));
		}
		if (tcs.isEmpty())
			throw new Exception("no typeCodes");
		filterTypeCodes = new SelectItem[tcs.size() + 1];
		filterTypeCodes[0] = new SelectItem(EMPTY, "Any");
		selectTypeCodes = new SelectItem[tcs.size() + 1];
		selectTypeCodes[0] = new SelectItem(EMPTY, "-- Select typeCode --");
		for (int i = 0; i < tcs.size(); i++) {
			filterTypeCodes[i + 1] = tcs.get(i);
			selectTypeCodes[i + 1] = tcs.get(i);
		}
	}

	private void initializeClassCodes() throws Exception {
		List<SelectItem> ccs = new ArrayList<>();
		Path ccsPath = runDirectory.resolve(CLASS_CODES_PFN);
		Util.isValidPfn("classCode", ccsPath, false, "r");
		List<String> lines = Files.readAllLines(ccsPath,
				Charset.defaultCharset());
		for (String line : lines) {
			int pound = line.indexOf("#");
			if (pound >= 0)
				line = line.substring(0, pound);
			line = line.trim();
			if (line.isEmpty())
				continue;
			ccs.add(new SelectItem(line, line));
		}
		if (ccs.isEmpty())
			throw new Exception("no classCodes");
		filterClassCodes = new SelectItem[ccs.size() + 1];
		filterClassCodes[0] = new SelectItem(EMPTY, "Any");
		selectClassCodes = new SelectItem[ccs.size() + 1];
		selectClassCodes[0] = new SelectItem(EMPTY, "-- Select classCode --");
		for (int i = 0; i < ccs.size(); i++) {
			filterClassCodes[i + 1] = ccs.get(i);
			selectClassCodes[i + 1] = ccs.get(i);
		}
	}

	private void initializeProvideAndRegisterEndpoints() throws Exception {
		Path pnrPath = runDirectory.resolve(PNR_ENDPOINTS_PFN);
		Util.isValidPfn("provideAndRegisterEndpoints", pnrPath, false, "r");
		provideAndRegisterMap = new HashMap<String, String>();
		List<String> lines = Files.readAllLines(pnrPath,
				Charset.defaultCharset());
		List<String> culledLines = this.cullTextLines(lines);
		if (culledLines.size() == 0)
			throw new Exception("no Provide and Register Endpoints");

		filterProvideAndRegisterEndpoints = new SelectItem[culledLines.size() + 1];
		filterProvideAndRegisterEndpoints[0] = new SelectItem(EMPTY, "None");
		selectProvideAndRegisterEndpoints = new SelectItem[culledLines.size() + 1];
		selectProvideAndRegisterEndpoints[0] = new SelectItem(EMPTY,
				"-- Select Provide and Register Endpoint --");

		for (int i = 0; i < culledLines.size(); i++) {
			String thisEndpoint = culledLines.get(i);
			String[] endpointTokens = thisEndpoint.split("\t");
			// [0] = human readable name
			// [1] = URI
			filterProvideAndRegisterEndpoints[i + 1] = new SelectItem(
					endpointTokens[0], endpointTokens[0]);
			selectProvideAndRegisterEndpoints[i + 1] = new SelectItem(
					endpointTokens[0], endpointTokens[0]);
			provideAndRegisterMap.put(endpointTokens[0], endpointTokens[1]);
		}
	}

	// ----------------------------------------------------------------------
	// organization / system processing
	// filterOrganizations is organizations plus "Any", used for filtering
	// selectOrganizations is organizations plus "Select org", used to force
	// user selection
	// ----------------------------------------------------------------------

	private SelectItem[] filterOrganizations = null;
	private SelectItem[] selectOrganizations = null;
	private Map<String, SelectItem[]> systems = null;
	private SelectItem[] selectExportType;
	private HashMap<String, ArrayList<SelectItem>> XDSBMenus;

	/**
	 * Initializes export menu
	 */
	private void initializeExport() {
		// select export type menu
		List<SelectItem> exportSet = new ArrayList<SelectItem>();
		exportSet.add(new SelectItem("empty", "--Select an Export Type--"));

		// Turned off for 2014 Connectathon because it wasn't complete
		// exportSet.add(new SelectItem("Email"));

		exportSet.add(new SelectItem("XDS.b"));
		exportSet.add(new SelectItem("Download"));
		selectExportType = exportSet.toArray(new SelectItem[0]);

		XDSBMenus = initializeXDSB();
	}

	/**
	 * Initializes XDSB menus
	 * 
	 * @return map of XDSB codes and their option menus
	 */
	private HashMap<String, ArrayList<SelectItem>> initializeXDSB() {
		ArrayList<String> XDSBInputs = new ArrayList<String>(Arrays.asList(
				"classCode", "confidentialityCode", "formatCode",
				"healthcareFacilityTypeCode", "mimeType",
				"practiceSettingCode", "typeCode"));
		HashMap<String, ArrayList<SelectItem>> XDSBMenus = new HashMap<String, ArrayList<SelectItem>>();
		for (String codeType : XDSBInputs) {
			ArrayList<SelectItem> codes = exportXDSBRetrieve(codeType);
			XDSBMenus.put(codeType, codes);
		}
		return XDSBMenus;
	}

	/**
	 * Retrieves XDSB option menu items from the database.
	 * 
	 * @return ArrayList of the selectItems for the given XDSB code.
	 */
	private ArrayList<SelectItem> exportXDSBRetrieve(String codeType) {
		ArrayList<SelectItem> codes = new ArrayList<SelectItem>();
		codes.add(new SelectItem("empty", "--" + codeType + "--"));
		try (DataBaseConnection dbc = DataBaseConnection
				.getDataBaseConnection(DBUtil.DBNAME)) {
			ResultSet rs = new Query(DBUtil.CDA_XDSB_RETRIEVE).set(
					"codeTypeName", codeType).dbQuery(dbc);
			while (rs.next()) {
				String code = rs.getString("codeCode");
				String display = rs.getString("codeDisplay");
				codes.add(new SelectItem(code + ": " + display));
			}
			return codes;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Initializes Content Creators using Excel spreadsheet
	 * 
	 * @throws Exception
	 */
	private void initializeContentCreators() throws Exception {

		// --------- Validate content creators spreadsheet file in runDirectory
		String pfn = Util.getParameterString("ContentCreatorsSpreadsheetName");
		if (StringUtils.isBlank(pfn))
			throw new Exception("ContentCreatorsSpreadsheetName not found in "
					+ APPLICATION_NAME + ".ini file");
		if (!(pfn.endsWith(".xlsx") | pfn.endsWith(".xls"))) {
			pfn += ".xlsx";
		}
		Path ccPath = runDirectory.resolve(pfn);
		Util.isValidPfn("Content Creators List", ccPath, false, "r");

		Workbook wb = new XSSFWorkbook(new FileInputStream(ccPath.toFile()));
		Sheet s = wb.getSheetAt(0);
		List<SelectItem> orgs = new ArrayList<>();
		Map<String, List<SelectItem>> syss = new HashMap<>();
		int numRows = 0;
		for (Row r : s) {
			if (r.getRowNum() == 0)
				continue;
			String org = r.getCell(0).getStringCellValue();
			String sys = r.getCell(1).getStringCellValue();
			if (StringUtils.isBlank(org) || StringUtils.isBlank(sys))
				continue;
			numRows++;

			// ------- get list of systems for this org, if any
			List<SelectItem> ss = syss.get(org);
			// -------------- none (this is first one)
			if (ss == null) {
				orgs.add(new SelectItem(org, org));
				ss = new ArrayList<>();
				syss.put(org, ss);
			}
			ss.add(new SelectItem(sys, sys));
		} // EO pass rows
			// ---- Generate final forms of SelectItems
		filterOrganizations = new SelectItem[orgs.size() + 1];
		filterOrganizations[0] = new SelectItem(EMPTY, "Any");
		selectOrganizations = new SelectItem[orgs.size() + 1];
		selectOrganizations[0] = new SelectItem(EMPTY,
				"-- Select Organization --");
		for (int i = 0; i < orgs.size(); i++) {
			filterOrganizations[i + 1] = orgs.get(i);
			selectOrganizations[i + 1] = orgs.get(i);
		}
		systems = new HashMap<>();
		for (Map.Entry<String, List<SelectItem>> e : syss.entrySet()) {
			systems.put(e.getKey(), e.getValue().toArray(new SelectItem[0]));
		}
		log.info(orgs.size() + " CDA organizations, " + numRows
				+ " systems initialized");

	} // EO initializeContentCreators method

	/**
	 * Initializes XDS.b metadata using Excel spreadsheet
	 * 
	 * @throws Exception
	 */
	private void initializeXDSMetadata() throws Exception {

		// --------- Validate content creators spreadsheet file in runDirectory
		String pfn = Util.getParameterString("XDSbMetadataSpreadsheetName");
		if (StringUtils.isBlank(pfn))
			throw new Exception("XDSbMetadataSpreadsheetName not found in "
					+ APPLICATION_NAME + ".ini file");
		if (!(pfn.endsWith(".xlsx") | pfn.endsWith(".xls"))) {
			pfn += ".xlsx";
		}
		Path ccPath = runDirectory.resolve(pfn);
		log.debug("ApplicationBean::initializeXDSMetadata path to spreadsheet: "
				+ ccPath.toString());
		Util.isValidPfn("XDS.b Metadata Spreadsheet", ccPath, false, "r");

		Workbook wb = new XSSFWorkbook(new FileInputStream(ccPath.toFile()));
		Sheet s = wb.getSheetAt(0);
		List<SelectItem> docTypeNames = new ArrayList<>();
		SortedSet<String> formatCodes = new TreeSet<String>();
		SortedSet<String> classCodes = new TreeSet<String>();
		SortedSet<String> typeCodes = new TreeSet<String>();

		mapDocumentTypeToFormatCode = new HashMap<String, String>();
		mapDocumentTypeToClassCode = new HashMap<String, String>();
		mapDocumentTypeToTypeCode = new HashMap<String, String>();

		int numRows = 0;
		for (Row r : s) {
			if (r.getRowNum() == 0)
				continue;
			String domain = r.getCell(0).getStringCellValue();
			String status = r.getCell(1).getStringCellValue();
			String docNam = r.getCell(2).getStringCellValue();
			if (StringUtils.isBlank(domain) || StringUtils.isBlank(domain))
				continue;
			if (domain.startsWith("#"))
				continue;

			String fullName = domain + ", " + status + ", " + docNam;
			log.debug("Full Document Name: " + fullName);

			String typeCode = r.getCell(5).getStringCellValue();
			String typeCodeDisplay = r.getCell(6).getStringCellValue();
			String typeCodeCodingScheme = r.getCell(7).getStringCellValue();
			String fullTypeCode = typeCode + ", " + typeCodeCodingScheme + ", "
					+ typeCodeDisplay;

			String formatCode = r.getCell(8).getStringCellValue();
			String formatCodeCodingScheme = r.getCell(9).getStringCellValue();
			String fullFormatCode = formatCode + ", " + formatCodeCodingScheme;

			String classCode = r.getCell(10).getStringCellValue();
			String classCodeDisplay = r.getCell(11).getStringCellValue();
			String classCodeCodingScheme = r.getCell(12).getStringCellValue();
			String fullClassCode = classCode + ", " + classCodeCodingScheme
					+ ", " + classCodeDisplay;

			docTypeNames.add(new SelectItem(fullName, fullName));
			if (!typeCode.startsWith("*"))
				typeCodes.add(fullTypeCode);
			if (!formatCode.startsWith("*"))
				formatCodes.add(fullFormatCode);
			if (!classCode.startsWith("*"))
				classCodes.add(fullClassCode);
			mapDocumentTypeToTypeCode.put(fullName, new String(fullTypeCode));
			mapDocumentTypeToFormatCode.put(fullName,
					new String(fullFormatCode));
			mapDocumentTypeToClassCode.put(fullName, new String(fullClassCode));
			numRows++;
		} // EO pass rows
			// ---- Generate final forms of SelectItems

		filterDocTypes = new SelectItem[docTypeNames.size() + 1];
		filterDocTypes[0] = new SelectItem(EMPTY, "Any");
		selectDocTypes = new SelectItem[docTypeNames.size() + 1];
		selectDocTypes[0] = new SelectItem(EMPTY, "-- Select Document Type --");
		for (int i = 0; i < docTypeNames.size(); i++) {
			filterDocTypes[i + 1] = docTypeNames.get(i);
			selectDocTypes[i + 1] = docTypeNames.get(i);
		}

		filterTypeCodes = new SelectItem[typeCodes.size() + 1];
		filterTypeCodes[0] = new SelectItem(EMPTY, "Any");
		selectTypeCodes = new SelectItem[typeCodes.size() + 1];
		selectTypeCodes[0] = new SelectItem(EMPTY, "-- Select typeCode --");
		int idx = 0;
		for (String x : typeCodes) {
			filterTypeCodes[idx + 1] = new SelectItem(x, x);
			selectTypeCodes[idx + 1] = new SelectItem(x, x);
			idx++;
		}

		filterFormatCodes = new SelectItem[formatCodes.size() + 1];
		filterFormatCodes[0] = new SelectItem(EMPTY, "Any");
		selectFormatCodes = new SelectItem[formatCodes.size() + 1];
		selectFormatCodes[0] = new SelectItem(EMPTY, "-- Select formatCode --");
		idx = 0;
		for (String x : formatCodes) {
			filterFormatCodes[idx + 1] = new SelectItem(x, x);
			selectFormatCodes[idx + 1] = new SelectItem(x, x);
			idx++;
		}

		filterClassCodes = new SelectItem[classCodes.size() + 1];
		filterClassCodes[0] = new SelectItem(EMPTY, "Any");
		selectClassCodes = new SelectItem[classCodes.size() + 1];
		selectClassCodes[0] = new SelectItem(EMPTY, "-- Select classCode --");
		idx = 0;
		for (String x : classCodes) {
			filterClassCodes[idx + 1] = new SelectItem(x, x);
			selectClassCodes[idx + 1] = new SelectItem(x, x);
			idx++;
		}

		log.info(docTypeNames.size() + " Document Types, " + numRows
				+ " initialized");
		log.info("typeCodes:   " + typeCodes.size());
		log.info("formatCodes: " + formatCodes.size());
		log.info("classCodes:  " + classCodes.size());
	} // EO initializeXDSMetadata method

	public static ApplicationBean getInstance() {
		return instance;
	}

	public static boolean isCASAvailable() {
		return CASAvailable;
	}

	public String casLogoff() {
		log.trace("casLogoff() called: " + casLogoutURL);
		if (casHostName.isEmpty())
			FacesUtil.closeSession(Util.getHomeURL());
		else
			FacesUtil.closeSession(casLogoutURL);
		return null;
	}

	/**
	 * Creates menu of document types for the document builder.
	 */
	private void initializeBuilderMenu() {
		try (DataBaseConnection dbc = DataBaseConnection
				.getDataBaseConnection(DBUtil.DBNAME)) {
			ResultSet documents = new Query(
					DBUtil.CDA_BUILDER_DOCUMENTS_RETRIEVE).dbQuery(dbc);
			ArrayList<String> names = new ArrayList<String>();
			while (documents.next()) {
				names.add(documents.getString("document"));
			}
			Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
			documentBuilderTypes = new ArrayList<SelectItem>();
			documentBuilderTypes.add(new SelectItem("empty",
					"--Select a Document Template"));
			for (String name : names) {
				documentBuilderTypes.add(new SelectItem(name));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Inserts the documents, sections, and XML section and header codes into
	 * the database.
	 */
	private void initializeBuilderDB() {
		CDABuilderExcelDBInsert ep = new CDABuilderExcelDBInsert();
		ep.parse();
		CDABuilderXMLDBInsert bxi = new CDABuilderXMLDBInsert();
		bxi.insert();
		bxi.insertHeaders();

	}

	/**
	 * Inserts the XDSB codes and their options into the database.
	 */
	private void initializeXDSBDB() {
		Path runDir = Util.getRunDirectory();
		Path other = Paths.get("XML", "codes.xml");
		Path codesPath = runDir.resolve(other);
		XDSBCodesDBInsert parser = new XDSBCodesDBInsert(new File(
				codesPath.toString()));
		parser.parse();
	}

	private List<String> cullTextLines(List<String> inputLines) {
		List<String> outputLines = new ArrayList<String>();
		for (String line : inputLines) {
			int poundIndex = line.indexOf("#");
			if (poundIndex >= 0)
				line = line.substring(0, poundIndex);
			line = line.trim();
			if (line.isEmpty())
				continue;
			outputLines.add(new String(line));
		}
		return outputLines;
	}

	private void initializeExporterThread() {
		new Thread(new ExporterThread()).start();
	}

	// ------------------------------------------------
	// Getters and Setters
	// ------------------------------------------------

	public String getApplicationName() {
		return APPLICATION_NAME;
	}

	public static Path getRunDirectory() {
		return runDirectory;
	}

	public String getDisplayName() {
		return Util.getDisplayName();
	}

	public SelectItem[] getDocuments() {
		return documents;
	}

	public void setDocuments(SelectItem[] docs) {
		this.documents = docs;
	}

	public DocumentValidator getValidator(String name) {
		return validators.get(name);
	}

	public DocumentValidator[] getValidators() {
		return validators.values().toArray(new DocumentValidator[0]);
	}

	public SelectItem[] getFilterOrganizations() {
		return filterOrganizations;
	}

	public SelectItem[] getSelectOrganizations() {
		return selectOrganizations;
	}

	public SelectItem[] getFilterPatientIDs() {
		return filterPatientIDs;
	}

	public SelectItem[] getSelectPatientIDs() {
		return selectPatientIDs;
	}

	public String getPatientIDFromSystemName(String systemName) {
		log.debug("ApplicationBean::getPatientIDFromSystemName: " + systemName);
		log.debug("  Returned Patient ID: <"
				+ mapSystemNameToPatientID.get(systemName) + ">");
		return mapSystemNameToPatientID.get(systemName);
	}

	public SelectItem[] getFilterDocTypes() {
		return filterDocTypes;
	}

	public SelectItem[] getSelectDocTypes() {
		return selectDocTypes;
	}

	public SelectItem[] getFilterFormatCodes() {
		return filterFormatCodes;
	}

	public SelectItem[] getSelectFormatCodes() {
		return selectFormatCodes;
	}

	public SelectItem[] getFilterTypeCodes() {
		return filterTypeCodes;
	}

	public SelectItem[] getSelectTypeCodes() {
		return selectTypeCodes;
	}

	public SelectItem[] getFilterClassCodes() {
		return filterClassCodes;
	}

	public SelectItem[] getSelectClassCodes() {
		return selectClassCodes;
	}

	public SelectItem[] getFilterProvideAndRegisterEndpoints() {
		return filterProvideAndRegisterEndpoints;
	}

	public SelectItem[] getSelectProvideAndRegisterEndpoints() {
		return selectProvideAndRegisterEndpoints;
	}

	public boolean profileIs(Profiles p) {
		return profile == p;
	}

	public SelectItem[] getFilterOrganizationSystems(String organization) {
		SelectItem[] os = systems.get(organization);
		SelectItem[] ros = new SelectItem[os.length + 1];
		ros[0] = new SelectItem(EMPTY, "Any");
		for (int i = 0; i < os.length; i++)
			ros[i + 1] = os[i];
		return ros;
	}

	public SelectItem[] getSelectOrganizationSystems(String organization) {
		SelectItem[] os = systems.get(organization);
		SelectItem[] ros = new SelectItem[os.length + 1];
		ros[0] = new SelectItem(EMPTY, "-- Select System -- ");
		for (int i = 0; i < os.length; i++)
			ros[i + 1] = os[i];
		return ros;
	}

	public static String getCasLogoutURL() {
		return casLogoutURL; // + "?faces-redirect=true";
	}

	public static String getCasHostName() {
		return casHostName;
	}

	public ArrayList<SelectItem> getDocumentBuilderTypes() {
		return documentBuilderTypes;
	}

	public void setDocumentBuilderTypes(
			ArrayList<SelectItem> documentBuilderTypes) {
		this.documentBuilderTypes = documentBuilderTypes;
	}

	public SelectItem[] getSelectExportType() {
		return selectExportType;
	}

	public void setSelectExportType(SelectItem[] selectExportType) {
		this.selectExportType = selectExportType;
	}

	public HashMap<String, ArrayList<SelectItem>> getXDSBMenus() {
		return XDSBMenus;
	}

	public void setXDSBMenus(HashMap<String, ArrayList<SelectItem>> xDSBMenus) {
		XDSBMenus = xDSBMenus;
	}

	public String getProvideAndRegisterURI(String name) {
		if (provideAndRegisterMap.containsKey(name)) {
			return provideAndRegisterMap.get(name);
		} else {
			return null;
		}
	}

	public String getDocumentTypeToFormatCode(String documentType) {
		if (mapDocumentTypeToFormatCode.containsKey(documentType)) {
			return mapDocumentTypeToFormatCode.get(documentType);
		} else {
			return null;
		}
	}

	public String getDocumentTypeToClassCode(String documentType) {
		if (mapDocumentTypeToClassCode.containsKey(documentType)) {
			return mapDocumentTypeToClassCode.get(documentType);
		} else {
			return null;
		}
	}

	public String getDocumentTypeToTypeCode(String documentType) {
		if (mapDocumentTypeToTypeCode.containsKey(documentType)) {
			return mapDocumentTypeToTypeCode.get(documentType);
		} else {
			return null;
		}
	}

} // EO ApplicationBean class