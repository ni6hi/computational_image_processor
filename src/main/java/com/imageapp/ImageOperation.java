package com.imageapp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public interface ImageOperation {
    BufferedImage process(BufferedImage input);
    String getName();

    /** Op key and parameters used to export this operation to pipeline JSON, or null if it cannot be exported. */
    default PipelineConfig.Entry toSpec() { return null; }

    static BufferedImage copyImage(BufferedImage input) {
        if (input == null) return null;
        BufferedImage copy = new BufferedImage(
            input.getWidth(),
            input.getHeight(),
            input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType()
        );
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(input, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}
