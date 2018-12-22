package edu.hm.networks2.salsify.receiver.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.hm.networks2.salsify.common.config.NetworkConfiguration;
import edu.hm.networks2.salsify.common.packets.SalsifyAck;
import edu.hm.networks2.salsify.common.packets.SalsifyFragment;
import edu.hm.networks2.salsify.common.packets.SalsifyFrame;
import edu.hm.networks2.salsify.receiver.IReceiver;
import edu.hm.networks2.salsify.receiver.helper.IReceiverListener;

public class Receiver extends Thread implements IReceiver {

    private final List<IReceiverListener> listeners;

    private SalsifyFrame latestFrame;

    private long lastFragmentTimestamp;

    private double bandwidthEstimate;

    private static final double MOVING_AVERAGE_FACTOR = 0.1;

    private static final double BILLION = 1000000000.0;

    /**
     * Socket which is used to send ACKS and receive frames.
     */
    private DatagramSocket socket;

    public Receiver() {
        listeners = new ArrayList<>();

        try {
            socket = new DatagramSocket(NetworkConfiguration.RECEIVER_PORT, InetAddress.getByName(NetworkConfiguration.RECEIVER_IP));
        } catch (SocketException | UnknownHostException exception) {
            System.err.println("Salsify Receiver had problems opening a DatagramSocket.");
            System.out.println(exception.toString());
        }

        this.lastFragmentTimestamp = 0;
        this.bandwidthEstimate = -1;
    }

    @Override
    public void run() {
        System.out.println("RECEIVER: \t waiting for fragments");
        // allocate memory for incoming packet data
        final byte[] fragmentData = ByteBuffer.allocate(SalsifyFragment.MAXIMUM_DATA_SIZE).array();
        final DatagramPacket fragment = new DatagramPacket(fragmentData, fragmentData.length);

        // loop infinite
        while (!isInterrupted()) {
            try {
                // block until a fragment arrives
                socket.receive(fragment);
                // extract data from reveived frame
                final SalsifyFragment salsifyFragment = new SalsifyFragment(fragmentData);

                // first fragment of frame
                if (latestFrame == null) {
                    // this should happen on every first fragment of a new frame
                    startBandwidthMeasurement();
                } else {
                    estimateBandwidth(salsifyFragment.getSize());

                }

                System.out.println("RECEIVER: \t received fragment " + salsifyFragment.getFragmentIndex() + " for frame " + salsifyFragment.getFrameIndex());

                // send an acknowledgement
                sendAck(salsifyFragment.getFrameIndex(), salsifyFragment.getFragmentIndex(), Math.toIntExact(Math.round(bandwidthEstimate)));

                // collect all fragments for one frame before reporting to salsify core
                if (latestFrame == null) {
                    latestFrame = new SalsifyFrame(salsifyFragment);
                } else if (!latestFrame.addFragment(salsifyFragment)) {
                    // send a duplicate acck --> not implemented
                    System.out.println("found duplicate! FEATURE NOT IMPLEMENTED! IGNORING...");
                }

                // if this was the last fragment of the frame and all fragments were collected earlier
                if (salsifyFragment.getRemainingFragments() == 0 && latestFrame.getNumberOfFragments() == (salsifyFragment.getFragmentIndex() + 1)) {
                    listeners.forEach(listener -> listener.receiveFrame(latestFrame.getFrame(), latestFrame.getFrameIndex(), latestFrame.getFrameIndexState()));
                    // reset frame
                    latestFrame = null;
                }

            } catch (SocketException exception) {
                // thread is stopping when socket is closed
                System.out.println("RECEIVER: \t stopped waiting for fragments");
            } catch (IOException exception) {
                System.err.println("Salsify Receiver had problems receiving frames.");
                System.out.println(exception.toString());
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

        // send ack
        System.out.println("ACK: \t\t sending ack for frame " + ack.getFrameIndex() + " fragment " + ack.getFragmentIndex() + " with bandwidth " + bandwidth);
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
        System.out.println("Setting new bandwidth estimate to " + bandwidthEstimate);
        // update last fragment timestamp
        lastFragmentTimestamp = now;
    }

}
