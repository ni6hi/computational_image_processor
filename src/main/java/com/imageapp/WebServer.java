package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.http.Context;

public class WebServer {
    private static BufferedImage currentSourceImage;
    private static final ImagePipeline pipeline = new ImagePipeline();

    public static void start(int port) {
        initDefaultCanvas();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.jsonMapper(new JavalinJackson());
            config.http.maxRequestSize = 50L * 1024L * 1024L; // 50 MB
            config.jetty.multipartConfig.maxTotalRequestSize(50L * 1024L * 1024L, io.javalin.config.SizeUnit.BYTES);
            config.jetty.multipartConfig.maxFileSize(50L * 1024L * 1024L, io.javalin.config.SizeUnit.BYTES);
        }).start(port);

        // Upload new image buffer
        app.post("/api/upload", ctx -> {
            byte[] bytes = ctx.bodyAsBytes();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                currentSourceImage = img;
                ctx.result("Image uploaded successfully. Dimensions: " + img.getWidth() + "x" + img.getHeight());
            } else {
                ctx.status(400).result("Invalid image payload.");
            }
        });

        // Add operation step to active pipeline sequence
        app.post("/api/pipeline/add", ctx -> {
            String op = ctx.queryParam("op");
            if (op == null) {
                ctx.status(400).result("Missing 'op' parameter");
                return;
            }

            switch (op.toLowerCase()) {
                // Basic Operations
                case "grayscale" -> pipeline.addOperation(new GrayscaleOperation());
                case "resize" -> {
                    int w = Integer.parseInt(getParam(ctx, "width", "256"));
                    int h = Integer.parseInt(getParam(ctx, "height", "256"));
                    pipeline.addOperation(new ResizeOperation(w, h));
                }
                case "rotate" -> {
                    double angle = Double.parseDouble(getParam(ctx, "angle", "90.0"));
                    pipeline.addOperation(new RotateOperation(angle));
                }
                case "flip" -> {
                    boolean horizontal = Boolean.parseBoolean(getParam(ctx, "horizontal", "true"));
                    pipeline.addOperation(new FlipOperation(horizontal));
                }
                case "crop" -> {
                    int x = Integer.parseInt(getParam(ctx, "x", "0"));
                    int y = Integer.parseInt(getParam(ctx, "y", "0"));
                    int w = Integer.parseInt(getParam(ctx, "width", "256"));
                    int h = Integer.parseInt(getParam(ctx, "height", "256"));
                    pipeline.addOperation(new CropOperation(x, y, w, h));
                }
                case "invert" -> pipeline.addOperation(new InvertOperation());
                case "watermark" -> {
                    String text = getParam(ctx, "text", "CILab");
                    float opacity = Float.parseFloat(getParam(ctx, "opacity", "0.5"));
                    pipeline.addOperation(new WatermarkOperation(text, opacity));
                }

                // Scientific / Lab Operations
                case "gaussian_blur" -> {
                    float sigma = Float.parseFloat(getParam(ctx, "sigma", "2.0"));
                    pipeline.addOperation(new GaussianBlurOperation(sigma));
                }
                case "median_filter" -> {
                    int radius = Integer.parseInt(getParam(ctx, "radius", "3"));
                    pipeline.addOperation(new MedianFilterOperation(radius));
                }
                case "bilateral_filter" -> {
                    double sigmaColor = Double.parseDouble(getParam(ctx, "sigmaColor", "75.0"));
                    double sigmaSpace = Double.parseDouble(getParam(ctx, "sigmaSpace", "75.0"));
                    pipeline.addOperation(new BilateralFilterOperation(sigmaColor, sigmaSpace));
                }
                case "global_hist_eq" -> pipeline.addOperation(new GlobalHistogramEqualizationOperation());
                case "clahe" -> {
                    int tileSize = Integer.parseInt(getParam(ctx, "tileSize", "8"));
                    float clipLimit = Float.parseFloat(getParam(ctx, "clipLimit", "2.0"));
                    pipeline.addOperation(new CLAHEOperation(tileSize, clipLimit));
                }
                case "unsharp_mask" -> {
                    float amount = Float.parseFloat(getParam(ctx, "amount", "1.5"));
                    pipeline.addOperation(new UnsharpMaskOperation(amount));
                }
                case "sobel" -> pipeline.addOperation(new SobelEdgeDetectionOperation());
                case "otsu" -> pipeline.addOperation(new OtsuThresholdOperation());
                case "sauvola" -> {
                    int window = Integer.parseInt(getParam(ctx, "window", "15"));
                    double k = Double.parseDouble(getParam(ctx, "k", "0.2"));
                    pipeline.addOperation(new SauvolaThresholdOperation(window, k));
                }
                case "morphology_dilation" -> {
                    int size = Integer.parseInt(getParam(ctx, "size", "3"));
                    pipeline.addOperation(new MorphologyOperation(MorphologyOperation.Type.DILATION, size));
                }
                case "morphology_erosion" -> {
                    int size = Integer.parseInt(getParam(ctx, "size", "3"));
                    pipeline.addOperation(new MorphologyOperation(MorphologyOperation.Type.EROSION, size));
                }

                // Advanced Operations
                case "box_filter" -> {
                    int size = Integer.parseInt(getParam(ctx, "size", "3"));
                    pipeline.addOperation(new BoxFilterOperation(size));
                }
                case "fft_spectrum" -> pipeline.addOperation(new FFTSpectrumOperation());
                case "retinex" -> {
                    float sigma = Float.parseFloat(getParam(ctx, "sigma", "15.0"));
                    pipeline.addOperation(new SingleScaleRetinexOperation(sigma));
                }
                case "reinhard_tone" -> {
                    float key = Float.parseFloat(getParam(ctx, "key", "0.18"));
                    pipeline.addOperation(new ReinhardToneMapOperation(key));
                }
                case "demosaic_mhc" -> pipeline.addOperation(new DemosaicMHCOperation());
                case "wiener_deconv" -> {
                    double noise = Double.parseDouble(getParam(ctx, "noise", "0.01"));
                    pipeline.addOperation(new WienerDeconvolutionOperation(noise));
                }
                case "morph_open" -> {
                    int size = Integer.parseInt(getParam(ctx, "size", "3"));
                    pipeline.addOperation(new MorphologicalOpenCloseOperation(MorphologicalOpenCloseOperation.Mode.OPENING, size));
                }
                case "morph_close" -> {
                    int size = Integer.parseInt(getParam(ctx, "size", "3"));
                    pipeline.addOperation(new MorphologicalOpenCloseOperation(MorphologicalOpenCloseOperation.Mode.CLOSING, size));
                }
                case "top_hat" -> {
                    int size = Integer.parseInt(getParam(ctx, "size", "3"));
                    pipeline.addOperation(new TopHatTransformOperation(size));
                }
                case "vignetting" -> {
                    double alpha = Double.parseDouble(getParam(ctx, "alpha", "0.5"));
                    pipeline.addOperation(new VignettingCorrectionOperation(alpha));
                }
                case "haar_wavelet" -> pipeline.addOperation(new HaarWaveletTransformOperation());

                default -> {
                    ctx.status(400).result("Unknown operation: " + op);
                    return;
                }
            }
            ctx.result("Added operation: " + op);
        });

        // Remove a step by index
        app.post("/api/pipeline/remove", ctx -> {
            int index = Integer.parseInt(getParam(ctx, "index", "0"));
            if (index >= 0 && index < pipeline.getOperations().size()) {
                pipeline.getOperations().remove(index);
                ctx.result("Removed step at index " + index);
            } else {
                ctx.status(400).result("Invalid step index.");
            }
        });

        // Clear active pipeline
        app.post("/api/pipeline/clear", ctx -> {
            pipeline.clear();
            ctx.result("Pipeline cleared.");
        });

        // List active pipeline steps
        app.get("/api/pipeline/list", ctx -> {
            List<String> names = new ArrayList<>();
            for (ImageOperation op : pipeline.getOperations()) {
                names.add(op.getName());
            }
            ctx.json(names);
        });

        // Stream current pipeline PNG result
        app.get("/api/preview", ctx -> {
            if (currentSourceImage == null) {
                ctx.status(400).result("No source image loaded.");
                return;
            }
            BufferedImage result = pipeline.execute(currentSourceImage);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(result, "png", baos);
            ctx.contentType("image/png").result(baos.toByteArray());
        });
    }

    private static String getParam(Context ctx, String name, String defaultValue) {
        String value = ctx.queryParam(name);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    private static void initDefaultCanvas() {
        currentSourceImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = currentSourceImage.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, 512, 512);
        g.setColor(Color.CYAN);
        g.fillOval(128, 128, 256, 256);
        g.setColor(Color.BLACK);
        g.drawString("CILab Scientific Core Buffer", 170, 260);
        g.dispose();
    }

    public static void main(String[] args) {
        int port = 7070;
        System.out.println("Starting CILab Image Processor server on http://localhost:" + port);
        start(port);
    }
}