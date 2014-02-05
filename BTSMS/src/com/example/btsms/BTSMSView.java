package com.example.btsms;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class BTSMSView extends View {

	private int w;
	private int h;
	private Paint paint = new Paint();
	private int nMsg = 30;

	public BTSMSView(Context context, BluetoothAdapter mBluetoothAdapter) {
		super(context);
		
//		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//		Log.report(pairedDevices.size()+" paired devices.", Log.INFO);
//	    for (BluetoothDevice device : pairedDevices) {
//	        Log.report(device.getName() + ":" + device.getAddress(),Log.INFO);
//	    }
//	    
//	    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//	        public void onReceive(Context context, Intent intent) {
//	            String action = intent.getAction();
//	            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//	                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//	                Log.report(device.getName() + ":" + device.getAddress(),Log.INFO);
//	            }
//	        }
//	    };
//
//	    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//	    context.registerReceiver(mReceiver, filter);
//	    Log.report("Starting discovery", Log.INFO);
//	    if (mBluetoothAdapter.isDiscovering()) {
//	    	mBluetoothAdapter.cancelDiscovery();
//	    }
//	    mBluetoothAdapter.startDiscovery();
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		paint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, this.w, this.h, this.paint);
		paint.setColor(Color.BLACK);
		for(int i=1; i<=Math.min(this.nMsg, Log.log.size()); i++) {
			canvas.drawText(Log.log.get(Log.log.size()-i).toString(), 0, i*this.paint.getTextSize(), this.paint);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//Log.report("Touch event " + (int) event.getX() + "," + (int) event.getY(), Log.INFO);
		return false;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		this.w = w;
		this.h = h;
		this.paint.setTextSize((int) (this.h/this.nMsg));
	}

}
