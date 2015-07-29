package uk.gav.event.email;

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

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import uk.gav.event.EventEntity;
import uk.gav.event.constants.JMSConstants;
import uk.gav.event.utilities.Environment;
import uk.gav.event.utilities.MessageState;

/**
 * MessageDrivenBean to access the Email event queue and provide the necessary delegation for
 * issueing the physical emails to the SMTP server.
 * @author gavin
 *
 */
@SuppressWarnings("restriction")
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
@MessageDriven(
		  name = "EmailQueueListenerMDB",
		  activationConfig = {
		    @ActivationConfigProperty(propertyName  = "destinationType", 
		                              propertyValue = "javax.jms.Queue"),
		 		 
		    @ActivationConfigProperty(propertyName  = "connectionFactoryJndiName",
		                              propertyValue = JMSConstants.JMS_FACTORY_EVENT), // External JNDI Name
		 
		    @ActivationConfigProperty(propertyName  = "destinationJndiName",
		                              propertyValue = JMSConstants.JMS_Q_EMAIL)
		  }
		)
public class EmailMDB implements MessageListener {
	final static Logger log = Logger.getLogger(EmailMDB.class.getName());

	private final static String EVENT_DATASOURCE = Environment.class.getName() + ".DATA_SOURCE";

	private final static String EMAIL_ISSUED_SQL = "UPDATE service_event_queue SET status = ? WHERE event_id = ?";

	/**
	 * The injected message context.
	 */
	@Resource
	javax.ejb.MessageDrivenContext mc;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	// Could be @Resource injected, but don't want to hard code JNDI ref.
	private DataSource eventSource;

	/**
	 * Initialise the datasource for updating source event table
	 * @throws Exception
	 */
	public EmailMDB() throws Exception {
		// Grab the Datasource
		Properties prop = Environment.getContextEnv();
		InitialContext context = new InitialContext(prop);
		eventSource = (DataSource) context.lookup(prop.getProperty(EVENT_DATASOURCE));
		log.debug("DATASOURCE located in EmailMDB:: " + eventSource);
		
	}
	
	/**
	 * Standard MDB entry method. Method will delegate to utilty classes to issue event content
	 * to email destination.
	 */
	public void onMessage(Message message) {
		
		try {
			//serialise the JSON message into entity objects.
			EventEntity ee = mapper.readValue(((TextMessage)message).getText(), EventEntity.class);
		
			EventEmailContent eec = new EventEmailContent(ee);
			
			log.debug("The actual event content is::" + eec + "...and will send the email...");
	
			Emailer mailer = Emailer.getInstance();
			mailer.issueEmail(eec);
			
			//Get ready to update status on initial table
			Connection c = eventSource.getConnection();
			PreparedStatement s = c
					.prepareStatement(EMAIL_ISSUED_SQL);

			//Update the status of the original event message
			s.setString(1, MessageState.QUEUED.progressState().getStateID().toString());
			//Add the id to SQL batch
			s.setInt(2, ee.getId());
			
			int affectedRecords = s.executeUpdate();

			c.close();
			
			log.debug("Affected Records following email send:::" + affectedRecords);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot extract event from message::" + e);
		}
	}
}
