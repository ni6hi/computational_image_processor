package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

public class WebServer {

    private static BufferedImage currentSourceImage;
    private static File currentVideoFile;
    private static final ImagePipeline pipeline = new ImagePipeline();
    private static final VideoProcessorService videoService = new VideoProcessorService();

    public static void start(int port) {
        initDefaultCanvas();

        // auto-load pipeline config if present
        try {
            java.io.File cfgFile = new java.io.File("pipeline.json");
            if (cfgFile.exists()) {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                PipelineConfig cfg = om.readValue(cfgFile, PipelineConfig.class);
                java.util.List<String> warnings = pipeline.loadFromConfig(cfg);
                System.out.println("Loaded pipeline.json with " + pipeline.getOperations().size() + " steps.");
                for (String w : warnings) System.err.println("pipeline.json: " + w);
            }
        } catch (Exception e) {
            System.err.println("Failed to auto-load pipeline.json: " + e.getMessage());
        }

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "src/main/resources/public";
                staticFiles.location = Location.EXTERNAL;
            });
        }).start(port);

        // Upload static image (Supports both Multipart FormData and raw bytes)
        app.post("/api/upload", ctx -> {
            byte[] bytes = null;
            var file = ctx.uploadedFile("file");
            if (file != null) {
                try (InputStream is = file.content()) {
                    bytes = is.readAllBytes();
                }
            } else {
                bytes = ctx.bodyAsBytes();
            }

            if (bytes == null || bytes.length == 0) {
                ctx.status(400).result("Empty image payload.");
                return;
            }
            
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                currentSourceImage = img;
                ctx.result("Image uploaded successfully.");
            } else {
                ctx.status(400).result("Invalid image format.");
            }
        });

        // Upload video file
        app.post("/api/video/upload", ctx -> {
            var file = ctx.uploadedFile("file");
            if (file == null) {
                ctx.status(400).result("No video file uploaded.");
                return;
            }
            File tempVideo = File.createTempFile("cilab_upload_", "_" + file.filename());
            try (InputStream is = file.content();
                 OutputStream os = new FileOutputStream(tempVideo)) {
                is.transferTo(os);
            }
            currentVideoFile = tempVideo;
            ctx.result("Video uploaded successfully.");
        });

        // Live MJPEG Video Stream Preview
        app.get("/api/video/stream-preview", ctx -> {
            if (currentVideoFile == null || !currentVideoFile.exists()) {
                ctx.status(400).result("No video file uploaded.");
                return;
            }

            ctx.contentType("multipart/x-mixed-replace; boundary=--jpgboundary");
            OutputStream os = ctx.outputStream();
            pipeline.resetState();

            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(currentVideoFile);
                 Java2DFrameConverter converter = new Java2DFrameConverter()) {

                grabber.start();
                org.bytedeco.javacv.Frame videoFrame;
                long frameIndex = 0;
                double fps = grabber.getFrameRate() > 0 ? grabber.getFrameRate() : 30.0;

                while ((videoFrame = grabber.grabImage()) != null) {
                    BufferedImage bImg = converter.getBufferedImage(videoFrame);
                    if (bImg != null) {
                        Frame frame = new Frame(bImg, frameIndex, frameIndex / fps);
                        BufferedImage processed = pipeline.executeFrame(frame, List.of());

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(processed, "jpeg", baos);

                        os.write(("--jpgboundary\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());
                        os.write(baos.toByteArray());
                        os.write("\r\n".getBytes());
                        os.flush();

                        frameIndex++;
                    }
                    Thread.sleep(33); // Stream at ~30 FPS
                }
                grabber.stop();
            } catch (Exception ignored) {}
        });

        // Background export trigger: POST /api/video/process?format=mp4|webm
        app.post("/api/video/process", ctx -> {
            if (currentVideoFile == null || !currentVideoFile.exists()) {
                ctx.status(400).result("No video file loaded.");
                return;
            }
            String format = getParam(ctx, "format", "mp4");
            VideoJob job = videoService.submitJob(currentVideoFile, pipeline, format);
            ctx.json(Map.of("jobId", job.getJobId()));
        });

        // Download processed video (mp4 or webm, depending on the job)
        app.get("/api/video/download/{jobId}", ctx -> {
            VideoJob job = videoService.getJob(ctx.pathParam("jobId"));
            if (job == null || job.getStatus() != JobStatus.COMPLETED) {
                ctx.status(400).result("Job not ready or failed.");
                return;
            }
            File file = job.getOutputFile();
            if (!file.exists() || file.length() == 0) {
                ctx.status(500).result("Exported video file missing or empty.");
                return;
            }
            boolean webm = file.getName().endsWith(".webm");
            ctx.contentType(webm ? "video/webm" : "video/mp4");
            ctx.header("Content-Disposition",
                    "attachment; filename=\"processed_video." + (webm ? "webm" : "mp4") + "\"");
            ctx.result(new java.io.FileInputStream(file));
        });

        // Query background job status
        app.get("/api/video/status/{jobId}", ctx -> {
            String jobId = ctx.pathParam("jobId");
            VideoJob job = videoService.getJob(jobId);
            if (job == null) {
                ctx.status(404).result("Job not found.");
                return;
            }
            ctx.json(Map.of(
                "jobId", job.getJobId(),
                "status", job.getStatus().name(),
                "progress", job.getProgress(),
                "errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : ""
            ));
        });

        // Add operation step to execution pipeline (Handles ALL tree catalog operations)
        app.post("/api/pipeline/add", ctx -> {
            String op = ctx.queryParam("op");
            if (op == null) {
                ctx.status(400).result("Missing operation 'op' parameter.");
                return;
            }

            ImageOperation operationInstance = OperationFactory.create(op, ctx);
            if (operationInstance != null) {
                pipeline.addOperation(operationInstance);
                ctx.result("Added operation: " + operationInstance.getName());
            } else {
                ctx.status(400).result("Unknown or unsupported operation: " + op);
            }
        });

        // Remove operation step by index
        app.post("/api/pipeline/remove", ctx -> {
            int index = Integer.parseInt(getParam(ctx, "index", "0"));
            if (index >= 0 && index < pipeline.getOperations().size()) {
                pipeline.removeOperation(index);
                ctx.result("Removed step at index " + index);
            } else {
                ctx.status(400).result("Invalid index.");
            }
        });

        // Clear all pipeline steps
        app.post("/api/pipeline/clear", ctx -> {
            pipeline.clear();
            ctx.result("Pipeline cleared.");
        });

        // List active pipeline operations
        app.get("/api/pipeline/list", ctx -> {
            List<String> names = new ArrayList<>();
            for (ImageOperation op : pipeline.getOperations()) {
                names.add(op.getName());
            }
            ctx.json(names);
        });

        // Export pipeline as JSON
        app.get("/api/pipeline/export", ctx -> {
            try {
                ctx.json(pipeline.toConfig());
            } catch (IllegalStateException e) {
                ctx.status(500).result("Export failed: " + e.getMessage());
            }
        });

        // Import pipeline from JSON body
        app.post("/api/pipeline/import", ctx -> {
            try {
                PipelineConfig cfg = ctx.bodyAsClass(PipelineConfig.class);
                java.util.List<String> warnings = pipeline.loadFromConfig(cfg);
                String msg = "Pipeline imported. Steps: " + pipeline.getOperations().size();
                if (!warnings.isEmpty()) msg += "\nSkipped:\n" + String.join("\n", warnings);
                ctx.result(msg);
            } catch (Exception e) {
                ctx.status(400).result("Invalid pipeline JSON: " + e.getMessage());
            }
        });

        // Static image preview
        app.get("/api/preview", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            if (currentSourceImage == null) {
                ctx.status(400).result("No active image loaded.");
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
        g.dispose();
    }

    public static void main(String[] args) {
        start(7070);
    }
}