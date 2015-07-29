package uk.gav.event.constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import uk.gav.event.utilities.Environment;

/**
 * 
 * @author gavin
 *
 *         Specific constants relating to the context in which the service in
 *         running
 */
public class ContextConstants {
	private final static String TRIGG_PROP = ContextConstants.class.getName()
			+ ".TIMER_TRIGGER_PERIOD";
	private final static String INTERVAL_PROP = ContextConstants.class
			.getName() + ".TIMER_INTERVAL";

	private final static Properties prop = Environment.getContextEnv();

	public final static Map<Integer, String> EVENT_TYPES = new HashMap<Integer, String>();

	/**
	 * The first execution of the timer ejb following its creation in
	 * milliseconds.
	 */
	public final static int TIMER_TRIGGER_PERIOD = Integer.parseInt(prop
			.getProperty(TRIGG_PROP) != null ? prop.getProperty(TRIGG_PROP)
			: "10000");

	/**
	 * The interval between subsequent timer executions in milliseconds.
	 */
	public final static int TIMER_INTERVAL = Integer.parseInt(prop
			.getProperty(INTERVAL_PROP) != null ? prop
			.getProperty(INTERVAL_PROP) : "200000");

	static {
		EVENT_TYPES.put(1, "Email");
		EVENT_TYPES.put(2, "Anon");
	}
}
