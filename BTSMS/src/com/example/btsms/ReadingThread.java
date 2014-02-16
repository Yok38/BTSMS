package com.example.btsms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;

public class ReadingThread extends Thread {

	private InputInterface inInt;
	private OutputInterface outInt;
	private SmsManager smsm;
	private Context context;
	private int nClient;

	public ReadingThread(Context context, InputInterface inInt, OutputInterface outInt, SmsManager smsm, int nClient) {
		this.inInt = inInt;
		this.smsm = smsm;
		this.context = context;
		this.nClient = nClient;
		this.outInt = outInt;
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

				case 'H':
					int lNumber = packet[1]&0xff;
					String number = "";
					for(int i=0;i<lNumber;i++) {
						number += byteToChar(packet[2+i]);
					}

					ArrayList<Message> history = new ArrayList<Message>();

					Cursor cursorThreads = this.context.getContentResolver().query(Uri.parse("content://sms/"), new String[]{"address","thread_id"}, null, null, null);

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
							Cursor cursorSmsThread = this.context.getContentResolver().query(Uri.parse("content://sms/"), new String[]{"thread_id","type","body","date"}, "thread_id="+thread_id, null,"date desc limit 5");
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
							Log.report("No thread associated with number "+number, Log.INFO);
						}
					}
					else {
						Log.report("No thread", Log.INFO);
					}

					//
					//					int thread_id = -1;
					//					Cursor conv = this.context.getContentResolver().query(Uri.parse("content://mms-sms/conversations/"), new String[]{"thread_id","address"}, null, null, null);
					//					int idnumber = conv.getColumnIndex("address");
					//					int idthread = conv.getColumnIndex("thread_id");
					//					if(conv.getCount()>0) {
					//						conv.moveToFirst();
					//						do {
					//							Log.report("PNU.compare("+number+","+conv.getString(idnumber)+"", Log.INFO);
					//							if(PhoneNumberUtils.compare(number, conv.getString(idnumber))) {
					//								thread_id = conv.getInt(idthread);
					//							}
					//						} while(conv.moveToNext());
					//
					//						if(thread_id >= 0) {
					//							Cursor msg = this.context.getContentResolver().query(Uri.parse("content://sms/"), new String[]{"thread_id","date","type","body"}, "thread_id=+'"+thread_id+"'", null, "date DESC limit 5");
					//							int iddate = msg.getColumnIndex("date");
					//							int idtype = msg.getColumnIndex("type");
					//							int idbody = msg.getColumnIndex("body");
					//							if(msg.getCount()>0) {
					//								ArrayList<Message> history = new ArrayList<Message>();
					//								msg.moveToFirst();
					//								do {
					//									long date = msg.getLong(iddate);
					//									int type = msg.getInt(idtype);
					//									String body = msg.getString(idbody);
					//									
					//									history.add(new Message(date, body, (type == 1)));
					//									
					//								} while(msg.moveToNext());
					//								
					//								(new HistorySenderThread(number,history,this.outInt)).start();
					//							}
					//							
					//						}
					//						else {
					//							Log.report(thread_id+" No thread associated with "+number, Log.INFO);
					//						}
					//					}
					//					else {
					//						Log.report("No threads at all", Log.INFO);
					//					}

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
