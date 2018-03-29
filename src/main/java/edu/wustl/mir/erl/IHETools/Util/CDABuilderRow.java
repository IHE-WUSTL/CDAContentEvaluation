package edu.wustl.mir.erl.IHETools.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.icefaces.ace.event.AccordionPaneChangeEvent;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Contains the information found in a row of the CDA Document Builder table of sections. SessionBean contains List of BuilderRow instances
 * called cdaBuilderRows used to form the table.
 * @author Steven Bosch
 *
 */
public class CDABuilderRow  implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static Logger log = Util.getLog();
	private String section;
	private String opt;
	private String templateId;
	private String XML = "";
	private String selectedType = "empty";
	private int active = -1;
	private ArrayList<SelectItem> selections = new ArrayList<SelectItem>();

	public CDABuilderRow(String section, String opt, String templateId, ArrayList<SelectItem> selections){

		this.section = section;
		this.opt = opt;
		this.templateId = templateId;
		this.selections = selections;
	}

	public String selected(){
		if (selectedType.equals("empty"))
			return "Empty Selection";
		return selectedType;
	}

	public void selectionMade(ValueChangeEvent event){
		selectedType = (String) event.getNewValue();
		retrieveXML();
		FacesUtil.refreshPage();

	}

	public void paneChange(AccordionPaneChangeEvent event){
		FacesUtil.refreshPage();
	}

	/**
	 * Retrieve XML section from database.
	 */
	public void retrieveXML(){
		Valid v = new Valid();
		v.startValidations();
		if (selectedType.equals("empty")){
			XML = "";
			return;
		}
		try(DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			ResultSet rs = new Query(DBUtil.CDA_BUILDER_XMLPath_RETRIEVE).set("type", selectedType).set("templateId", templateId).dbQuery(dbc);
			String XMLPath="";
			while (rs.next()){
				XMLPath = rs.getString("XMLPath");
			}
			Path path = Util.getRunDirectory().resolve(Paths.get(XMLPath));
			byte[] xmlBytes = Files.readAllBytes(path);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document codes = db.parse(new ByteArrayInputStream(xmlBytes));
			XML = Util.prettyPrintXML(codes);
			XML = StringUtils.substringAfter(XML, ">");
		}catch(IOException e){
			log.warn(e.getMessage());
			v.error("Could not read in XML code");
		}catch(ParserConfigurationException e){
			log.warn(e.getMessage());
			v.error("Could not create DocumentBuilder");
		}catch(SAXException e){
			log.warn(e.getMessage());
			v.error("Could not parse XML code byte array");
		}catch(Exception e){
			log.warn("Could not connect to database");
			v.error("Could not connect to database");
		}
		
	}

	public boolean rendered(){
		if (selectedType.equals("empty"))
			return false;
		return true;
	}

	public ArrayList<SelectItem> getSelections() {
		return selections;
	}



	public void setSelections(ArrayList<SelectItem> selections) {
		this.selections = selections;
	}



	public String getSection() {
		return section;
	}


	public void setSection(String section) {
		this.section = section;
	}


	public String getOpt() {
		return opt;
	}


	public void setOpt(String opt) {
		this.opt = opt;
	}


	public String getTemplateId() {
		return templateId;
	}


	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}


	public String getXML() {
		return XML;
	}


	public void setXML(String xML) {
		XML = xML;
	}



	public String getSelectedType() {
		return selectedType;
	}



	public void setSelectedType(String selectedType) {
		this.selectedType = selectedType;
	}

	public int getActive() {
		return active;
	}

	public void setActive(int active) {
		this.active = active;
	}



}
