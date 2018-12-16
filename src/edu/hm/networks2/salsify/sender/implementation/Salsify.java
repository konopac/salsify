package edu.hm.networks2.salsify.sender.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.sender.ISalsify;
import edu.hm.networks2.salsify.sender.ISender;
import edu.hm.networks2.salsify.sender.IWebcam;
import edu.hm.networks2.salsify.sender.helper.IWebcamListener;

public class Salsify implements ISalsify, IWebcamListener {

	private IWebcam webcam;
	private ICodec codec;
	private ISender sender;
	
	public Salsify() {
		webcam = new Webcam();
		codec = new Codec();
		sender = new Sender();
	}
	
	@Override
	public void start() {
		webcam.register(this);
		webcam.start();
	}
	
	@Override
	public void receiveFrame(BufferedImage frame) {
		System.out.println("Received frame: " + frame);
		// encode the frame
//		final byte[] encodedFrame = codec.encode(null, frame, 1).get();
		// send encoded frame
//		sender.sendFrame(encodedFrame);
	}

}
