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
    @DisplayName("Background Subtraction outputs an empty mask on first frame and resets state properly")
    void testBackgroundSubtractionResetState() {
        TemporalVideoOperation bgSub = (TemporalVideoOperation) backgroundSubtractionOp;

        // First frame only initializes the background model, so there is no foreground yet
        BufferedImage res1 = bgSub.processTemporal(createSolidFrame(Color.RED, 0), new ArrayList<>());
        assertEquals(Color.BLACK.getRGB(), res1.getRGB(0, 0), "First frame must produce an empty (black) mask");

        bgSub.resetState();

        // After reset, a blue frame must re-initialize the model instead of being compared against red
        BufferedImage res2 = bgSub.processTemporal(createSolidFrame(Color.BLUE, 1), new ArrayList<>());
        assertEquals(Color.BLACK.getRGB(), res2.getRGB(0, 0), "After reset, first frame must re-initialize background");

        // The model now holds blue, so an identical blue frame shows no foreground
        BufferedImage res3 = bgSub.processTemporal(createSolidFrame(Color.BLUE, 2), new ArrayList<>());
        assertEquals(Color.BLACK.getRGB(), res3.getRGB(0, 0), "Unchanged scene must show no foreground");
    }

    @Test
    @DisplayName("Background Subtraction without reset reports the change against the old background")
    void testBackgroundSubtractionDetectsChangeWithoutReset() {
        TemporalVideoOperation bgSub = (TemporalVideoOperation) backgroundSubtractionOp;

        bgSub.processTemporal(createSolidFrame(Color.RED, 0), new ArrayList<>());
        BufferedImage res = bgSub.processTemporal(createSolidFrame(Color.BLUE, 1), new ArrayList<>());

        // Red (255,0,0) vs blue (0,0,255): max channel difference is 255 -> white foreground
        assertEquals(Color.WHITE.getRGB(), res.getRGB(0, 0), "Changed scene must show full foreground");
    }
}