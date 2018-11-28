package edu.hm.networks2.salsify.sender;

public interface Sender {

	void sendFrame();
	
	void startReceivingAcks();
	
}
