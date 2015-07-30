package uk.gav.event.email;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

/**
 * The class responsible for issuing the physical emails to the SMTP server.
 * This class is registered as a Singleton class to prevent the need for continuous
 * accessing of properties.
 * @author gavin
 *
 */
public final class Emailer {
	final static Logger log = Logger.getLogger(Emailer.class.getName());

	/**
	 * Singleton object instance of this class
	 */
	private final static Emailer instance = new Emailer();

	/**
	 * The system properties required to access the SMTP server
	 */
	private Properties smtp;

	private Emailer() {
		smtp = new Properties();
		smtp.put("mail.smtp.auth", true); //This will work in latest versions of Java mail.
		smtp.put("mail.smtp.startttls.enable", true);
		smtp.put("mail.smtp.host", EmailConstants.EMAIL_HOST);
		smtp.put("mail.smtp.port", EmailConstants.EMAIL_PORT);
	}

	public static Emailer getInstance() {
		return instance;
	}

	/**
	 * Utilising Javamail API, issue the email content through the SMTP server
	 * @param eec The contents for issue in the email.
	 * @throws Exception If issues with transport of email content.
	 */
	public void issueEmail(EventEmailContent eec) throws Exception {

		log.info("Message to Send of type:::" + eec.getContentType() + " for event id:" + eec.getOriginalEvent().getId());
		log.debug("SMTP Details:::" + smtp);
		
		// If a secure send isn't required, the credential information will be ignored.
		Session session = Session.getInstance(smtp, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(EmailConstants.EMAIL_USERNAME, EmailConstants.EMAIL_PASSWORD);
			}
		});

		// SMTP debug
		session.setDebug(log.isDebugEnabled());
		
		Message message = new MimeMessage(session);

		message.setFrom(new InternetAddress(EmailConstants.EMAIL_SENDER));

		// Primary recipients
		int ln = (eec.getTo() == null?0:eec.getTo().length);
		for (int i = 0; i <ln;i++) {
			message.addRecipient(Message.RecipientType.TO,
					InternetAddress.parse(eec.getTo()[i])[0]);
		}

		// CC list
		ln = (eec.getCc() == null?0:eec.getCc().length);
		for (int i = 0; i <ln;i++) {
			message.addRecipient(Message.RecipientType.CC,
					InternetAddress.parse(eec.getCc()[i])[0]);
		}

		// Bcc list
		ln = (eec.getBcc() == null?0:eec.getBcc().length);
		for (int i = 0; i <ln;i++) {
			message.addRecipient(Message.RecipientType.BCC,
					InternetAddress.parse(eec.getBcc()[i])[0]);
		}

		message.setSubject(eec.getSubject());

		// Parse the template, perform any replacements and set text of email.
		EmailContentProcessor cont = new EmailContentProcessor();
		cont.setEventEmailContent(eec);
		message.setContent(cont.processContent(EmailConstants.getTemplate(eec)), "text/html");

		try {
			//Transport.send(message);  //This is for later versions; forget the Weblogic frig below.
			Transport tr = session.getTransport("smtp");
			tr.connect(EmailConstants.EMAIL_HOST, EmailConstants.EMAIL_USERNAME, EmailConstants.EMAIL_PASSWORD);
			message.saveChanges();      // don't forget this
			tr.sendMessage(message, message.getAllRecipients());
			tr.close();		
		}
		catch (Exception e) {
			log.error("Sending the message, Exception::" + e);
			throw e;
		}
		
		log.debug("id:" + eec.getOriginalEvent().getId() + ":Message is sent successfully");
	}

}
