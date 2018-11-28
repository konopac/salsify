package edu.hm.networks2.salsify.common.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.common.ICodec;

public class Codec implements ICodec {

	@Override
	public BufferedImage encode(BufferedImage state, BufferedImage frame, int quality) {
		System.out.println("encoding frame " + frame + " with quality " + quality);
		return null;
	}

	@Override
	public BufferedImage decode(BufferedImage state, BufferedImage frame) {
		System.out.println("decoding frame " + frame);
		return null;
	}

}
