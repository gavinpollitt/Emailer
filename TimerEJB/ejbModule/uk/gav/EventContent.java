package uk.gav;

import java.io.StringReader;

import org.codehaus.jackson.map.ObjectMapper;

public abstract class EventContent {
	
	private EventEntity originalEvent; 
	
	public EventContent() {
		throw new RuntimeException("No content available in event");
	}
	
	public EventContent(EventEntity e) {
		this.originalEvent = e;
		loadFromJSON();
	}
	
	private void loadFromJSON() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			
			String json = originalEvent.getEventContent();	
			
			mapper.readerForUpdating(this).readValue(json);	
		}
		catch (Exception e) {
			throw new RuntimeException("The JSON provided in the event is malformed:" + e);
		}
	}
	
	protected EventEntity getOriginalEvent() {
		return originalEvent;
	}
	

}
 