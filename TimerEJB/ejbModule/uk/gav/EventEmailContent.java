package uk.gav;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

// Note, this annotation is not used from V2.0 onwards; will ignore a field if null
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
// Ignore any fields provided in JSON that are unknown for now
@JsonIgnoreProperties(ignoreUnknown=true)
public class EventEmailContent extends EventContent {
	
	private String[] to;
	private String[] cc;
	private String subject;
	private String contentType;
	private float  version;
	
	public static void main(String[] args) {
		EventEntity ee = new EventEntity();
		ee.setEventContent("{\"to\":[\"gavlad@bad.com\"],\"cc\":[\"gav2@bad.com\",\"gav99@bad.com\"],\"subject\":\"Lav It\", \"contentType\":\"one\",\"version\":1.0, \"temp\":\"no\"}");
		EventEmailContent eec = new EventEmailContent(ee);
		System.out.println(eec);
	}
	
	public EventEmailContent(EventEntity ee) {
		super(ee);
	}
	
	public String[] getTo() {
		return to;
	}
	public void setTo(String[] to) {
		this.to = to;
	}
	public String[] getCc() {
		return cc;
	}

	public void setCc(String[] cc) {
		this.cc = cc;
	}

	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public float getVersion() {
		return version;
	}
	public void setVersion(float version) {
		this.version = version;
	}
	
	public static String serialise(String[] sA) {
		String SEP = "";
		String ser = "";
		for (int i = 0; i < sA.length; i++) {
			ser += (SEP + sA[i]);
			SEP = ";";
		}
		
		return ser;
	}
	public String toString() {
		return "TO::" + serialise(to) +
			   "\nCC::"+ serialise(cc) +
			   "\nSUBJECT::" + subject +
			   "\ntype::" + contentType +
			   "\nversion::" + version;
	}
	
	
}
