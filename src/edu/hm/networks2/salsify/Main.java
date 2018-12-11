package edu.hm.networks2.salsify;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import edu.hm.networks2.salsify.common.ICodec;
import edu.hm.networks2.salsify.common.implementation.Codec;
import edu.hm.networks2.salsify.common.implementation.SalsifyFragmentPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) {
		System.out.println("starting sender...");
                
            ByteBuffer allocate = ByteBuffer.allocate(1024);
            byte[] array = allocate.putInt(12) // fragment index
                    .putInt(15) // grace period
                    .putInt(3)  // currten frame index
                    .putInt(2)  // previous (state) frame index
                    .putInt(5)  // 5 fragments remaining
                    .array();
            SalsifyFragmentPacket salsifyFragmentPacket = new SalsifyFragmentPacket(array);
            System.out.println("fragment index: " + salsifyFragmentPacket.getFragmentIndex());
            System.out.println("grace period: " + salsifyFragmentPacket.getGracePeriod());
            System.out.println("frame index: " + salsifyFragmentPacket.getFrameIndex());
            System.out.println("state index: " + salsifyFragmentPacket.getFrameIndexState());
            System.out.println("remaining fragments: " + salsifyFragmentPacket.getRemainingFragments());
            
            salsifyFragmentPacket = new SalsifyFragmentPacket(12, 15, 3, 2, 5, ByteBuffer.allocate(4).putInt(123456).array());
            
            System.out.println("fragment index: " + salsifyFragmentPacket.getFragmentIndex());
            System.out.println("grace period: " + salsifyFragmentPacket.getGracePeriod());
            System.out.println("frame index: " + salsifyFragmentPacket.getFrameIndex());
            System.out.println("state index: " + salsifyFragmentPacket.getFrameIndexState());
            System.out.println("remaining fragments: " + salsifyFragmentPacket.getRemainingFragments());
            System.out.println("data: " + ByteBuffer.wrap(salsifyFragmentPacket.getData()).getInt());
            System.out.println("raw bytes: " + Arrays.toString(salsifyFragmentPacket.getRawPacket()));
            
		// encode
		BufferedImage img1 = null;
		BufferedImage img2 = null;
		try {
			img1 = ImageIO.read(new File("/home/nicfel/Pictures/one.jpeg"));
			img2 = ImageIO.read(new File("/home/nicfel/Pictures/two.jpeg"));
		} catch (IOException e) {
		}

		ICodec codec = new Codec();

		try (FileOutputStream outputStream = new FileOutputStream("difference.jpg")) {

			
			final Optional<BufferedImage> state = Optional.of(img1);
			final Optional<byte[]> difference = codec.encode(state, img2, 100);
			outputStream.write(difference.get());
			
			final Optional<BufferedImage> result = codec.decode(state, difference.get());
			
		    // retrieve image
		    BufferedImage bi = result.get();
		    File outputfile = new File("result.jpg");
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
