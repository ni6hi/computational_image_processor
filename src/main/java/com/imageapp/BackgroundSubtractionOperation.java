package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.List;

public class BackgroundSubtractionOperation implements TemporalVideoOperation {

    private final double alpha;
    private double[][] bgR;
    private double[][] bgG;
    private double[][] bgB;

    public BackgroundSubtractionOperation(double alpha) {
        this.alpha = alpha;
    }

    public BackgroundSubtractionOperation() {
        this(0.05);
    }

    @Override
    public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
        BufferedImage currentImg = currentFrame.image();
        int width = currentImg.getWidth();
        int height = currentImg.getHeight();

        if (bgR == null || bgR.length != width || bgR[0].length != height) {
            bgR = new double[width][height];
            bgG = new double[width][height];
            bgB = new double[width][height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = currentImg.getRGB(x, y);
                    bgR[x][y] = (rgb >> 16) & 0xFF;
                    bgG[x][y] = (rgb >> 8) & 0xFF;
                    bgB[x][y] = rgb & 0xFF;
                }
            }
            BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    copy.setRGB(x, y, currentImg.getRGB(x, y));
                }
            }
            return copy;
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = currentImg.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int diffR = Math.abs(r - (int) bgR[x][y]);
                int diffG = Math.abs(g - (int) bgG[x][y]);
                int diffB = Math.abs(b - (int) bgB[x][y]);

                int diff = Math.max(diffR, Math.max(diffG, diffB));

                bgR[x][y] = (1 - alpha) * bgR[x][y] + alpha * r;
                bgG[x][y] = (1 - alpha) * bgG[x][y] + alpha * g;
                bgB[x][y] = (1 - alpha) * bgB[x][y] + alpha * b;

                result.setRGB(x, y, (diff << 16) | (diff << 8) | diff);
            }
        }
        return result;
    }

    @Override
    public void resetState() {
        bgR = null;
        bgG = null;
        bgB = null;
    }

    @Override
    public String getName() {
        return "Background Subtraction (alpha=" + alpha + ")";
    }
}