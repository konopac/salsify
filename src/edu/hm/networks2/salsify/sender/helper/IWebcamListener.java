package edu.hm.networks2.salsify.sender.helper;

import java.awt.image.BufferedImage;

public interface IWebcamListener {

	void receiveFrame(BufferedImage frame);
	
}
