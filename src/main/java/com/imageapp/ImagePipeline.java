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

    // Export pipeline to a config object for JSON serialization.
    // Each operation reports its own op key and the exact params it was built with.
    public PipelineConfig toConfig() {
        PipelineConfig cfg = new PipelineConfig();
        for (ImageOperation op : operations) {
            if (op == null) continue;
            PipelineConfig.Entry spec = op.toSpec();
            if (spec == null) {
                throw new IllegalStateException("Operation '" + op.getName() + "' cannot be exported to pipeline JSON");
            }
            cfg.add(spec.op, spec.params);
        }
        return cfg;
    }

    // Load pipeline from a deserialized config (best-effort mapping).
    // Valid steps are loaded; each skipped step is reported in the returned list (empty if all loaded).
    public List<String> loadFromConfig(PipelineConfig cfg) {
        clear();
        List<String> warnings = new ArrayList<>();
        if (cfg == null || cfg.pipeline == null) return warnings;
        for (int i = 0; i < cfg.pipeline.size(); i++) {
            PipelineConfig.Entry e = cfg.pipeline.get(i);
            String step = "Step " + (i + 1);
            if (e == null || e.op == null || e.op.isBlank()) {
                warnings.add(step + ": missing \"op\" key, skipped");
                continue;
            }
            try {
                ImageOperation op = OperationFactory.createFromSpec(e.op, e.params);
                if (op != null) addOperation(op);
                else warnings.add(step + ": unknown operation '" + e.op + "', skipped");
            } catch (Exception ex) {
                warnings.add(step + ": could not create '" + e.op + "' (" + ex.getMessage() + "), skipped");
            }
        }
        return warnings;
    }
}