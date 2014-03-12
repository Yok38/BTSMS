package com.example.btsms;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class OutputInterface extends Thread {

	private final int MAX_DATA_SIZE;
	private OutputStream out;

	public OutputInterface(OutputStream out, int packetSize) {
		MAX_DATA_SIZE = packetSize-3;
		this.out = out;
	}

	public void sendPacket(byte type, byte[] data) throws IOException {
		ArrayList<byte[]> packets = formatPackets(type, data);
		for(byte[] packet:packets) {
			out.write(packet);
		}
		out.flush();
	}

	private ArrayList<byte[]> formatPackets(byte type, byte[] data) {

		ArrayList<byte[]> packets = new ArrayList<byte[]>();
		byte[] packet;

		int nbPacket = data.length/MAX_DATA_SIZE;
		if(data.length%MAX_DATA_SIZE > 0) nbPacket++;

		for(int i=0;i<nbPacket;i++) {
			packet = new byte[256];
			packet[0] = (byte) ((i==nbPacket-1)?0:1);
			packet[1] = validBytes(i,data.length);
			packet[2] = (i==0?type:(byte)'T');
			System.arraycopy(data, i*MAX_DATA_SIZE, packet, 3, packet[1]&0xff);
			packets.add(packet);
		}
		
		return packets;
	}

	private byte validBytes(int nPacket, int dataLength) {
		
		int nbPacket = dataLength/MAX_DATA_SIZE;
		if(dataLength%MAX_DATA_SIZE > 0) nbPacket++;
				
		if(nPacket+1<nbPacket) {
			return (byte) MAX_DATA_SIZE;
		}
		else if(nPacket+1 == nbPacket) {
			if(dataLength%MAX_DATA_SIZE == 0) {
				return (byte) MAX_DATA_SIZE;
			}
			else {
				return (byte) (dataLength%MAX_DATA_SIZE);
			}
		}
		else {
			return 0;
		}	
	}
}
