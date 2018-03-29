package edu.wustl.mir.erl.IHETools.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads in the document and section names from an Excel spreadsheet and inserts them into the database.
 */
public class CDABuilderExcelDBInsert  implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static Logger log = Util.getLog();
	private Path xlPath;
	private Workbook wb;

	public CDABuilderExcelDBInsert(){
		Path sub = Paths.get("Excel", "CDA-Documents-Sections.xlsx");
		Path runDir = Util.getRunDirectory();
		xlPath = runDir.resolve(sub);
	}

	/**
	 * Reads the document and section types from an Excel spreadsheet.
	 */
	public void parse(){
		try{
			wb = WorkbookFactory.create(new File(xlPath.toString()));
			Sheet docs = wb.getSheetAt(0);
			Sheet docSec = wb.getSheetAt(2);

			for (Row row:docs){
				String docName = StringUtils.trimToNull(row.getCell(0).getStringCellValue());
				String docKeyName = StringUtils.trimToNull(row.getCell(1).getStringCellValue());
				String docTempId = StringUtils.trimToNull(row.getCell(2).getStringCellValue());
				if (docName==null||docKeyName==null)
					continue;
				insertDocuments(docName, docKeyName, docTempId);
			}

			for (Row row:docSec){
				if (row.getCell(0).getCellType()!=1||row.getCell(1).getCellType()!=1||row.getCell(2).getCellType()!=1)
					continue;
				String secKeyName = StringUtils.trimToNull(row.getCell(0).getStringCellValue());
				String secName = StringUtils.trimToNull(row.getCell(1).getStringCellValue());
				String secTempId = StringUtils.trimToNull(row.getCell(2).getStringCellValue());
				String secReq = StringUtils.trimToNull(row.getCell(4).getStringCellValue());
				if (secName==null||secKeyName==null||secTempId==null)
					continue;
				insertSections(secKeyName, secName, secTempId, secReq);
			}
		}catch(IOException e){
			log.error("Could not load CDA_Documents-Sections.xlsx");
			log.error(e.getMessage());
		}catch(InvalidFormatException e){
			log.error("Could not load CDA_Documents-Sections.xlsx");
			log.error(e.getMessage());
		}
	}

	/**
	 * Inserts documents into the database.
	 */
	private void insertDocuments(String name, String keyname, String tempId){
		try(DataBaseConnection dbc = 
				DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			new Query(DBUtil.CDA_BUILDER_DOCUMENTS_INSERT)
			.set("document", name)
			.set("keyname", keyname)
			.set("templateId", tempId)
			.dbUpdate(dbc);
		}catch(Exception e){
			log.error("Could not connect to database");
			log.error(e.getMessage());
		}
	}

	/**
	 * Inserts sections into the database.
	 */
	private void insertSections(String keyname, String name, String tempId, String req){
		try(DataBaseConnection dbc = 
				DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			new Query(DBUtil.CDA_BUILDER_TEMPLATEIDS_INSERT)
			.set("templateId", tempId)
			.set("section", name)
			.set("keyname", keyname)
			.set("opt", req)
			.dbUpdate(dbc);
		}catch(Exception e){
			log.error("Could not connect to database");
			log.error(e.getMessage());
		}
	}
}
