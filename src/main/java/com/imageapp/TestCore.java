package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class TestCore {
    public static void main(String[] args) throws Exception {
        System.out.println("--- Starting Core Pipeline Test ---");

        // 1. Generate a 400x400 red test image in memory
        BufferedImage sample = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sample.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 400, 400);
        g.dispose();

        // 2. Build pipeline: Grayscale -> Resize (200x200) -> Watermark
        ImagePipeline pipeline = new ImagePipeline();
        pipeline.addOperation(new GrayscaleOperation())
                .addOperation(new ResizeOperation(200, 200))
                .addOperation(new WatermarkOperation("TEST PASSED", 0.8f));

        // 3. Execute
        BufferedImage result = pipeline.execute(sample);

        // 4. Save output file
        File output = new File("test_core_output.png");
        ImageIO.write(result, "png", output);

        // 5. Assertions
        boolean widthCorrect = result.getWidth() == 200;
        boolean heightCorrect = result.getHeight() == 200;
        boolean fileExists = output.exists();

        if (widthCorrect && heightCorrect && fileExists) {
            System.out.println("SUCCESS: Pipeline processed image correctly.");
            System.out.println("Saved output to: " + output.getAbsolutePath());
        } else {
            System.err.println("FAILED: Output dimensions or file creation failed.");
        }
    }
}