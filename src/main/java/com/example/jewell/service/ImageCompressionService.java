package com.example.jewell.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageCompressionService {
    
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;
    private static final float QUALITY = 0.85f; // 85% quality for JPEG
    private static final int MAX_FILE_SIZE_KB = 500; // Max 500KB after compression

    /**
     * Compress and resize an image file.
     * @param file The original image file
     * @return Compressed image as InputStream
     * @throws IOException If compression fails
     */
    public InputStream compressImage(MultipartFile file) throws IOException {
        // Read original image
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Invalid image file");
        }

        // Calculate new dimensions while maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);
            
            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }

        // Resize image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // Determine output format
        String originalFilename = file.getOriginalFilename();
        String format = "jpg"; // Default to JPEG for better compression
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if ("png".equals(extension)) {
                format = "png";
            } else if ("gif".equals(extension)) {
                format = "gif";
            }
        }

        // Compress and write to output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        if ("jpg".equals(format) || "jpeg".equals(format)) {
            // Use JPEG with quality setting for better compression
            java.util.Iterator<javax.imageio.ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IOException("No JPEG writer available");
            }
            javax.imageio.ImageWriter writer = writers.next();
            javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(QUALITY);
            }
            
            writer.write(null, new javax.imageio.IIOImage(resizedImage, null, null), param);
            writer.dispose();
            ios.close();
        } else {
            // For PNG and other formats, use standard ImageIO
            ImageIO.write(resizedImage, format, outputStream);
        }

        byte[] compressedBytes = outputStream.toByteArray();
        
        // If still too large, reduce quality further
        int sizeKB = compressedBytes.length / 1024;
        if (sizeKB > MAX_FILE_SIZE_KB) {
            return compressWithLowerQuality(resizedImage, sizeKB);
        }

        return new ByteArrayInputStream(compressedBytes);
    }

    private InputStream compressWithLowerQuality(BufferedImage image, int currentSizeKB) throws IOException {
        float quality = QUALITY;
        int targetSizeKB = MAX_FILE_SIZE_KB;
        
        // Reduce quality until we reach target size
        while (currentSizeKB > targetSizeKB && quality > 0.3f) {
            quality -= 0.1f;
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            java.util.Iterator<javax.imageio.ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                break;
            }
            javax.imageio.ImageWriter writer = writers.next();
            javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();
            
            byte[] compressedBytes = outputStream.toByteArray();
            currentSizeKB = compressedBytes.length / 1024;
            
            if (currentSizeKB <= targetSizeKB) {
                return new ByteArrayInputStream(compressedBytes);
            }
        }
        
        // If still too large, return the last attempt
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
