package com.example.btsms;

import java.io.IOException;
import java.util.ArrayList;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;

public class ReadingThread extends Thread {

	private static final String TAG = "ReadingThread";
	public InputInterface inInt;
	public OutputInterface outInt;
	private SmsManager smsm;
	private ContentResolver contentResolver;
	private Context context;
	private ListeningThread l;
	private boolean mustStop = false;

	public ReadingThread(ListeningThread l, Context context, ContentResolver contentResolver, InputInterface inInt, OutputInterface outInt, SmsManager smsm) {
		this.inInt = inInt;
		this.smsm = smsm;
		this.contentResolver = contentResolver;
		this.context = context;
		this.outInt = outInt;
		this.l = l;
	}

	public void run() {
		byte[] packet;

		while(! this.mustStop) {
			try {
				packet = this.inInt.readPacket();
				switch(byteToChar(packet[0])) {
				case 'S':
					int nClient = l.clientThreads.indexOf(this);
					
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

					ArrayList<String> parts = this.smsm.divideMessage(message);

					ArrayList<PendingIntent> deliveredPIList = new ArrayList<PendingIntent>();
					for(int j=0;j<parts.size();j++) {
						Intent deliveredIntent = new Intent("SMS_DELIVERED");
						deliveredIntent.putExtra("idm", id);
						deliveredIntent.putExtra("idp", j);
						deliveredIntent.putExtra("nclient", nClient);
						deliveredPIList.add(PendingIntent.getBroadcast(this.context, 0, deliveredIntent, PendingIntent.FLAG_CANCEL_CURRENT));
					}

					ArrayList<PendingIntent> sentPIList = new ArrayList<PendingIntent>();
					for(int j=0;j<parts.size();j++) {
						Intent sentIntent = new Intent("SMS_SENT");
						sentIntent.putExtra("idm", id);
						sentIntent.putExtra("idp", j);
						sentIntent.putExtra("nclient", nClient);
						sentPIList.add(PendingIntent.getBroadcast(this.context, 0, sentIntent, PendingIntent.FLAG_CANCEL_CURRENT));
					}				

					this.smsm.sendMultipartTextMessage(num, null, parts, sentPIList, deliveredPIList);
					break;

				case 'H':
					int lNumber = packet[1]&0xff;
					String number = "";
					for(int i=0;i<lNumber;i++) {
						number += byteToChar(packet[2+i]);
					}

					ArrayList<Message> history = new ArrayList<Message>();

					Cursor cursorThreads = this.contentResolver.query(Uri.parse("content://sms/"), new String[]{"address","thread_id"}, null, null, null);

					int thread_id = -1;
					if(cursorThreads.getCount()>0) {
						int idAddress = cursorThreads.getColumnIndex("address");
						int idThread = cursorThreads.getColumnIndex("thread_id");

						cursorThreads.moveToFirst();
						do {
							if(PhoneNumberUtils.compare(cursorThreads.getString(idAddress), number)) {
								thread_id = cursorThreads.getInt(idThread);
								break;
							}	
						} while(cursorThreads.moveToNext());

						if(thread_id>=0) {
							Cursor cursorSmsThread = this.contentResolver.query(Uri.parse("content://sms/"), new String[]{"thread_id","type","body","date"}, "thread_id="+thread_id, null,"date desc limit 5");
							int idtype = cursorSmsThread.getColumnIndex("type");
							int idbody = cursorSmsThread.getColumnIndex("body");
							int iddate = cursorSmsThread.getColumnIndex("date");
							if(cursorSmsThread.getCount()>0) {
								cursorSmsThread.moveToFirst();
								do {
									history.add(0,new Message(cursorSmsThread.getLong(iddate), cursorSmsThread.getString(idbody), (cursorSmsThread.getInt(idtype) == 1)));			
								} while(cursorSmsThread.moveToNext());
							}
							(new HistorySenderThread(number,history,this.outInt)).start();
						}
						else {
							Log.i(TAG,"No thread associated with number");
						}
					}
					else {
						Log.i(TAG,"No threads at all");
					}

					break;
				case 'X':
					Log.i(TAG,"Packet X received");
					this.mustStop = true;
					l.clientThreads.remove(this);
					break;
				}
			} catch (IOException e) {
				Log.e(TAG,"IO issue reading the buffer");
				l.clientThreads.remove(this);
				break;
			}


		}
	}

	private char byteToChar(byte b) {
		return (char) (b&0xff);
	}

	public void stopService() {
		this.mustStop  = true;
	}
}
