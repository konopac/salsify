package edu.hm.networks2.salsify.common.packets;

import java.nio.ByteBuffer;

/**
 * This class is a container for all data needed in an ACK.
 * 
 * @author Philipp
 */
public class SalsifyAck {

	/**
	 * Size of the whole ACK in bytes.
	 */
	public static final int SIZE = Integer.BYTES * 3;
	
    /**
     * Index of the acknowledged frame.
     */
    private final int frameIndex;

    /**
     * Index of the acknowledged fragment.
     */
    private final int fragmentIndex;
    
    /**
     * Bandwidth for this ACK. In case this is -1 the bandwidth could
     * not be measured.
     */
    private final int bandwidth;    

    
    /**
     * Constructor that builds a data container from raw data.<br>
     * <b>This Constructor should be used for receiving ACKs from the network.</b>
     * 
     * @param rawData is the raw data coming from the network.
     * 
     * @throws NullPointerException if raw data is null
     */
    public SalsifyAck(byte[] rawData) {
        if (rawData == null) {
            throw new NullPointerException("SalsifyAck: data is not allowed to be null!");
        }
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(rawData);
        this.frameIndex = byteBuffer.getInt();
        this.fragmentIndex = byteBuffer.getInt();
        this.bandwidth = byteBuffer.getInt();
	}

	/**
	 * Constructor that builds a data container from given data.<br>
     * <b>This Constructor should be used for building ACKs for the network.</b>
     * 
	 * @param frameIndex is the frame index of the acknowledged frame
	 * @param fragmentIndex is the fragment index of the acknowledged fragment
	 * @param bandwidth is the estimated bandwidth of the network connection
	 */
	public SalsifyAck(int frameIndex, int fragmentIndex, int bandwidth) {
		this.frameIndex = frameIndex;
		this.fragmentIndex = fragmentIndex;
		this.bandwidth = bandwidth;
	}
    
    /**
     * Build the raw data from information stored in this container.
     * 
     * @return the raw data as a byte array
     */
    public byte[] getRawData() {
        return ByteBuffer.allocate(SIZE)
        		.putInt(this.frameIndex)
                .putInt(this.fragmentIndex)
                .putInt(this.bandwidth)
                .array();               
    }

    
	public int getFrameIndex() {
		return frameIndex;
	}

	public int getFragmentIndex() {
		return fragmentIndex;
	}

	public int getBandwidth() {
		return bandwidth;
	}
    
}
