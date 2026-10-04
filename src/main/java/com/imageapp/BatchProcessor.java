package com.imageapp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

public class BatchProcessor {

    public interface ProgressListener {
        void onProgress(int completed, int total, String currentFile);
        void onComplete();
    }

    public static void processDirectory(File inputDir, File outputDir, ImagePipeline pipeline, String format, ProgressListener listener) {
        // Deprecated single-level batch; redirect to recursive processor
        processDirectoryRecursive(inputDir.toPath(), inputDir.toPath(), outputDir.toPath(), pipeline, format, listener);
    }

    // Recursive directory walker that preserves relative paths and filenames.
    public static void processDirectoryRecursive(java.nio.file.Path rootInput,
                                                 java.nio.file.Path currentInput,
                                                 java.nio.file.Path rootOutput,
                                                 ImagePipeline pipeline,
                                                 String format,
                                                 ProgressListener listener) {
        java.util.List<java.nio.file.Path> files = new java.util.ArrayList<>();
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(currentInput)) {
            stream.filter(p -> java.nio.file.Files.isRegularFile(p))
                  .filter(p -> p.getFileName().toString().toLowerCase().matches(".*\\.(png|jpg|jpeg|bmp|gif)"))
                  .forEach(files::add);
        } catch (IOException e) {
            if (listener != null) listener.onComplete();
            return;
        }

        if (files.isEmpty()) {
            if (listener != null) listener.onComplete();
            return;
        }

        int total = files.size();
        int[] completed = {0};

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (java.nio.file.Path inPath : files) {
            executor.submit(() -> {
                try {
                    java.nio.file.Path rel = rootInput.relativize(inPath);
                    java.nio.file.Path outPath = rootOutput.resolve(rel);
                    java.nio.file.Path outDir = outPath.getParent();
                    if (outDir != null && !java.nio.file.Files.exists(outDir)) {
                        java.nio.file.Files.createDirectories(outDir);
                    }

                    BufferedImage source = ImageIO.read(inPath.toFile());
                    if (source != null) {
                        BufferedImage processed = pipeline.execute(source);

                        String outFileName = outPath.getFileName().toString();
                        String srcExt = "";
                        int dot = outFileName.lastIndexOf('.');
                        if (dot >= 0) srcExt = outFileName.substring(dot + 1).toLowerCase();

                        if (!format.equalsIgnoreCase(srcExt)) {
                            // replace extension with requested format
                            if (dot >= 0) outFileName = outFileName.substring(0, dot) + "." + format;
                            else outFileName = outFileName + "." + format;
                        }

                        java.nio.file.Path finalOut = (outDir != null) ? outDir.resolve(outFileName) : rootOutput.resolve(outFileName);
                        ImageIO.write(processed, format, finalOut.toFile());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    synchronized (completed) {
                        completed[0]++;
                        if (listener != null) listener.onProgress(completed[0], total, inPath.getFileName().toString());
                    }
                }
            });
        }

        executor.shutdown();
        new Thread(() -> {
            while (!executor.isTerminated()) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
            if (listener != null) listener.onComplete();
        }).start();
    }
}