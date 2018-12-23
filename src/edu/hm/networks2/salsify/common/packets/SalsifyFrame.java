/*
 * Source file for SalsifyFrame class.
 * Authors:
 *     - Nico <dassler@hm.edu>
 *     - Philipp <konopac@hm.edu>
 */
package edu.hm.networks2.salsify.common.packets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is responsible for taking the raw data of a frame and split it up
 * into different fragments to send.
 * 
 * @author nicfel
 */
public class SalsifyFrame {
   
    private final int frameIndex;
    
    private final int frameIndexState;
    
    private final List<SalsifyFragment> fragments;

    /**
     * Construct a salsify frame from raw data (bytes). The frame is 
     * split up into multiple fragments with size up to (1024 bytes 
     * including header).
     * 
     * Use this when you have a raw frame as a byte array and want to split it
     * up to send the fragments packet via network.
     * 
     * @param frame The raw frame/image data. 
     * @param frameIndex Index of the frame that is stored.
     * @param frameIndexState  Index of the frame this frame is based on.
     * @param gracePeriod from salsify protocol.
     */
    public SalsifyFrame(byte[] frame, int frameIndex, int frameIndexState, int gracePeriod) {
        
        // number of fragment that will have the maximum size
        int fullFragments = frame.length / SalsifyFragment.MAXIMUM_DATA_SIZE;
        int remainingBytes = frame.length % SalsifyFragment.MAXIMUM_DATA_SIZE;
        
        int numberOfFragments = fullFragments;
        // one more fragment in case we have remaining bytes
        if (remainingBytes > 0) {
            numberOfFragments++;
        }
        this.frameIndex = frameIndex;
        this.frameIndexState = frameIndexState;
        
        // we need at least numberofFragment space
        this.fragments = new ArrayList<>(numberOfFragments);
        // split frame into fragments
        for (int counter = 0; counter < numberOfFragments; counter++) {
            // this will step like this: 0, 1004, 2008, 3012, ...
            int fragmentStart = counter * SalsifyFragment.MAXIMUM_DATA_SIZE;
            // this will step like this: 1004, 2008, 3012, ...
            int fragmentEnd = (counter + 1) * SalsifyFragment.MAXIMUM_DATA_SIZE;
            // now add
            if (counter == numberOfFragments -1) {
                // last iteration is special (end is exclusive)
                byte[] data = Arrays.copyOfRange(frame, fragmentStart, frame.length);
                fragments.add(new SalsifyFragment(
                        counter, gracePeriod, 
                        frameIndex, frameIndexState, 
                        numberOfFragments - counter - 1, data)
                );
            } else {
                // the fragment end index is exclusive so it doesnt matter that its
                // one higher from the computation above
                byte[] data = Arrays.copyOfRange(frame, fragmentStart, fragmentEnd);
                fragments.add(new SalsifyFragment(
                        counter, gracePeriod, 
                        frameIndex, frameIndexState, 
                        numberOfFragments - counter - 1, data)
                );
            }
        } 
    }

    /**
     * Construct a new frame based on an initial fragment. Use this when you 
     * received the first fragment of a frame and want to add more fragments 
     * later. Use the addFragment method for that.
     * 
     * @param initialFragment
     */
    public SalsifyFrame(SalsifyFragment initialFragment) {
        this.fragments = new ArrayList<>();
        this.frameIndex = initialFragment.getFrameIndex();
        this.frameIndexState = initialFragment.getFrameIndexState();
        this.fragments.add(initialFragment);
    }
    
    /**
     * Adds the fragment to this frame. On success it will return true. It 
     * will be unsuccessful if:
     *     - frameIndex of new fragment and frame do not match
     *     - frameIndexState of new fragment and frame do not match
     *     - fragmentIndex is not 1 higher than that of the last added fragment
     * 
     * @param fragmentToAdd The fragment that will be added.
     * @return boolean indicating success
     */
    public boolean addFragment(SalsifyFragment fragmentToAdd) {
        boolean result = 
                // frameIndex of new fragment and frame do not match
                fragmentToAdd.getFrameIndex() == getFrameIndex() 
                // frameIndexState of new fragment and frame do not match
                && fragmentToAdd.getFrameIndexState() == getFrameIndexState()
                // fragmentIndex is not 1 higher than that of the last added fragment
                && fragmentToAdd.getFragmentIndex() == getLastFragment().getFragmentIndex() + 1;
        
        if (result) {
            // TODO: should be copied
            this.fragments.add(fragmentToAdd);
        }
        
        return result;
    }
    
    /**
     * Collects all the bytes from all fragments and returns them. This will
     * only return the full frame if you have added all necessary fragments.
     * 
     * Note: This only makes sense if you used the following:
     *     - SalsifyFrame(SalsifyFragmentPacket initialFragment)  
     *     - addFragment(SalsifyFragmentPacket fragmentToAdd)
     * 
     * @return byte array containing the data of all frames.
     */
    public byte[] getFrame() {
        // initialize array
        byte[] frame = new byte[getFrameSize()];
        int currentIndex = 0;
        for (SalsifyFragment fragment : this.fragments) {
            System.arraycopy(fragment.getData(), 0, frame, currentIndex, fragment.getDataSize());
            currentIndex += fragment.getDataSize();
        }
        return frame;
    }
    
    /**
     * Counts the number of bytes across all fragments. Note that this will
     * only return the actual frame size if you have added all fragments that
     * are part of the full frame.
     * 
     * @return integer representing the frame size. 
     */
    public int getFrameSize() {
        int result = 0;
        result = this.fragments
                .stream()
                .map((fragment) -> fragment.getDataSize())
                .reduce(result, Integer::sum);
        
        return result;
    }
    
    /**
     * Get the number of fragments the frame got split into.
     * 
     * @return integer representing the number of fragments.
     */
    public int getNumberOfFragments() {
        return this.fragments.size();
    }
    
    /**
     * Fragments will be stored in the same order as the bytes in the
     * provided byte array. That means the first 1004 bytes will be stored
     * in the first fragment which can be accessed using this method and
     * index = 0.
     * 
     * Throws IndexOutOfBoundsException in case the index is invalid.
     * 
     * @param index The fragments are stored sequentially. The upper bound for
     * index is the value returned by getNumberOfFragments - 1 and the lower bound is
     * zero. 
     * 
     * @return 
     */
    public SalsifyFragment getFragment(int index) {
        if (index < 0 || index >= getNumberOfFragments()) {
            throw new IndexOutOfBoundsException("Index must be between 0 and " + getNumberOfFragments());
        }
        return this.fragments.get(index);
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public int getFrameIndexState() {
        return frameIndexState;
    } 
    
    /**
     * Helper Gets the last fragment that was added to this frame.
     * 
     * @return The last fragment. 
     */
    public SalsifyFragment getLastFragment() {
        return this.fragments.get(this.fragments.size() - 1);
    }
    
}
