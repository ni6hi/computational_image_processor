package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class TestBatch {
    public static void main(String[] args) throws Exception {
        System.out.println("--- Starting Batch Processor Test ---");

        File inputDir = new File("test_in");
        File outputDir = new File("test_out");
        inputDir.mkdirs();

        // 1. Generate 3 dummy image files in test_in
        for (int i = 1; i <= 3; i++) {
            BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(i == 1 ? Color.RED : (i == 2 ? Color.GREEN : Color.BLUE));
            g.fillRect(0, 0, 150, 150);
            g.dispose();
            ImageIO.write(img, "png", new File(inputDir, "test_image_" + i + ".png"));
        }

        // 2. Set up pipeline
        ImagePipeline pipeline = new ImagePipeline();
        pipeline.addOperation(new GrayscaleOperation());

        // 3. Process directory asynchronously
        BatchProcessor.processDirectory(inputDir, outputDir, pipeline, "png", new BatchProcessor.ProgressListener() {
            @Override
            public void onProgress(int completed, int total, String currentFile) {
                System.out.println("Processed [" + completed + "/" + total + "]: " + currentFile);
            }

            @Override
            public void onComplete() {
                File[] processedFiles = outputDir.listFiles();
                int count = (processedFiles != null) ? processedFiles.length : 0;

                if (count == 3) {
                    System.out.println("SUCCESS: All 3 images processed and saved into 'test_out'.");
                } else {
                    System.err.println("FAILED: Expected 3 processed files, found " + count);
                }
            }
        });
    }
}