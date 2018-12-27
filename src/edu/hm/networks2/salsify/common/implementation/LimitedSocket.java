package edu.hm.networks2.salsify.common.implementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class LimitedSocket extends DatagramSocket {
	
	private static final int THROUGHPUT = 100_000;

	private final Queue<DatagramPacket> queue;
	
	private boolean loseNextPacket;

	public LimitedSocket(int port, InetAddress ip, int packetSize) throws SocketException, UnknownHostException {
		super(port, ip);
		queue = new LinkedBlockingQueue<>();
		
		// delay = 1000 ms / number of packets that fit into the network
		int delay = 1000 / (THROUGHPUT / packetSize);
		// delay must not be 0 -> fastest = sending every millisecond
		delay = delay > 0 ? delay : 1;
		final Timer timer = new Timer();
		final TimerTask task = new TimerTask() {                    
                        
			@Override
			public void run() {
				// poll the next packet from the queue
				final DatagramPacket packet = queue.poll();
				// if it exists, send it
				if (packet != null) {
					// is a packet loss forced?
					if (loseNextPacket) {
						loseNextPacket = false;
					} else {
						try {
							sendSuper(packet);
						} catch (SocketException exception) {
		                    // thread is stopping when socket is closed
							cancel();
						} catch (IOException exception) {
							System.err.println("Error while sending packet from queue.");
							System.out.println(exception);
						}
					}
				}
			}
			
		};
		timer.schedule(task, 0, delay);
	}

	@Override
	public void send(DatagramPacket packet) throws IOException {
		// add the packet to the queue
		queue.add(packet);
	}

	public void loseNexPacket() {
		loseNextPacket = true;
	}
	

	/**
	 * Small hack, so we can access super.send() within the timer task.
	 * 
	 * @param packet is the packet to send
	 * @throws IOException when an i/o error occurs
	 */
	private void sendSuper(DatagramPacket packet) throws IOException {
		super.send(packet);
	}

}
