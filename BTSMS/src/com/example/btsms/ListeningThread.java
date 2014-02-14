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
	public ArrayList<OutputInterface> clientsOutInt = new ArrayList<OutputInterface>();
	public ArrayList<InputInterface> clientsInInt = new ArrayList<InputInterface>();
	public int nClients = 0;
	private Cursor contactList;
	private final int PACKET_SIZE = 256;
	
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
			
			InputInterface inInt = null;
			try {
				inInt = new InputInterface(bts.getInputStream(),PACKET_SIZE);
			} catch (IOException e) {
				Log.report("Pb getting the input stream", Log.ERROR);
			}
			
			OutputInterface outInt = null;
			try {
				
				outInt = new OutputInterface(bts.getOutputStream(),PACKET_SIZE);
				this.clientsOutInt.add(outInt);
				
				(new ContactSenderThread(this.contactList,outInt)).start();
			} catch (IOException e) {
				Log.report("Pb getting the output stream", Log.ERROR);
			}
			
			(new ReadingThread(this.context, inInt,outInt,smsm,this.nClients)).start();
			
			this.nClients++;
	}
		
	}
}
