package edu.hm.networks2.salsify.sender;

/**
 * This interface describes the salsify core for external use and contains all needed methods.
 * 
 * @author Philipp
 */
public interface ISalsify {

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
