package edu.hm.networks2.salsify.sender.implementation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
	
	private List<BufferedImage> frames;
	private int currentFrame;
	
	public Salsify() {
		webcam = new Webcam();
		codec = new Codec();
		sender = new Sender();
		
		frames = new ArrayList<>();
		currentFrame = 0;
	}
	
	@Override
	public void start() {
		webcam.register(this);
		webcam.start();
	}
	
	@Override
	public void receiveFrame(BufferedImage frame) {
		System.out.println("SALSIFY: \t processing frame " + currentFrame);
		// store frame until this frame is acknowledged by the receiver
		frames.add(frame);
		
		// get the source state
		final Optional<BufferedImage> source;
		if (currentFrame <= 0) {
			source = Optional.empty();
		} else {
			source = Optional.of(frames.get(currentFrame - 1));
		}
		
		// encode the frame
		final byte[] encodedFrame = codec.encode(source, frame, 1).get();
		
		// send encoded frame
		try {
			sender.sendFrame(encodedFrame, currentFrame, currentFrame - 1, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		currentFrame++;
	}
	
	@Override
	public void disconnected() {
		sender.stopListening();
	}
	
	@Override
	public void join() throws InterruptedException {
		sender.join();
	}

}
