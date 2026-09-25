package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class TestLabBatch {
    public static void main(String[] args) throws Exception {
        System.out.println("--- Validating Scientific Lab Pipeline ---");

        BufferedImage canvas = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 400, 400);
        g.setColor(Color.DARK_GRAY);
        g.fillOval(100, 100, 200, 200);
        g.dispose();

        ImagePipeline pipeline = new ImagePipeline();
        pipeline.addOperation(new RotateOperation(30))
                .addOperation(new CropOperation(10, 10, 300, 300))
                .addOperation(new GaussianBlurOperation(1.2f))
                .addOperation(new MedianFilterOperation(1))
                .addOperation(new BilateralFilterOperation(3.0, 20.0))
                .addOperation(new CLAHEOperation(16, 2.0f))
                .addOperation(new UnsharpMaskOperation(0.8f))
                .addOperation(new SobelEdgeDetectionOperation())
                .addOperation(new OtsuThresholdOperation())
                .addOperation(new MorphologyOperation(MorphologyOperation.Type.DILATION, 1));

        BufferedImage outputImg = pipeline.execute(canvas);
        File outFile = new File("test_lab_output.png");
        ImageIO.write(outputImg, "png", outFile);

        if (outFile.exists()) {
            System.out.println("SUCCESS: Scientific image processing pipeline executed flawlessly!");
        } else {
            System.err.println("FAILED: Output file was not generated.");
        }
    }
}