package edu.hm.networks2.salsify.sender.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import edu.hm.networks2.salsify.common.config.NetworkConfiguration;
import edu.hm.networks2.salsify.common.packets.SalsifyAck;
import edu.hm.networks2.salsify.common.packets.SalsifyFrame;
import edu.hm.networks2.salsify.sender.ISender;
import edu.hm.networks2.salsify.sender.helper.ITransportProtocolListener;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an implementation of ISender. It sends all frames as UDP-packets and
 * starts an extra thread to listen for ACKs to the network.
 *
 * @author Philipp
 */
public class Sender implements ISender {

    /**
     * This is an inner class which extends Thread. This thread receives ACKs
     * until it gets interrupted.
     *
     * @author Philipp
     */
    private class AckReceiver extends Thread {

        /**
         * {@inheritDoc}<br>
         * This implementation waits for UDP packets to interpret them as ACKs
         * from the receiver.
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            System.out.println("ACK-RECEIVER: \t waiting for ACKs");
            // allocate memory for incoming ack data
            final byte[] ackData = ByteBuffer.allocate(SalsifyAck.SIZE).array();
            final DatagramPacket ack = new DatagramPacket(ackData, ackData.length);

            // loop infinite
            while (!isInterrupted()) {
                try {
                    // block until an ACK arrives
                    socket.receive(ack);
                    // extract data from reveived ACK
                    final SalsifyAck salsifyAck = new SalsifyAck(ackData);
                    System.out.println("ACK-RECEIVER: \t received ACK for frame " + salsifyAck.getFrameIndex() + " fragment " + salsifyAck.getFragmentIndex());
                    // set new inter arrival time
                    setLatestBandwidth(salsifyAck.getBandwidth());

                    // process ack. handles duplicate ack check...
                    processAck(salsifyAck.getFrameIndex(), salsifyAck.getFragmentIndex());

                } catch (SocketException exception) {
                    // thread is stopping when socket is closed
                    System.out.println("ACK-RECEIVER: \t stopped waiting for ACKs");
                } catch (IOException exception) {
                    System.err.println("Salsify Sender had problems receiving ACKs.");
                    System.out.println(exception.toString());
                }
            }
        }

        /**
         * {@inheritDoc}<br>
         * When this implementation gets interrupted, the socket used for
         * receiving ACKs gets closed.
         *
         * @see java.lang.Thread#interrupt()
         */
        @Override
        public void interrupt() {
            super.interrupt();
            socket.close();
        }
    }

    /**
     * Socket which is used to send frames and receive ACKs.
     */
    private DatagramSocket socket;

    /**
     * Thread that receives ACKs.
     */
    private final AckReceiver ackReceiver;

    /**
     * Latest measured inter-arrival time.
     */
    private int latestBandwidth;
    
    /**
     * Stores listeners to notify about events.
     */
    private final ITransportProtocolListener listener;

    /**
     * This keeps track of acknowledged frames by keeping track of all
     * acknowledged fragments.
     *
     * Maps frameIndex to pair which contains: (numberOfFragments, index of
     * highest acknowledged fragment. starts with -1 and is complete when equals
     * numberOfFragments - 1)
     *
     */
    private final Map<Integer, Map.Entry<Integer, Integer>> acknowledgements;

    /**
     * Constructor that creates the socket for sending frames and receiving ACKs
     * and starts the thread that receives ACKs.
     * 
     * @param listener The given listener will get notified about events.
     */
    public Sender(ITransportProtocolListener listener) {
        latestBandwidth = 0;

        try {
            socket = new DatagramSocket(NetworkConfiguration.SENDER_PORT, InetAddress.getByName(NetworkConfiguration.SENDER_IP));
        } catch (SocketException | UnknownHostException exception) {
            System.err.println("Salsify Sender had problems opening a DatagramSocket.");
            System.out.println(exception.toString());
        }
        acknowledgements = new HashMap<>();
        ackReceiver = new AckReceiver();
        this.listener = listener;
        ackReceiver.start();
    }

    /**
     * {@inheritDoc}<br>
     * This implementation splits a frame into several fragments and uses UDP
     * packets to send them.
     *
     * @see edu.hm.networks2.salsify.sender.ISender#sendFrame(byte[], int, int,
     * int)
     */
    @Override
    public void sendFrame(byte[] data, int frameIndex, int sourceFrameIndex, int gracePeriod) throws IOException {
        // build a salsify frame from input data
        final SalsifyFrame frame = new SalsifyFrame(data, frameIndex, sourceFrameIndex, gracePeriod);

        // add to acknowledgements
        addUnacknowledgedFrame(frameIndex, frame.getNumberOfFragments());

        System.out.print("SENDER: \t sending frame " + frame.getFrameIndex() + " fragment");
        // send each salsify fragment inside the frame
        for (int index = 0; index < frame.getNumberOfFragments(); index++) {
            final byte[] fragment = frame.getFragment(index).getRawPacket();
            System.out.print(" " + frame.getFragment(index).getFragmentIndex());
            socket.send(new DatagramPacket(fragment, fragment.length, InetAddress.getByName(NetworkConfiguration.RECEIVER_IP), NetworkConfiguration.RECEIVER_PORT));
        }
        System.out.println("");
    }

