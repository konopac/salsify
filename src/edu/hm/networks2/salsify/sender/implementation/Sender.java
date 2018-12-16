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
 * This is an implementation of ISender. It sends all frames as UDP-packets and starts
 * an extra thread to listen for ACKs to the network.
 * 
 * @author Philipp
 */
public class Sender implements ISender {
	
	/**
	 * This is an inner class which extends Thread. This thread receives ACKs until it gets interrupted.
	 * 
	 * @author Philipp
	 */
	private class AckReceiver extends Thread {

		/**
		 * {@inheritDoc}<br>
		 * This implementation waits for UDP packets to interpret them as ACKs from the receiver.
		 * 
		 * @see java.lang.Thread#run()
		 */
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
		
		/**
		 * {@inheritDoc}<br>
		 * When this implementation gets interrupted, the socket used for receiving ACKs gets closed.
		 * 
		 * @see java.lang.Thread#interrupt()
		 */
		@Override
		public void interrupt() {
			super.interrupt();
			socket.close();
		}
	}
	
	
	/**
	 * IP address of the receiver.
	 */
	private static final String DESTINATION_IP = "localhost";
	
	/**
	 * Port of the receiver.
	 */
	private static final int DESTINATION_PORT = 48938;
	
	/**
	 * Socket which is used to send frames and receive ACKs.
	 */
	private DatagramSocket socket;
	
	/**
	 * Thread that receives ACKs.
	 */
	private AckReceiver ackReceiver;
	
	/**
	 * Latest measured inter-arrival time.
	 */
	private int latestInterArrivalTime;
	
	
	/**
	 * Constructor that creates the socket for sending frames and receiving ACKs
	 * and starts the thread that receives ACKs.
	 */
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
	

	/**
	 * {@inheritDoc}<br>
	 * This implementation splits a frame into several fragments and uses UDP packets to send them.
	 * 
	 * @see edu.hm.networks2.salsify.sender.ISender#sendFrame(byte[], int, int, int)
	 */
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

	/**
	 * {@inheritDoc}<br>
	 * This implementation is just a normal getter.
	 * 
	 * @see edu.hm.networks2.salsify.sender.ISender#getLatestInterArrivalTime()
	 */
	@Override
	public int getLatestInterArrivalTime() {
		return latestInterArrivalTime;
	}
	
	/**
	 * {@inheritDoc}<br>
	 * This implementation interrupts the threat that receives ACKs.
	 * 
	 * @see edu.hm.networks2.salsify.sender.ISender#stopListening()
	 */
	@Override
	public void stopListening() {
		ackReceiver.interrupt();
	}
	
	/**
	 * {@inheritDoc}<br>
	 * This implementation just joins the threat that receives ACKs.
	 * 
	 * @see edu.hm.networks2.salsify.sender.ISender#join()
	 */
	@Override
	public void join() throws InterruptedException {
		ackReceiver.join();
	}

	
	private void setLatestInterArrivalTime(int latestInterArrivalTime) {
		this.latestInterArrivalTime = latestInterArrivalTime;
	}
	
}
