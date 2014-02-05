package com.example.btsms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.database.Cursor;
import android.telephony.SmsManager;

public class ListeningThread extends Thread {
	
	private BluetoothAdapter mBTAdapter;
	private SmsManager smsm;
	private Context context;
	public ArrayList<OutputStream> clients_outputs = new ArrayList<OutputStream>();
	public int nClients = 0;
	private Cursor contactList;
	
	public ListeningThread(Context context, BluetoothAdapter mBTAdapter, SmsManager smsm, Cursor contactList) {
		this.mBTAdapter = mBTAdapter;
		this.smsm = smsm;
		this.context = context;
		this.contactList = contactList;
	}
	
	public void run() {
		
		BluetoothServerSocket btss = null;
		try {
			btss = this.mBTAdapter.listenUsingRfcommWithServiceRecord("BTAndroid", UUID.fromString("2bbef510-875d-11e3-baa7-0800200c9a66"));
		} catch (IOException e) {
			Log.report("Pb initializing the listening socket", Log.ERROR);
		}
		
		while(true) {
			BluetoothSocket bts = null;
			try {
				Log.report("Waiting for a connection...", Log.INFO);
				bts = btss.accept();
			} catch (IOException e) {
				Log.report("Pb accepting the connection", Log.ERROR);
			}
			
			InputStream inputStream = null;
			try {
				inputStream = bts.getInputStream();
			} catch (IOException e) {
				Log.report("Pb getting the input stream", Log.ERROR);
			}
			(new ReadingThread(this.context, inputStream,smsm,this.nClients)).start();
			
			OutputStream out = null;
			try {
				out = bts.getOutputStream();
				this.clients_outputs.add(out);
				(new ContactSenderThread(this.contactList,out)).start();
			} catch (IOException e) {
				Log.report("Pb getting the output stream", Log.ERROR);
			}
			
			this.nClients++;
	}
		
	}
}
