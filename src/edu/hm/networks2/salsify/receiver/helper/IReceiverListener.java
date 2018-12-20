package edu.hm.networks2.salsify.receiver.helper;

/**
 * This interface describes a listener to a receiver and contains all needed methods.
 * 
 * @author Philipp
 */
public interface IReceiverListener {
    
	/**
	 * This method gets called every time a new frame is available.
	 * 
	 * @param data contains the raw frame data
	 * @param frameIndex is the frame index of the frame
	 * @param sourceFrameIndex is the source state of the encoding
	 * @param gracePeriod is the grace period
	 */
	void frameReceived(byte[] data, int frameIndex, int sourceFrameIndex, int gracePeriod);
	
}
