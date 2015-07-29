package uk.gav.event.constants;


import java.util.Properties;

import uk.gav.event.utilities.Environment;

/**
 * 
 * @author gavin
 *
 *         The constants required to provide the necessary JMS environment.
 */
public class JMSConstants {
	/**
	 * The property used to indicate the maximum number of events to consume in
	 * a single cycle.
	 */
	private final static String MAX_EVENT_BLOCK = JMSConstants.class.getName()
			+ ".EVENTS_IN_SINGLE_READ";

	private static Properties prop = Environment.getJmsEnv();

	/**
	 * The JNDI reference for the connection factory to acquire the various
	 * event queue connections.
	 */
	public final static String JMS_FACTORY_EVENT = "jms/EventConnectionFactory";

	/**
	 * The JNDI reference for the Email Q for processing email-specific events.
	 */
	public final static String JMS_Q_EMAIL = "jms/EmailQueue";

	/**
	 * The JNDI reference for a dummy queue to be replaced by true events later.
	 */
	public final static String JMS_Q_ANON = "jms/AnonQueue";

	/**
	 * Maximum number of events to consume in a single timer cycle.
	 */
	public final static int MAX_EVENT_PER_CYCLE = Integer.parseInt(prop
			.getProperty(MAX_EVENT_BLOCK) != null ? prop
			.getProperty(MAX_EVENT_BLOCK) : "100");

}
