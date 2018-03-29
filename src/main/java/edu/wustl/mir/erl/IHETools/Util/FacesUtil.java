package edu.wustl.mir.erl.IHETools.Util;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Class provides convenience methods for common JSF functions.
 * @author rmoult01
 */
public class FacesUtil implements Serializable {
	private static final long serialVersionUID = 1L;

	public static ServletContext getServletContext() {
		return (ServletContext) FacesContext.getCurrentInstance()
				.getExternalContext().getContext();
	}

	public static ExternalContext getExternalContext() {
		FacesContext fc = FacesContext.getCurrentInstance();
		return fc.getExternalContext();
	}

	public static HttpServletRequest getHttpServletRequest() {
		return (HttpServletRequest) getExternalContext().getRequest();
	}

	public static HttpSession getHttpSession(boolean create) {
		return (HttpSession) FacesContext.getCurrentInstance()
				.getExternalContext().getSession(create);
	}

	/**
	 * Forces refresh of current browser page
	 */
	public static void refreshPage() {
		FacesContext fc = FacesContext.getCurrentInstance();
		String page = fc.getViewRoot().getViewId();
		ViewHandler vh = fc.getApplication().getViewHandler();
		UIViewRoot uiv = vh.createView(fc, page);
		uiv.setViewId(page);
		fc.setViewRoot(uiv);
	}

	/**
	 * Forces redirect to specific page. Useful for controls which do not
	 * implement navigation.
	 * @param url String page to redirect to.
	 * @throws IOException on error.
	 */
	public static void goToPage(String url) throws IOException {
		FacesContext.getCurrentInstance().getExternalContext().redirect(url);
	}

	public static void closeSession(String url) {
		ExternalContext ec = FacesContext.getCurrentInstance()
				.getExternalContext();
		ec.invalidateSession();
		try {
			ec.redirect(url);
		} catch (Exception e) {
			Util.getLog().warn("closeSession() error: " + e.getMessage());
		}
	}

	/**
	 * Get managed bean based on the bean name.
	 * @param beanName String the bean name
	 * @return the managed bean associated with the bean name
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getManagedBean(String beanName) {

		FacesContext ctx = FacesContext.getCurrentInstance();
		return (T) ctx.getApplication().evaluateExpressionGet(ctx,
				"#{" + beanName + "}", Object.class);
		}

	/**
	 * Add information message at the global level.
	 * @param msg String the information message
	 */
	public static void addInfoMessage(String msg) {
		addInfoMessage(null, msg);
	}

	/**
	 * Add information message to a specific client.
	 * @param clientId String the client id
	 * @param msg String the information message
	 */
	public static void addInfoMessage(String clientId, String msg) {
		FacesContext.getCurrentInstance().addMessage(clientId,
				new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));
	}

	/**
	 * Add error message at the global level
	 * @param msg String the error message
	 */
	public static void addErrorMessage(String msg) {
		addErrorMessage(null, msg);
	}

	/**
	 * Add error message to a specific client.
	 * @param clientId String the client id
	 * @param msg String the error message
	 */
	public static void addErrorMessage(String clientId, String msg) {
		FacesContext.getCurrentInstance().addMessage(clientId,
				new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
	}
	
	private static InitialContext ic = null;
	public static String getContextString(String name) throws NamingException {
		if (ic == null) ic = new InitialContext();
		return ic.lookup("java:comp/env/" + name).toString();
	}
	public static String getContextString(String name, String defaultValue) {
		try {
			return getContextString(name);
		} catch (NamingException ne) {
			return defaultValue;
		}
	}

} // EO FacesUtil class
