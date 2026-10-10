package com.imageapp;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BatchRunnerMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java com.imageapp.BatchRunnerMain <pipeline.json> <inputDir> <outputDir> [format]");
            System.exit(2);
        }

        File cfgFile = new File(args[0]);
        File input = new File(args[1]);
        File output = new File(args[2]);
        String format = (args.length >= 4) ? args[3] : "png";

        if (!cfgFile.exists()) {
            System.err.println("Pipeline JSON not found: " + cfgFile.getAbsolutePath());
            System.exit(3);
        }
        if (!input.exists() || !input.isDirectory()) {
            System.err.println("Input directory not found: " + input.getAbsolutePath());
            System.exit(4);
        }
        if (!output.exists()) {
            output.mkdirs();
        }

        ObjectMapper mapper = new ObjectMapper();
        PipelineConfig cfg = mapper.readValue(cfgFile, PipelineConfig.class);

        ImagePipeline pipeline = new ImagePipeline();
        java.util.List<String> warnings = pipeline.loadFromConfig(cfg);
        if (!warnings.isEmpty()) {
            // A partially loaded pipeline would silently write wrong output for every file, so refuse to run
            System.err.println("Pipeline JSON has invalid steps:");
            for (String w : warnings) System.err.println(" - " + w);
            System.exit(5);
        }

        System.out.println("Starting batch processing:");
        System.out.println(" - pipeline: " + cfgFile.getAbsolutePath());
        System.out.println(" - input: " + input.getAbsolutePath());
        System.out.println(" - output: " + output.getAbsolutePath());
        System.out.println(" - format: " + format);

        BatchProcessor.processDirectory(input, output, pipeline, format, new BatchProcessor.ProgressListener() {
            @Override
            public void onProgress(int completed, int total, String currentFile) {
                System.out.printf("Processed %d/%d — %s\n", completed, total, currentFile);
            }

            @Override
            public void onComplete() {
                System.out.println("Batch complete.");
            }
        });
    }
}
