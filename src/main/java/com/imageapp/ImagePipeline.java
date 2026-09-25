package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImagePipeline {
    private final List<ImageOperation> operations = new ArrayList<>();

    public ImagePipeline addOperation(ImageOperation op) {
        operations.add(op);
        return this;
    }

    public void removeOperation(int index) {
        if (index >= 0 && index < operations.size()) {
            operations.remove(index);
        }
    }

    public List<ImageOperation> getOperations() {
        return operations;
    }

    public BufferedImage execute(BufferedImage input) {
        BufferedImage current = input;
        for (ImageOperation op : operations) {
            current = op.process(current);
        }
        return current;
    }

    public void clear() {
        operations.clear();
    }
}