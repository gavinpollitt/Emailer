package uk.gav.event.web;

import java.util.Date;

/**
 * The execution details for the timer being processed.
 * 
 * @author gavin
 *
 */
public class EventExecution {
	private String 	id;
	private Date	timestamp;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	

}
