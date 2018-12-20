package edu.hm.networks2.salsify.sender.implementation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.sender.ISalsifySenderCore;
import edu.hm.networks2.salsify.sender.ISender;
import edu.hm.networks2.salsify.sender.IWebcam;
import edu.hm.networks2.salsify.sender.helper.ITransportProtocolListener;
import edu.hm.networks2.salsify.sender.helper.IWebcamListener;
import java.util.HashMap;
import java.util.Map;

public class SalsifySenderCore implements ISalsifySenderCore, IWebcamListener, ITransportProtocolListener {

    private final IWebcam webcam;
    private final ICodec codec;
    private final ISender sender;

    /**
     * Maps integer (frame index) to buffered image (frame).
     */
    private final Map<Integer, BufferedImage> frames;

    /**
     * The next frame will be based on this source frame.
     */
    private int sourceFrameIndex;

    /**
     * The index of the current frame.
     */
    private int currentFrameIndex;

    /**
     * The quality of the last frame [0, 100] where 100 is best possible quality
     * --> biggest frame size.
     */
    private int lastFrameQuality;

    /**
     * Locks access to source frame index.
     */
    private final Object lock;

    public SalsifySenderCore() {
        webcam = new Webcam();
        codec = new Codec();
        sender = new Sender(this);
        frames = new HashMap<>();
        frames.put(-1, null);
        sourceFrameIndex = -1;
        lastFrameQuality = 70;  // 70% quality is jpeg default
        lock = new Object();
    }

    @Override
    public void start() {
        webcam.register(this);
        webcam.start();
    }

    @Override
    public void receiveFrame(BufferedImage frame) {
        System.out.println("SALSIFY: \t processing frame " + currentFrameIndex);
        // store frame until this frame is acknowledged by the receiver
        frames.put(currentFrameIndex, frame);

        // this is the sourcee state for the encoder
        // (the new frame will be encoded on source)
        final Optional<BufferedImage> source;
        if (getSourceFrameIndex() == -1) {
            source = Optional.empty();
        } else {
            //
            source = Optional.of(frames.get(getSourceFrameIndex()));
        }

        // encode the frame
        int qualityBetter = lastFrameQuality + 5;
        int qualityWorse = lastFrameQuality - 5;
        if (qualityBetter > 100) {
            qualityBetter = 100;
        }
        if (qualityWorse < 0) {
            qualityWorse = 0;
        }

        final byte[] encodedFrameBetter
                = codec.encode(source, frame, qualityBetter).get();

        final byte[] encodedFrameWorse
                = codec.encode(source, frame, qualityWorse).get();

        // we need a bandwidht estimation to decide which frame to send
        int bandwidthEstimate = sender.getLatestBandwidth();

        if (bandwidthEstimate == -1) {
            // it seems like there is no bandwidth data available yet

            // send the lower quality version
            try {
                sender.sendFrame(encodedFrameWorse, currentFrameIndex, getSourceFrameIndex(), 0);
            } catch (IOException exception) {
                System.out.println(exception.toString());
            }
            lastFrameQuality = qualityWorse;
            System.out.println("SALSIFY: \t sending frame " + currentFrameIndex + " with lower quality");

        }
        // this is the time we have to send one frame in seconds
        double frameDelay = Webcam.FRAME_DELAY / 1000.0;
        long bytesPossible = Math.round(frameDelay * bandwidthEstimate);

        if (bytesPossible > encodedFrameBetter.length) {
            // it seems like there is enough bandwidth for the better quality frame available

            try {
                sender.sendFrame(encodedFrameBetter, currentFrameIndex, getSourceFrameIndex(), 0);
            } catch (IOException exception) {
                System.out.println(exception.toString());
            }
            lastFrameQuality = qualityBetter;
            System.out.println("SALSIFY: \t sending frame " + currentFrameIndex + " with lower quality");
        }

        if (bytesPossible < encodedFrameBetter.length && bytesPossible > encodedFrameWorse.length) {
            // it seems like there is enough bandwidth for the worse quality
            // but not enough for the better quality

            try {
                sender.sendFrame(encodedFrameBetter, currentFrameIndex, getSourceFrameIndex(), 0);
            } catch (IOException exception) {
                System.out.println(exception.toString());
            }
            lastFrameQuality = qualityWorse;
            System.out.println("SALSIFY: \t sending frame " + currentFrameIndex + " with higher quality");
        }

        // if there is not enough bandwidth for any of these frames we will
        // simply skip this one
        // next frame will be based on the frame we jst sent
        setSourceFrameIndex(currentFrameIndex);
        // index for the next frame
        currentFrameIndex++;
    }

    @Override
    public void disconnected() {
        sender.stopListening();
    }

    @Override
    public void join() throws InterruptedException {
        sender.join();
    }

    @Override
    public void reset() {
        System.out.println("SALSIFY: \t received reset notification");

        // we need to find the lowest available index

        // we'll start with current frame index
        int index = currentFrameIndex;
        // while there is a lower key
        while (frames.containsKey(index - 1)) {
            index--;
        }

        System.out.println("SALSIFY: \t received reset notification. Resetting to source with index " + index);

        // the result of the above might be -1 --> we have no basis yet
        // everything else means that is our new basis
        setSourceFrameIndex(index);

        // also tell sender to reset
        sender.resetSender();
    }

    @Override
    public void acknowledged(int frameIndex) {
        System.out.println("SALSIFY: \t received ack for frame " + frameIndex);
        // remove previous frames from map (except there are none)
        boolean moreFramesToRemove = true;
        int index = frameIndex - 1;
        while (moreFramesToRemove) {
            frames.remove(index);
            index--;
            moreFramesToRemove = frames.containsKey(index);
        }
    }

    /**
     * Synchronized access to source frame index variable.
     *
     * @return int containing the value
     */
    private int getSourceFrameIndex() {
        synchronized (lock) {
            return this.sourceFrameIndex;
        }
    }

    /**
     * Synchronized access to source frame index variable.
     *
     * @param sourceFrameIndex the index to set
     */
    public void setSourceFrameIndex(int sourceFrameIndex) {
        synchronized (lock) {
            this.sourceFrameIndex = sourceFrameIndex;
        }
    }

}
