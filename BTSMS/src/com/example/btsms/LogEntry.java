package com.example.btsms;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogEntry {
	
	private String s;
	private Date d;
	
	public LogEntry(String s) {
		this.s = s;
		this.d = new Date();
	}
	
	public String toString() {
		return (new SimpleDateFormat("HH:mm:ss.SSS")).format(this.d) + " " + s;
	}
}
