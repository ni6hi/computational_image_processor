package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class PipelineExportTest {

    private static final List<String> ALL_KEYS = List.of(
            "grayscale", "invert", "sobel", "global_hist_eq", "clahe", "unsharp_mask", "gaussian_blur",
            "median_filter", "bilateral_filter", "otsu", "sauvola", "resize", "rotate", "flip", "crop",
            "watermark", "morphology_dilation", "morphology_erosion", "morph_open", "morph_close", "top_hat",
            "box_filter", "fft_spectrum", "retinex", "reinhard_tone", "demosaic_mhc", "wiener_deconv",
            "vignetting", "haar_wavelet", "spc", "frame_diff", "frame_avg", "bg_subtraction");

    private final ObjectMapper mapper = new ObjectMapper();

    /** Export to JSON text and import it into a fresh pipeline, failing if any step is skipped. */
    private ImagePipeline roundTrip(ImagePipeline source) throws Exception {
        String json = mapper.writeValueAsString(source.toConfig());
        ImagePipeline loaded = new ImagePipeline();
        List<String> warnings = loaded.loadFromConfig(mapper.readValue(json, PipelineConfig.class));
        assertTrue(warnings.isEmpty(), "Round trip skipped steps: " + warnings);
        return loaded;
    }

    private static Map<String, Object> exportedParams(ImagePipeline p, int index) {
        return p.toConfig().pipeline.get(index).params;
    }

    @Test
    @DisplayName("Every factory operation survives export -> JSON -> import with identical key and params")
    void allFactoryOperationsRoundTrip() throws Exception {
        ImagePipeline source = new ImagePipeline();
        for (String key : ALL_KEYS) {
            ImageOperation op = OperationFactory.createFromSpec(key, Map.of());
            assertNotNull(op, "Factory returned null for " + key);
            source.addOperation(op);
        }

        ImagePipeline loaded = roundTrip(source);

        PipelineConfig before = source.toConfig(), after = loaded.toConfig();
        assertEquals(ALL_KEYS.size(), after.pipeline.size());
        for (int i = 0; i < ALL_KEYS.size(); i++) {
            assertEquals(ALL_KEYS.get(i), after.pipeline.get(i).op);
            assertEquals(before.pipeline.get(i).params, after.pipeline.get(i).params, "Params changed for " + ALL_KEYS.get(i));
        }
    }

    @Test
    @DisplayName("Non-default factory params are exported (previously lost, e.g. sigma)")
    void nonDefaultFactoryParamsArePreserved() throws Exception {
        ImagePipeline source = new ImagePipeline()
                .addOperation(OperationFactory.createFromSpec("gaussian_blur", Map.of("sigma", 3.5)))
                .addOperation(OperationFactory.createFromSpec("clahe", Map.of("tileSize", 16, "clipLimit", 4.0)))
                .addOperation(OperationFactory.createFromSpec("rotate", Map.of("angle", "45"))); // web sends strings

        ImagePipeline loaded = roundTrip(source);

        assertEquals(Map.of("sigma", 3.5), exportedParams(loaded, 0));
        assertEquals(Map.of("tileSize", 16, "clipLimit", 4.0), exportedParams(loaded, 1));
        assertEquals(Map.of("angle", 45.0), exportedParams(loaded, 2));
    }

    @Test
    @DisplayName("GUI-built operations export to the equivalent factory key and params")
    void guiOperationsExportEquivalentSpec() throws Exception {
        ImagePipeline source = new ImagePipeline()
                .addOperation(new GaussianBlurOperation(3.5f))
                .addOperation(new MorphologyOperation(MorphologyOperation.Type.EROSION, 2))
                .addOperation(new BilateralFilterOperation(3, 50))
                .addOperation(new WatermarkOperation("Lab", 0.3f))
                .addOperation(new UnsharpMaskOperation(2.0f))
                .addOperation(new FlipOperation(false));

        ImagePipeline loaded = roundTrip(source);

        assertEquals("gaussian_blur", loaded.toConfig().pipeline.get(0).op);
        assertEquals(Map.of("sigma", 3.5), exportedParams(loaded, 0));
        // radius 2 -> 5x5 window
        assertEquals("morphology_erosion", loaded.toConfig().pipeline.get(1).op);
        assertEquals(Map.of("size", 5), exportedParams(loaded, 1));
        assertEquals(Map.of("sigmaSpace", 3.0, "sigmaColor", 50.0), exportedParams(loaded, 2));
        // 0.3f must export as 0.3, not 0.30000001192092896
        assertEquals(Map.of("text", "Lab", "opacity", 0.3), exportedParams(loaded, 3));
        assertEquals(Map.of("amount", 2.0, "sigma", 1.5), exportedParams(loaded, 4));
        assertEquals(Map.of("horizontal", false), exportedParams(loaded, 5));
    }

    @Test
    @DisplayName("Factory wrapping keeps temporal operations temporal")
    void temporalOperationsStayTemporal() {
        for (String key : List.of("frame_diff", "frame_avg", "bg_subtraction")) {
            assertTrue(OperationFactory.createFromSpec(key, Map.of()) instanceof TemporalVideoOperation, key);
        }
    }

    @Test
    @DisplayName("Exporting an operation with no spec fails loudly instead of writing a bogus entry")
    void exportWithoutSpecThrows() {
        ImagePipeline p = new ImagePipeline().addOperation(new ImageOperation() {
            @Override public BufferedImage process(BufferedImage in) { return in; }
            @Override public String getName() { return "Custom"; }
        });

        IllegalStateException e = assertThrows(IllegalStateException.class, p::toConfig);
        assertTrue(e.getMessage().contains("Custom"));
    }
}
