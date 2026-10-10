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
        for (String key : OperationKeys.ALL) {
            ImageOperation op = OperationFactory.createFromSpec(key, Map.of());
            assertNotNull(op, "Factory returned null for " + key);
            source.addOperation(op);
        }

        ImagePipeline loaded = roundTrip(source);

        PipelineConfig before = source.toConfig(), after = loaded.toConfig();
        assertEquals(OperationKeys.ALL.size(), after.pipeline.size());
        for (int i = 0; i < OperationKeys.ALL.size(); i++) {
            assertEquals(OperationKeys.ALL.get(i), after.pipeline.get(i).op);
            assertEquals(before.pipeline.get(i).params, after.pipeline.get(i).params, "Params changed for " + OperationKeys.ALL.get(i));
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
    @DisplayName("Params as the GUI sends them (ints from sliders, divided values) export cleanly")
    void guiStyleParamsExportCleanly() throws Exception {
        ImagePipeline source = new ImagePipeline()
                .addOperation(OperationFactory.createFromSpec("rotate", Map.of("angle", 90)))                // int slider value
                .addOperation(OperationFactory.createFromSpec("morphology_erosion", Map.of("size", 2 * 2 + 1))) // radius 2 in the GUI
                .addOperation(OperationFactory.createFromSpec("bilateral_filter", Map.of("sigmaSpace", 3, "sigmaColor", 50)))
                .addOperation(OperationFactory.createFromSpec("watermark", Map.of("text", "Lab", "opacity", 30 / 100.0)))
                .addOperation(OperationFactory.createFromSpec("unsharp_mask", Map.of("amount", 20 / 10.0)));

        ImagePipeline loaded = roundTrip(source);

        assertEquals(Map.of("angle", 90.0), exportedParams(loaded, 0));
        assertEquals(Map.of("size", 5), exportedParams(loaded, 1));
        assertEquals(Map.of("sigmaColor", 50.0, "sigmaSpace", 3.0), exportedParams(loaded, 2));
        // opacity is a float internally; it must export as 0.3, not 0.30000001192092896
        assertEquals(Map.of("text", "Lab", "opacity", 0.3), exportedParams(loaded, 3));
        // sigma was not given, so the default actually used is exported
        assertEquals(Map.of("amount", 2.0, "sigma", 1.5), exportedParams(loaded, 4));
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
