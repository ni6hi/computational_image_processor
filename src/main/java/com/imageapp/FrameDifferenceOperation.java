package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.List;

public class FrameDifferenceOperation implements TemporalVideoOperation {

    private BufferedImage previousFrame = null;
    private final int threshold;

    public FrameDifferenceOperation(int threshold) {
        this.threshold = threshold;
    }

    public FrameDifferenceOperation() {
        this(30);
    }

    @Override
    public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
        BufferedImage currentImg = currentFrame.image();
        int width = currentImg.getWidth();
        int height = currentImg.getHeight();

        if (previousFrame == null || previousFrame.getWidth() != width || previousFrame.getHeight() != height) {
            previousFrame = copyImage(currentImg);
            BufferedImage initialized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    initialized.setRGB(x, y, currentImg.getRGB(x, y));
                }
            }
            return initialized;
        }

        BufferedImage diffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int currRgb = currentImg.getRGB(x, y);
                int prevRgb = previousFrame.getRGB(x, y);

                int rDiff = Math.abs(((currRgb >> 16) & 0xFF) - ((prevRgb >> 16) & 0xFF));
                int gDiff = Math.abs(((currRgb >> 8) & 0xFF) - ((prevRgb >> 8) & 0xFF));
                int bDiff = Math.abs((currRgb & 0xFF) - (prevRgb & 0xFF));

                int maxDiff = Math.max(rDiff, Math.max(gDiff, bDiff));
                int val = (maxDiff >= threshold) ? 255 : 0;

                diffImg.setRGB(x, y, (val << 16) | (val << 8) | val);
            }
        }

        previousFrame = copyImage(currentImg);
        return diffImg;
    }

    @Override
    public void resetState() {
        previousFrame = null;
    }

    @Override
    public String getName() {
        return "Frame Difference (threshold=" + threshold + ")";
    }

    private BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        copy.getGraphics().drawImage(src, 0, 0, null);
        return copy;
    }
}