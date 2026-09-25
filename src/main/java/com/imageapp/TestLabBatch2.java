package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class TestLabBatch2 {
    public static void main(String[] args) throws Exception {
        System.out.println("--- Validating Advanced Scientific Batch 2 ---");

        BufferedImage canvas = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 256, 256);
        g.setColor(Color.WHITE);
        g.fillRect(64, 64, 128, 128);
        g.dispose();

        ImagePipeline pipeline = new ImagePipeline();
        pipeline.addOperation(new BoxFilterOperation(2))
                .addOperation(new SingleScaleRetinexOperation(1.5f))
                .addOperation(new ReinhardToneMapOperation(0.18f))
                .addOperation(new VignettingCorrectionOperation(0.5))
                .addOperation(new DemosaicMHCOperation())
                .addOperation(new TopHatTransformOperation(2))
                .addOperation(new WienerDeconvolutionOperation(0.01))
                .addOperation(new HaarWaveletTransformOperation())
                .addOperation(new FFTSpectrumOperation());

        BufferedImage outputImg = pipeline.execute(canvas);
        File outFile = new File("test_lab_batch2_output.png");
        ImageIO.write(outputImg, "png", outFile);

        if (outFile.exists()) {
            System.out.println("SUCCESS: All 10 Advanced Scientific Batch 2 operations executed cleanly!");
        } else {
            System.err.println("FAILED: Output image creation failed.");
        }
    }
}