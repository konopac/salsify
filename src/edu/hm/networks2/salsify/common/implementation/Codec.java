package edu.hm.networks2.salsify.common.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.common.ICodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class Codec implements ICodec {

    private static final String ENCODING = "jpg";

    @Override
    public Optional<byte[]> encode(Optional<BufferedImage> state, BufferedImage frame, int quality) {

        if (frame == null) {
            throw new IllegalArgumentException("Encode: Provided frame must not be null!");
        }

        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("Encode: Range for quality must be [0; 100]!");
        }

        Optional<byte[]> encodedImage;

        // convert quality 
        float percentQuality = quality / 100f;

        if (state.isPresent()) {

            if (frame.getType() != state.get().getType()) {
                throw new IllegalArgumentException("Encode: Type of frame is not same as state!");
            }
            BufferedImage difference = calculateDifference(state.get(), frame);
            // calculate difference --> encode
            encodedImage = convertToJpeg(difference, percentQuality);
        } else {

            encodedImage = convertToJpeg(frame, percentQuality);
        }

        return encodedImage;

    }

    @Override
    public Optional<BufferedImage> decode(Optional<BufferedImage> state, byte[] encodedFrame) {
        
        if (encodedFrame == null) {
            throw new IllegalArgumentException("Decode: Provided frame must not be null!");
        }
        
        Optional<BufferedImage> decodedFrame = Optional.empty();
        try {
            decodedFrame = 
                    Optional.of(ImageIO.read(new ByteArrayInputStream(encodedFrame)));
        } catch (IOException exception) {
            GlobalLogger.getInstance().log(Level.SEVERE, "Decode: Could not decode encoded frame!{0}", exception.toString());
        }
        
        Optional<BufferedImage> result;
        
        // modify result in case state is present
        // we need to add the difference
        if (state.isPresent() && decodedFrame.isPresent()) {
            result = Optional.of(addDifference(state.get(), decodedFrame.get()));
        } else {
            result = decodedFrame;
        }

        return result;
    }

    /**
     * Takes image and converts to a raw byte array containing an image in the
     * jpeg format.
     *
     * @param toConvert the buffered image to convert.
     * @param quality a float in [0.0; 100.0]
     *
     * @return byte array containing the jpeg data.
     */
    private Optional<byte[]> convertToJpeg(BufferedImage toConvert, float quality) {

        // default empty in case something goes wrong
        Optional<byte[]> jpegImage = Optional.empty();

        // prepare parameters for the jpeg writer with quality being the provided
        // quality
        final JPEGImageWriteParam jpegParameters = new JPEGImageWriteParam(null);
        jpegParameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParameters.setCompressionQuality(quality);

        // get an image writer
        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName(ENCODING).next();

        // get an image output stream
        try (ByteArrayOutputStream byteArrayOutputStream
                = new ByteArrayOutputStream();
                ImageOutputStream imageOutputStream
                = new MemoryCacheImageOutputStream(byteArrayOutputStream)) {

            // set where the image data should be written
            jpegWriter.setOutput(imageOutputStream);
            // convet the image to something that can be used with write()
            // of the jpeg writer
            IIOImage outputImage = new IIOImage(toConvert, null, null);
            // finally write the image with the given parameters
            jpegWriter.write(null, outputImage, jpegParameters);
            // allow ressources to be freed
            jpegWriter.dispose();

            imageOutputStream.flush();

            jpegImage = Optional.of(byteArrayOutputStream.toByteArray());

        } catch (IOException exception) {
            GlobalLogger.getInstance().log(Level.SEVERE, "Encode: Error occured while encoding image to jpeg: {0}", exception.toString());
        }

        return jpegImage;
    }

    /**
     * Calculate the difference between two images. The difference is simply
     * calculated using method calculatePixelDifference(...).
     *
     * @param before Image before.
     * @param after Image after.
     *
     * @return BufferedImage containing the difference.
     */
    private BufferedImage calculateDifference(BufferedImage before, BufferedImage after) {

        if (before == null) {
            throw new IllegalArgumentException("Difference Calculation: Image before must not be null!");
        }

        if (after == null) {
            throw new IllegalArgumentException("Difference Calculation: Image after must not be null!");
        }

        if (after.getHeight() == before.getHeight() && after.getWidth() == before.getHeight()) {
            throw new IllegalArgumentException("Difference Calculation: Image dimensions must align!");
        }

        // result image
        BufferedImage result = new BufferedImage(
                before.getWidth(),
                before.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        // loop and calculate difference
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                result.setRGB(x, y, calculatePixelDifference(before.getRGB(x, y), after.getRGB(x, y)));
            }
        }

        return result;

    }

    /**
     * Calculates the difference between two pixel components. The difference
     * is always expressed as an addition with modulo 256 to do a wrap around.
     *
     * @param rgbOne First pixel value.
     * @param rgbTwo Second pixel value.
     *
     * @return integer containing the difference
     */
    private int calculatePixelDifference(int rgbOne, int rgbTwo) {

        // extract red, green and blue with these
        Function<Integer, Integer> getRed = rgb -> (rgb >> 16) & 0xFF;
        Function<Integer, Integer> getGreen = rgb -> (rgb >> 8) & 0xFF;
        Function<Integer, Integer> getBlue = rgb -> rgb & 0xFF;
        
        BiFunction<Integer, Integer, Integer> componentDifference = 
                (componentOne, componentTwo) -> {
                    int result;
                    // difference between the two components
                    // positive: component value increase (easy case)
                    // negative: component value decrease (not so easy case)
                    int difference = componentTwo - componentOne;
                    
                    if (difference < 0) {
                        // not so easy case
                        // we will send the difference as addition with mod 256
                        difference = difference + 256;
                    }
                    
                    return difference;
                };

        return (componentDifference.apply(getRed.apply(rgbOne), getRed.apply(rgbTwo))) << 16
                | (componentDifference.apply(getGreen.apply(rgbOne), getGreen.apply(rgbTwo))) << 8
                | (componentDifference.apply(getBlue.apply(rgbOne), getBlue.apply(rgbTwo)));
    }
    
    /**
     * Adds the difference to the given state.
     * 
     * @param state The image that the difference will be added to.
     * @param difference The difference values stored in an image.
     * 
     * @return The resulting image. 
     */
    private BufferedImage addDifference(BufferedImage state, BufferedImage difference) {
        
        if (state == null) {
            throw new IllegalArgumentException("Difference Calculation: Image state must not be null!");
        }

        if (difference == null) {
            throw new IllegalArgumentException("Difference Calculation: Image difference must not be null!");
        }

        if (difference.getHeight() == state.getHeight() && difference.getWidth() == state.getHeight()) {
            throw new IllegalArgumentException("Difference Calculation: Image dimensions must align!");
        }
        
        // result image
        BufferedImage result = new BufferedImage(
                state.getWidth(),
                state.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        // loop and calculate difference
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                result.setRGB(x, y, addPixelDifference(
                        state.getRGB(x, y), 
                        difference.getRGB(x, y))
                );
            }
        }
        
        return result;
        
    }
    
    /**
     * This performs the reverse operation of the operation performed in
     * calculated pixel difference.
     * 
     * @param rgbState The pixel value before adding the difference.
     * @param rgbDifference The difference.
     * 
     * @return the new pixel value coded into an integer. 
     */
    private int addPixelDifference(int rgbState, int rgbDifference) {
        
        int red = getRed(rgbState) + getRed(rgbDifference) % 256;
        int green = getGreen(rgbState) + getGreen(rgbDifference) % 256;
        int blue = getBlue(rgbState) + getBlue(rgbDifference) % 256;
        
        return  (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Extract red from rgb values coded into integer.
     * @param rgb The rgb value.
     * @return The value of the red component.
     */
    private int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }
    
    /**
     * Extract green from rgb values coded into integer.
     * @param rgb The rgb value.
     * @return The value of the green component.
     */
    private int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }
    
    /**
     * Extract blue from rgb values coded into integer.
     * @param rgb The rgb value.
     * @return The value of the blue component.
     */
    private int getBlue(int rgb) {
        return rgb & 0xFF;
    }
   
}
