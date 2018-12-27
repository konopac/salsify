package edu.hm.networks2.salsify.receiver.implementation;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import edu.hm.networks2.salsify.receiver.IScreen;

public class Screen implements IScreen {

	private static final int WIDTH = 864;
	private static final int HEIGHT = 480;
	
    private final JFrame parent;
    private final JLabel imageContainer;

    public Screen() {
        super();
        this.parent = new JFrame("Salsify Receiver");
        this.parent.getContentPane().setLayout(new FlowLayout());
        this.parent.setSize(WIDTH, HEIGHT);
        this.imageContainer = new JLabel();
        this.parent.getContentPane().add(this.imageContainer);
    }
    
    @Override
    public void start() {
        this.parent.setVisible(true);
    }
    
    @Override
    public void displayFrame(BufferedImage frame) {
        imageContainer.removeAll();
        final ImageIcon image = new ImageIcon(frame.getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH));
        imageContainer.setIcon(image);
    }
    
    
    

}
