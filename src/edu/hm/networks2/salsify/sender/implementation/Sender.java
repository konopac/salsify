package edu.hm.networks2.salsify.sender.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import edu.hm.networks2.salsify.common.config.NetworkConfiguration;
import edu.hm.networks2.salsify.common.implementation.GlobalLogger;
import edu.hm.networks2.salsify.common.implementation.LimitedSocket;
import edu.hm.networks2.salsify.common.packets.SalsifyAck;
import edu.hm.networks2.salsify.common.packets.SalsifyFragment;
import edu.hm.networks2.salsify.common.packets.SalsifyFrame;
import edu.hm.networks2.salsify.sender.ISender;
import edu.hm.networks2.salsify.sender.helper.ITransportProtocolListener;

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
                    // set new inter arrival time
                    setLatestBandwidth(salsifyAck.getBandwidth());

                    // process ack. handles duplicate ack check...
                    processAck(salsifyAck.getFrameIndex(), salsifyAck.getFragmentIndex());

                } catch (SocketException exception) {
                    // thread is stopping when socket is closed
                    GlobalLogger.getInstance().log(Level.SEVERE, "Stopped waiting for ACKs {0}", exception.toString());
                } catch (IOException exception) {
                    GlobalLogger.getInstance().log(Level.SEVERE, "Salsify Sender had problems receiving ACKs. {0}", exception.toString());
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
    private LimitedSocket socket;

    /**
     * Thread that receives ACKs.
     */
    private final AckReceiver ackReceiver;

    /**
     * Latest measured bandwidth time.
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
    public Sender(ITransportProtocolListener listener, int bandwidth) {
        latestBandwidth = -1;

        try {
            socket = new LimitedSocket(NetworkConfiguration.SENDER_PORT, InetAddress.getByName(NetworkConfiguration.SENDER_IP), SalsifyFragment.COMPLETE_SIZE, bandwidth);
        } catch (SocketException | UnknownHostException exception) {
            GlobalLogger.getInstance().log(Level.SEVERE, "Salsify Sender had problems opening a DatagramSocket. {0}", exception.toString());
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

    	// debug purposes
//    	try {
//        	BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
//            File outputfile = new File("results" + File.separator + "sender-diff" + frameIndex + ".jpg");
//            ImageIO.write(image, "jpg", outputfile);
//        } catch (IOException exception) {
//            System.out.println("error occured while writing file to disk");
//        }
    	
        // build a salsify frame from input data
        final SalsifyFrame frame = new SalsifyFrame(data, frameIndex, sourceFrameIndex, gracePeriod);

        // add to acknowledgements
        addUnacknowledgedFrame(frameIndex, frame.getNumberOfFragments());

        // send each salsify fragment inside the frame
        for (int index = 0; index < frame.getNumberOfFragments(); index++) {
            final byte[] fragment = frame.getFragment(index).getRawPacket();
            socket.send(new DatagramPacket(fragment, fragment.length, InetAddress.getByName(NetworkConfiguration.RECEIVER_IP), NetworkConfiguration.RECEIVER_PORT));
        }
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
        if (!getAcknowledgements().containsKey(frameIndex)) {
            // start with -1 (nothing is ackowledged)
            getAcknowledgements().put(frameIndex, new AbstractMap.SimpleEntry<>(numberOfFragments, -1));
        }
    }

    /**
     * Remove the given frame from the acknowledgements.
     *
     * @param frameIndex frame identified by its index.
     */
    private void removeFrameFromAcknowledgements(int frameIndex) {
        if (getAcknowledgements().containsKey(frameIndex)) {
            getAcknowledgements().remove(frameIndex);
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
        if (getAcknowledgements().containsKey(frameIndex)) {
            result = (getAcknowledgements().get(frameIndex).getKey() - 1) == getAcknowledgements().get(frameIndex).getValue();
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
        if (getAcknowledgements().containsKey(frameIndex)) {
            // is already acknowledged?
            int oldValue = getAcknowledgements().get(frameIndex).getValue();
            duplicate = oldValue >= fragmentIndex;

            // only modify if old value smaller than new one
            if (oldValue < fragmentIndex) {
                getAcknowledgements().get(frameIndex).setValue(fragmentIndex);
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
            listener.reset();

        } else {
            // everything fine
            if (isFrameAcknowledged(frameIndex)) {
                // perfect we have a complete frame --> report to salsify
                listener.acknowledged(frameIndex);
                // we dont need to wait for acknowledgements for older frames than this
                int index = frameIndex -1;
                while (getAcknowledgements().containsKey(frameIndex - 1)) {
                    removeFrameFromAcknowledgements(index);
                    index--;
                }
            }
        }

    }

    @Override
    public void resetSender() {
        // clear all pending acknowledgements
        getAcknowledgements().clear();
    }

    @Override
    public void loseNexPacket() {
    	socket.loseNexPacket();
    }
    
    /**
     * Synchronized access to acknowledgements.
     * @return acknowledgements map
     */
    public synchronized Map<Integer, Map.Entry<Integer, Integer>> getAcknowledgements() {      
        return acknowledgements;
    }

}
