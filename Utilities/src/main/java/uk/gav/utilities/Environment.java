package uk.gav.utilities;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Environment {

	private final static String PROP_LOC = "/opt/app/ufa/ufacit1/live/ufadomain/properties/";
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

	
	public static void reload() {
		init();
	}
	
	private final static void init() {
		try {
			jmsEnv = loadEnvironment(JMSENVSOURCE);
			contextEnv = loadEnvironment(CONTEXTENVSOURCE);
			emailEnv = loadEnvironment(EMAILENVSOURCE);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot load environment variables::" + e);
		}
		
	}
	
 	private final static Properties loadEnvironment(String envSource) throws Exception {
		Properties prop = new Properties();
		InputStream iStream = null;
		
		System.out.println("Attempting to load properties for::"+ envSource);
		//Try and load from filesystem first
		try {
			iStream = new FileInputStream(PROP_LOC + envSource);
			prop.load(iStream);
			System.out.println(envSource + " loaded from filesystem");
		}
		catch (Exception e) {
				System.out.println("GAVVYYYYY::::" + e); 
		}
			
		//Otherwise from classpath
		if (iStream == null) {
			iStream = Environment.class.getClassLoader()
					.getResourceAsStream("META-INF/" + envSource);
			prop.load(iStream);
			System.out.println(envSource + " loaded from classpath");
		}
		
		return prop;
	}

}
