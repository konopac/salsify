package edu.hm.networks2.salsify.sender.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.hm.networks2.salsify.common.implementation.SalsifyAck;
import edu.hm.networks2.salsify.sender.helper.IAckListener;

public class AckReceiver extends Thread {

	private DatagramSocket socket;
	
	private List<IAckListener> listeners;
	
	public AckReceiver(DatagramSocket socket) {
		this.socket = socket;
		this.listeners = new ArrayList<>();
	}
	
	@Override
	public void run() {
		// allocate memory for incoming ack data
		final byte[] ackData = ByteBuffer.allocate(SalsifyAck.SIZE).array();
		final DatagramPacket ack = new DatagramPacket(ackData, ackData.length);
		// loop infinite
		while (!isInterrupted()) {
			try {
				System.out.println("blocking until ACK arrives");
				// block until an ACK arrives
				socket.receive(ack);
			} catch (SocketException exception) {
				// thread is stopping when socket is closed
			} catch (IOException exception) {
				System.err.println("Salsify Sender had problems receiving ACKs.");
				exception.printStackTrace();
			}
			// extract data from reveived ACK
			final SalsifyAck salsifyAck = new SalsifyAck(ackData);
			// notify listeners about the change
			listeners.forEach(listener -> listener.receiveValue(salsifyAck.getInterArrivalTime()));
		}
	}
	
	@Override
	public void interrupt() {
		super.interrupt();
		socket.close();
	}
	
	public void register(IAckListener listener) {
		listeners.add(listener);
	}
}
