package com.example.btsms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class ReadingThread extends Thread {

	private InputStream inputStream;
	private SmsManager smsm;
	private Context context;
	private int nClient;

	public ReadingThread(Context context, InputStream inputStream, SmsManager smsm, int nClient) {
		this.inputStream = inputStream;
		this.smsm = smsm;
		this.context = context;
		this.nClient = nClient;
	}

	private byte[] readPacket() throws IOException {

		byte[] buffer = new byte[256];
		byte[] packet = null;
		int lu = 0;


		lu = this.inputStream.read(buffer);

		while(lu<buffer.length) {
			buffer[lu]=(byte) (this.inputStream.read());
			lu++;
		}

		// Un buffer entier a été lu

		if(buffer[0] == 0) {
			// Pas de suite
			packet = Arrays.copyOfRange(buffer, 2, (buffer[1]&0xff)+2+1);
		} 
		else {
			// Suite
			byte[] suite = readPacket();
			if(byteToChar(suite[0])=='T') {
				packet = concat(Arrays.copyOfRange(buffer, 2, (buffer[1]&0xff)+2+1),Arrays.copyOfRange(suite,1,suite.length));
			}
			else {
				Log.report("\"Then\" packet was expected", Log.WARNING);
			}
		}

		return packet;
	}

	private char byteToChar(byte b) {
		return (char) (b&0xff);
	}

	private byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length+b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	public void run() {
		byte[] packet;

		while(true) {
			try {
				packet = readPacket();
				switch(byteToChar(packet[0])) {
				case 'S':
					int id = packet[1]&0xff;
					String num = "";
					for(int i=2;i<=11;i++) {
						num += byteToChar(packet[i]);
					}
					String message = "";
					for(int i=12;i<packet.length;i++) {
						message += byteToChar(packet[i]);
					}
					
					Log.report("Packet.length = "+packet.length, Log.INFO);
					Log.report("Number : "+num, Log.INFO);
					Log.report("Message : "+message, Log.INFO);
					
					ArrayList<String> parts = this.smsm.divideMessage(message);

					ArrayList<PendingIntent> deliveredPIList = new ArrayList<PendingIntent>();
					for(int j=0;j<parts.size();j++) {
						Intent deliveredIntent = new Intent("SMS_DELIVERED");
						deliveredIntent.putExtra("idm", id);
						deliveredIntent.putExtra("idp", j);
						deliveredIntent.putExtra("nclient", this.nClient);
						deliveredPIList.add(PendingIntent.getBroadcast(this.context, 0, deliveredIntent, PendingIntent.FLAG_CANCEL_CURRENT));
					}

					ArrayList<PendingIntent> sentPIList = new ArrayList<PendingIntent>();
					for(int j=0;j<parts.size();j++) {
						Intent sentIntent = new Intent("SMS_SENT");
						sentIntent.putExtra("idm", id);
						sentIntent.putExtra("idp", j);
						sentIntent.putExtra("nclient", this.nClient);
						sentPIList.add(PendingIntent.getBroadcast(this.context, 0, sentIntent, PendingIntent.FLAG_CANCEL_CURRENT));
					}				

					this.smsm.sendMultipartTextMessage(num, null, parts, sentPIList, deliveredPIList);
					break;
				}
			} catch (IOException e) {
				Log.report("Pb reading buffer", Log.ERROR);
			}


		}
	}
}
