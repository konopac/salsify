package edu.hm.networks2.salsify.sender.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import edu.hm.networks2.salsify.common.implementation.SalsifyFrame;
import edu.hm.networks2.salsify.sender.ISender;
import edu.hm.networks2.salsify.sender.helper.IAckListener;

public class Sender implements ISender, IAckListener {
	
	/**
	 * 
	 */
	private static final String DESTINATION_IP = "localhost";
	
	/**
	 * 
	 */
	private static final int DESTINATION_PORT = 48938;
	
	/**
	 * 
	 */
	private DatagramSocket socket;
	
	/**
	 * 
	 */
	private Thread ackReceiver;
	
	/**
	 * 
	 */
	private int latestInterArrivalTime;
	
	public Sender() {
		latestInterArrivalTime = 0;
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException exception) {
			System.err.println("Salsify Sender had problems opening a DatagramSocket.");
			exception.printStackTrace();
		}
		

		ackReceiver = new AckReceiver(socket);
		ackReceiver.start();
	}
	

	@Override
	public void sendFrame(byte[] data, int frameIndex, int sourceFrameIndex, int gracePeriod) throws IOException {
		// build a salsify frame from input data
		final SalsifyFrame frame = new SalsifyFrame(data, frameIndex, sourceFrameIndex, gracePeriod);
		System.out.print("SENDER: \t sending frame " + frame.getFrameIndex() + " fragment");
		// send each salsify fragment inside the frame
		for (int index = 0; index < frame.getNumberOfFragments(); index++) {
			final byte[] fragment = frame.getFragment(index).getRawPacket();
			System.out.print(" " + frame.getFragment(index).getFragmentIndex());
			socket.send(new DatagramPacket(fragment, fragment.length, InetAddress.getByName(DESTINATION_IP), DESTINATION_PORT));
		}
		System.out.println();
	}

	@Override
	public int getLatestInterArrivalTime() {
		return latestInterArrivalTime;
	}
	
	@Override
	public void stopListening() {
		ackReceiver.interrupt();
	}
	
	@Override
	public void receiveValue(int value) {
		latestInterArrivalTime = value;
	}
	
	@Override
	public void join() throws InterruptedException {
		ackReceiver.join();
	}

}
