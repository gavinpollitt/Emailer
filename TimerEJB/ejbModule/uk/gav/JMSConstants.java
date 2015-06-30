package uk.gav;

import java.util.Properties;

import uk.gav.utilities.Environment;

public class JMSConstants {
	private final static String MAX_EVENT_BLOCK = JMSConstants.class.getName()
			+ ".EVENTS_IN_SINGLE_READ";
	
	private static Properties prop = Environment.getJmsEnv();
	
	public final static String JMS_FACTORY 			= "jms/EventConnectionFactory";
	public final static String JMS_EMAIL_Q			= "jms/EventQueue";
	public final static int    MAX_EMAIL_PER_CYCLE	= Integer.parseInt(prop.getProperty(MAX_EVENT_BLOCK) != null?prop.getProperty(MAX_EVENT_BLOCK):"100");
}
