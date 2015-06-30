package uk.gav;

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
