package edu.hm.networks2.salsify.receiver;

import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

/**
 * This interface describes a receiver and contains all needed methods.
 * 
 * @author Philipp
 */
public interface IReceiver {

	/**
	 * Send an acknowledgement.
	 * 
	 * @param frameIndex is the index of the acknowledged frame
	 * @param fragmentIndex is the index of the fragment inside the frame
	 */
	void sendAck(int frameIndex, int fragmentIndex);
	
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
	
}
