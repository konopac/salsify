package edu.hm.networks2.salsify.common.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.common.ICodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
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
    public BufferedImage decode(Optional<BufferedImage> state, BufferedImage frame) {
        System.out.println("decoding frame " + frame);
        return null;
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
            System.out.println(
                    "Encode: Error occured while encoding image to jpeg: "
                    + exception.toString()
            );
        }

        return jpegImage;
    }

    /**
     * Calculate the difference between two images. The difference is simply
     * calculated with pixelBefore - pixelAfter.
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
     * Calculates the difference between two pixel components.
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

        return (Math.abs(getRed.apply(rgbOne) - getRed.apply(rgbTwo)) << 16)
                | (Math.abs(getGreen.apply(rgbOne) - getGreen.apply(rgbTwo)) << 8)
                | Math.abs(getBlue.apply(rgbOne) - getBlue.apply(rgbTwo));
    }
}
