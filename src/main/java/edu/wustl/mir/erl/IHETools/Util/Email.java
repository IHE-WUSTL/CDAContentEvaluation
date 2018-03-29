package edu.wustl.mir.erl.IHETools.Util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.lang.StringUtils;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Handler for email
 */
public class Email implements Serializable {

   private static final long serialVersionUID = 1L;

   Properties emailConfig;
   Session session;

   public Email() {
      initialize();
   }
   
   public void send(String to, String subject, String body)
           throws Exception {
      MimeMessage email = new MimeMessage(session);
      email.addRecipient(RecipientType.TO, new InternetAddress(to));
      email.setSubject(subject);
      email.setContent(body, "text/html");
      Transport.send(email);
   }

   public boolean sendSilent(String to, String subject, String body) {
      try {
         send(to, subject, body);
         return false;
      } catch (Exception e) {
         StringBuilder em = new StringBuilder("Error sending email:\n to ");
         em.append(to).append(" - ").append(e.getMessage());
         Util.getLog().warn(em.toString());
         return true;
      }
   }


   public void sendToEmails(Collection<String> emails, String subject, String
           body, Collection<String> files) throws Exception {
      Message msg = new MimeMessage(session);
      for (String em : emails)
         msg.addRecipient(RecipientType.TO, new InternetAddress(em));
      msg.setSubject(subject);

      Multipart multipart = new MimeMultipart();

      BodyPart mbp = new MimeBodyPart();
      mbp.setText(body);
      multipart.addBodyPart(mbp);

      for (String f : files) {
         mbp = new MimeBodyPart();
         DataSource source = new FileDataSource(f);
         mbp.setDataHandler(new DataHandler(source));
         mbp.setFileName(StringUtils.substringAfterLast(f, "/"));
         multipart.addBodyPart(mbp);
      }
      msg.setContent(multipart);
      Transport.send(msg);
   }

   public boolean sendToEmailsSilent(Collection<String> emails, String subject, String body, Collection<String> files) {
      try {
         sendToEmails(emails, subject, body, files);
         return false;
      } catch (Exception e) {
         Util.getLog().warn("sendToEmailSilent: " + e.getMessage());
         return true;
      }
   }


   private void initialize() {
      HierarchicalINIConfiguration ini = Util.getIni();
      emailConfig = new Properties();
      @SuppressWarnings("unchecked")
      Iterator<String> properties = (Iterator<String>) ini.getKeys("Email");
      while (properties.hasNext()) {
         String key = properties.next();
         String value = ini.getString(key, "NOF");
         if (!value.equalsIgnoreCase("NOF")) {
            key = StringUtils.replace(key, "..", ".");
            key = StringUtils.replace(key, "Email.", "", 1);
            emailConfig.setProperty(key, value);
            Util.getLog().trace("Email setProperty(" + key + "," + value + ")");
         }
      }
      session = Session.getDefaultInstance(emailConfig);
   }
} // EO Email class
