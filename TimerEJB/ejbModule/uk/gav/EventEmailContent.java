package uk.gav;

public class EventEmailContent extends EventContent {
	
	private String[] to;
	private String subject;
	private String contentType;
	private float  version;
	
	public static void main(String[] args) {
		EventEntity ee = new EventEntity();
		ee.setEventContent("{\"to\":\"gavlad@bad.com\",\"subject\":\"Lav It\", \"contentType\":\"one\",\"version\":1.0}");
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
	
	public String toString() {
		return "TO::" + to +
			   "\nSUBJECT::" + subject +
			   "\ntype::" + contentType +
			   "\nversion::" + version;
	}
	
	
}
