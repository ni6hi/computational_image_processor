package com.imageapp;

import java.io.File;

public class VideoJob {

    private final String jobId;
    private final File inputFile;
    private final File outputFile;
    private JobStatus status;
    private double progress;
    private String errorMessage;

    public VideoJob(String jobId, File inputFile, File outputFile) {
        this.jobId = jobId;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.status = JobStatus.QUEUED;
        this.progress = 0.0;
        this.errorMessage = null;
    }

    public String getJobId() { return jobId; }
    public File getInputFile() { return inputFile; }
    public File getOutputFile() { return outputFile; }

    public synchronized JobStatus getStatus() { return status; }
    public synchronized void setStatus(JobStatus status) { this.status = status; }

    public synchronized double getProgress() { return progress; }
    public synchronized void setProgress(double progress) { this.progress = progress; }

    public synchronized String getErrorMessage() { return errorMessage; }
    public synchronized void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}