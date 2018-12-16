package edu.hm.networks2.salsify.sender;

public interface ISender {

	void sendFrame(byte[] frame, int gracePeriod, int frameIndex);
	
	void startReceivingAcks();
	
}
