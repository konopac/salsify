package edu.hm.networks2.salsify.common;

import java.awt.image.BufferedImage;
import java.util.Optional;

public interface ICodec {

    /**
     * Encode an image. The encoding is pretty simple. It calculates the 
     * difference encodes it to jpeg and returns it. In case you do not 
     * provide a state the image will simply be the same as frame but encoded
     * to jpeg.
     * 
     * @param state
     * @param frame
     * @param quality
     * @return 
     */
    Optional<byte[]> encode(Optional<BufferedImage> state, BufferedImage frame, int quality);

    BufferedImage decode(Optional<BufferedImage> state, BufferedImage frame);
	
}
