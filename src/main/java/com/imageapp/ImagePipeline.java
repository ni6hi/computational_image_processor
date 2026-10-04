package com.imageapp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Export pipeline to a config object for JSON serialization
    public PipelineConfig toConfig() {
        PipelineConfig cfg = new PipelineConfig();
        for (ImageOperation op : operations) {
            if (op == null) continue;
            String name = op.getName();
            // Best-effort mapping from display name back to op key + params
            // Many getName() strings contain readable params; parse common ones.
            if (name == null) continue;

            name = name.trim();

            if (name.startsWith("Grayscale")) cfg.add("grayscale", Map.of());
            else if (name.startsWith("Invert")) cfg.add("invert", Map.of());
            else if (name.startsWith("Resize")) {
                // Resize (WxH)
                try {
                    int s = name.indexOf('(');
                    int x = name.indexOf('x', s);
                    int end = name.indexOf(')', x);
                    String w = name.substring(s + 1, x).trim();
                    String h = name.substring(x + 1, end).trim();
                    cfg.add("resize", Map.of("width", Integer.parseInt(w), "height", Integer.parseInt(h)));
                } catch (Exception e) { cfg.add("resize", Map.of()); }
            }
            else if (name.startsWith("Rotate")) {
                try {
                    int s = name.indexOf('(');
                    int end = name.indexOf('\u00B0', s);
                    String v = name.substring(s + 1, end).replace("°", "").trim();
                    cfg.add("rotate", Map.of("angle", Double.parseDouble(v)));
                } catch (Exception e) { cfg.add("rotate", Map.of()); }
            }
            else if (name.startsWith("Flip")) {
                cfg.add("flip", Map.of("horizontal", name.contains("Horizontal") || name.contains("H")));
            }
            else if (name.startsWith("Crop")) {
                try {
                    int s = name.indexOf('(');
                    int x = name.indexOf('x', s);
                    int end = name.indexOf(')', x);
                    String w = name.substring(s + 1, x).trim();
                    String h = name.substring(x + 1, end).trim();
                    cfg.add("crop", Map.of("width", Integer.parseInt(w), "height", Integer.parseInt(h)));
                } catch (Exception e) { cfg.add("crop", Map.of()); }
            }
            else if (name.startsWith("Watermark")) cfg.add("watermark", Map.of());
            else if (name.toLowerCase().contains("gaussian")) cfg.add("gaussian_blur", Map.of());
            else if (name.toLowerCase().contains("median")) cfg.add("median_filter", Map.of());
            else if (name.toLowerCase().contains("bilateral")) cfg.add("bilateral_filter", Map.of());
            else if (name.toLowerCase().contains("global")) cfg.add("global_hist_eq", Map.of());
            else if (name.toLowerCase().contains("clahe")) cfg.add("clahe", Map.of());
            else if (name.toLowerCase().contains("unsharp")) cfg.add("unsharp_mask", Map.of());
            else if (name.toLowerCase().contains("sobel")) cfg.add("sobel", Map.of());
            else if (name.toLowerCase().contains("otsu")) cfg.add("otsu", Map.of());
            else if (name.toLowerCase().contains("sauvola")) cfg.add("sauvola", Map.of());
            else if (name.toLowerCase().contains("morph")) cfg.add("morphology_dilation", Map.of());
            else if (name.toLowerCase().contains("spc")) cfg.add("spc", Map.of());
            else if (name.toLowerCase().contains("frame difference" )|| name.toLowerCase().contains("frame diff")) cfg.add("frame_diff", Map.of());
            else if (name.toLowerCase().contains("averag")) cfg.add("frame_avg", Map.of());
            else if (name.toLowerCase().contains("background")) cfg.add("bg_subtraction", Map.of());
            else cfg.add(name, Map.of());
        }
        return cfg;
    }

    // Load pipeline from a deserialized config (best-effort mapping)
    public void loadFromConfig(PipelineConfig cfg) {
        clear();
        if (cfg == null || cfg.pipeline == null) return;
        for (PipelineConfig.Entry e : cfg.pipeline) {
            try {
                ImageOperation op = OperationFactory.createFromSpec(e.op, e.params);
                if (op != null) addOperation(op);
            } catch (Exception ignored) {}
        }
    }
}