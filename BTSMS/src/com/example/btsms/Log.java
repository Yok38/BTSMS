package com.example.btsms;

import java.util.ArrayList;

public class Log {
	
	public static int ERROR = 2;
	public static int WARNING = 1;
	public static int INFO = 0;
	public static String[] markers = new String[]{"-- ","!! ","@@ "};
	public static ArrayList<LogEntry> log = new ArrayList<LogEntry>();
	public static ArrayList<Notifyable> toNotify = new ArrayList<Notifyable>();
	
	public static void report(String msg, int priority) {
		Log.log.add(new LogEntry(Log.markers[priority]+msg));
		for(Notifyable n:Log.toNotify) {
			n.not(Log.log.get(Log.log.size()-1));
		}
	}
	
	public static void addObs(Notifyable n) {
		Log.toNotify.add(n);
	}
}
