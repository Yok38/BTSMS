package com.example.btsms;

import java.io.IOException;
import java.io.OutputStream;
import android.database.Cursor;
import android.provider.ContactsContract;

public class ContactSenderThread extends Thread {
	
	private static final int MAX_DATA_SIZE = 253;
	private OutputStream out;
	private Cursor contactList;
	
	ContactSenderThread(Cursor contactList, OutputStream out) {
		this.contactList = contactList;
		this.out = out;
	}
	
	public void run() {
		int idn = this.contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int idnu = this.contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
        String name;
        String num;
        this.contactList.moveToFirst();
        do {    
        	name = this.contactList.getString(idn);
        	if(name.length()+10>MAX_DATA_SIZE) {
        		name = name.substring(0, MAX_DATA_SIZE-10); // pour ne pas que le nom dépasse du packet
        	}
        	num = this.contactList.getString(idnu); // au format +33---------
        	num = "0"+num.substring(3); // au format 06..
        	
        	if(num.length()!= 10) {
        		Log.report("Number's size should be 10", Log.ERROR);
        		break;
        	}
        	
        	byte[] bytesToSend = new byte[256];
        	bytesToSend[0] = 0; // Un seul packet suffit
        	bytesToSend[1] = (byte) (10+name.length()); // Bytes "valides" dans le data
        	bytesToSend[2] = (byte) 'C'; // Type du packet "contact"
        	for(int i=0;i<10;i++) {
        		bytesToSend[i+3] = (byte) num.charAt(i);
        	}
        	for(int i=0;i<name.length();i++) {
        		bytesToSend[i+13] = (byte) name.charAt(i);
        	}
        	
        	
        	try {
        		this.out.write(bytesToSend);
				this.out.flush();
			} catch (IOException e) {
				Log.report("IO issue sending contact", Log.ERROR);
			}
        	
        } while (this.contactList.moveToNext());
	}

}
