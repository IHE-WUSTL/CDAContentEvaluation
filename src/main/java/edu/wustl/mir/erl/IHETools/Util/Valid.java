package edu.wustl.mir.erl.IHETools.Util;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import javax.faces.context.FacesContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;

/**
 * Used to provide validation for screen input values
 */
@SuppressWarnings("deprecation")
public class Valid implements Serializable {

   static final long serialVersionUID = 1L;
   private FacesContext context;
   private boolean errors;

   public FacesContext getContext() {
      return context;
   }

   public void setContext(FacesContext context) {
      this.context = context;
   }

   public boolean isErrors() {
      return errors;
   }

   public void setErrors(boolean errors) {
      this.errors = errors;
   }

   //**************************************************************************
   // General purpose validation methods
   //**************************************************************************
   public void startValidations() {
      context = FacesContext.getCurrentInstance();
      errors = false;
   }

   public void error(String id, String msg) {
      FacesUtil.addErrorMessage(id + " " + msg);
      errors = true;
   }

   public void error(String msg) {
      FacesUtil.addErrorMessage(msg);
      errors = true;
   }

   public void NB(String id, String v) {
      if (StringUtils.isBlank(v)) {
         error(id, " Can't be null, empty, or just whitespace");
      }
   }
   public void Port(String id, int p, boolean required) {
      if (!required && p == 0) return;
      if (p < 1 || p > 65535) {
         error(id, "Invalid port number");
      }
   }

   public void URL(String id, String v, boolean required) {
      if (!required && StringUtils.isBlank(v)) return;
      try {
         new URL(v);
      } catch (MalformedURLException ex) {
         error(id, "Invalid URL");
      }
   }

   /** Validate IPV4 address string, for example 127.0.0.1 */
   public void Ip(String id, String v, boolean required) {
      if (!required && StringUtils.isBlank(v)) return;
      boolean valid = true;
      String[] tuples = v.split("\\.");
      if (tuples.length == 4) {
         for (String tuple : tuples) {
            int i = Integer.parseInt(tuple);
            if (i >= 0 && i <= 255) {
               continue;
            }
            valid = false;
            break;
         }
      } else {
         valid = false;
      }
      if (!valid) {
         error(id, "Not a valid IP V4 address");
      }
   }

   /** Validate DICOM AE Title String */
   public void AeTitle(String id, String v, boolean required) {
      if (!required && StringUtils.isBlank(v)) return;
      if (!StringUtils.trimToEmpty(v).matches("\\w{1,16}"))
         error(id, "Invalid AE Title");
   }

   /** Validate email address string */
   public void Email(String id, String v, boolean required) {
      if (!required && StringUtils.isBlank(v)) return;
      if (!EmailValidator.getInstance().isValid(StringUtils.trimToEmpty(v))) {
         error(id, "Invalid Email Address");
      }
   }

} // EO Class Valid
