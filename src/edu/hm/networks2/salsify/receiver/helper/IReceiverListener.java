package edu.hm.networks2.salsify.receiver.helper;

public interface IReceiverListener {
    
	void frameReceived(byte[] data, int frameIndex, int sourceFrameIndex, int gracePeriod);
	
}
