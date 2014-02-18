package com.example.btsms;

import java.io.IOException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactSenderThread extends Thread {
	
	private static final String TAG = "ContactSenderThread";
	private OutputInterface outInt;
	private ContentResolver contentResolver;
	
	ContactSenderThread(ContentResolver contentResolver, OutputInterface outInt) {
		this.contentResolver = contentResolver;
		this.outInt = outInt;
	}
	
	public void run() {
		
		String[] proj = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER};
		Cursor contactList = this.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null, null);
		
		int idn = contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int idnu = contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
        String name;
        String num;
        contactList.moveToFirst();
        do {    
        	name = contactList.getString(idn);
        	num = contactList.getString(idnu); // au format +33---------
        	
        	byte[] data = new byte[1+num.length()+name.length()];
        	
        	data[0] = (byte) num.length();
        	for(int i=0;i<num.length();i++) {
        		data[1+i] = (byte) num.charAt(i);
        	}
        	for(int i=0;i<name.length();i++) {
        		data[i+1+num.length()] = (byte) name.charAt(i);
        	}
        	
        	
        	try {
        		this.outInt.sendPacket((byte) 'C', data);
        		Log.i(TAG, "Sending contact");
			} catch (IOException e) {
				Log.e(TAG,"IO issue sending contact");
			}
        	
        } while (contactList.moveToNext());
	}

}
