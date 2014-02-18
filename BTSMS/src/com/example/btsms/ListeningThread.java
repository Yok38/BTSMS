package com.example.btsms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

public class ListeningThread extends Thread {

	private static final String TAG = "ListeningThread";
	private BluetoothAdapter mBTAdapter;
	private SmsManager smsm;
	private final int PACKET_SIZE = 256;
	public ArrayList<ReadingThread> clientThreads = new ArrayList<ReadingThread>();
	private boolean mustStop = false;
	private ContentResolver contentResolver;
	private Context context;

	public ListeningThread(Context context, ContentResolver contentResolver, BluetoothAdapter mBTAdapter, SmsManager smsm) {
		this.mBTAdapter = mBTAdapter;
		this.smsm = smsm;
		this.contentResolver = contentResolver;
		this.context = context;
	}

	public void run() {

		BluetoothServerSocket btss = null;
		try {
			btss = this.mBTAdapter.listenUsingRfcommWithServiceRecord("BTAndroid", UUID.fromString("2bbef510-875d-11e3-baa7-0800200c9a66"));
		} catch (IOException e) {
			Log.e(TAG,"Pb initializing the listening socket");
		}

		while(! this.mustStop ) {
			BluetoothSocket bts = null;
			try {
				bts = btss.accept();
			} catch (IOException e) {
				Log.e(TAG,"IO issue sending accepting the connection");
			}

			InputInterface inInt = null;
			try {
				inInt = new InputInterface(bts.getInputStream(),PACKET_SIZE);
			} catch (IOException e) {
				Log.e(TAG,"IO issue getting the input stream");
			}

			OutputInterface outInt = null;
			try {
				
				outInt = new OutputInterface(bts.getOutputStream(),PACKET_SIZE);

				(new ContactSenderThread(this.contentResolver,outInt)).start();
			} catch (IOException e) {
				Log.e(TAG,"IO issue getting the output stream");
			}

			ReadingThread r = new ReadingThread(this, this.context, this.contentResolver, inInt,outInt,smsm);
			this.clientThreads.add(r);
			r.start();

		}

	}

	public void stopService() {
		this.mustStop = true;
		for(ReadingThread r:this.clientThreads) {
			r.stopService();
		}
	}
}
