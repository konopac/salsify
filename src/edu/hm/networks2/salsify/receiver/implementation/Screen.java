package edu.hm.networks2.salsify.receiver.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.receiver.IScreen;

public class Screen implements IScreen {

	@Override
	public void start() {
        System.out.println("SCREEN: \t started");
	}

	@Override
	public void displayFrame(BufferedImage frame) {
        System.out.println("SCREEN: \t displaying frame " + frame);
	}

}
