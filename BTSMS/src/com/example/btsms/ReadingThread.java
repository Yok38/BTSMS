package com.example.btsms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class ReadingThread extends Thread {

	private InputInterface inInt;
	private SmsManager smsm;
	private Context context;
	private int nClient;

	public ReadingThread(Context context, InputInterface inInt, SmsManager smsm, int nClient) {
		this.inInt = inInt;
		this.smsm = smsm;
		this.context = context;
		this.nClient = nClient;
	}

	public void run() {
		byte[] packet;

		while(true) {
			try {
				packet = this.inInt.readPacket();
				switch(byteToChar(packet[0])) {
				case 'S':
					int id = packet[1]&0xff;
					int lNum = packet[2]&0xff;
					String num = "";
					for(int i=0;i<lNum;i++) {
						num += byteToChar(packet[i+3]);
					}
					String message = "";
					for(int i=3+lNum;i<packet.length;i++) {
						message += byteToChar(packet[i]);
					}
					
					Log.report("Packet.length = "+packet.length, Log.INFO);
					Log.report("Number : "+num, Log.INFO);
					Log.report("Message : "+message, Log.INFO);
					
					ArrayList<String> parts = this.smsm.divideMessage(message);

					ArrayList<PendingIntent> deliveredPIList = new ArrayList<PendingIntent>();
					for(int j=0;j<parts.size();j++) {
						Intent deliveredIntent = new Intent("SMS_DELIVERED");
						deliveredIntent.putExtra("idm", id);
						deliveredIntent.putExtra("idp", j);
						deliveredIntent.putExtra("nclient", this.nClient);
						deliveredPIList.add(PendingIntent.getBroadcast(this.context, 0, deliveredIntent, PendingIntent.FLAG_CANCEL_CURRENT));
					}

					ArrayList<PendingIntent> sentPIList = new ArrayList<PendingIntent>();
					for(int j=0;j<parts.size();j++) {
						Intent sentIntent = new Intent("SMS_SENT");
						sentIntent.putExtra("idm", id);
						sentIntent.putExtra("idp", j);
						sentIntent.putExtra("nclient", this.nClient);
						sentPIList.add(PendingIntent.getBroadcast(this.context, 0, sentIntent, PendingIntent.FLAG_CANCEL_CURRENT));
					}				

					this.smsm.sendMultipartTextMessage(num, null, parts, sentPIList, deliveredPIList);
					break;
				}
			} catch (IOException e) {
				Log.report("Pb reading buffer", Log.ERROR);
			}


		}
	}

	private char byteToChar(byte b) {
		return (char) (b&0xff);
	}
}
