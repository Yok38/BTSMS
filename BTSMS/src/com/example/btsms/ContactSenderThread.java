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
			} catch (IOException e) {
				Log.report("IO issue sending contact", Log.ERROR);
			}
        	
        } while (this.contactList.moveToNext());
	}

}
