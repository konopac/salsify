package edu.hm.networks2.salsify.sender.implementation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import edu.hm.networks2.salsify.sender.IWebcam;
import edu.hm.networks2.salsify.sender.helper.IWebcamListener;

/**
 * This is an implementation of IWebcam. It loads all available frames from the file system
 * and sends them to all listeners.
 * 
 * @author Philipp
 */
public class Webcam implements IWebcam {
	
	/**
	 * Directory that contains all frames.
	 */
	private static final String FRAME_DIRECTORY= System.getProperty("user.dir") + "\\frames\\";
	
	/**
	 * Prefix for all frame names.
	 */
	private static final String FRAME_NAME = "frame";
	
	/**
	 * File extension for all frames.
	 */
	private static final String FRAME_EXTENSION = ".png";
	
	/**
	 * Frames per second at which the webcam is sending.
	 */
	private static final int FPS = 1;
	
	
	/**
	 * A queue that stores all available frames.
	 */
	private Queue<BufferedImage> frames;
	
	/**
	 * A list of all webcam listeners.
	 */
	private List<IWebcamListener> listeners;
	
	
	/**
	 * Constructor that instantiates all needed fields.
	 */
	public Webcam() {
		frames = new LinkedBlockingQueue<>();
		listeners = new ArrayList<>();
	}
	
	/**
	 * {@inheritDoc}<br>
	 * This implementation loads all available frames from a file system and stores them in a queue.
	 * After that a timer thread is started, which sends all frames from the queue.
	 * 
	 * @see edu.hm.networks2.salsify.sender.IWebcam#start()
	 */
	@Override
	public void start() {
		// load all available images in an array
		int counter = 0;
		boolean searching = true;
		while (searching) {
			try {
				// load next frame and store it in memory
				frames.add(ImageIO.read(new File(FRAME_DIRECTORY + FRAME_NAME + counter + FRAME_EXTENSION)));
				counter++;
			} catch (IOException exception) {
				// when next frame is not found, end searching
				System.out.println(counter + " images found and loaded");
				searching = false;
			};
		}
		
		// produce a new frame every 500 ms
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					// get next frame and send the frame to all listeners
					listeners.forEach(listener -> listener.receiveFrame(frames.remove()));
				} catch (NoSuchElementException exception) {
					timer.cancel();
				}
			}
		}, 0, 1000 / FPS);
	}
	
	/**
	 * {@inheritDoc}<br>
	 * This implementation adds the given listener to the listeners list.
	 * 
	 * @see edu.hm.networks2.salsify.sender.IWebcam#register(edu.hm.networks2.salsify.sender.helper.IWebcamListener)
	 */
	@Override
	public void register(IWebcamListener listener) {
		this.listeners.add(listener);
	}

}
