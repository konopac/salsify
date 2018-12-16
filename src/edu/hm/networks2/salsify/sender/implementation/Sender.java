package edu.hm.networks2.salsify.sender.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import edu.hm.networks2.salsify.common.implementation.SalsifyAck;
import edu.hm.networks2.salsify.common.implementation.SalsifyFrame;
import edu.hm.networks2.salsify.sender.ISender;

/**
 * @author Philipp
 */
public class Sender implements ISender {
	
	/**
	 * @author Philipp
	 */
	private class AckReceiver extends Thread {

		@Override
		public void run() {
			System.out.println("ACK-RECEIVER: \t waiting for ACKs");
			// allocate memory for incoming ack data
			final byte[] ackData = ByteBuffer.allocate(SalsifyAck.SIZE).array();
			final DatagramPacket ack = new DatagramPacket(ackData, ackData.length);
			// loop infinite
			while (!isInterrupted()) {
				try {
					// block until an ACK arrives
					socket.receive(ack);
					// extract data from reveived ACK
					final SalsifyAck salsifyAck = new SalsifyAck(ackData);
					System.out.println("ACK-RECEIVER: \t received ACK for frame " + salsifyAck.getFrameIndex() + " fragment " + salsifyAck.getFragmentIndex());
					// set new inter arrival time
					setLatestInterArrivalTime(salsifyAck.getInterArrivalTime());
				} catch (SocketException exception) {
					// thread is stopping when socket is closed
					System.out.println("ACK-RECEIVER: \t stopped waiting for ACKs");
				} catch (IOException exception) {
					System.err.println("Salsify Sender had problems receiving ACKs.");
					exception.printStackTrace();
				}
			}
		}
		
		@Override
		public void interrupt() {
			super.interrupt();
			socket.close();
		}
	}
	
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
		
		ackReceiver = new AckReceiver();
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
	
	private void setLatestInterArrivalTime(int latestInterArrivalTime) {
		this.latestInterArrivalTime = latestInterArrivalTime;
	}
	
	@Override
	public void stopListening() {
		ackReceiver.interrupt();
	}
	
	
	@Override
	public void join() throws InterruptedException {
		ackReceiver.join();
	}
	
}
