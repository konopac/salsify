/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hm.networks2.salsify.sender.helper;

/**
 *
 * @author nicfel
 */
public interface ITransportProtocolListener {
    
    /**
     * Tells the transport protocol listener that it should reset. In Salsify
     * that means that an ACK was received that indicates a fragment loss. We
     * need to go back to a state that is safe (a state that was completely 
     * received and acknowledged).
     */
    void reset();
    
    /**
     * Tells the transport protocol listener that the frame with the given index
     * was completely acknowledged.
     * 
     * @param frameIndex identifying the frame.
     * 
     */
    void acknowledged(int frameIndex);
    
}
