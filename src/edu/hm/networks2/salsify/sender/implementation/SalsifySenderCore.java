package edu.hm.networks2.salsify.sender.implementation;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.common.implementation.GlobalLogger;
import edu.hm.networks2.salsify.sender.ISalsifySenderCore;
import edu.hm.networks2.salsify.sender.ISender;
import edu.hm.networks2.salsify.sender.IWebcam;
import edu.hm.networks2.salsify.sender.helper.ITransportProtocolListener;
import edu.hm.networks2.salsify.sender.helper.IWebcamListener;

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

    private final JFrame parent;

    public SalsifySenderCore(int bandwidth) {
        webcam = new Webcam();
        codec = new Codec();
        sender = new Sender(this, bandwidth);
        frames = new HashMap<>();
        frames.put(-1, null);
        sourceFrameIndex = -1;
        lastFrameQuality = 0;  // 70% quality is jpeg default
        lock = new Object();

        // show a GUI with a button for forced packet loss
        parent = new JFrame("Salsify Receiver");
        parent.getContentPane().setLayout(new FlowLayout());
        parent.setSize(180, 100);
        final JButton packetLossButton = new JButton("Packet Loss!");
        packetLossButton.addActionListener(event -> {
            sender.loseNexPacket();
        });
        parent.getContentPane().add(packetLossButton);
    }

    @Override
    public void start() {
        parent.setVisible(true);
        webcam.register(this);
        webcam.start();
    }

    @Override
    public void receiveFrame(BufferedImage frame) {
        // store frame until this frame is acknowledged by the receiver
        frames.put(currentFrameIndex, frame);

        // this is the sourcee state for the encoder
        // (the new frame will be encoded on source)
        final Optional<BufferedImage> source;
        if (getSourceFrameIndex() == -1) {
            source = Optional.empty();
        } else {
            //
            source = Optional.empty();
            //source = Optional.of(frames.get(getSourceFrameIndex()));
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
            GlobalLogger.getInstance().log(Level.INFO, "Sending low quality ({0}) because we have no bandwidth estimate yet. Length: {1}", new Object[]{qualityWorse, encodedFrameWorse.length});
            try {
                sender.sendFrame(encodedFrameWorse, currentFrameIndex, getSourceFrameIndex(), 0);
            } catch (IOException exception) {
                GlobalLogger.getInstance().log(Level.SEVERE, "Error occured while sending frame: {0}", exception.toString());
            }
            lastFrameQuality = qualityWorse;
            // next frame will be based on the frame we jst sent
            setSourceFrameIndex(currentFrameIndex);
            // index for the next frame
            currentFrameIndex++;

        } else {
            // this is the time we have to send one frame in seconds
            double frameDelay = Webcam.FRAME_DELAY / 1000.0;
            long bytesPossible = Math.round(frameDelay * bandwidthEstimate);

            if (bytesPossible > encodedFrameBetter.length) {
                // it seems like there is enough bandwidth for the better quality frame available
                GlobalLogger.getInstance().log(Level.INFO, "Sending frame {0} with high quality ({1}) because it fits into possible bytes {2} with length: {3}", new Object[]{currentFrameIndex, qualityBetter, bytesPossible, encodedFrameBetter.length});

                try {
                    sender.sendFrame(encodedFrameBetter, currentFrameIndex, getSourceFrameIndex(), 0);
                } catch (IOException exception) {
                    GlobalLogger.getInstance().severe(exception.toString());
                }
                lastFrameQuality = qualityBetter;

                // next frame will be based on the frame we jst sent
                setSourceFrameIndex(currentFrameIndex);
                // index for the next frame
                currentFrameIndex++;

            } else if (bytesPossible < encodedFrameBetter.length && bytesPossible > encodedFrameWorse.length) {
                // it seems like there is enough bandwidth for the worse quality
                // but not enough for the better quality

                GlobalLogger.getInstance().log(Level.INFO, "Sending frame {0} with low quality ({1}) because it fits into possible bytes {2} with length: {3}", new Object[]{currentFrameIndex, qualityWorse, bytesPossible, encodedFrameBetter.length});

                try {
                    sender.sendFrame(encodedFrameBetter, currentFrameIndex, getSourceFrameIndex(), 0);
                } catch (IOException exception) {
                    GlobalLogger.getInstance().severe(exception.toString());
                }
                lastFrameQuality = qualityWorse;
                // next frame will be based on the frame we jst sent
                setSourceFrameIndex(currentFrameIndex);
                // index for the next frame
                currentFrameIndex++;
            } else {
                // if there is not enough bandwidth for any of these frames we will
                // simply skip this one
                GlobalLogger.getInstance().log(Level.INFO, "Dropping frame {0} because it does not fit into possible bytes {1} with length: {2}", new Object[]{currentFrameIndex, bytesPossible, encodedFrameBetter.length});
                lastFrameQuality = qualityWorse - 5;
                currentFrameIndex++;
            }
        }

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

        // we need to find the lowest available index
        // we'll start with current frame index
        int index = currentFrameIndex;
        // while there is a lower key
        while (frames.containsKey(index - 1)) {
            index--;
        }

        // the result of the above might be -1 --> we have no basis yet
        // everything else means that is our new basis
        setSourceFrameIndex(index);
        // adjust quality...
        if (lastFrameQuality - 20 > 0) {
            lastFrameQuality = lastFrameQuality - 20;
        } else {
            lastFrameQuality = 0;
        }

        GlobalLogger.getInstance().log(Level.INFO, "Received reset notification (duplicate ack). Resetting to source with index {0} and quality {1}.", new Object[]{index, lastFrameQuality});

        // also tell sender to reset
        sender.resetSender();
    }

    @Override
    public void acknowledged(int frameIndex) {
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
