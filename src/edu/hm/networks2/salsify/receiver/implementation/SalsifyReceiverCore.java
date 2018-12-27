package edu.hm.networks2.salsify.receiver.implementation;

import java.awt.image.BufferedImage;
import java.util.Optional;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.receiver.IReceiver;
import edu.hm.networks2.salsify.receiver.ISalsifyReceiverCore;
import edu.hm.networks2.salsify.receiver.IScreen;
import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

public class SalsifyReceiverCore implements ISalsifyReceiverCore, IReceiverListener {

    private final IScreen screen;
    private final ICodec codec;
    private final IReceiver receiver;

    /**
     * Stores source states which can be used for decoding
     */
    private final Map<Integer, BufferedImage> sourceStates;

    public SalsifyReceiverCore() {
        screen = new Screen();
        codec = new Codec();
        receiver = new Receiver();
        sourceStates = new TreeMap<>();
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
    	
    	// debug purposes
    	try {
    		BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
    		File outputfile = new File("results/receiver-diff" + frameIndex + ".jpg");
    		ImageIO.write(image, "jpg", outputfile);
    	} catch (IOException exception) {
    		System.out.println("error occured while writing file to disk");
    	}

    	
    	System.out.println("SALSIFY: \t processing frame " + frameIndex + " based on source with index " + sourceFrameIndex + " with size " + data.length);

        Optional<BufferedImage> sourceState;
        // has no source state?
        if (sourceFrameIndex == -1) {
            sourceState = Optional.empty();
        } else if (sourceStates.containsKey(sourceFrameIndex)) {
            sourceState = Optional.empty();
//            sourceState = Optional.of(sourceStates.get(sourceFrameIndex));
        } else {
            System.err.println("SALSIFY: \t fatal error source frame index (" + sourceFrameIndex + ")  is not stored!");
            sourceState = Optional.empty();
        }
        
        final Optional<BufferedImage> frame = codec.decode(sourceState, data);
        
        if (frame.isPresent()) {
            screen.displayFrame(frame.get());
            sourceStates.put(frameIndex, frame.get());
            try {
                // retrieve image
                File outputfile = new File("results/result" + frameIndex + ".jpg");
                ImageIO.write(frame.get(), "jpg", outputfile);
            } catch (IOException exception) {
                System.out.println("error occured while writing file to disk");
            }
        }

    }

}
