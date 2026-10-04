package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; // Added missing import
import org.junit.jupiter.api.Test;

class TemporalOperationsTest {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;

    private ImageOperation frameDifferenceOp;
    private ImageOperation frameAveragingOp;
    private ImageOperation backgroundSubtractionOp;

    @BeforeEach
    void setUp() {
        // Use OperationFactory.create with null Context; defaults match previous behavior
        frameDifferenceOp = OperationFactory.create("frame_diff", null);
        frameAveragingOp = OperationFactory.create("frame_avg", null);
        backgroundSubtractionOp = OperationFactory.create("bg_subtraction", null);
    }

    private Frame createSolidFrame(Color color, long index) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.dispose();
        return new Frame(img, index, index / 30.0);
    }

    @Test
    @DisplayName("Frame Difference returns original frame when history is empty")
    void testFrameDifferenceEmptyHistory() {
        Frame currentFrame = createSolidFrame(Color.WHITE, 0);
        BufferedImage result = ((TemporalVideoOperation) frameDifferenceOp).processTemporal(currentFrame, new ArrayList<>());

        assertNotNull(result);
        assertEquals(Color.WHITE.getRGB(), result.getRGB(50, 50));
    }

    @Test
    @DisplayName("Frame Difference highlights pixel movement above threshold")
    void testFrameDifferenceWithMotion() {
        Frame prevFrame = createSolidFrame(Color.BLACK, 0);
        Frame currentFrame = createSolidFrame(Color.WHITE, 1);

        List<Frame> history = List.of(prevFrame);
        BufferedImage result = ((TemporalVideoOperation) frameDifferenceOp).processTemporal(currentFrame, history);

        int rgb = result.getRGB(50, 50) & 0xFFFFFF;
        assertEquals(0xFFFFFF, rgb, "Motion above threshold should output white pixel (0xFFFFFF)");
    }

    @Test
    @DisplayName("Frame Averaging correctly blends pixel values across history window")
    void testFrameAveraging() {
        Frame frame1 = createSolidFrame(new Color(240, 0, 0), 0);
        Frame frame2 = createSolidFrame(new Color(0, 0, 240), 1);

        List<Frame> history = List.of(frame1);
        BufferedImage result = ((TemporalVideoOperation) frameAveragingOp).processTemporal(frame2, history);

        int resultRGB = result.getRGB(50, 50);
        int r = (resultRGB >> 16) & 0xFF;
        int g = (resultRGB >> 8) & 0xFF;
        int b = resultRGB & 0xFF;

        assertEquals(120, r, 2, "Red channel average should be ~120");
        assertEquals(0, g, "Green channel average should be 0");
        assertEquals(120, b, 2, "Blue channel average should be ~120");
    }

    @Test
    @DisplayName("Background Subtraction initializes on first frame and resets state properly")
    void testBackgroundSubtractionResetState() {
        TemporalVideoOperation bgSub = (TemporalVideoOperation) backgroundSubtractionOp;

        Frame frame1 = createSolidFrame(Color.RED, 0);
        BufferedImage res1 = bgSub.processTemporal(frame1, new ArrayList<>());
        
        assertEquals(Color.RED.getRGB(), res1.getRGB(0, 0));

        bgSub.resetState();

        Frame frame2 = createSolidFrame(Color.BLUE, 1);
        BufferedImage res2 = bgSub.processTemporal(frame2, new ArrayList<>());
        assertEquals(Color.BLUE.getRGB(), res2.getRGB(0, 0), "After reset, first frame must re-initialize background");
    }
}