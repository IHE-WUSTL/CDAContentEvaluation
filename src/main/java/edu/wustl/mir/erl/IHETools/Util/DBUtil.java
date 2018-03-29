package edu.wustl.mir.erl.IHETools.Util;

import java.io.Serializable;
import java.sql.ResultSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
/**
 * Encapsulates JDBC data strings and methods specific to a particular DBMS.
 * In this case, postgresql.
 * @author rmoult01
 */
public class DBUtil implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public  static final String DRIVER_NAME   = "org.postgresql.Driver";
	public  static final String URL           = "jdbc:postgresql://localhost/";
	private static final String ROOT_DB       = "postgres";
	public  static final String DBNAME = "cda_doc_store";
	public static Logger syslog = null;
	private static DataBaseConnection rootDb = null;

	public static final String[] CREATE_CDA_DOC_STORE_TABLE = {
		"CREATE SEQUENCE seq_cda_doc_store_id START 1;",

		" CREATE TABLE cda_doc_store (" +
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_doc_store_id'), " +

			"organization          VARCHAR(64)  NOT NULL, " +
			"system                VARCHAR(64)  NOT NULL, " +
			"patient_id            VARCHAR(256) NOT NULL, " +
			"last_name             VARCHAR(64)  NOT NULL, " +
			"document_type         VARCHAR(256) NOT NULL, " +
			"format_code           VARCHAR(128) NOT NULL, " +
			"type_code             VARCHAR(128) NOT NULL, " +
			"type_code_designator  VARCHAR(128) NOT NULL, " +
			"type_code_display     VARCHAR(256) NOT NULL, " +
			"class_code            VARCHAR(128) NOT NULL, " +
			"class_code_designator VARCHAR(128) NOT NULL, " +
			"class_code_display    VARCHAR(256) NOT NULL, " +
			"notes                 TEXT NOT NULL DEFAULT '', " +
			"document              TEXT NOT NULL DEFAULT ''); ",

			"GRANT ALL ON cda_doc_store to GROUP public;"
	};

	public static final String[] CREATE_CDA_EXPORT_INFO_TABLE = {
		"CREATE SEQUENCE seq_cda_export_info_id START 1;",

		" CREATE TABLE cda_export_info (" + 
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_export_info_id'), " +

			"user_id					VARCHAR(64) NOT NULL, " +
			"document_id				INT         NOT NULL, " +
//			"documentType				VARCHAR(64) NOT NULL, " +
			"status						TEXT        NOT NULL, " +
			"export_type				TEXT        NOT NULL, " +
			"pnr_endpoint               VARCHAR(256),   " +
//			"numDocs					INT NOT NULL, " +
			"to_address					VARCHAR(128),   " +
			"subject					VARCHAR(128),   " +
			"body						VARCHAR(256),   " +
//			"patientId					VARCHAR(64),    " +
//			"sourcePatientId			VARCHAR(64),    " +
//			"classCode					VARCHAR(64),    " +
			"confidentiality_code		VARCHAR(64),    " +
//			"formatCode					VARCHAR(64),    " +
//			"healthCareFacilityTypeCode	VARCHAR(64),    " +
//			"homeCommunityId			VARCHAR(64),    " +
			"language_code				VARCHAR(10),    " +
			"mime_type					VARCHAR(64));   " +
//			"practiceSettingCode		VARCHAR(64),    " +
//			"typeCode					VARCHAR(64));   ",

			"GRANT ALL ON cda_export_info to GROUP public;"
	};

	public static final String[] CREATE_CDA_EXPORT_FILES_TABLE = {
		"CREATE SEQUENCE seq_cda_export_files_id START 1;",

		" CREATE TABLE cda_export_files (" + 
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_export_files_id'), " +

				"infoId					INT NOT NULL, " +
				"docPath				TEXT, " +
				"SDID					INT); ",

				"GRANT ALL ON cda_export_files to GROUP public;"
	};

	public static final String[] CREATE_CDA_BUILDER_TEMPLATEIDS_TABLE = {
		"CREATE SEQUENCE seq_cda_builder_templateids_id START 1;",

		" CREATE TABLE cda_builder_templateids (" +
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_builder_templateids_id'), " +

				"templateId				TEXT NOT NULL, " +
				"section				TEXT NOT NULL, "+
				"keyname				TEXT NOT NULL, "+
				"opt					TEXT NOT NULL);",
				
				"GRANT ALL ON cda_builder_templateids to GROUP public;"
	};

	public static final String[] CREATE_CDA_BUILDER_DOCUMENTS_TABLE = {
		"CREATE SEQUENCE seq_cda_builder_documents_id START 1;",

		" CREATE TABLE cda_builder_documents (" +
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_builder_documents_id'), "+

				"document				TEXT NOT NULL," +
				"keyname				TEXT NOT NULL," +
				"templateId				TEXT NOT NULL," +
				"header					TEXT);", 

				"GRANT ALL ON cda_builder_documents to GROUP public;"
	};
	
	public static final String[] CREATE_CDA_BUILDER_SELECTIONS_TABLE = {
		"CREATE SEQUENCE seq_cda_builder_selections_id START 1;",

		" CREATE TABLE cda_builder_selections (" +
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_builder_selections_id'), "+

				"type					TEXT NOT NULL, " +
				"XMLPath					TEXT, " +
				"templateId				TEXT NOT NULL);",

				"GRANT ALL ON cda_builder_selections to GROUP public;"
	};
	
	public static final String[] CREATE_CDA_XDSB_TABLE = {
		"CREATE SEQUENCE seq_cda_XDSB_id START 1;",

		" CREATE TABLE cda_XDSB (" +
				"id BIGINT PRIMARY KEY DEFAULT NEXTVAL('seq_cda_XDSB_id'), "+

				"codeTypeName					TEXT NOT NULL, " +
				"codeTypeClassScheme			TEXT NOT NULL, " +
				"codeCode						TEXT NOT NULL, " +
				"codeCodingScheme				TEXT NOT NULL, " +
				"codeDisplay					TEXT NOT NULL);", 
				
				"GRANT ALL ON cda_XDSB to GROUP public;"
	};

	public static final String[] CDA_DOC_STORE_INSERT = {
		"INSERT INTO cda_doc_store VALUES(DEFAULT, '${organization}', " +
				"'${system}', '${patientID}', '${lastName}', '${documentType}', " +
				"'${formatCode}', " +
				" '${typeCode}',  '${typeCodeDesignator}',  '${typeCodeDisplay}', " +
				"'${classCode}', '${classCodeDesignator}', '${classCodeDisplay}', " +
				"'${notes}', '${document}');"
	};

	public static final String[] CDA_EXPORT_INFO_INSERT = {
		"INSERT INTO cda_export_info VALUES(DEFAULT, " +
				"'${userId}',         " +
				"'${documentId}',     " +
//				"'${documentType}'," +
				"'${status}',         " +
				"'${exportType}',     " +
				"'${pnrEndpoint}',    " +
//				"${numDocs}," +
				"'${toAddress}',      " +
				"'${subject}',        " +
				"'${body}',           " +
//				"'${patientId}'," +
//				"'${sourcePatientId}'," +
//				"'${classCode}'," +
				"'${confidentialityCode}'," +
//				"'${formatCode}'," +
//				"'${healthCareFacilityTypeCode}'," +
//				"'${homeCommunityId}'," +
				"'${languageCode}',   " +
				"'${mimeType}'        " +
//				"'${practiceSettingCode}'," +
//				"'${typeCode}'        " +
				");"
	};

	public static final String[] CDA_EXPORT_FILES_INSERT = {
		"INSERT INTO cda_export_files VALUES(DEFAULT, " +
				"${infoId}," +
				"'${docPath}'," +
				"${SDID});"
	};

	public static final String[] CDA_BUILDER_TEMPLATEIDS_INSERT = {
		"INSERT INTO cda_builder_templateids VALUES(DEFAULT, " +
				"'${templateId}'," +
				"'${section}'," +
				"'${keyname}'," +
				"'${opt}');"
	};

	public static final String[] CDA_BUILDER_DOCUMENTS_INSERT = {
		"INSERT INTO cda_builder_documents VALUES(DEFAULT, " +
				"'${document}'," +
				"'${keyname}',"	+
				"'${templateId}');"
	};

	public static final String[] CDA_BUILDER_SELECTIONS_INSERT = {
		"INSERT INTO cda_builder_selections VALUES(DEFAULT, " +
				"'${type}'," +
				"'${XMLPath}'," +
				"'${templateId}');"	
	};
	
	public static final String[] CDA_XDSB_INSERT = {
		"INSERT INTO cda_XDSB VALUES(DEFAULT, " +
				"'${codeTypeName}'," +
				"'${codeTypeClassScheme}'," +
				"'${codeCode}'," +
				"'${codeCodingScheme}'," +
				"'${codeDisplay}');"
	};
	
	public static final String[] CDA_EXPORT_INFO_RETRIEVE = {
		"SELECT * FROM cda_export_info WHERE " +
				"documentType = '${documentType}' AND " +
				"status = '${status}' AND " +
				"exportType = '${exportType}';"
	};
	
	public static final String[] CDA_EXPORT_INFO_RETRIEVE_STATUS = {
		"SELECT * FROM cda_export_info WHERE " +
				"status = '${status}';"
	};

	public static final String[] CDA_EXPORT_FILES_RETRIEVE = {
		"SELECT * FROM cda_export_files WHERE infoId = ${infoId};"
	};

	public static final String[] CDA_EXPORT_SAMPLES_RETRIEVE = {
		"SELECT * FROM cda_doc_store WHERE id=${id};"
	};

	public static final String[] CDA_EXPORT_INFO_UPDATE_STATUS = {
		"UPDATE cda_export_info SET status='${status}' WHERE id = ${id};"
	};
	
	public static final String[] CDA_BUILDER_DOCUMENTS_RETRIEVE = {
		"SELECT * FROM cda_builder_documents;"
	};
	
	public static final String[] CDA_BUILDER_KEYNAMES_RETRIEVE = {
		"SELECT * FROM cda_builder_documents WHERE document='${document}';"
	};
	
	public static final String[] CDA_BUILDER_TEMPLATEIDS_RETRIEVE = {
		"SELECT * FROM cda_builder_templateids WHERE keyname='${keyname}';"
	};
	
	public static final String[] CDA_BUILDER_HEADER_RETRIEVE = {
		"SELECT * FROM cda_builder_documents WHERE document='${document}';"
	};
	
	public static final String[] CDA_BUILDER_SELECTIONS_RETRIEVE = {
		"SELECT * FROM cda_builder_selections WHERE templateId='${templateId}';"
	};
	
	public static final String[] CDA_BUILDER_XMLPath_RETRIEVE = {
		"SELECT * FROM cda_builder_selections WHERE type='${type}' AND templateId='${templateId}';"
	};

	public static final String[] CDA_XDSB_RETRIEVE= {
		"SELECT * FROM cda_XDSB WHERE codeTypeName='${codeTypeName}';"
	};
	
	public static final String[] CDA_BUILDER_DOCUMENTS_UPDATE= {
		"UPDATE cda_builder_documents SET header='${header}' WHERE document='${document}';"
	};
	
	public static final String[] CDA_DOC_STORE_DELETE = {
		"DELETE FROM cda_doc_store WHERE id = ${id};"
	};

	public static final String[] CDA_EXPORT_INFO_DELETE = {
		"DELETE FROM cda_export_info WHERE id = ${id};"
	};

	public static final String[] CDA_DOC_STORE_LID = {
		"SELECT currval('seq_cda_doc_store_id') as lid;"
	};

	public static final String[] CDA_EXPORT_INFO_LID = {
		"SELECT currval('seq_cda_export_info_id') as lid;"
	};

	public static final String[] CDA_EXPORT_FILES_LID = {
		"SELECT currval('seq_cda_export_files_id') as lid;"
	};

	public static final String[] CDA_BUILDER_TEMPLATEIDS_LID = {
		"SELECT currval('seq_cda_builder_templateids') as lid;"
	};
	public static final String[] CDA_BUILDER_DOCUMENTS_LID = {
		"SELECT currval('seq_cda_builder_documents') as lid;"
	};
	public static final String[] CDA_BUILDER_SELECTIONS_LID = {
		"SELECT currval('seq_cda_builder_selections') as lid;"
	};
	public static final String[] CDA_XDSB_LID = {
		"SELECT currval('seq_cda_XDSB') as lid;"
	};	

	public static int getCdaMsgStoreLid(DataBaseConnection c) {
		try {
			ResultSet r = new Query(CDA_DOC_STORE_LID).dbQuery(c);
			if (!r.next()) throw new Exception("no results");
			return r.getInt("lid");
		} catch (Exception e) {
			syslog.warn(CDA_DOC_STORE_LID + " error: " + e.getMessage());
			return -1;
		}
	}

	public static int getCdaExportInfoLid(DataBaseConnection c) {
		try {
			ResultSet r = new Query(CDA_EXPORT_INFO_LID).dbQuery(c);
			if (!r.next()) throw new Exception("no results");
			return r.getInt("lid");
		} catch (Exception e) {
			syslog.warn(CDA_EXPORT_INFO_LID + " error: " + e.getMessage());
			return -1;
		}
	}

	static {
		syslog = Util.getLog();
		try {
			rootDb = DataBaseConnection.getDataBaseConnection(ROOT_DB);
		} catch (Exception e) {
			syslog.warn("Could not access " + ROOT_DB + " database");
		}
	} // EO static init block


	/**
	 * Determines if the passed user exists in the DBMS.
	 * @param dbc DataBaseConnection containing user to check for.
	 * @return boolean true if user exists, false otherwise.
	 * @throws Exception on error.
	 */
	public static boolean userExists(DataBaseConnection dbc) throws Exception {
		String un = StringUtils.trimToEmpty(dbc.getUser());
		if (un.length() == 0) return false;
		ResultSet rs =
				new Query("select usename from pg_user where usename = lower('${user}');")
		.set("user", un)
		.dbQuery(DataBaseConnection.getDataBaseConnection(ROOT_DB));
		boolean exists = false;
		if (rs.next()) exists =  true;
		rs.close();
		return exists;
	}

	/**
	 * Creates database user IAW with passed data.
	 * @param DataBaseConnection; contains user and password
	 * @param createDB boolean should user have power to create databases.
	 * @throws Exception on error or if user name is invalid.
	 */
	public static void createUser(DataBaseConnection dbc,
			boolean createDB) throws Exception {
		String un = StringUtils.trimToEmpty(dbc.getUser());
		String pw = StringUtils.trimToEmpty(dbc.getPassword());
		if (un.length() == 0) throw new Exception("invalid user name");
		Query qry = new Query("create user ${name}").set("name", un);
		if (pw.length() != 0)
			qry.append(" password '${password}'").set("password", pw);
		if (createDB) qry.append(" CREATEDB");
		qry.append(";");
		qry.dbUpdate(DataBaseConnection.getDataBaseConnection(ROOT_DB));
	}

	/**
	 * Drops named user from DB.
	 * @param userName String user name to drop
	 * @throws Exception on error or if user name is invalid.
	 */
	public static void dropUser(String userName) throws Exception {
		String un = StringUtils.trimToEmpty(userName);
		if (un.length() == 0) throw new Exception("invalid user name");
		new Query("drop user ${name}").set("name", un).dbUpdate(rootDb);
	}

	/**
	 * Determines if the names database exists in the DBMS.
	 * @param dbName String name of the database to check for.
	 * @return boolean true if database exists, false otherwise.
	 * @throws Exception on error.
	 */
	public static boolean databaseExists(DataBaseConnection dbc) throws Exception {

		if (dbc.getPhysicalDbName() == null) return false;
		ResultSet rs =
				new Query("select datname from pg_database where datname = lower('${dbName}');")
		.set("dbName", dbc.getPhysicalDbName())
		.dbQuery(rootDb);
		boolean exists = false;
		if (rs.next()) exists =  true;
		rs.close();
		return exists;
	}

	/**
	 * Creates database with passed name.
	 * @param dbName Name of database to create
	 * @throws Exception on error.
	 */
	public static void createDatabase(DataBaseConnection dbc)
			throws Exception {
		if (dbc.getPhysicalDbName() == null) 
			throw new Exception("createDatabase error: dbName is null");
		new Query("CREATE DATABASE ${dbName} WITH OWNER ${user}")
		.set("dbName", dbc.getPhysicalDbName())
		.set("user", dbc.getUser())
		.dbUpdate(rootDb);
	}

	/**
	 * Drops passed database name (must not be in use)
	 * @param c DataBaseConnection
	 * @throws Exception on error
	 */
	public static void dropDatabase(DataBaseConnection c) throws Exception {
		Util.dbClose(c);
		new Query("drop database if exists '${dbName}'")
		.set("dbName", c.getDbName())
		.dbUpdate(rootDb);
	}

	/**
	 * Checks to see if passed db table exists
	 * @param c DataBaseConnection
	 * @param tblName name of table in dbName
	 * @return boolean, Does this table exists? <br/>
	 * NOTE: Does not distinguish between table not existing and whole db not
	 * existing.
	 */
	public static boolean tableExists(DataBaseConnection c, String tblName) {
		if (c.getDbName() == null || tblName == null) return false;
		try {
			ResultSet rs = new Query("Select * from ${tblName} limit 1")
			.set("tblName", tblName)
			.dbQuery(c);
			rs.close();
			return true;
		} catch (Exception e) {}
		return false;
	}

	public static void closeDb(DataBaseConnection c) {
		try { Util.dbClose(c); }
		catch (Exception e) {
			syslog.warn(e.getMessage());
		}
	}

	public static void beginTransaction(DataBaseConnection c) throws Exception {
		Util.dbUpdate(c, "begin;");
		syslog.debug(c.getDbName() + " begin transaction");

	}
	public static void commitTransaction(DataBaseConnection c) throws Exception {
		Util.dbUpdate(c, "commit;");
		syslog.debug(c.getDbName() + " commit transaction");
	}
	public static void rollbackTransaction(DataBaseConnection c) throws Exception {
		Util.dbUpdate(c, "rollback;");
		syslog.debug(c.getDbName() + " rollback transaction");

	}

} // EO DBUtil class
