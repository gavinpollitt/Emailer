package uk.gav;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ContentProcessor {
	private Pattern replacementRegEx;
		
	protected void setReplacementRegEx(Pattern regEx) {
		replacementRegEx = regEx;
	}
	
	private Pattern getReplacementRegEx() {
		return replacementRegEx;
	}
	
	public abstract String replaceField(String regEx);
	
	public final String processContent(final String template) {
		String output = template;
		
		Matcher m = replacementRegEx.matcher(template);
		
		StringBuffer os = new StringBuffer();
		while (m.find()) {
			String trueString = replaceField(m.group());
			m.appendReplacement(os, trueString);
		}
		m.appendTail(os);
		output = os.toString();
		System.out.println(output);
		
		return output;
	}
	
}
