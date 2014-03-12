package com.example.btsms;

import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class BackupSenderThread extends Thread {
	
	private static final String TAG = "HistorySenderThread";
	private String number;
	ArrayList<Message> history;
	OutputInterface outInt;
	
	public BackupSenderThread(String number, ArrayList<Message> history, OutputInterface outInt) {
		this.number = number;
		this.history = history;
		this.outInt = outInt;
	}
	
	@Override
	public void run() {
		int k = 0;
		for(Message m:this.history) {
			byte[] data = new byte[2+this.number.length()+8+m.body.length()];
			data[0] = (byte) (m.received?1:0);
			data[1] = (byte) this.number.length();
			for(int i=0;i<this.number.length();i++) {
				data[i+2] = (byte) this.number.charAt(i);
			}
			for(int i=0;i<8;i++) {
				data[i+2+this.number.length()] = (byte) (m.date >>> (7-i)*8);
			}
			for(int i=0;i<m.body.length();i++) {
				data[i+2+this.number.length()+8] = (byte) m.body.charAt(i);
			}
			
			try {
				this.outInt.sendPacket((byte) 'B', data);
				Log.i(TAG,"B packet "+k+"/"+this.history.size()+" sent ("+(data.length+1)+")");
			} catch (IOException e) {
				Log.e(TAG,"IO issue sending backup");
			}
			k++;
		}
		
		try {
			this.outInt.sendPacket((byte) 'B', new byte[1]); // Fin de transmission de backup
		} catch (IOException e) {
			Log.e(TAG,"IO issue sending backup");
		}
		
	}
}
