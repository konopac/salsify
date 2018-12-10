package edu.hm.networks2.salsify;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;

public class Main {

	public static void main(String[] args) {
		System.out.println("starting sender...");

		
		// encode
		BufferedImage img1 = null;
		BufferedImage img2 = null;
		try {
			img1 = ImageIO.read(new File("/home/philipp/Studium/workspaces/netzwerke2/Salsify/images/image1.jpg"));
			img2 = ImageIO.read(new File("/home/philipp/Studium/workspaces/netzwerke2/Salsify/images/image2.jpg"));
		} catch (IOException e) {
		}

		ICodec codec = new Codec();

		try (FileOutputStream outputStream = new FileOutputStream("/home/philipp/Studium/workspaces/netzwerke2/Salsify/images/difference.jpg")) {

			
			final Optional<BufferedImage> state = Optional.of(img1);
			final Optional<byte[]> difference = codec.encode(state, img2, 100);
			outputStream.write(difference.get());
			
			final Optional<BufferedImage> result = codec.decode(state, difference.get());
			
		    // retrieve image
		    BufferedImage bi = result.get();
		    File outputfile = new File("/home/philipp/Studium/workspaces/netzwerke2/Salsify/images/result.jpg");
		    ImageIO.write(bi, "jpg", outputfile);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// decode
		
		

//		ISalsify sender = new Salsify();
//		sender.start();
//		System.out.println("exiting sender...");
	}

}
