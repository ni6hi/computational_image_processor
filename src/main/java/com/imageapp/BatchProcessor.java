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
        File[] files = inputDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg|bmp|gif)"));
        if (files == null || files.length == 0) {
            if (listener != null) listener.onComplete();
            return;
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int total = files.length;
        int[] completed = {0};

        for (File file : files) {
            executor.submit(() -> {
                try {
                    BufferedImage source = ImageIO.read(file);
                    if (source != null) {
                        BufferedImage processed = pipeline.execute(source);
                        String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                        String outName = baseName + "_processed." + format;
                        ImageIO.write(processed, format, new File(outputDir, outName));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } synchronized (completed) {
                    completed[0]++;
                    if (listener != null) {
                        listener.onProgress(completed[0], total, file.getName());
                    }
                }
            });
        }

        executor.shutdown();
        new Thread(() -> {
            while (!executor.isTerminated()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
            if (listener != null) {
                listener.onComplete();
            }
        }).start();
    }
}