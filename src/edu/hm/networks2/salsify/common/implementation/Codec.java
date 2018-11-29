package edu.hm.networks2.salsify.common.implementation;

import java.awt.image.BufferedImage;

import edu.hm.networks2.salsify.common.ICodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
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
        
        if (frame.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            throw new IllegalArgumentException("Encode: Type of frame is not BGR!");
        }
        
        Optional<byte[]> encodedImage;
        
        // convert quality 
        float floatQuality = quality / 100f;
        
        if (state.isPresent()) {
            // calculate difference --> encode
            encodedImage = Optional.empty();
            System.out.println("Difference not supported yet");
        } else {
            
            encodedImage = convertToJpeg(frame, floatQuality);
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
        Optional<byte[]>jpegImage = Optional.empty();
        
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

}
