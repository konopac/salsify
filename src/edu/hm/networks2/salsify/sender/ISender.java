package edu.hm.networks2.salsify.sender;

import java.io.IOException;

public interface ISender {

	void sendFrame(byte[] frame, int frameIndex, int sourceFrameIndex, int gracePeriod) throws IOException;
	
	int getLatestInterArrivalTime();
	
	void stopListening();
	
}
