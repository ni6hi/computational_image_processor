package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImagePipeline {

    private final List<ImageOperation> operations = new ArrayList<>();

    // Returns 'this' to allow method chaining in Test classes
    public ImagePipeline addOperation(ImageOperation op) {
        operations.add(op);
        return this;
    }

    // Added to resolve ImageProcessorGUI error
    public void removeOperation(int index) {
        if (index >= 0 && index < operations.size()) {
            operations.remove(index);
        }
    }

    public ImagePipeline clear() {
        operations.clear();
        return this;
    }

    public List<ImageOperation> getOperations() {
        return operations;
    }

    public void resetState() {
        for (ImageOperation op : operations) {
            if (op instanceof TemporalVideoOperation tOp) {
                tOp.resetState();
            }
        }
    }

    public BufferedImage execute(BufferedImage input) {
        if (input == null) return null;
        BufferedImage current = ImageOperation.copyImage(input);
        for (ImageOperation op : operations) {
            current = op.process(current);
        }
        return current;
    }

    public BufferedImage executeFrame(Frame frame, List<Frame> history) {
        if (frame == null || frame.image() == null) return null;
        BufferedImage currentImg = ImageOperation.copyImage(frame.image());

        for (ImageOperation op : operations) {
            if (op == null) continue;
            if (op instanceof TemporalVideoOperation tOp) {
                currentImg = tOp.processTemporal(new Frame(currentImg, frame.frameIndex(), frame.timestampSeconds()), history);
            } else {
                currentImg = op.process(currentImg);
            }
        }
        return currentImg;
    }
}