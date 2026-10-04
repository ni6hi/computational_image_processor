package com.imageapp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Java2DFrameConverter;

public class VideoProcessorService {

    static {
        FFmpegLogCallback.set();
    }

    private final Map<String, VideoJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public VideoJob submitJob(File inputFile, ImagePipeline pipeline) {
        return submitJob(inputFile, pipeline, "mp4");
    }

    /** format: "mp4" (H.264, falls back to MPEG-4) or "webm" (VP8). */
    public VideoJob submitJob(File inputFile, ImagePipeline pipeline, String format) {
        String fmt = "webm".equalsIgnoreCase(format) ? "webm" : "mp4";
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        File outputFile = new File(System.getProperty("java.io.tmpdir"), "processed_" + jobId + "." + fmt);
        VideoJob job = new VideoJob(jobId, inputFile, outputFile);
        jobs.put(jobId, job);

        executor.submit(() -> processVideo(job, pipeline, fmt));
        return job;
    }

    public VideoJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * attempt 0: H.264 (openh264 or x264) with constrained_baseline profile
     * attempt 1: H.264 with encoder defaults
     * attempt 2: MPEG-4 Part 2 (last resort, MP4 only)
     */
    private FFmpegFrameRecorder startRecorder(File out, int w, int h, double fps, String fmt, int attempt)
            throws Exception {
        FFmpegFrameRecorder r = new FFmpegFrameRecorder(out, w, h);
        r.setFrameRate(fps);
        r.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        r.setGopSize((int) Math.max(1, Math.round(fps)));
        int bitrate = (int) Math.max(500_000L, Math.min(8_000_000L, (long) (w * (long) h * fps * 0.15)));
        r.setVideoBitrate(bitrate);

        if ("webm".equals(fmt)) {
            r.setFormat("webm");
            r.setVideoCodec(avcodec.AV_CODEC_ID_VP8);
            if (attempt == 0) {
                r.setVideoOption("deadline", "realtime");
            }
        } else {
            r.setFormat("mp4");
            r.setOption("movflags", "+faststart");
            if (attempt <= 1) {
                r.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                if (attempt == 0) {
                    r.setVideoOption("profile", "constrained_baseline");
                }
            } else {
                r.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
            }
        }

        try {
            r.start();
            System.out.println("Recorder started: " + fmt + ", attempt " + attempt);
            return r;
        } catch (Exception e) {
            System.err.println("Recorder attempt " + attempt + " failed: " + e.getMessage());
            try { r.release(); } catch (Exception ignored) {}
            throw e;
        }
    }

    private FFmpegFrameRecorder startRecorderWithFallback(File out, int w, int h, double fps, String fmt)
            throws Exception {
        int maxAttempt = "webm".equals(fmt) ? 1 : 2;
        Exception last = null;
        for (int attempt = 0; attempt <= maxAttempt; attempt++) {
            try {
                if (out.exists()) out.delete();
                return startRecorder(out, w, h, fps, fmt, attempt);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last;
    }

    private void processVideo(VideoJob job, ImagePipeline pipeline, String fmt) {
        job.setStatus(JobStatus.PROCESSING);
        pipeline.resetState();

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(job.getInputFile());
        FFmpegFrameRecorder recorder = null;
        Java2DFrameConverter inConverter = new Java2DFrameConverter();
        Java2DFrameConverter outConverter = new Java2DFrameConverter();
        boolean recorderClosed = false;

        try {
            grabber.start();

            int width = (grabber.getImageWidth() > 0 ? grabber.getImageWidth() : 640) & ~1;
            int height = (grabber.getImageHeight() > 0 ? grabber.getImageHeight() : 480) & ~1;
            double fps = grabber.getFrameRate() > 0 ? grabber.getFrameRate() : 30.0;
            int totalFrames = grabber.getLengthInFrames();

            recorder = startRecorderWithFallback(job.getOutputFile(), width, height, fps, fmt);

            org.bytedeco.javacv.Frame videoFrame;
            long frameIndex = 0;
            List<Frame> frameHistory = new ArrayList<>();

            while ((videoFrame = grabber.grabImage()) != null) {
                BufferedImage bImg = inConverter.getBufferedImage(videoFrame);
                if (bImg == null) continue;

                BufferedImage bImgCopy = cloneImage(bImg);
                Frame currentFrame = new Frame(bImgCopy, frameIndex, frameIndex / fps);

                BufferedImage processedImg = pipeline.executeFrame(currentFrame, frameHistory);
                if (processedImg == null) processedImg = bImgCopy;

                if (processedImg.getWidth() != width || processedImg.getHeight() != height
                        || processedImg.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                    BufferedImage formatted = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g = formatted.createGraphics();
                    g.drawImage(processedImg, 0, 0, width, height, null);
                    g.dispose();
                    processedImg = formatted;
                }

                recorder.record(outConverter.convert(processedImg));

                frameHistory.add(currentFrame);
                if (frameHistory.size() > 30) frameHistory.remove(0);

                frameIndex++;
                if (totalFrames > 0) {
                    job.setProgress(Math.min(99.0, (double) frameIndex / totalFrames * 100.0));
                }
            }

            // Finalize the container BEFORE reporting completion
            recorder.stop();
            recorder.release();
            recorderClosed = true;

            if (frameIndex == 0 || job.getOutputFile().length() == 0) {
                throw new IllegalStateException("No frames were written to the output video.");
            }

            job.setProgress(100.0);
            job.setStatus(JobStatus.COMPLETED);

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        } finally {
            if (recorder != null && !recorderClosed) {
                try { recorder.stop(); } catch (Exception ignored) {}
                try { recorder.release(); } catch (Exception ignored) {}
            }
            try { grabber.stop(); } catch (Exception ignored) {}
            try { grabber.release(); } catch (Exception ignored) {}
            inConverter.close();
            outConverter.close();
        }
    }

    private BufferedImage cloneImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}