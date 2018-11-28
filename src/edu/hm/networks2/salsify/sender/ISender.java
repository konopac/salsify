package edu.hm.networks2.salsify.sender;

import java.awt.image.BufferedImage;

public interface ISender {

	void sendFrame(BufferedImage frame);
	
	void startReceivingAcks();
	
}
