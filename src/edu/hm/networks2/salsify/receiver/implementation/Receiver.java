package edu.hm.networks2.salsify.receiver.implementation;

import java.util.ArrayList;
import java.util.List;

import edu.hm.networks2.salsify.receiver.IReceiver;
import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

public class Receiver implements IReceiver {

	final List<IReceiverListener> listeners;
	
	public Receiver() {
		listeners = new ArrayList<>();
	}
	
	@Override
	public void sendAck(int frameIndex, int fragmentIndex) {
		System.out.println("sending ack");
	}

	@Override
	public void register(IReceiverListener listener) {
		listeners.add(listener);
	}

	@Override
	public void start() {
		System.out.println("receiving...");
	}

}
