package edu.hm.networks2.salsify;

import edu.hm.networks2.salsify.sender.ISalsify;
import edu.hm.networks2.salsify.sender.implementation.Salsify;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		
		final String mode = args.length != 0 ? args[0] : "";
		
		switch (mode) {
		case "sender":
		case "s":
			System.out.println("MAIN: \t\t starting salsify sender");
			ISalsify sender = new Salsify();
			sender.start();
			sender.join();
			System.out.println("MAIN: \t\t stopped salsify sender");
			break;
		case "receiver":
		case "r":
			System.out.println("MAIN: \t\t starting salsify receiver");
			// TODO
			System.out.println("MAIN: \t\t stopped salsify receiver");
			break;
		default:
			System.out.println("Please start this program with one of the following arguments to determine which mode to use:\n");
			System.out.println("Sender: \"sender\" or \"s\"");
			System.out.println("Receiver:  \"receiver\" or \"r\"");
			break;
		}
	}

}
