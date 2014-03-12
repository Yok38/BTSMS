package com.example.btsms;

import java.io.IOException;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class BTSMSService extends Service {

	protected static final String TAG = "BTSMSService";
	public ListeningThread l;
	private BroadcastReceiver smsSentReceiver;
	private BroadcastReceiver smsDeliveredReceiver;
	private BroadcastReceiver smsReceived;

	public void onCreate() {
		super.onCreate();
		
		l = new ListeningThread(this, getContentResolver(),BluetoothAdapter.getDefaultAdapter(), SmsManager.getDefault());
		l.start();


		this.smsSentReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				int idm = intent.getIntExtra("idm", -1);
				int idp = intent.getIntExtra("idp", -1);
				int nClient = intent.getIntExtra("nclient", -1);
				if(idm != -1 && idp != -1 && nClient != -1) {
					byte[] data = new byte[]{0,(byte) idm,(byte) idp};

					try {
						l.clientThreads.get(nClient).outInt.sendPacket((byte) 'D', data);
					} catch (IOException e) {
						Log.e(TAG,"IO issue sending delivery report to PC");
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
					byte[] data = new byte[]{1,(byte) idm,(byte) idp};

					try {
						l.clientThreads.get(nClient).outInt.sendPacket((byte) 'D', data);
					} catch (IOException e) {
						Log.e(TAG,"IO issue sending delivery report to PC");
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
							for(ReadingThread r:l.clientThreads) {
								r.outInt.sendPacket((byte) 'H', data);
							}
						} catch (IOException e) {
							Log.e(TAG,"Pb sending received message");
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
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(this.smsDeliveredReceiver);
		unregisterReceiver(this.smsSentReceiver);
		unregisterReceiver(this.smsReceived);
		
		l.stopService();
		super.onDestroy();
	}


}
