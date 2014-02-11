package com.example.btsms;

import java.io.IOException;
import android.database.Cursor;
import android.provider.ContactsContract;

public class ContactSenderThread extends Thread {
	
	private OutputInterface outInt;
	private Cursor contactList;
	
	ContactSenderThread(Cursor contactList, OutputInterface outInt) {
		this.contactList = contactList;
		this.outInt = outInt;
	}
	
	public void run() {
		int idn = this.contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int idnu = this.contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
        String name;
        String num;
        this.contactList.moveToFirst();
        do {    
        	name = this.contactList.getString(idn);
        	num = this.contactList.getString(idnu); // au format +33---------
        	num = "0"+num.substring(3); // au format 06..
        	
        	if(num.length()!= 10) {
        		Log.report("Number's size should be 10", Log.ERROR);
        		break;
        	}
        	
        	byte[] data = new byte[10+name.length()];
        	for(int i=0;i<10;i++) {
        		data[i] = (byte) num.charAt(i);
        	}
        	for(int i=0;i<name.length();i++) {
        		data[i+10] = (byte) name.charAt(i);
        	}
        	
        	
        	try {
        		this.outInt.sendPacket((byte) 'C', data);
			} catch (IOException e) {
				Log.report("IO issue sending contact", Log.ERROR);
			}
        	
        } while (this.contactList.moveToNext());
	}

}
