package com.example.btsms;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;

public class BTSMSActivity extends Activity  {

	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_DISCOVERABLE = 2;
	private static final String TAG = "BTSMSActivity";
	private Button button;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean readyForService = false;
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_btsms);

		this.button = (Button) findViewById(R.id.button1);
		this.button.setText("Starting app...");
		
		this.button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(isMyServiceRunning()) {
					stopService(new Intent(BTSMSActivity.this, BTSMSService.class));
					button.setText("Start service");
				}
				else {
					if(readyForService) {
						startService(new Intent(BTSMSActivity.this, BTSMSService.class));
						button.setText("Stop service");
					}
				}

			}
		});

		
		if(isMyServiceRunning()) {
			button.setText("Stop service");
		}
		else {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				this.button.setText("Device does not support BT");
			} else {

				if (mBluetoothAdapter.isEnabled()) {
					makeDiscoverable();
				}
				else {
					enableBT();
				}
			}
		}
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (BTSMSService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void makeDiscoverable() {
		this.button.setText("Making device discoverable...");
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
	}


	private void enableBT() {
		this.button.setText("Enabling bluetooth...");
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_ENABLE_BT:
			if(resultCode == RESULT_OK) makeDiscoverable();
			break;
		case REQUEST_DISCOVERABLE:
			if(resultCode != RESULT_CANCELED) {
				this.readyForService = true;
				this.button.setText("Start service");
			}
		}
	}


}
