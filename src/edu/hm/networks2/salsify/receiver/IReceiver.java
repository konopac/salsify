package edu.hm.networks2.salsify.receiver;

import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

/**
 * This interface describes a receiver and contains all needed methods.
 * 
 * @author Philipp
 */
public interface IReceiver {

	/**
	 * Start the receiver.
	 */
	void start();
	
	/**
	 * Register a new listener to this receiver. All updates must be sent to this listener.
	 * 
	 * @param listener is the new listener
	 */
	void register(IReceiverListener listener);
	
	/**
	 * Wait until the Receiver is done waiting for fragments.
	 * 
	 * @throws InterruptedException if the thread gets interrupted while waiting
	 */
	void join() throws InterruptedException;
	
}
