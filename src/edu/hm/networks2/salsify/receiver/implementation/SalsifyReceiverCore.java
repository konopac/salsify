package edu.hm.networks2.salsify.receiver.implementation;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.common.implementation.GlobalLogger;
import edu.hm.networks2.salsify.receiver.IReceiver;
import edu.hm.networks2.salsify.receiver.ISalsifyReceiverCore;
import edu.hm.networks2.salsify.receiver.IScreen;
import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

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
//    	try {
//    		BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
//    		File outputfile = new File("results" + File.separator + "receiver-diff" + frameIndex + ".jpg");
//    		ImageIO.write(image, "jpg", outputfile);
//    	} catch (IOException exception) {
//    		System.out.println("error occured while writing file to disk");
//    	}

    	
        GlobalLogger.getInstance().log(Level.INFO, "Received complete frame with index {0} based on frame with index {1} from transport layer! Decoding and displaying now!", new Object[]{frameIndex, sourceFrameIndex});

        Optional<BufferedImage> sourceState;
        // has no source state?
        if (sourceFrameIndex == -1) {
            sourceState = Optional.empty();
        } else if (sourceStates.containsKey(sourceFrameIndex)) {
            sourceState = Optional.empty();
            // this is to disable encoding based on a source state
            // we will always send encoding based only on the current image
            // sourceState = Optional.of(sourceStates.get(sourceFrameIndex));
        } else {
            GlobalLogger.getInstance().log(Level.SEVERE, "Source frame index ({0})  is not stored! Needed for frame with index {1}.", new Object[]{sourceFrameIndex, frameIndex});
            sourceState = Optional.empty();
        }
        
        final Optional<BufferedImage> frame = codec.decode(sourceState, data);
        
        if (frame.isPresent()) {
            screen.displayFrame(frame.get());
            sourceStates.put(frameIndex, frame.get());
            
            // debug purposes
//            try {
//                // retrieve image
//                File outputfile = new File("results" + File.separator + "result" + frameIndex + ".jpg");
//                ImageIO.write(frame.get(), "jpg", outputfile);
//            } catch (IOException exception) {
//                System.out.println("error occured while writing file to disk");
//            }
        }

    }

}
