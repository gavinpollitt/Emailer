package uk.gav.event.email;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.gav.event.EventEntity;
import uk.gav.event.utilities.Environment;

public class EmailTemplateTest {

	@Test
	public void getTemplateTestFrom() {
		EventEntity ee = new EventEntity();
		ee.setEventContent("{}");
		EventEmailContent eec = new EventEmailContent(ee);
		eec.setContentType("IFP");
		
		// Ensure environment is collecting files from correct place.
		Environment.setPropertyLocation(System.getProperty("propertyLocation"));
		Environment.reload();
		
		//Now try and get inline text
		eec.setVersion(1.0f);
		String output = EmailConstants.getTemplate(eec);
		
		String expected = "Hello %%to%%";
		assertTrue("output " + output + " does not match that expected: " + expected,
					expected != null && output.startsWith(expected));
		
		// Now try and get file text
		eec.setVersion(2.0f);
		output = EmailConstants.getTemplate(eec);
		
		expected = "<html>\n  Hello there\n</html>";
		assertTrue("output " + output + " does not match that expected: " + expected,
					expected != null && output.startsWith(expected));
	}

}
