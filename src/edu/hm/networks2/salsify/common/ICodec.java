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
     * @param state The state of the encoder.
     * @param frame The frame to be encoded.
     * @param quality The quality of the encoding (integer in range [0, 100]).
     * 
     * @return The jpeg encoded frame.
     */
    Optional<byte[]> encode(Optional<BufferedImage> state, BufferedImage frame, int quality);

    /**
     * Decode an image. The decoding operations is the opposite of the encode
     * operation. It will decode the provided jpeg and in case a state is given
     * it will perform the reverse operation to the difference calculation on the
     * decoder state.
     * 
     * @param state The state of the decoder.
     * @param encodedFrame The jpeg encoded frame which will be decoded.
     * 
     * @return Optional containing a BufferedImaged in case everything was
     * successful.
     */
    Optional<BufferedImage> decode(Optional<BufferedImage> state, byte[] encodedFrame);
	
}
