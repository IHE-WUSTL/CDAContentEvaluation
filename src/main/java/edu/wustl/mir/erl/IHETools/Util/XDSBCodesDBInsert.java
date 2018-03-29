package edu.wustl.mir.erl.IHETools.Util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
public class XDSBCodesDBInsert implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static Logger log = Util.getLog();
	private File file;

	public XDSBCodesDBInsert(File file){
		this.file = file;
	}

	/**
	 * Reads in the XDSB codes and their options from an XML document.
	 */
	public void parse(){
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList codeTypes = doc.getElementsByTagName("CodeType");
			for (int x=0; x<codeTypes.getLength(); x++){
				Node node = codeTypes.item(x);
				if (node.getNodeType() == Node.ELEMENT_NODE){
					String codeTypeName="";
					String codeTypeClassScheme="";

					Element codeType = (Element) node;
					codeTypeName = codeType.getAttribute("name");
					codeTypeClassScheme = codeType.getAttribute("classScheme");
					NodeList codes = codeType.getChildNodes();
					for (int y=0; y<codes.getLength(); y++){
						Node subNode = codes.item(y);
						if(subNode.getNodeType() == Node.ELEMENT_NODE){
							String codeCode="";
							String codeCodingScheme="";
							String codeDisplay="";

							Element code = (Element) subNode;
							codeCode = code.getAttribute("code");
							codeCodingScheme = code.getAttribute("codingScheme");
							codeDisplay = code.getAttribute("display");
							insertDatabase(codeTypeName, codeTypeClassScheme, codeCode, codeCodingScheme, codeDisplay);
						}
					}
				}
			}
		}catch(ParserConfigurationException e){
			log.error("Could not create DocumentBuilder");
			log.error(e.getMessage());
		}catch(IOException e){
			log.error("Could not parse file");
			log.error(e.getMessage());
		}catch(SAXException e){
			log.error(e.getMessage());
		}
	}

	/**
	 * Inserts XDSB codes and their options into the database.
	 */
	public void insertDatabase(String codeTypeName, String codeTypeClassScheme, String codeCode, String codeCodingScheme, String codeDisplay){
		Valid v = new Valid();
		try(DataBaseConnection dbc = 
				DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME);){			
			new Query(DBUtil.CDA_XDSB_INSERT)
			.set("codeTypeName", codeTypeName)
			.set("codeTypeClassScheme", codeTypeClassScheme)
			.set("codeCode", codeCode)
			.set("codeCodingScheme", codeCodingScheme)
			.set("codeDisplay", codeDisplay)
			.dbUpdate(dbc);
		}catch(Exception e){
			log.error("Could not connect to database");
			log.error(e.getMessage());
		}
	}
}
