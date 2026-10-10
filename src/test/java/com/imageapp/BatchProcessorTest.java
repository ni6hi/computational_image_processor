package com.imageapp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BatchProcessorTest {

    @TempDir
    Path tmp;

    private static void writeSolid(Path file, Color c) throws Exception {
        Files.createDirectories(file.getParent());
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) img.setRGB(x, y, c.getRGB());
        ImageIO.write(img, file.getFileName().toString().endsWith(".bmp") ? "bmp" : "png", file.toFile());
    }

    /** Runs the (asynchronous) batch and waits for onComplete. */
    private static int runBatch(Path in, Path out, ImagePipeline pipeline, String format) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        int[] progressCalls = {0};
        BatchProcessor.processDirectory(in.toFile(), out.toFile(), pipeline, format, new BatchProcessor.ProgressListener() {
            @Override public void onProgress(int completed, int total, String currentFile) { progressCalls[0]++; }
            @Override public void onComplete() { done.countDown(); }
        });
        assertTrue(done.await(30, TimeUnit.SECONDS), "Batch did not complete");
        return progressCalls[0];
    }

    @Test
    @DisplayName("Batch processes every image recursively, mirrors the folder tree and applies the pipeline")
    void processesTreeAndAppliesPipeline() throws Exception {
        Path in = tmp.resolve("in"), out = tmp.resolve("out");
        writeSolid(in.resolve("a.png"), Color.RED);
        writeSolid(in.resolve("sub/b.png"), Color.GREEN);
        writeSolid(in.resolve("sub/deeper/c.bmp"), Color.BLUE);
        Files.writeString(in.resolve("notes.txt"), "not an image");

        ImagePipeline pipeline = new ImagePipeline().addOperation(OperationFactory.createFromSpec("grayscale", Map.of()));
        int progress = runBatch(in, out, pipeline, "png");

        assertEquals(3, progress, "One progress callback per image, text file ignored");
        assertTrue(Files.exists(out.resolve("a.png")));
        assertTrue(Files.exists(out.resolve("sub/b.png")));
        // extension changes only because a different output format was requested
        assertTrue(Files.exists(out.resolve("sub/deeper/c.png")));
        assertFalse(Files.exists(out.resolve("notes.txt")));

        int rgb = ImageIO.read(out.resolve("sub/b.png").toFile()).getRGB(4, 4);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        assertTrue(r == g && g == b, "Output must be grayscale, got " + r + "," + g + "," + b);
    }

    @Test
    @DisplayName("Empty input directory completes without creating output files")
    void emptyInputCompletes() throws Exception {
        Path in = Files.createDirectories(tmp.resolve("empty")), out = tmp.resolve("out");

        int progress = runBatch(in, out, new ImagePipeline(), "png");

        assertEquals(0, progress);
        File[] written = out.toFile().listFiles();
        assertTrue(written == null || written.length == 0);
    }
}
