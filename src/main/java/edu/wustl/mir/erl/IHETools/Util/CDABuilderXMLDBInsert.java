package edu.wustl.mir.erl.IHETools.Util;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
/**
 * Matches the sections to the paths where the XML code of their different permutations is located.  Inserts the paths into the database.
 * Also updates the cdaBuilderDocuments table of the database with the paths to the XML code of their respective headers.
 */
public class CDABuilderXMLDBInsert implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static Logger log = Util.getLog();
	private List<SectionType> sections = new ArrayList<SectionType>();

	public CDABuilderXMLDBInsert(){}

	/**
	 * Class used to store the information about a section type.
	 */
	private class SectionType {
		private String templateID;
		private String type;
		private String XMLPath;

		public SectionType(String templateID, String type, String XMLPath){
			this.templateID = templateID;
			this.type = type;
			this.XMLPath = XMLPath;
		}
		public String getTemplateID(){
			return templateID;
		}
		public String getType(){
			return type;
		}
		public String getXML(){
			return XMLPath;
		}
	}

	/**
	 * Reads section types in from files.
	 */
	public void insert(){
		Valid v = new Valid();
		v.startValidations();
		Path sectionPath = Util.getRunDirectory().resolve(Paths.get("CDASections"));
		File secDir = sectionPath.toFile();
		for (File sec: secDir.listFiles()){
			String type = "";
			String templateID = "";
			String XMLPath = "";
			Path temp = sec.toPath().resolve(Paths.get("TemplateID"));
			File test = new File(temp.toString());
			if (!test.exists())
				continue;
			//read in templateID
			try{
				BufferedReader input = new BufferedReader(new FileReader(new File(temp.toString())));
				String line = null;
				while ((line = input.readLine()) != null){
					templateID += line;
				}
				input.close();

				for (File child: sec.listFiles()){
					if (!child.getName().equals("TemplateID")){
						type = child.getName();
						XMLPath = (Util.getRunDirectory()).relativize(child.toPath()).toString();
						System.out.println(XMLPath);
						sections.add(new SectionType(templateID, type, XMLPath));
					}
				}
			}catch(FileNotFoundException e){
				log.error("Could not find file 'TemplateID'");
			}catch(IOException e){
				log.error("Could not read file 'TemplateID'");
			}
		}
		addToDatabase(sections);	
	}

	/**
	 * Inserts section types into the database.
	 */
	public void addToDatabase(List<SectionType> sections){
		Valid v = new Valid();
		v.startValidations();
		for (SectionType st: sections){
			try (DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
				new Query(DBUtil.CDA_BUILDER_SELECTIONS_INSERT)
				.set("type", st.getType())
				.set("XMLPath", st.getXML())
				.set("templateId", st.getTemplateID())
				.dbUpdate(dbc);
			}catch(Exception e){
				log.error(e.getMessage());
			}
		}
	}

	/**
	 * Reads headers from files.
	 */
	public void insertHeaders(){
		Path headerPath = Util.getRunDirectory().resolve(Paths.get("CDAHeaders"));
		File headDir = headerPath.toFile();
		for (File head: headDir.listFiles()){
			String header = head.getName();
			String path = (Util.getRunDirectory()).relativize(head.toPath()).toString();
			addHeaderToDatabase(header, path);
		}
	}

	/**
	 * Inserts headers into the database.
	 */
	public void addHeaderToDatabase(String header, String path){
		try (DataBaseConnection dbc = DataBaseConnection.getDataBaseConnection(DBUtil.DBNAME)){
			new Query(DBUtil.CDA_BUILDER_DOCUMENTS_UPDATE)
			.set("header", path)
			.set("document", header)
			.dbUpdate(dbc);
		}catch(Exception e){
			log.error(e.getMessage());
		}
	}
}
