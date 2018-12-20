package edu.hm.networks2.salsify.receiver;

import java.awt.image.BufferedImage;

/**
 * This interface describes a computer screen and contains all needed methods.
 * 
 * @author Philipp
 */
public interface IScreen {

	/**
	 * Start the screen. It must not display data yet.
	 */
	void start();
	
	/**
	 * Display a frame on this screen.
	 * 
	 * @param frame is the frame to be displayed
	 */
	void displayFrame(BufferedImage frame);

}
