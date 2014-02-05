package com.example.btsms;

import java.io.IOException;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;

public class BTSMSActivity extends Activity implements Notifyable {

	private BluetoothAdapter mBluetoothAdapter;
	private View view;
	private ListeningThread l;
	private BroadcastReceiver smsSentReceiver;
	private BroadcastReceiver smsDeliveredReceiver;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.report("Device does not support BT", Log.ERROR);
		} else {

			if (!mBluetoothAdapter.isEnabled()) {
				Log.report("BT not enabled, asking user to enable it", Log.INFO);
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 1);

				while(!mBluetoothAdapter.isEnabled()) {
					try {
						Thread.sleep(500L);
					} catch (InterruptedException e) {
						Log.report("Sleeping interrupted !", Log.WARNING);
					}
				}
			}

			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivity(discoverableIntent);

			Log.report("Local adress : " + mBluetoothAdapter.getAddress(), Log.INFO);
			BTSMSView v = new BTSMSView(this, mBluetoothAdapter);
			this.view = v;
			Log.addObs(this);
			setContentView(v);

			SmsManager smsM = SmsManager.getDefault();

			String[] proj = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER};
			Cursor contactList = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null, null);

			l = new ListeningThread(this,mBluetoothAdapter, smsM, contactList);
			l.start();

		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mBluetoothAdapter.cancelDiscovery();
		try {
			unregisterReceiver(this.smsDeliveredReceiver);
			unregisterReceiver(this.smsSentReceiver);
		} catch(IllegalArgumentException e) {
			Log.report("Can't unregister receivers", Log.WARNING);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.smsSentReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				int idm = intent.getIntExtra("idm", -1);
				int idp = intent.getIntExtra("idp", -1);
				int nClient = intent.getIntExtra("nclient", -1);
				if(idm != -1 && idp != -1 && nClient != -1) {
					Log.report("SMS "+ idm + " sent (part " + idp + ")", Log.INFO);
					
					byte[] bytesToSend = new byte[256];
					bytesToSend[0] = 0;
					bytesToSend[1] = 3;
					bytesToSend[2] = (byte) 'D';
					bytesToSend[3] = 0; // Sent
					bytesToSend[4] = (byte) idm;
					bytesToSend[5] = (byte) idp;
					
					try {
						l.clients_outputs.get(nClient).write(bytesToSend);
						l.clients_outputs.get(nClient).flush();
					} catch (IOException e) {
						Log.report("IO issue sending delivery report to PC", Log.ERROR);
					}
					
				}
			}
		};
		
		this.smsDeliveredReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				int idm = intent.getIntExtra("idm", -1);
				int idp = intent.getIntExtra("idp", -1);
				int nClient = intent.getIntExtra("nclient", -1);
				if(idm != -1 && idp != -1 && nClient != -1) {
					Log.report("SMS "+ idm + " delivered (part " + idp + ")", Log.INFO);
					
					byte[] bytesToSend = new byte[256];
					bytesToSend[0] = 0;
					bytesToSend[1] = 3;
					bytesToSend[2] = (byte) 'D';
					bytesToSend[3] = 1; // Delivered
					bytesToSend[4] = (byte) idm;
					bytesToSend[5] = (byte) idp;
					
					try {
						l.clients_outputs.get(nClient).write(bytesToSend);
						l.clients_outputs.get(nClient).flush();
					} catch (IOException e) {
						Log.report("IO issue sending delivery report to PC", Log.ERROR);
					}
					
				}
			}
		};
		
		registerReceiver(this.smsSentReceiver, new IntentFilter("SMS_SENT"));
		registerReceiver(this.smsSentReceiver, new IntentFilter("SMS_DELIVERED"));
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}


	public void not() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.invalidate();
			}
		});
	}

}
