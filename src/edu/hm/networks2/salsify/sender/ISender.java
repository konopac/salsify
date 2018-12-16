package edu.hm.networks2.salsify.sender;

import java.io.IOException;

/**
 * This interface describes a sender and contains all needed methods.
 * 
 * @author Philipp
 */
public interface ISender {

	/**
	 * Send a frame to the receiver.
	 * 
	 * @param frame contains the frame data.
	 * @param frameIndex is the index of the frame and new state of the codec
	 * @param sourceFrameIndex is the source state of the codec
	 * @param gracePeriod is the grace period
	 * @throws IOException if sending fails
	 */
	void sendFrame(byte[] frame, int frameIndex, int sourceFrameIndex, int gracePeriod) throws IOException;
	
	/**
	 * Get the latest measured inter-arrival time.
	 * 
	 * @return the inter-arrival time
	 */
	int getLatestInterArrivalTime();
	
	/**
	 * Stop listening for ACKs to the network.
	 */
	void stopListening();
	
	/**
	 * Wait until the Sender is done waiting for ACKs.
	 * 
	 * @throws InterruptedException if the thread gets interrupted while waiting
	 */
	void join() throws InterruptedException;
	
}
