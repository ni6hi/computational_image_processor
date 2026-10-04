package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.List;

public interface TemporalVideoOperation extends ImageOperation {

    BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory);

    void resetState();

    @Override
    default BufferedImage process(BufferedImage input) {
        return processTemporal(new Frame(input, 0, 0.0), List.of());
    }
}