package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperationFactoryTest {

    // Geometry ops change the output size, all others must preserve it
    private static final List<String> GEOMETRY_KEYS = List.of("resize", "rotate", "crop");

    /** Non-square test image so a swapped width/height shows up. */
    private static BufferedImage sampleImage() {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 40, 30);
        g.setColor(Color.ORANGE);
        g.fillOval(10, 5, 20, 20);
        g.dispose();
        return img;
    }

    @Test
    @DisplayName("Every operator processes an image with default params and keeps its size (except geometry ops)")
    void everyOperatorRunsWithDefaults() {
        for (String key : OperationKeys.ALL) {
            ImageOperation op = OperationFactory.createFromSpec(key, Map.of());
            BufferedImage out = op.process(sampleImage());

            assertNotNull(out, key + " returned null");
            if (!GEOMETRY_KEYS.contains(key)) {
                assertEquals(40, out.getWidth(), key + " changed the width");
                assertEquals(30, out.getHeight(), key + " changed the height");
            }
        }
    }

    @Test
    @DisplayName("Keys are case- and whitespace-insensitive; unknown keys return null")
    void keyNormalisation() {
        assertNotNull(OperationFactory.createFromSpec("  GrayScale ", Map.of()));
        assertNull(OperationFactory.createFromSpec("no_such_op", Map.of()));
        assertNull(OperationFactory.createFromSpec(null, Map.of()));
    }

    @Test
    @DisplayName("Invert of a known colour gives its exact complement")
    void invertKnownAnswer() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, new Color(10, 20, 30).getRGB());

        BufferedImage out = OperationFactory.createFromSpec("invert", Map.of()).process(img);

        assertEquals(new Color(245, 235, 225).getRGB(), out.getRGB(0, 0) | 0xFF000000);
    }
}
