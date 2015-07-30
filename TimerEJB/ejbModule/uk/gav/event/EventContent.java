package uk.gav.event;

import java.io.StringReader;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * 
 * @author gavin
 *
 *         Superclass of JSON driven events. Uses Jackson mapping API to
 *         deserialise the event JSON into the event-specific Java object
 *         extending this class.
 */
public abstract class EventContent {

	/**
	 * The full event entry originally triggered by the calling application.
	 */
	private EventEntity originalEvent;

	/**
	 * An originating event must be supplied at instantiation. Therefore, this
	 * constructor will result in an exception
	 */
	public EventContent() {
		throw new RuntimeException("No content available in event");
	}

	/**
	 * 
	 * @param The
	 *            original application triggered event obect The JSON contained
	 *            in the supplied EventEntity will be deserialised into the
	 *            extending class.
	 */
	public EventContent(EventEntity e) {
		this.originalEvent = e;
		loadFromJSON();
	}

	private void loadFromJSON() {
		try {
			ObjectMapper mapper = new ObjectMapper();

			String json = originalEvent.getEventContent();

			mapper.readerForUpdating(this).readValue(json);
		} catch (Exception e) {
			throw new RuntimeException(
					"The JSON provided in the event is malformed:" + e);
		}
	}

	public EventEntity getOriginalEvent() {
		return originalEvent;
	}

}
