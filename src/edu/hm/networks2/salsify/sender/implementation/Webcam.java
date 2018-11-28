package edu.hm.networks2.salsify.sender.implementation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import edu.hm.networks2.salsify.sender.IWebcam;
import edu.hm.networks2.salsify.sender.helper.IWebcamListener;

public class Webcam implements IWebcam {

	private List<IWebcamListener> listeners;
	
	public Webcam() {
		listeners = new ArrayList<>();
	}
	
	@Override
	public void start() {
		produceFrame();
	}
	
	private void produceFrame() {
		// produce a new frame
		final BufferedImage frame = null;
		// send the frame to all listeners
		listeners.forEach(listener -> listener.receiveFrame(frame));
	}

}
