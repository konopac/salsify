package edu.hm.networks2.salsify.common.implementation;

import java.nio.ByteBuffer;

public class SalsifyAck {

	public static final int SIZE = Integer.BYTES * 3;
	
    private final int frameIndex;
    private final int fragmentIndex;
    private final int interArrivalTime;    

    
    public SalsifyAck(byte[] rawData) {
        if (rawData == null) {
            throw new NullPointerException("SalsifyAck: data is not allowed to be null!");
        }
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(rawData);
        this.frameIndex = byteBuffer.getInt();
        this.fragmentIndex = byteBuffer.getInt();
        this.interArrivalTime = byteBuffer.getInt();
	}

	public SalsifyAck(int frameIndex, int fragmentIndex, int interArrivalTime) {
		this.frameIndex = frameIndex;
		this.fragmentIndex = fragmentIndex;
		this.interArrivalTime = interArrivalTime;
	}
    
    public byte[] getRawData() {
        return ByteBuffer.allocate(SIZE)
        		.putInt(this.frameIndex)
                .putInt(this.fragmentIndex)
                .putInt(this.interArrivalTime)
                .array();               
    }

	public int getFrameIndex() {
		return frameIndex;
	}

	public int getFragmentIndex() {
		return fragmentIndex;
	}

	public int getInterArrivalTime() {
		return interArrivalTime;
	}
    
}