    /**
     * {@inheritDoc}<br>
     * This implementation is just a normal getter.
     *
     * @see edu.hm.networks2.salsify.sender.ISender#getLatestInterArrivalTime()
     */
    @Override
    public int getLatestBandwidth() {
        return latestBandwidth;
    }

    /**
     * {@inheritDoc}<br>
     * This implementation interrupts the threat that receives ACKs.
     *
     * @see edu.hm.networks2.salsify.sender.ISender#stopListening()
     */
    @Override
    public void stopListening() {
        ackReceiver.interrupt();
    }

    /**
     * {@inheritDoc}<br>
     * This implementation just joins the threat that receives ACKs.
     *
     * @see edu.hm.networks2.salsify.sender.ISender#join()
     */
    @Override
    public void join() throws InterruptedException {
        ackReceiver.join();
    }

    private void setLatestBandwidth(int latestBandwidth) {
        this.latestBandwidth = latestBandwidth;
    }

    /**
     * Add a new entry to acknowledged frames. Make sure the index does not
     * exist yet.
     *
     * @param frameIndex The index of the new frame.
     * @param numberOfFragments The number of fragments that will be unack-
     * nowledged for this frame.
     */
    private void addUnacknowledgedFrame(int frameIndex, int numberOfFragments) {
        if (!acknowledgements.containsKey(frameIndex)) {
            // start with -1 (nothing is ackowledged)
            acknowledgements.put(frameIndex, new AbstractMap.SimpleEntry<>(numberOfFragments, -1));
        }
    }

    /**
     * Remove the given frame from the acknowledgements.
     *
     * @param frameIndex frame identified by its index.
     */
    private void removeFrameFromAcknowledgements(int frameIndex) {
        if (acknowledgements.containsKey(frameIndex)) {
            acknowledgements.remove(frameIndex);
        }
    }

    /**
     * Check if a given frame is completely acknowledged. That means that the
     * key of the entry minus one equals the highest received fragment
     * acknowledgement index.
     *
     * @param frameIndex frame identified by its index.
     *
     * @return boolean indicating if the given frame is completely acknowledged.
     */
    private boolean isFrameAcknowledged(int frameIndex) {
        boolean result = false;
        if (acknowledgements.containsKey(frameIndex)) {
            result = (acknowledgements.get(frameIndex).getKey() - 1) == acknowledgements.get(frameIndex).getValue();
        }
        return result;
    }

    /**
     * Acknowledge a specific fragment in a specific frame.
     *
     * @param frameIndex identifies fragment
     * @param fragmentIndex identifies fragment
     *
     * @return true if its a duplicate ack
     */
    private boolean acknowledgeFragment(int frameIndex, int fragmentIndex) {
        boolean duplicate = false;

        // exists?
        if (acknowledgements.containsKey(frameIndex)) {
            // is already acknowledged?
            int oldValue = acknowledgements.get(frameIndex).getValue();
            duplicate = oldValue == fragmentIndex;

            // only modify if old value smaller than new one
            if (oldValue < fragmentIndex) {
                acknowledgements.get(frameIndex).setValue(fragmentIndex);
            }

        }
        return duplicate;
    }

    /**
     * This checks if the acknowledgment is duplicate
     *
     * @param frameIndex
     * @param fragmentIndex
     */
    private void processAck(int frameIndex, int fragmentIndex) {

        // add and check for duplicate
        if (acknowledgeFragment(frameIndex, fragmentIndex)) {
            // DUPLICATE FOUND!
            // listener has to reset back to a frame that is completely 
            // acknowledged
            System.out.println("SENDER: \t received duplicate frame " 
                    + frameIndex 
                    + " fragment " 
                    + fragmentIndex 
                    + ". Notifying listener to reset");
            listener.reset();

        } else {
            // everything fine
            if (isFrameAcknowledged(frameIndex)) {
                // perfect we have a complete frame --> report to salsify
                System.out.println("SENDER: \t notifying listener about complete frame " 
                        + frameIndex);
                listener.acknowledged(frameIndex);
            }
        }

    }

}
