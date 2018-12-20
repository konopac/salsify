package edu.hm.networks2.salsify.receiver.implementation;

import java.awt.image.BufferedImage;
import java.util.Optional;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.receiver.IReceiver;
import edu.hm.networks2.salsify.receiver.ISalsifyReceiverCore;
import edu.hm.networks2.salsify.receiver.IScreen;
import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

public class SalsifyReceiverCore implements ISalsifyReceiverCore, IReceiverListener {

	private final IScreen screen;
	private final ICodec codec;
	private final IReceiver receiver;
	
	public SalsifyReceiverCore() {
		screen = new Screen();
		codec = new Codec();
		receiver = new Receiver();
	}
	
	@Override
	public void start() {
		screen.start();
		receiver.register(this);
		receiver.start();
	}

	@Override
	public void join() throws InterruptedException {
		receiver.join();
	}

	@Override
	public void receiveFrame(byte[] data, int frameIndex, int sourceFrameIndex) {
        System.out.println("SALSIFY: \t processing frame " + frameIndex + "...");
		// TODO: store source state and use it here
		final Optional<BufferedImage> frame = codec.decode(Optional.empty(), data);
		screen.displayFrame(frame.get());
	}

}
