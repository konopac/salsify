package edu.hm.networks2.salsify.sender.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.sender.ISender;

public class Sender implements ISender {

	@Override
	public void sendFrame(BufferedImage frame) {
		System.out.println("sending frame " + frame + " over network");
	}

	@Override
	public void startReceivingAcks() {
		System.out.println("starting thread to receive ACKs");
	}

}
