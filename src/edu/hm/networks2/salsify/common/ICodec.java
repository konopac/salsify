package edu.hm.networks2.salsify.common;

import java.awt.image.BufferedImage;

public interface ICodec {

	BufferedImage encode(BufferedImage state, BufferedImage frame, int quality);
	
	BufferedImage decode(BufferedImage state, BufferedImage frame);
	
}
