package uk.gav.event;

/**
 * 
 * @author gavin
 *
 * Holder of the event information persisted on the holding database table.
 * id - 			Unique reference for this particular event.
 * type - 			the type of event being processed, e.g. 1 - email
 * eventContent - 	the data content representing the event itself. Expectation is that this
 * 					will be JSON format, but this will only be enforced by the event consumer.
 */
public class EventEntity {
	private Integer 	id;
	private Integer		type;
	private String		eventContent;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public String getEventContent() {
		return eventContent;
	}
	public void setEventContent(String eventContent) {
		this.eventContent = eventContent;
	}
	public String toString() {
		return "ID:" + id +
			   "\nType: " + type + 
			   "\nEvent:" + eventContent;
	}
}
