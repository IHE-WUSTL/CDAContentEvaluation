package edu.wustl.mir.erl.IHETools.CDAContentEvaluation;

import java.io.Serializable;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Node;

public class Assertion implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String assertion;  // Entire command line
	private Command cmd = Command.COMMENT;
	private XPathExpression  xPathExpression = null; // compiled xpath
	private Node node = null; // node from gold msg
	private String xpath = null; // string xpath
	private String comment = null; // comment
	
	//----------- constructor used to add comments
	public Assertion(String assertion) {
		this.assertion = assertion;
	}
	//------------ constructor used to add non-comments
	public Assertion(String assertion, Command cmd, 
			XPathExpression  xPathExpression, 	Node node, String xpath,
			String comment) {
		this.assertion = assertion;
		this.cmd = cmd;
		this.xPathExpression = xPathExpression;
		this.node = node;
		this.xpath = xpath;
		this.comment = comment;
	}
	public String getAssertion() {
		return assertion;
	}
	public void setAssertion(String assertion) {
		this.assertion = assertion;
	}
	public Command getCmd() {
		return cmd;
	}
	public void setCmd(Command cmd) {
		this.cmd = cmd;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public XPathExpression getxPathExpression() {
		return xPathExpression;
	}
	public void setxPathExpression(XPathExpression xPathExpression) {
		this.xPathExpression = xPathExpression;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getXpath() {
		return xpath;
	}
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
	
	

} // EO class Assertion
