package com.imageapp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue; // Added missing import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoProcessorServiceTest {

    @TempDir
    Path tempDir;

    private File sampleInputVideo;
    private VideoProcessorService videoProcessorService;

    @BeforeEach
    void setUp() throws Exception {
        videoProcessorService = new VideoProcessorService();
        sampleInputVideo = tempDir.resolve("sample_input.mp4").toFile();

        generateSyntheticVideo(sampleInputVideo, 10, 320, 240, 30.0);
    }

    @Test
    @DisplayName("Submit video job completes processing and produces playable output file")
    void testSubmitJobSuccess() throws InterruptedException {
        ImagePipeline pipeline = new ImagePipeline();
        pipeline.addOperation(OperationFactory.create("grayscale", null));

        VideoJob job = videoProcessorService.submitJob(sampleInputVideo, pipeline);
        assertNotNull(job);
        assertNotNull(job.getJobId()); // Fixed: changed getId() to getJobId()

        boolean completed = awaitJobCompletion(job, 10, TimeUnit.SECONDS);

        assertTrue(completed, "Job execution timed out");
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals(100.0, job.getProgress(), 0.01);
        
        File outputFile = job.getOutputFile();
        assertTrue(outputFile.exists(), "Processed output video file should exist");
        assertTrue(outputFile.length() > 0, "Processed output video file should not be zero bytes");
    }

    @Test
    @DisplayName("Video processor correctly handles temporal pipeline operations without failing")
    void testTemporalPipelineProcessing() throws InterruptedException {
        ImagePipeline pipeline = new ImagePipeline();
        pipeline.addOperation(OperationFactory.create("frame_diff", null));

        VideoJob job = videoProcessorService.submitJob(sampleInputVideo, pipeline);

        boolean completed = awaitJobCompletion(job, 10, TimeUnit.SECONDS);

        assertTrue(completed, "Temporal job execution timed out");
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertNull(job.getErrorMessage(), "Job should complete without exceptions");
    }

    private boolean awaitJobCompletion(VideoJob job, long timeout, TimeUnit unit) throws InterruptedException {
        long timeoutMillis = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private void generateSyntheticVideo(File outputFile, int totalFrames, int width, int height, double fps) throws Exception {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height);
        recorder.setFormat("mp4");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(fps);
        recorder.start();

        Java2DFrameConverter converter = new Java2DFrameConverter();

        for (int i = 0; i < totalFrames; i++) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = img.createGraphics();
            g.setColor((i % 2 == 0) ? Color.RED : Color.BLUE);
            g.fillRect(0, 0, width, height);
            g.dispose();

            recorder.record(converter.convert(img));
        }

        recorder.stop();
        recorder.release();
        converter.close();
    }
}