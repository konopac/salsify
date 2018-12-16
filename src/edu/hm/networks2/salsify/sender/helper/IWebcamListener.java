package edu.hm.networks2.salsify.sender.helper;

import java.awt.image.BufferedImage;

/**
 * This interface describes a listener to a webcam and contains all needed methods.
 * 
 * @author Philipp
 */
public interface IWebcamListener {

	/**
	 * This method gets called every time a new frame is available.
	 * 
	 * @param frame is the new frame
	 */
	void receiveFrame(BufferedImage frame);
	
	/**
	 * This method gets called when the webcam stops working.
	 */
	void disconnected();
	
}
