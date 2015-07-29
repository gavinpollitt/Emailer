package uk.gav.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author gavin
 *
 *         Superclass of utility classes that will scan a 'block' of content,
 *         identify replaceable pieces of text and delegate to the extending
 *         class to provide the replacement text.
 */
public abstract class ContentProcessor {
	private Pattern replacementRegEx;

	/**
	 * 
	 * @param regEx
	 *            The regular expression pattern to search for to identify
	 *            replaceable fields.
	 */
	protected void setReplacementRegEx(Pattern regEx) {
		replacementRegEx = regEx;
	}

	protected Pattern getReplacementRegEx() {
		return replacementRegEx;
	}

	/**
	 * 
	 * @param regEx
	 *            String identified by the regular expression parser that can be
	 *            replaced.
	 * @return If replacement located, return the output string Method to be
	 *         implemented by extending classes to provide the replacement
	 *         algorithm.
	 */
	protected abstract String replaceField(String regEx);

	/**
	 * 
	 * @param template
	 *            The text supplied potentially containing fragments of text to
	 *            be replaced.
	 * @return The template texts with all identified replacements made. Scans
	 *         through the template to identify and execute all required
	 *         replacements. Specific replacements will be delegated to
	 *         extending class.
	 * 
	 */
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

		return output;
	}

}
