package edu.hm.networks2.salsify.receiver.implementation;

import java.awt.image.BufferedImage;
import javax.swing.JApplet;
import edu.hm.networks2.salsify.receiver.IScreen;
import java.awt.FlowLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Screen implements IScreen {

    private final JFrame parent;
    private final JLabel imageContainer;

    public Screen() {
        super();
        this.parent = new JFrame("Salsify Receiver");
        this.parent.getContentPane().setLayout(new FlowLayout());
        this.parent.setSize(1280, 720);
        this.imageContainer = new JLabel();
        this.imageContainer.setSize(1280, 720);
        this.parent.getContentPane().add(this.imageContainer);
    }
    
    @Override
    public void start() {
        this.parent.setVisible(true);
    }
    
    @Override
    public void displayFrame(BufferedImage frame) {
        System.out.println("SCREEN: \t displaying frame " + frame);
        imageContainer.removeAll();
        imageContainer.setIcon(new ImageIcon(frame));
    }
    
    
    

}
