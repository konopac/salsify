/*
 * Source file for SalsifyFragmentPacket class.
 * Authors:
 *     - Nico <dassler@hm.edu>
 *     - Philipp <konopac@hm.edu>
 */
package edu.hm.networks2.salsify.common.implementation;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Instances of this class encapsulate a Salsify fragment.
 *
 * A fragment is sent from sender to receiver and contains the actual video
 * data. Usually a single frame is split up into several fragments.
 *
 * Each instance variable represents one field of the final packet. That makes a
 * total header size of 160 bits or 20 bytes. This size is included in calls of
 * getSize().
 *
 * Each fragment has the maximum size of 1024 bytes. That gives the maximum
 * payload size of 1004 bytes which is always checked.
 *
 * @author Nico
 */
public class SalsifyFragment {

    /**
     * MAXIMUM size of data in a fragment. This is equal to the number of bytes.
     */
    public final static int MAXIMUM_DATA_SIZE = 1004;
    
    /**
     * Bytes 20-1023 is data.
     */
    private final int DATA_OFFSET = 20;

    /**
     * A frame consists of multiple fragments. Each has an index to report
     * fragments that got lost.
     */
    private int fragmentIndex;

    /**
     * The delay between two frames.
     */
    private int gracePeriod;

    /**
     * Index of the frame that this fragment is part of.
     */
    private int frameIndex;

    /**
     * Index of the frame that this fragments frame is based on.
     */
    private int frameIndexState;

    /**
     * Indicates how many fragments are left to complete the frame.
     */
    private int remainingFragments;

    /**
     * The actual data that is transported using this fragment.
     */
    private byte[] data;

    /**
     * Use this to initialize a packet that you want to send. That means
     * converting your data into bytes.
     * 
     * @param fragmentIndex
     * @param gracePeriod
     * @param frameIndex
     * @param frameIndexState
     * @param remainingFragments
     * @param data The actual data to be included in the packet. Not allowed to
     * exceed MAXIMUM_DATA_SIZE.
     */
    public SalsifyFragment(int fragmentIndex, int gracePeriod, 
            int frameIndex, int frameIndexState, int remainingFragments, byte[] data) {

        if (data == null) {
            throw new NullPointerException("SalsifyFragment: data is not allowed to be null!");
        }

        if (data.length > MAXIMUM_DATA_SIZE) {
            throw new IllegalArgumentException(
                    "SalsifyFragment: Size of data is not allowed to exceed " + MAXIMUM_DATA_SIZE + "bytes."
            );
        }

        this.fragmentIndex = fragmentIndex;
        this.gracePeriod = gracePeriod;
        this.frameIndex = frameIndex;
        this.frameIndexState = frameIndexState;
        this.remainingFragments = remainingFragments;
        this.data = Arrays.copyOf(data, data.length);
    }
    
    /**
     * This is to initialize a salsify fragment with rawData that was just received
     * on the network. Without the UDP header of course but including the salsify 
     * header.
     * 
     * @param rawData The raw bytes of the rawData that is to be converted to a salsify
     * fragment.
     */
    public SalsifyFragment(byte[] rawData) {
        if (rawData == null) {
            throw new NullPointerException("SalsifyFragment: data is not allowed to be null!");
        }
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(rawData);
        
        this.fragmentIndex = byteBuffer.getInt();
        this.gracePeriod = byteBuffer.getInt();
        this.frameIndex = byteBuffer.getInt();
        this.frameIndexState = byteBuffer.getInt();
        this.remainingFragments = byteBuffer.getInt();
        // allocate the new rawData array which will store the image rawData with
        // - 20 in size to not contain header
        this.data = new byte[byteBuffer.capacity() - 20];
        // copy into data
        byteBuffer.get(this.data);
    }

    /**
     * Only use this if you want to initialize by hand --> Using setters.
     */
    public SalsifyFragment() {
        this.fragmentIndex = 0;
        this.gracePeriod = 0;
        this.frameIndex = 0;
        this.frameIndexState = 0;
        this.remainingFragments = 0;
        this.data = null;
    }
    
    /**
     * Get all the fields in this packet coded into raw bytes. The length will vary
     * depending on the size of data. You can get the size either by invoking
     * length() on the result or by calling getSize().
     * 
     * @return the raw bytes of all the fields.
     */
    public byte[] getRawPacket() {
        return ByteBuffer.allocate(this.getSize())
                .putInt(this.fragmentIndex)
                .putInt(this.gracePeriod)
                .putInt(this.frameIndex)
                .putInt(this.frameIndexState)
                .putInt(this.remainingFragments)
                .put(this.data)
                .array();               
    }
    
    /**
     * Get the size of the raw packet which can be requested using getRawPacket.
     * 
     * @return an integer containing the size.
     */
    public int getSize() {
        // 20 is the fixed header size
        return this.data.length + 20;
    }
    
    /**
     * Get the size (number of bytes) contained in this packet.
     * 
     * @return integer representing the size. 
     */
    public int getDataSize() {
        return this.data.length;
    }
    
    public int getFragmentIndex() {
        return fragmentIndex;
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public int getFrameIndexState() {
        return frameIndexState;
    }

    public int getRemainingFragments() {
        return remainingFragments;
    }

    /**
     * Get the data in this packet. Returns a copy so you
     * can do whatever you want to it. No kinky stuff allowed.
     * 
     * @return byte[] (copy of data)
     */
    public byte[] getData() {
        return Arrays.copyOf(this.data, this.data.length);
    }

    public void setFragmentIndex(int fragmentIndex) {
        this.fragmentIndex = fragmentIndex;
    }

    public void setGracePeriod(int gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public void setFrameIndexState(int frameIndexState) {
        this.frameIndexState = frameIndexState;
    }

    public void setRemainingFragments(int remainingFragments) {
        this.remainingFragments = remainingFragments;
    }

    /**
     * Set data of this fragment.
     * 
     * @param data The raw data that will be send with this fragment. Not allowed
     * to be null.
     */
    public void setData(byte[] data) {
        
        if (data == null) {
            throw new NullPointerException("SalsifyFragment: data is not allowed to be null!");
        }
        
        this.data = data;
    }
    
    

}
