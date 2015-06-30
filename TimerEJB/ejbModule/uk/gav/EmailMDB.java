package uk.gav;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.codehaus.jackson.map.ObjectMapper;

import uk.gav.utilities.Environment;

/**
 * Session Bean implementation class EmailEJB
 */
//@MessageDriven(ejbName = "EventQueueListenerMDB", destinationType="javax.jms.Queue",destinationJndiName=JMSConstants.JMS_EMAIL_Q)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
@MessageDriven(
		  name = "EventQueueListenerMDB",
		  activationConfig = {
		    @ActivationConfigProperty(propertyName  = "destinationType", 
		                              propertyValue = "javax.jms.Queue"),
		 		 
		    @ActivationConfigProperty(propertyName  = "connectionFactoryJndiName",
		                              propertyValue = JMSConstants.JMS_FACTORY), // External JNDI Name
		 
		    @ActivationConfigProperty(propertyName  = "destinationJndiName",
		                              propertyValue = JMSConstants.JMS_EMAIL_Q)
		  }
		)
public class EmailMDB implements MessageListener {

	private final static String EVENT_DATASOURCE = Environment.class.getName() + ".DATA_SOURCE";

	@Resource
	javax.ejb.MessageDrivenContext mc;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	// Could be @Resource injected, but don't want to hard code JNDI ref.
	private DataSource eventSource;

	public EmailMDB() throws Exception {
		// Grab the Datasource
		Properties prop = Environment.getContextEnv();
		InitialContext context = new InitialContext(prop);
		eventSource = (DataSource) context.lookup(prop.getProperty(EVENT_DATASOURCE));
		System.out.println("DATASOURCE located in EmailMDB:: " + eventSource);
		
	}
	
	public void onMessage(Message message) {
		System.out.println("The message is::" + message);
		
		try {
			EventEntity ee = mapper.readValue(((TextMessage)message).getText(), EventEntity.class);
		
			EventEmailContent eec = new EventEmailContent(ee);
			
			System.out.println("The actual event content is::" + eec + "...and will send the email...");
	
			Emailer mailer = Emailer.getInstance();
			mailer.issueEmail(eec);
			
			//Get ready to update status on initial table
			Connection c = eventSource.getConnection();
			PreparedStatement s = c
					.prepareStatement("UPDATE service_event_queue SET status = '2' WHERE event_id = ?");

			//Add the id to SQL batch
			s.setInt(1, ee.getId());
			
			int affectedRecords = s.executeUpdate();

			c.close();
			
			System.out.println("Affected Records following email send:::" + affectedRecords);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot extract event from message::" + e);
		}
	}
}
