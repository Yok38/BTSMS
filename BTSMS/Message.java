
public class Message {
	
	public long date;
	public String body;
	public boolean received;
	
	public Message(long date, String body, boolean received) {
		this.date = date;
		this.body = body;
		this.received = received;
	}
}
