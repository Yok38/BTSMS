package com.example.btsms;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import android.util.Log;


public class InputInterface {
	
	private static final String TAG = "InputInterface";
	private final int PACKET_SIZE;
	private InputStream in;
	
	public InputInterface(InputStream in, int packetSize) {
		PACKET_SIZE = packetSize;
		this.in = in;
	}
	
	public byte[] readPacket() throws IOException {

		byte[] buffer = new byte[PACKET_SIZE];
		byte[] packet = null;
		
		int lu = this.in.read(buffer);

		while(lu<PACKET_SIZE) {
			buffer[lu]=(byte) (this.in.read());
			lu++;
		}

		// Un buffer entier a été lu
		// buffer : suite? ; bytes valides ; type packet

		if(buffer[0] == 0) {
			// Pas de suite
			packet = Arrays.copyOfRange(buffer, 2, (buffer[1]&0xff)+2+1);
		} 
		else {
			// Suite
			byte[] suite = readPacket();
			if((char) suite[0]=='T') {
				packet = concat(Arrays.copyOfRange(buffer, 2, (buffer[1]&0xff)+2+1),Arrays.copyOfRange(suite,1,suite.length));
			}
			else {
				Log.w(TAG,"Then packet was expected");
			}
		}

		return packet;
	}

	private byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length+b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}
