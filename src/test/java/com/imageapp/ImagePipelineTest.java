package com.imageapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ImagePipelineTest {

    private BufferedImage testImage;
    private ImagePipeline pipeline;

    @BeforeEach
    void setUp() {
        testImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 128, 128);
        g.setColor(Color.RED);
        g.fillRect(32, 32, 64, 64);
        g.dispose();

        pipeline = new ImagePipeline();
    }

    @Test
    @DisplayName("Verify basic operations sequence execution")
    void testBasicOperationsSequence() {
        pipeline.addOperation(new GrayscaleOperation());
        pipeline.addOperation(new ResizeOperation(64, 64));
        pipeline.addOperation(new FlipOperation(true));
        pipeline.addOperation(new InvertOperation());

        BufferedImage result = pipeline.execute(testImage);

        assertNotNull(result, "Processed image output should not be null.");
        assertEquals(64, result.getWidth(), "Output width should equal target resize width.");
        assertEquals(64, result.getHeight(), "Output height should equal target resize height.");
    }

    @Test
    @DisplayName("Verify scientific filtering and thresholding pipeline")
    void testScientificPipeline() {
        pipeline.addOperation(new GaussianBlurOperation(1.5f));
        pipeline.addOperation(new BilateralFilterOperation(50.0, 50.0));
        pipeline.addOperation(new SobelEdgeDetectionOperation());
        pipeline.addOperation(new OtsuThresholdOperation());

        BufferedImage result = pipeline.execute(testImage);

        assertNotNull(result, "Processed image output should not be null.");
        assertEquals(128, result.getWidth());
        assertEquals(128, result.getHeight());
    }

    @Test
    @DisplayName("Verify morphological open and close operations")
    void testMorphologicalOpenCloseOperations() {
        pipeline.addOperation(new MorphologicalOpenCloseOperation(MorphologicalOpenCloseOperation.Mode.OPENING, 3));
        pipeline.addOperation(new MorphologicalOpenCloseOperation(MorphologicalOpenCloseOperation.Mode.CLOSING, 3));

        BufferedImage result = pipeline.execute(testImage);

        assertNotNull(result, "Processed output from morphological open/close should not be null.");
        assertEquals(128, result.getWidth());
        assertEquals(128, result.getHeight());
    }

    @Test
    @DisplayName("Verify advanced frequency and transform operations")
    void testAdvancedPipeline() {
        pipeline.addOperation(new BoxFilterOperation(3));
        pipeline.addOperation(new WienerDeconvolutionOperation(0.01));
        pipeline.addOperation(new HaarWaveletTransformOperation());

        BufferedImage result = pipeline.execute(testImage);

        assertNotNull(result, "Processed output from advanced transforms should not be null.");
        assertEquals(128, result.getWidth());
        assertEquals(128, result.getHeight());
    }

    @Test
    @DisplayName("Verify pipeline management (add, list, clear)")
    void testPipelineManagement() {
        assertEquals(0, pipeline.getOperations().size());

        pipeline.addOperation(new GrayscaleOperation());
        pipeline.addOperation(new GlobalHistogramEqualizationOperation());
        assertEquals(2, pipeline.getOperations().size());

        pipeline.clear();
        assertEquals(0, pipeline.getOperations().size());
    }

    @Test
    @DisplayName("loadFromConfig loads valid steps and reports nothing when all steps are valid")
    void testLoadFromConfigAllValid() {
        PipelineConfig cfg = new PipelineConfig();
        cfg.add("grayscale", java.util.Map.of());
        cfg.add("sobel", java.util.Map.of());

        java.util.List<String> warnings = pipeline.loadFromConfig(cfg);

        assertTrue(warnings.isEmpty(), "No warnings expected, got: " + warnings);
        assertEquals(2, pipeline.getOperations().size());
    }

    @Test
    @DisplayName("loadFromConfig reports unknown and missing op keys instead of dropping them silently")
    void testLoadFromConfigReportsSkippedSteps() {
        PipelineConfig cfg = new PipelineConfig();
        cfg.add("grayscale", java.util.Map.of());
        cfg.add("no_such_op", java.util.Map.of());
        cfg.add(null, java.util.Map.of());
        cfg.add("invert", java.util.Map.of());

        java.util.List<String> warnings = pipeline.loadFromConfig(cfg);

        assertEquals(2, pipeline.getOperations().size(), "Valid steps around the bad ones must still load");
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).startsWith("Step 2") && warnings.get(0).contains("no_such_op"));
        assertTrue(warnings.get(1).startsWith("Step 3") && warnings.get(1).contains("missing"));
    }
}