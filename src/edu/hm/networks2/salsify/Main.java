package edu.hm.networks2.salsify;

import edu.hm.networks2.salsify.sender.ISalsify;
import edu.hm.networks2.salsify.sender.implementation.Salsify;

public class Main {

	public static void main(String[] args) {
		System.out.println("starting sender...");
		ISalsify sender = new Salsify();
		sender.start();
		System.out.println("exiting sender...");
	}

}
