package edu.hm.networks2.salsify.receiver;

/**
 * This interface describes the salsify core of the receiver side for external use and contains all needed methods.
 * 
 * @author Philipp
 */
public interface ISalsifyReceiverCore {

	/**
	 * Start salsify.
	 */
	void start();
	
	/**
	 * Wait for salsify to end. Salsify normally ends when there are no more frames to send and acknowledge.
	 * 
	 * @throws InterruptedException if this thread gets interrupted while waiting
	 */
	void join() throws InterruptedException;
	
}
