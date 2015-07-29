package uk.gav.event.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import uk.gav.event.utilities.Environment;

/**
 * Create the email constants required using the appropriate system properties.
 * 
 * @author gavin
 *
 */
public class EmailConstants {
	private final static String PR = EmailConstants.class.getName() + ".";
	private final static String HOST = PR + "host";
	private final static String PORT = PR + "port";
	private final static String SENDER = PR + "sender";
	private final static String USER = PR + "username";
	private final static String PASS = PR + "password";
	private final static String TEMP = PR + "template";
	
	private final static String TEMPLATE_INLINE = "INLINE:";
	private final static String TEMPLATE_FILE = "FILE:";

	private static Properties prop = Environment.getEmailEnv();

	public final static String EMAIL_HOST = prop.getProperty(HOST);
	public final static int EMAIL_PORT = Integer.parseInt(prop
			.getProperty(PORT) != null ? prop.getProperty(PORT) : "25");
	public final static String EMAIL_SENDER = prop.getProperty(SENDER);
	public final static String EMAIL_USERNAME = prop.getProperty(USER);
	public final static String EMAIL_PASSWORD = prop.getProperty(PASS);

	/**
	 * Note, if the number of templates becomes HUGE, may need to extend below
	 * with a caching capability.
	 */
	private static final Map<String, String> templates = new HashMap<String, String>();

	/**
	 * The template property takes the form: template_<contenttype>_<version>
	 * 
	 * @param eec
	 *            The full details of the provided event and its content.
	 * @return The contents of the template for this event.
	 */
	public static String getTemplate(EventEmailContent eec) {
		String templateKey = TEMP + "_" + eec.getContentType() + "_"
				+ eec.getVersion();
		
		String template = templates.get(templateKey);
		
		if (template == null) {
			String tempProp = prop.getProperty(templateKey);
	
			if (tempProp == null) {
				throw new IllegalArgumentException(
						"Cannot find corresponding template for:::" + TEMP + "_"
								+ eec.getContentType() + "_" + eec.getVersion());
			}
						
			if (tempProp.startsWith(TEMPLATE_INLINE)) {
				template = tempProp.substring(TEMPLATE_INLINE.length());
			}
			else if (tempProp.startsWith(TEMPLATE_FILE)) {
				template = Environment.readFile(tempProp.substring(TEMPLATE_FILE.length()));
			}
			else {
				throw new IllegalArgumentException(
						"Invalid template supplied for:::" + TEMP + "_"
								+ eec.getContentType() + "_" + eec.getVersion());				
			}
			
			templates.put(templateKey, template);
		}

		return template;
	}
	
}
