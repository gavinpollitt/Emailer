package uk.gav;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public final class Emailer {

	private final static Emailer instance = new Emailer();

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

	public void issueEmail(EventEmailContent eec) throws Exception {

		System.out.println("Message to Send:::");
		System.out.println("SMTP:::" + smtp);
		
		
		Session session = Session.getInstance(smtp, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(EmailConstants.EMAIL_USERNAME, EmailConstants.EMAIL_PASSWORD);
			}
		});

		session.setDebug(true);
		Message message = new MimeMessage(session);

		message.setFrom(new InternetAddress(EmailConstants.EMAIL_SENDER));

		for (int i = 0; i <eec.getTo().length;i++) {
			message.addRecipient(Message.RecipientType.TO,
					InternetAddress.parse(eec.getTo()[i])[0]);
		}
		
		message.setSubject(eec.getSubject());

		EmailContentProcessor cont = new EmailContentProcessor();
		cont.setEventEmailContent(eec);
		message.setText(cont.processContent(EmailConstants.getTemplate(eec)));

		try {
			//Transport.send(message);  //This is for later versions; forget the Weblogic frig below.
			Transport tr = session.getTransport("smtp");
			tr.connect(EmailConstants.EMAIL_HOST, EmailConstants.EMAIL_USERNAME, EmailConstants.EMAIL_PASSWORD);
			message.saveChanges();      // don't forget this
			tr.sendMessage(message, message.getAllRecipients());
			tr.close();		
		}
		catch (Exception e) {
			System.out.println("Sending the message, Exception::" + e);
			throw e;
		}
		
		System.out.println("Message is sent successfully");
	}

}
