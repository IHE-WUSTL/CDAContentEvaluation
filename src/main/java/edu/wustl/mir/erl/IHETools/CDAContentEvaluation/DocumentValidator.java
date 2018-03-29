package edu.wustl.mir.erl.IHETools.CDAContentEvaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DocumentValidator implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final String nl = System.getProperty("line.separator");
	
	private String documentName = "";
	private String goldDoc = "";
	private String assertMsg = "";
	private String[] assertLines = null;
	private List<Assertion> assertions = new ArrayList<>();

	private boolean selected = false;
	
	public DocumentValidator() {
	}
	
	public void init() {
		assertLines = new String[assertions.size()];
		for (int i = 0; i < assertions.size(); i++) 
			assertLines[i] = assertions.get(i).getAssertion();
	}
	
	public void addAssertion(Assertion a) {
		assertions.add(a);
	}
	
	public List<Assertion> getAssertions() {
		return assertions;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String docName) {
		this.documentName = docName;
	}

	public String[] getAssertLines() {
		return assertLines;
	}

	public String getGoldDoc() {
		return goldDoc;
	}
	public void setGoldDoc(String doc) {
		goldDoc = doc;
	}

	public String getAssertMsg() {
		if (assertMsg.length() == 0) {
			assertMsg = nl;
			for (String l : assertLines) {
				assertMsg += (l + nl);
			}
		}
		return assertMsg;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	

} // EO DocumentValidator class
