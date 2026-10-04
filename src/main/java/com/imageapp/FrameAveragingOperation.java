package com.imageapp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class FrameAveragingOperation implements TemporalVideoOperation {

    private final int windowSize;

    public FrameAveragingOperation(int windowSize) {
        this.windowSize = Math.max(1, windowSize);
    }

    public FrameAveragingOperation() {
        this(5);
    }

    @Override
    public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
        BufferedImage current = currentFrame.image();
        if (frameHistory == null || frameHistory.isEmpty()) {
            BufferedImage copy = new BufferedImage(current.getWidth(), current.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = copy.createGraphics();
            g.drawImage(current, 0, 0, null);
            g.dispose();
            return copy;
        }

        int width = current.getWidth();
        int height = current.getHeight();
        int start = Math.max(0, frameHistory.size() - (windowSize - 1));
        List<Frame> subHistory = frameHistory.subList(start, frameHistory.size());

        int count = subHistory.size() + 1;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rSum = (current.getRGB(x, y) >> 16) & 0xFF;
                int gSum = (current.getRGB(x, y) >> 8) & 0xFF;
                int bSum = current.getRGB(x, y) & 0xFF;

                for (Frame f : subHistory) {
                    int rgb = f.image().getRGB(x, y);
                    rSum += (rgb >> 16) & 0xFF;
                    gSum += (rgb >> 8) & 0xFF;
                    bSum += rgb & 0xFF;
                }

                int r = rSum / count;
                int g = gSum / count;
                int b = bSum / count;

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    @Override
    public void resetState() {}

    @Override
    public String getName() {
        return "Temporal Frame Averaging (window=" + windowSize + ")";
    }
}