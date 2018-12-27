package edu.hm.networks2.salsify;

import edu.hm.networks2.salsify.common.implementation.GlobalLogger;
import edu.hm.networks2.salsify.receiver.ISalsifyReceiverCore;
import edu.hm.networks2.salsify.receiver.implementation.SalsifyReceiverCore;
import edu.hm.networks2.salsify.sender.ISalsifySenderCore;
import edu.hm.networks2.salsify.sender.implementation.SalsifySenderCore;
import java.util.logging.Level;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        final String mode = args.length != 0 ? args[0] : "";

        switch (mode) {
            case "sender":
            case "s":
                GlobalLogger.getInstance("Sender_").log(Level.INFO, "Starting salsify sender...");
                ISalsifySenderCore sender = new SalsifySenderCore();
                sender.start();
                sender.join();
                GlobalLogger.getInstance().info("Stopped salsify sender.");
                break;
            case "receiver":
            case "r":
                GlobalLogger.getInstance("Receiver_").info("Starting salsify receiver...");
                ISalsifyReceiverCore receiver = new SalsifyReceiverCore();
                receiver.start();
                receiver.join();
                GlobalLogger.getInstance().info("Stopped salsify reciever.");
                break;
            default:
                System.out.println("Please start this program with one of the following arguments to determine which mode to use:\n");
                System.out.println("Sender: \"sender\" or \"s\"");
                System.out.println("Receiver:  \"receiver\" or \"r\"");
                break;
        }
    }

}
