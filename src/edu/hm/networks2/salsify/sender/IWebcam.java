package edu.hm.networks2.salsify.sender;

import edu.hm.networks2.salsify.sender.helper.IWebcamListener;

/**
 * This interface describes a webcam and contains all needed methods.
 * 
 * @author Philipp
 */
public interface IWebcam {
	
	/**
	 * Start the webcam.
	 */
	void start();
	
	/**
	 * Register a new listener to this webcam. All updates must be sent to this listener.
	 * 
	 * @param listener is the new listener
	 */
	void register(IWebcamListener listener);
	
}
