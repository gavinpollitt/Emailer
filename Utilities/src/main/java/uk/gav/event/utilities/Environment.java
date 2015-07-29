package uk.gav.event.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 
 * @author gavin
 *
 *         Centralise the acquisition of the external Java properties for this
 *         service. Filesystem will be examined first and, if not present,
 *         classpath loading will be the default.
 */
public class Environment {

	final static Logger log = Logger.getLogger(Environment.class.getName());

	private static String PROP_LOC = "/home/ufa/properties/";
	private final static String JMSENVSOURCE = "jms.properties";
	private final static String CONTEXTENVSOURCE = "connection.properties";
	private final static String EMAILENVSOURCE = "email.properties";
	private static Properties jmsEnv;
	private static Properties contextEnv;
	private static Properties emailEnv;

	static {
		init();
	}

	public static Properties getContextEnv() {
		return contextEnv;
	}

	public static Properties getJmsEnv() {
		return jmsEnv;
	}

	public static Properties getEmailEnv() {
		return emailEnv;
	}

	public static void setPropertyLocation(String loc) {
		PROP_LOC = loc;
	}
	
	public static void reload() {
		init();
	}

	private final static void init() {
		try {
			jmsEnv = loadEnvironment(JMSENVSOURCE);
			contextEnv = loadEnvironment(CONTEXTENVSOURCE);
			emailEnv = loadEnvironment(EMAILENVSOURCE);
		} catch (Exception e) {
			throw new RuntimeException("Cannot load environment variables::"
					+ e);
		}

	}

	private final static Properties loadEnvironment(String envSource)
			throws Exception {
		Properties prop = new Properties();
		InputStream iStream = null;

		log.info("Attempting to load properties for::" + envSource);
		// Try and load from filesystem first
		try {
			iStream = new FileInputStream(PROP_LOC + envSource);
			prop.load(iStream);
			log.info(envSource + " loaded from filesystem");
		} catch (Exception e) {
			log.info("Properties not available on file system");
		}

		// Otherwise from classpath
		if (iStream == null) {
			iStream = Environment.class.getClassLoader().getResourceAsStream(
					"META-INF/" + envSource);
			prop.load(iStream);
			log.info(envSource + " loaded from classpath");
		}

		return prop;
	}

	public static String readFile(String filename) {
		InputStream iStream = null;

		log.info("Attempting to load file::" + filename);
		// Try and load from filesystem first
		File f = null;
		try {
			f = new File(PROP_LOC + filename);

			if (f.exists()) {
				iStream = new FileInputStream(f);
			}
			log.info(filename + " loaded from filesystem");
		} catch (Exception e) {
			log.info("File " + f + " not available on file system");
		}

		// Otherwise from classpath
		if (iStream == null) {
			iStream = Environment.class.getClassLoader().getResourceAsStream(
					"META-INF/" + filename);
			log.info(filename + " loaded from classpath");
		}

		String contents = "";
		try {
			if (iStream == null) {
				throw new IllegalArgumentException("File " + filename
						+ " cannot be located");
			}

			contents = readFile(iStream);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		finally {
			try {
				if (iStream != null) {
					iStream.close();
				}
			}
			catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		return contents;

	}

	private static String readFile(InputStream fileStream) throws Exception {
		StringBuffer fileData = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				fileStream));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}
		reader.close();
		return fileData.toString();
	}
}
