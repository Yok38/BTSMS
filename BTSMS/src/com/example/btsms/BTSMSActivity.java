package com.example.btsms;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;

public class BTSMSActivity extends Activity implements Notifyable {

	private BluetoothAdapter mBluetoothAdapter;
	public ListeningThread l;
	private BroadcastReceiver smsSentReceiver;
	private BroadcastReceiver smsDeliveredReceiver;
	private BroadcastReceiver smsReceived;
	private ArrayAdapter<String> logAdapter;

	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_btsms);

		ListView listView = (ListView) findViewById(R.id.list);
		ArrayList<String> messages = new ArrayList<String>();
		logAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages);
		logAdapter.notifyDataSetChanged();
		listView.setAdapter(logAdapter);


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

			Log.addObs(this);

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
			unregisterReceiver(this.smsReceived);
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
					byte[] data = new byte[]{0,(byte) idm,(byte) idp};

					try {
						l.clientsOutInt.get(nClient).sendPacket((byte) 'D', data);
						Log.report("Delivery to client #"+nClient, Log.INFO);
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

					byte[] data = new byte[]{1,(byte) idm,(byte) idp};

					try {
						l.clientsOutInt.get(nClient).sendPacket((byte) 'D', data);
					} catch (IOException e) {
						Log.report("IO issue sending delivery report to PC", Log.ERROR);
					}

				}
			}
		};

		this.smsReceived = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle myBundle = intent.getExtras();

				if (myBundle != null)
				{
					Object [] pdus = (Object[]) myBundle.get("pdus");

					for (int j = 0; j < pdus.length; j++)
					{
						SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[j]);
						Message m = new Message(smsMessage.getTimestampMillis(), smsMessage.getMessageBody(), true);
						String number = smsMessage.getOriginatingAddress();
						
						byte[] data = new byte[2+number.length()+8+m.body.length()];
						data[0] = (byte) (m.received?1:0);
						data[1] = (byte) number.length();
						for(int i=0;i<number.length();i++) {
							data[i+2] = (byte) number.charAt(i);
						}
						for(int i=0;i<8;i++) {
							data[i+2+number.length()] = (byte) (m.date >>> (7-i)*8);
						}
						for(int i=0;i<m.body.length();i++) {
							data[i+2+number.length()+8] = (byte) m.body.charAt(i);
						}

						try {
							for(OutputInterface outInt:l.clientsOutInt) {
								outInt.sendPacket((byte) 'H', data);
								Log.report("Sent received message", Log.INFO);
							}
						} catch (IOException e) {
							Log.report("Pb sending received message", Log.ERROR);
						}
					}
				}
			}
		};

		registerReceiver(this.smsSentReceiver, new IntentFilter("SMS_SENT"));
		registerReceiver(this.smsDeliveredReceiver, new IntentFilter("SMS_DELIVERED"));
		registerReceiver(this.smsReceived, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}

	@Override
	public void not(final LogEntry l) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logAdapter.insert(l.toString(),0);
				logAdapter.notifyDataSetChanged();
			}
		});
	}

}
