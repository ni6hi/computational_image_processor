package com.imageapp;

import java.awt.image.BufferedImage;

public record Frame(
    BufferedImage image,
    long frameIndex,
    double timestampSeconds
) {}