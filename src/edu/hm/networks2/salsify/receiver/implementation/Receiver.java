package edu.hm.networks2.salsify.receiver.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.hm.networks2.salsify.common.config.NetworkConfiguration;
import edu.hm.networks2.salsify.common.implementation.GlobalLogger;
import edu.hm.networks2.salsify.common.implementation.LimitedSocket;
import edu.hm.networks2.salsify.common.packets.SalsifyAck;
import edu.hm.networks2.salsify.common.packets.SalsifyFragment;
import edu.hm.networks2.salsify.common.packets.SalsifyFrame;
import edu.hm.networks2.salsify.receiver.IReceiver;
import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;
import java.util.logging.Level;

public class Receiver extends Thread implements IReceiver {
    
    private static final double MOVING_AVERAGE_FACTOR = 0.1;
    
    private static final double BILLION = 1000000000.0;
    
    private final List<IReceiverListener> listeners;
    
    private int latestFrameIndex;
    
    private int latestFragmentIndex;
    
    private SalsifyFrame latestFrame;
    
    private long lastFragmentTimestamp;
    
    private double bandwidthEstimate;

    /**
     * Socket which is used to send ACKS and receive frames.
     */
    private DatagramSocket socket;
    
    public Receiver() {
        listeners = new ArrayList<>();
        
        try {
            socket = new LimitedSocket(NetworkConfiguration.RECEIVER_PORT, InetAddress.getByName(NetworkConfiguration.RECEIVER_IP), SalsifyAck.SIZE);
        } catch (SocketException | UnknownHostException exception) {
            GlobalLogger.getInstance().log(Level.SEVERE, "Salsify Receiver had problems opening a DatagramSocket.{0}", exception.toString());
        }
        
        this.lastFragmentTimestamp = 0;
        this.bandwidthEstimate = -1;
        this.latestFrameIndex = -1;
        this.latestFragmentIndex = -1;
    }
    
    @Override
    public void run() {
        // allocate memory for incoming packet data
        final byte[] fragmentData = new byte[SalsifyFragment.COMPLETE_SIZE];
        final DatagramPacket fragment = new DatagramPacket(fragmentData, SalsifyFragment.COMPLETE_SIZE);

        // loop infinite
        while (!isInterrupted()) {
            try {
                // block until a fragment arrives
                socket.receive(fragment);

                // extract data from received frame
                final SalsifyFragment salsifyFragment;
                if (fragment.getLength() == SalsifyFragment.COMPLETE_SIZE) {
                    // in case the udp packet has the expected size we need
                    // to extract the complete data
                    salsifyFragment = new SalsifyFragment(fragmentData);
                } else {
                    // in case the udp packet is smaller than expected we
                    // need to pass a smaller amount of data
                    salsifyFragment = new SalsifyFragment(Arrays.copyOf(fragmentData, fragment.getLength()));
                }

                // true, if this is the first fragment of the frame
                final boolean first
                        = // this frame index is bigger than the last received frame index
                        latestFrameIndex < salsifyFragment.getFrameIndex()
                        // this fragment index is 0
                        && salsifyFragment.getFragmentIndex() == 0;
                
                boolean packetLoss
                        = // not the first packet of this frame
                        !first
                        && ( // this fragment does not belong to the currently collected frame
                        latestFrameIndex != salsifyFragment.getFrameIndex()
                        // this fragment index is not the expected fragment index
                        || latestFragmentIndex + 1 != salsifyFragment.getFragmentIndex());
                
                if (packetLoss) {
                    // send a duplicate ack
                    GlobalLogger.getInstance().log(Level.INFO, 
                            "Received fragment out of order which indicates PACKET LOSS! Sending duplicate ACK for frame {0} and fragment {1}.", 
                            new Object[]{latestFrameIndex, latestFragmentIndex});
                    sendAck(latestFrameIndex, latestFragmentIndex, Math.toIntExact(Math.round(bandwidthEstimate)));
                } else {
                    // send an acknowledgement
                    sendAck(salsifyFragment.getFrameIndex(), salsifyFragment.getFragmentIndex(), Math.toIntExact(Math.round(bandwidthEstimate)));

                    // packet was accepted, so set it as the last accepted packet
                    latestFrameIndex = salsifyFragment.getFrameIndex();
                    latestFragmentIndex = salsifyFragment.getFragmentIndex();

                    // estimate bandwidth and collect the fragment
                    if (first) {          
                        startBandwidthMeasurement();
                        latestFrame = new SalsifyFrame(salsifyFragment);
                        GlobalLogger.getInstance().log(Level.INFO, "Received first fragment for frame {0}", latestFrame.getFrameIndex());
                    } else {
                        estimateBandwidth(salsifyFragment.getSize());
                        latestFrame.addFragment(salsifyFragment);
                    }

                    // true, if this is the last fragment of the frame
                    final boolean last
                            = // this fragment was the last fragment of the frame
                            salsifyFragment.getRemainingFragments() == 0
                            // all fragments were collected earlier
                            && latestFrame.getNumberOfFragments() == (salsifyFragment.getFragmentIndex() + 1);

                    // notify the salsify core about a new frame
                    if (last) {
                        listeners.forEach(listener -> listener.receiveFrame(latestFrame.getFrame(), latestFrame.getFrameIndex(), latestFrame.getFrameIndexState()));
                    }
                }
                
            } catch (SocketException exception) {
                GlobalLogger.getInstance().log(Level.SEVERE, "Socket got closed! Exception: {0}", exception.toString());
            } catch (IOException exception) {
                GlobalLogger.getInstance().log(Level.SEVERE, "Salsify Receiver had problems receiving frames.{0}", exception.toString());
            }
        }
    }
    
    @Override
    public void register(IReceiverListener listener) {
        listeners.add(listener);
    }
    
    private void sendAck(int frameIndex, int fragmentIndex, int bandwidth) throws UnknownHostException, IOException {
        // build a salsify ack from input data
        final SalsifyAck ack = new SalsifyAck(frameIndex, fragmentIndex, bandwidth);

        final byte[] rawData = ack.getRawData();
        socket.send(new DatagramPacket(rawData, rawData.length, InetAddress.getByName(NetworkConfiguration.SENDER_IP), NetworkConfiguration.SENDER_PORT));
    }

    /**
     * This will start the bandwidth measurement. This should only be called
     * when a new frame was startet (-> the first fragment of a new frame was
     * received).
     */
    private void startBandwidthMeasurement() {
        lastFragmentTimestamp = System.nanoTime();
    }

    /**
     * This will update the current bandwidth estimate.It will also update
     * lastFragmentTimestamp.
     *
     * @param fragmentSize supply the size of the latest fragment to compute
     * bandwidth.
     */
    private void estimateBandwidth(int fragmentSize) {
        long now = System.nanoTime();
        
        long difference = now - lastFragmentTimestamp;
        double small = fragmentSize / (difference / BILLION);
        
        if (bandwidthEstimate == -1) {
            // we do not have an estimate yet
            bandwidthEstimate = small;
        } else {
            // we have an estimate. do moving average
            bandwidthEstimate = MOVING_AVERAGE_FACTOR * small + (1 - MOVING_AVERAGE_FACTOR) * bandwidthEstimate;
            
        }
        //GlobalLogger.getInstance().log(Level.INFO, "Bandwidth estimate: {0}", bandwidthEstimate);
        // update last fragment timestamp
        lastFragmentTimestamp = now;
    }
    
}
