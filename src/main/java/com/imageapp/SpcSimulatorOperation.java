package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.Random;

public class SpcSimulatorOperation implements ImageOperation {

    private final double scalingFactor;
    private final int totalFrames;

    public SpcSimulatorOperation(double scalingFactor, int totalFrames) {
        this.scalingFactor = scalingFactor > 0 ? scalingFactor : 1000.0;
        this.totalFrames = totalFrames > 0 ? totalFrames : 50;
    }

    public SpcSimulatorOperation() {
        this(1000.0, 50);
    }

    @Override
    public BufferedImage process(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 1. Grayscale intensity
                double gray = 0.299 * r + 0.587 * g + 0.114 * b;

                // 2. Poisson rate per binary frame
                double lambda = gray / scalingFactor;

                // 3. Probability of at least 1 photon arrival
                double p = 1.0 - Math.exp(-lambda);

                // 4. Simulate photon counts across frames
                int photonDetectedCount = 0;
                for (int f = 0; f < totalFrames; f++) {
                    if (random.nextDouble() < p) {
                        photonDetectedCount++;
                    }
                }

                // 5. Reconstruct intensity from binary frame ratio
                double ratio = (double) photonDetectedCount / totalFrames;
                if (ratio >= 0.999) {
                    ratio = 0.999;
                }

                double estimatedLambda = -Math.log(1.0 - ratio);
                double reconstructedGray = estimatedLambda * scalingFactor;

                int val = Math.max(0, Math.min(255, (int) Math.round(reconstructedGray)));
                int newRgb = (val << 16) | (val << 8) | val;

                output.setRGB(x, y, newRgb);
            }
        }
        return output;
    }

    @Override
    public String getName() {
        return String.format("SPC Simulator (scaling=%.0f, frames=%d)", scalingFactor, totalFrames);
    }
}