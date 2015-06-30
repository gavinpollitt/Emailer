package uk.gav;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GavTempMail {

	public static void main(String[] args) throws Exception {

		Properties smtp = new Properties();
		smtp.put("mail.smtp.auth", true);
		smtp.put("mail.smtp.startttls.enable", true);
		smtp.put("mail.smtp.host", "mail.btinternet.com");
		smtp.put("mail.smtp.port", "25");

		Session session = Session.getInstance(smtp, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("gavin.pollitt@btinternet.com", "ferguson1");
			}
		});
		
		session.setDebug(true);
		
		Message message = new MimeMessage(session);
		
		message.setFrom(new InternetAddress("gav@lad.com"));
		
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("gavin.pollitt@btinternet.com"));
		
		message.setSubject("Interview for Defensive Position");
		
		message.setText("Chris,\n see you at 1500 on Friday to look at you filling Stevie G's boots for next season!\n Cheers,\n B");
		
		Transport.send(message);
		
		System.out.println("Message is sent successfully");

	}


}

