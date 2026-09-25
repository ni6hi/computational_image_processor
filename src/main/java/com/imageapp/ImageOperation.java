package com.imageapp;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;

public interface ImageOperation {
    BufferedImage process(BufferedImage input);
    String getName();
}

// ==========================================
// RETAINED BASE OPERATIONS
// ==========================================

class GrayscaleOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        BufferedImage gray = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = gray.createGraphics();
        g2d.drawImage(input, 0, 0, null);
        g2d.dispose();
        return gray;
    }
    @Override public String getName() { return "Grayscale"; }
}

class ResizeOperation implements ImageOperation {
    private final int width, height;
    public ResizeOperation(int width, int height) {
        this.width = width;
        this.height = height;
    }
    @Override
    public BufferedImage process(BufferedImage input) {
        BufferedImage resized = new BufferedImage(width, height, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(input, 0, 0, width, height, null);
        g2d.dispose();
        return resized;
    }
    @Override public String getName() { return "Resize (" + width + "x" + height + ")"; }
}

class RotateOperation implements ImageOperation {
    private final double degrees;
    public RotateOperation(double degrees) { this.degrees = degrees; }
    @Override
    public BufferedImage process(BufferedImage input) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newW = (int) Math.floor(input.getWidth() * cos + input.getHeight() * sin);
        int newH = (int) Math.floor(input.getHeight() * cos + input.getWidth() * sin);

        BufferedImage rotated = new BufferedImage(newW, newH, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform at = new AffineTransform();
        at.translate((newW - input.getWidth()) / 2.0, (newH - input.getHeight()) / 2.0);
        at.rotate(radians, input.getWidth() / 2.0, input.getHeight() / 2.0);
        g2d.drawRenderedImage(input, at);
        g2d.dispose();
        return rotated;
    }
    @Override public String getName() { return "Rotate (" + (int) degrees + "°)"; }
}

class FlipOperation implements ImageOperation {
    private final boolean horizontal;
    public FlipOperation(boolean horizontal) { this.horizontal = horizontal; }
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        Graphics2D g2d = flipped.createGraphics();
        if (horizontal) g2d.drawImage(input, w, 0, -w, h, null);
        else g2d.drawImage(input, 0, h, w, -h, null);
        g2d.dispose();
        return flipped;
    }
    @Override public String getName() { return "Flip (" + (horizontal ? "Horizontal" : "Vertical") + ")"; }
}

class CropOperation implements ImageOperation {
    private final int x, y, width, height;
    public CropOperation(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    @Override
    public BufferedImage process(BufferedImage input) {
        int safeX = Math.max(0, Math.min(x, input.getWidth() - 1));
        int safeY = Math.max(0, Math.min(y, input.getHeight() - 1));
        int safeW = Math.min(width, input.getWidth() - safeX);
        int safeH = Math.min(height, input.getHeight() - safeY);
        return input.getSubimage(safeX, safeY, safeW, safeH);
    }
    @Override public String getName() { return "Crop (" + width + "x" + height + ")"; }
}

class InvertOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = 255 - ((rgb >> 16) & 0xFF);
                int g = 255 - ((rgb >> 8) & 0xFF);
                int b = 255 - (rgb & 0xFF);
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }
    @Override public String getName() { return "Invert Colors"; }
}

class WatermarkOperation implements ImageOperation {
    private final String text;
    private final float opacity;
    public WatermarkOperation(String text, float opacity) { this.text = text; this.opacity = opacity; }
    @Override
    public BufferedImage process(BufferedImage input) {
        BufferedImage watermarked = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarked.createGraphics();
        g2d.drawImage(input, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(16, input.getWidth() / 20)));
        int x = input.getWidth() - (text.length() * 12) - 20;
        int y = input.getHeight() - 30;
        g2d.drawString(text, Math.max(10, x), y);
        g2d.dispose();
        return watermarked;
    }
    @Override public String getName() { return "Watermark (" + text + ")"; }
}

// ==========================================
// 10 SELECTED SCIENTIFIC LAB OPERATIONS
// ==========================================

// 1. Gaussian Filter
class GaussianBlurOperation implements ImageOperation {
    private final float sigma;
    public GaussianBlurOperation(float sigma) { this.sigma = sigma; }
    @Override
    public BufferedImage process(BufferedImage input) {
        int radius = (int) Math.ceil(sigma * 3);
        int size = radius * 2 + 1;
        float[] kernel = new float[size * size];
        float sum = 0.0f;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float val = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                kernel[(y + radius) * size + (x + radius)] = val;
                sum += val;
            }
        }
        for (int i = 0; i < kernel.length; i++) kernel[i] /= sum;
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, kernel), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(input, null);
    }
    @Override public String getName() { return "Gaussian Blur (σ=" + sigma + ")"; }
}

// 2. Median Filter
class MedianFilterOperation implements ImageOperation {
    private final int radius;
    public MedianFilterOperation(int radius) { this.radius = Math.max(1, radius); }
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        int windowSize = (2 * radius + 1) * (2 * radius + 1);
        int[] rArr = new int[windowSize], gArr = new int[windowSize], bArr = new int[windowSize];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int px = Math.min(Math.max(x + dx, 0), w - 1);
                        int py = Math.min(Math.max(y + dy, 0), h - 1);
                        int rgb = input.getRGB(px, py);
                        rArr[idx] = (rgb >> 16) & 0xFF;
                        gArr[idx] = (rgb >> 8) & 0xFF;
                        bArr[idx] = rgb & 0xFF;
                        idx++;
                    }
                }
                Arrays.sort(rArr); Arrays.sort(gArr); Arrays.sort(bArr);
                int medianIdx = windowSize / 2;
                int alpha = (input.getRGB(x, y) >> 24) & 0xFF;
                result.setRGB(x, y, (alpha << 24) | (rArr[medianIdx] << 16) | (gArr[medianIdx] << 8) | bArr[medianIdx]);
            }
        }
        return result;
    }
    @Override public String getName() { return "Median Filter (r=" + radius + ")"; }
}

// 3. Bilateral Edge-Preserving Filter
class BilateralFilterOperation implements ImageOperation {
    private final double sigmaSpatial, sigmaRange;
    public BilateralFilterOperation(double sigmaSpatial, double sigmaRange) {
        this.sigmaSpatial = sigmaSpatial;
        this.sigmaRange = sigmaRange;
    }
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        int radius = (int) Math.ceil(sigmaSpatial * 2);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int centerRGB = input.getRGB(x, y);
                int centerIntensity = ((centerRGB >> 16) & 0xFF + (centerRGB >> 8) & 0xFF + centerRGB & 0xFF) / 3;

                double sumWeights = 0, normR = 0, normG = 0, normB = 0;

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int px = Math.min(Math.max(x + dx, 0), w - 1);
                        int py = Math.min(Math.max(y + dy, 0), h - 1);
                        int rgb = input.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                        int intensity = (r + g + b) / 3;

                        double spatialDistSq = dx * dx + dy * dy;
                        double rangeDistSq = Math.pow(intensity - centerIntensity, 2);

                        double weight = Math.exp(-spatialDistSq / (2 * sigmaSpatial * sigmaSpatial) - rangeDistSq / (2 * sigmaRange * sigmaRange));
                        sumWeights += weight;
                        normR += r * weight;
                        normG += g * weight;
                        normB += b * weight;
                    }
                }
                int alpha = (centerRGB >> 24) & 0xFF;
                result.setRGB(x, y, (alpha << 24) | ((int)(normR / sumWeights) << 16) | ((int)(normG / sumWeights) << 8) | (int)(normB / sumWeights));
            }
        }
        return result;
    }
    @Override public String getName() { return "Bilateral Filter"; }
}

// 4. Global Histogram Equalization (GHE)
class GlobalHistogramEqualizationOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int[] hist = new int[256];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int lum = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                hist[lum]++;
            }
        }

        int[] cdf = new int[256];
        cdf[0] = hist[0];
        for (int i = 1; i < 256; i++) cdf[i] = cdf[i - 1] + hist[i];

        int totalPixels = w * h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int lum = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                int eqLum = (int) (((float) cdf[lum] / totalPixels) * 255);
                result.setRGB(x, y, (0xFF << 24) | (eqLum << 16) | (eqLum << 8) | eqLum);
            }
        }
        return result;
    }
    @Override public String getName() { return "Global Histogram Equalization (GHE)"; }
}

// 5. Contrast Limited Adaptive Histogram Equalization (CLAHE)
class CLAHEOperation implements ImageOperation {
    private final int tileSize;
    private final float clipLimit;

    public CLAHEOperation(int tileSize, float clipLimit) {
        this.tileSize = tileSize;
        this.clipLimit = clipLimit;
    }

    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        int tilesX = (int) Math.ceil((double) w / tileSize);
        int tilesY = (int) Math.ceil((double) h / tileSize);

        float[][][] cdfs = new float[tilesY][tilesX][256];

        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                int[] hist = new int[256];
                int actualTileW = Math.min(tileSize, w - tx * tileSize);
                int actualTileH = Math.min(tileSize, h - ty * tileSize);
                int tilePixels = actualTileW * actualTileH;

                for (int y = 0; y < actualTileH; y++) {
                    for (int x = 0; x < actualTileW; x++) {
                        int rgb = input.getRGB(tx * tileSize + x, ty * tileSize + y);
                        int lum = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                        hist[lum]++;
                    }
                }

                int clipThreshold = (int) (clipLimit * (tilePixels / 256.0f));
                int excess = 0;
                for (int i = 0; i < 256; i++) {
                    if (hist[i] > clipThreshold) {
                        excess += (hist[i] - clipThreshold);
                        hist[i] = clipThreshold;
                    }
                }
                int bonus = excess / 256;
                for (int i = 0; i < 256; i++) hist[i] += bonus;

                cdfs[ty][tx][0] = hist[0] / (float) tilePixels;
                for (int i = 1; i < 256; i++) {
                    cdfs[ty][tx][i] = cdfs[ty][tx][i - 1] + (hist[i] / (float) tilePixels);
                }
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int lum = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));

                float tx = (float) x / tileSize - 0.5f;
                float ty = (float) y / tileSize - 0.5f;

                int x1 = (int) Math.floor(tx), y1 = (int) Math.floor(ty);
                int x2 = x1 + 1, y2 = y1 + 1;

                x1 = Math.max(0, Math.min(tilesX - 1, x1)); x2 = Math.max(0, Math.min(tilesX - 1, x2));
                y1 = Math.max(0, Math.min(tilesY - 1, y1)); y2 = Math.max(0, Math.min(tilesY - 1, y2));

                float fx = tx - (float) Math.floor(tx), fy = ty - (float) Math.floor(ty);

                float val = (1 - fx) * (1 - fy) * cdfs[y1][x1][lum] + fx * (1 - fy) * cdfs[y1][x2][lum]
                          + (1 - fx) * fy * cdfs[y2][x1][lum] + fx * fy * cdfs[y2][x2][lum];

                int finalLum = Math.min(255, Math.max(0, (int) (val * 255)));
                result.setRGB(x, y, (0xFF << 24) | (finalLum << 16) | (finalLum << 8) | finalLum);
            }
        }
        return result;
    }
    @Override public String getName() { return "CLAHE"; }
}

// 6. Unsharp Masking (USM)
class UnsharpMaskOperation implements ImageOperation {
    private final float amount;
    public UnsharpMaskOperation(float amount) { this.amount = amount; }
    @Override
    public BufferedImage process(BufferedImage input) {
        BufferedImage blurred = new GaussianBlurOperation(1.5f).process(input);
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int orig = input.getRGB(x, y), blur = blurred.getRGB(x, y);
                int a = (orig >> 24) & 0xFF;
                int r = Math.min(255, Math.max(0, (int) (((orig >> 16) & 0xFF) + amount * (((orig >> 16) & 0xFF) - ((blur >> 16) & 0xFF)))));
                int g = Math.min(255, Math.max(0, (int) (((orig >> 8) & 0xFF) + amount * (((orig >> 8) & 0xFF) - ((blur >> 8) & 0xFF)))));
                int b = Math.min(255, Math.max(0, (int) ((orig & 0xFF) + amount * ((orig & 0xFF) - (blur & 0xFF)))));
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }
    @Override public String getName() { return "Unsharp Masking"; }
}

// 7. Sobel Gradient Operator
class SobelEdgeDetectionOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        int[][] gx = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] gy = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int px = 0, py = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int rgb = input.getRGB(x + dx, y + dy);
                        int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                        px += gx[dy + 1][dx + 1] * gray;
                        py += gy[dy + 1][dx + 1] * gray;
                    }
                }
                int mag = Math.min(255, (int) Math.sqrt(px * px + py * py));
                result.setRGB(x, y, (0xFF << 24) | (mag << 16) | (mag << 8) | mag);
            }
        }
        return result;
    }
    @Override public String getName() { return "Sobel Edge Operator"; }
}

// 8. Otsu Automatic Binarization
class OtsuThresholdOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int[] hist = new int[256];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                hist[gray]++;
            }
        }

        int total = w * h;
        float sum = 0;
        for (int t = 0; t < 256; t++) sum += t * hist[t];

        float sumB = 0;
        int wB = 0, bestThreshold = 0;
        float varMax = 0;

        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            int wF = total - wB;
            if (wF == 0) break;

            sumB += (float) (t * hist[t]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                bestThreshold = t;
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                int val = (gray >= bestThreshold) ? 255 : 0;
                result.setRGB(x, y, (0xFF << 24) | (val << 16) | (val << 8) | val);
            }
        }
        return result;
    }
    @Override public String getName() { return "Otsu Binarization"; }
}

// 9. Sauvola Adaptive Thresholding
class SauvolaThresholdOperation implements ImageOperation {
    private final int windowSize;
    private final double k;

    public SauvolaThresholdOperation(int windowSize, double k) {
        this.windowSize = windowSize;
        this.k = k;
    }

    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int half = windowSize / 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int count = 0, sum = 0, sqSum = 0;
                for (int dy = -half; dy <= half; dy++) {
                    for (int dx = -half; dx <= half; dx++) {
                        int px = Math.min(Math.max(x + dx, 0), w - 1);
                        int py = Math.min(Math.max(y + dy, 0), h - 1);
                        int rgb = input.getRGB(px, py);
                        int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                        sum += gray;
                        sqSum += gray * gray;
                        count++;
                    }
                }
                double mean = (double) sum / count;
                double std = Math.sqrt(((double) sqSum / count) - (mean * mean));
                double threshold = mean * (1.0 + k * ((std / 128.0) - 1.0));

                int currentRGB = input.getRGB(x, y);
                int currentGray = (int) (0.299 * ((currentRGB >> 16) & 0xFF) + 0.587 * ((currentRGB >> 8) & 0xFF) + 0.114 * (currentRGB & 0xFF));
                int val = (currentGray >= threshold) ? 255 : 0;
                result.setRGB(x, y, (0xFF << 24) | (val << 16) | (val << 8) | val);
            }
        }
        return result;
    }
    @Override public String getName() { return "Sauvola Binarization"; }
}

// 10. Morphological Erosion and Dilation
class MorphologyOperation implements ImageOperation {
    public enum Type { EROSION, DILATION }
    private final Type type;
    private final int radius;

    public MorphologyOperation(Type type, int radius) {
        this.type = type;
        this.radius = radius;
    }

    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int target = (type == Type.EROSION) ? 255 : 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int px = Math.min(Math.max(x + dx, 0), w - 1);
                        int py = Math.min(Math.max(y + dy, 0), h - 1);
                        int rgb = input.getRGB(px, py);
                        int gray = (rgb & 0xFF);
                        target = (type == Type.EROSION) ? Math.min(target, gray) : Math.max(target, gray);
                    }
                }
                result.setRGB(x, y, (0xFF << 24) | (target << 16) | (target << 8) | target);
            }
        }
        return result;
    }
    @Override public String getName() { return "Morphology (" + type.name() + ")"; }
}

// ==========================================
// 10 ADVANCED SCIENTIFIC LAB OPERATIONS (BATCH 2)
// ==========================================

// Helper class for Complex Number arithmetic used in Frequency Domain & Deconvolution
class Complex {
    double re, im;
    public Complex(double re, double im) { this.re = re; this.im = im; }
    public Complex add(Complex b) { return new Complex(re + b.re, im + b.im); }
    public Complex sub(Complex b) { return new Complex(re - b.re, im - b.im); }
    public Complex mul(Complex b) { return new Complex(re * b.re - im * b.im, re * b.im + im * b.re); }
    public Complex div(double val) { return new Complex(re / val, im / val); }
    public Complex conj() { return new Complex(re, -im); }
    public double abs() { return Math.sqrt(re * re + im * im); }
}

class FFT2DHelper {
    public static void fft1D(Complex[] x, boolean inverse) {
        int n = x.length;
        if (n <= 1) return;
        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];
        for (int i = 0; i < n / 2; i++) {
            even[i] = x[2 * i];
            odd[i] = x[2 * i + 1];
        }
        fft1D(even, inverse);
        fft1D(odd, inverse);
        double angle = (inverse ? 2 : -2) * Math.PI / n;
        for (int k = 0; k < n / 2; k++) {
            Complex w = new Complex(Math.cos(angle * k), Math.sin(angle * k));
            Complex t = w.mul(odd[k]);
            x[k] = even[k].add(t);
            x[k + n / 2] = even[k].sub(t);
            if (inverse) {
                x[k] = x[k].div(2.0);
                x[k + n / 2] = x[k + n / 2].div(2.0);
            }
        }
    }

    public static Complex[][] fft2D(Complex[][] input, boolean inverse) {
        int h = input.length, w = input[0].length;
        Complex[][] out = new Complex[h][w];
        for (int y = 0; y < h; y++) {
            out[y] = Arrays.copyOf(input[y], w);
            fft1D(out[y], inverse);
        }
        for (int x = 0; x < w; x++) {
            Complex[] col = new Complex[h];
            for (int y = 0; y < h; y++) col[y] = out[y][x];
            fft1D(col, inverse);
            for (int y = 0; y < h; y++) out[y][x] = col[y];
        }
        return out;
    }

    public static int nextPowerOf2(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }
}

// --- 1. Box / Mean Spatial Convolution Filter ---
class BoxFilterOperation implements ImageOperation {
    private final int radius;
    public BoxFilterOperation(int radius) { this.radius = Math.max(1, radius); }
    @Override
    public BufferedImage process(BufferedImage input) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size * size];
        Arrays.fill(kernel, 1.0f / (size * size));
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, kernel), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(input, null);
    }
    @Override public String getName() { return "Box Filter (r=" + radius + ")"; }
}

// --- 2. 2D Fast Fourier Transform Spectrum (Spatial Frequency Domain) ---
class FFTSpectrumOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int origW = input.getWidth(), origH = input.getHeight();
        int w = FFT2DHelper.nextPowerOf2(origW);
        int h = FFT2DHelper.nextPowerOf2(origH);

        Complex[][] spatial = new Complex[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x < origW && y < origH) {
                    int rgb = input.getRGB(x, y);
                    double gray = 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
                    // Center shift (-1)^(x+y)
                    spatial[y][x] = new Complex(gray * (((x + y) % 2 == 0) ? 1 : -1), 0);
                } else {
                    spatial[y][x] = new Complex(0, 0);
                }
            }
        }

        Complex[][] freq = FFT2DHelper.fft2D(spatial, false);
        BufferedImage spectrum = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        double maxVal = 0;
        double[][] mag = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mag[y][x] = Math.log(1 + freq[y][x].abs());
                if (mag[y][x] > maxVal) maxVal = mag[y][x];
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = (int) ((mag[y][x] / maxVal) * 255.0);
                spectrum.setRGB(x, y, (0xFF << 24) | (val << 16) | (val << 8) | val);
            }
        }
        return spectrum;
    }
    @Override public String getName() { return "2D-FFT Magnitude Spectrum"; }
}

// --- 3. Single-Scale Retinex (SSR Illumination-Reflectance Decomposition) ---
class SingleScaleRetinexOperation implements ImageOperation {
    private final float sigma;
    public SingleScaleRetinexOperation(float sigma) { this.sigma = sigma; }
    @Override
    public BufferedImage process(BufferedImage input) {
        BufferedImage blurred = new GaussianBlurOperation(sigma).process(input);
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int orig = input.getRGB(x, y), blur = blurred.getRGB(x, y);
                int a = (orig >> 24) & 0xFF;

                double r = Math.log(((orig >> 16) & 0xFF) + 1.0) - Math.log(((blur >> 16) & 0xFF) + 1.0);
                double g = Math.log(((orig >> 8) & 0xFF) + 1.0) - Math.log(((blur >> 8) & 0xFF) + 1.0);
                double b = Math.log((orig & 0xFF) + 1.0) - Math.log((blur & 0xFF) + 1.0);

                int outR = Math.min(255, Math.max(0, (int) ((r + 2.0) * 50.0)));
                int outG = Math.min(255, Math.max(0, (int) ((g + 2.0) * 50.0)));
                int outB = Math.min(255, Math.max(0, (int) ((b + 2.0) * 50.0)));

                result.setRGB(x, y, (a << 24) | (outR << 16) | (outG << 8) | outB);
            }
        }
        return result;
    }
    @Override public String getName() { return "Single-Scale Retinex (σ=" + sigma + ")"; }
}

// --- 4. Reinhard Global Tone Mapping Operator (TMO) ---
class ReinhardToneMapOperation implements ImageOperation {
    private final float key;
    public ReinhardToneMapOperation(float key) { this.key = key; }
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());

        double delta = 0.0001;
        double sumLog = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                double lum = (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255.0;
                sumLog += Math.log(delta + lum);
            }
        }
        double lAvg = Math.exp(sumLog / (w * h));

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                double r = ((rgb >> 16) & 0xFF) / 255.0;
                double g = ((rgb >> 8) & 0xFF) / 255.0;
                double b = (rgb & 0xFF) / 255.0;

                double lIn = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                double lScaled = (key / lAvg) * lIn;
                double lOut = lScaled / (1.0 + lScaled);

                double scale = (lIn > 0) ? lOut / lIn : 0;
                int outR = Math.min(255, (int) (r * scale * 255.0));
                int outG = Math.min(255, (int) (g * scale * 255.0));
                int outB = Math.min(255, (int) (b * scale * 255.0));

                result.setRGB(x, y, (a << 24) | (outR << 16) | (outG << 8) | outB);
            }
        }
        return result;
    }
    @Override public String getName() { return "Reinhard Tone Map"; }
}

// --- 5. Bayer Pattern Demosaicing (MHC / Linear Interpolation) ---
class DemosaicMHCOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage rgbImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // Assume standard RGGB Bayer Array pattern
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                boolean isEvenRow = (y % 2 == 0);
                boolean isEvenCol = (x % 2 == 0);

                int r, g, b;
                if (isEvenRow && isEvenCol) { // Red pixel
                    r = input.getRGB(x, y) & 0xFF;
                    g = ((input.getRGB(x - 1, y) & 0xFF) + (input.getRGB(x + 1, y) & 0xFF) + (input.getRGB(x, y - 1) & 0xFF) + (input.getRGB(x, y + 1) & 0xFF)) / 4;
                    b = ((input.getRGB(x - 1, y - 1) & 0xFF) + (input.getRGB(x + 1, y - 1) & 0xFF) + (input.getRGB(x - 1, y + 1) & 0xFF) + (input.getRGB(x + 1, y + 1) & 0xFF)) / 4;
                } else if (!isEvenRow && !isEvenCol) { // Blue pixel
                    b = input.getRGB(x, y) & 0xFF;
                    g = ((input.getRGB(x - 1, y) & 0xFF) + (input.getRGB(x + 1, y) & 0xFF) + (input.getRGB(x, y - 1) & 0xFF) + (input.getRGB(x, y + 1) & 0xFF)) / 4;
                    r = ((input.getRGB(x - 1, y - 1) & 0xFF) + (input.getRGB(x + 1, y - 1) & 0xFF) + (input.getRGB(x - 1, y + 1) & 0xFF) + (input.getRGB(x + 1, y + 1) & 0xFF)) / 4;
                } else { // Green pixel
                    g = input.getRGB(x, y) & 0xFF;
                    if (isEvenRow) { // Green on Red row
                        r = ((input.getRGB(x - 1, y) & 0xFF) + (input.getRGB(x + 1, y) & 0xFF)) / 2;
                        b = ((input.getRGB(x, y - 1) & 0xFF) + (input.getRGB(x, y + 1) & 0xFF)) / 2;
                    } else { // Green on Blue row
                        b = ((input.getRGB(x - 1, y) & 0xFF) + (input.getRGB(x + 1, y) & 0xFF)) / 2;
                        r = ((input.getRGB(x, y - 1) & 0xFF) + (input.getRGB(x, y + 1) & 0xFF)) / 2;
                    }
                }
                rgbImage.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return rgbImage;
    }
    @Override public String getName() { return "Bayer Demosaicing (MHC)"; }
}

// --- 6. Frequency-Domain Wiener Deconvolution Restoration ---
class WienerDeconvolutionOperation implements ImageOperation {
    private final double nsr; // Noise-to-Signal Ratio
    public WienerDeconvolutionOperation(double nsr) { this.nsr = nsr; }
    @Override
    public BufferedImage process(BufferedImage input) {
        int origW = input.getWidth(), origH = input.getHeight();
        int w = FFT2DHelper.nextPowerOf2(origW);
        int h = FFT2DHelper.nextPowerOf2(origH);

        Complex[][] imageFFT = new Complex[h][w];
        Complex[][] kernelFFT = new Complex[h][w];

        // 3x3 Gaussian PSF kernel approximation
        float[] psf = {0.05f, 0.15f, 0.05f, 0.15f, 0.20f, 0.15f, 0.05f, 0.15f, 0.05f};

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x < origW && y < origH) {
                    int rgb = input.getRGB(x, y);
                    double gray = (rgb & 0xFF);
                    imageFFT[y][x] = new Complex(gray, 0);
                } else {
                    imageFFT[y][x] = new Complex(0, 0);
                }

                if (x < 3 && y < 3) {
                    kernelFFT[y][x] = new Complex(psf[y * 3 + x], 0);
                } else {
                    kernelFFT[y][x] = new Complex(0, 0);
                }
            }
        }

        Complex[][] G = FFT2DHelper.fft2D(imageFFT, false);
        Complex[][] H = FFT2DHelper.fft2D(kernelFFT, false);

        Complex[][] F_hat = new Complex[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double hMagSq = H[y][x].abs() * H[y][x].abs();
                Complex wienerFilter = H[y][x].conj().div(hMagSq + nsr);
                F_hat[y][x] = G[y][x].mul(wienerFilter);
            }
        }

        Complex[][] restored = FFT2DHelper.fft2D(F_hat, true);
        BufferedImage output = new BufferedImage(origW, origH, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < origH; y++) {
            for (int x = 0; x < origW; x++) {
                int val = Math.min(255, Math.max(0, (int) Math.abs(restored[y][x].re)));
                output.setRGB(x, y, (0xFF << 24) | (val << 16) | (val << 8) | val);
            }
        }
        return output;
    }
    @Override public String getName() { return "Wiener Deconvolution"; }
}

// --- 7. Morphological Opening and Closing ---
class MorphologicalOpenCloseOperation implements ImageOperation {
    public enum Mode { OPENING, CLOSING }
    private final Mode mode;
    private final int radius;

    public MorphologicalOpenCloseOperation(Mode mode, int radius) {
        this.mode = mode;
        this.radius = radius;
    }

    @Override
    public BufferedImage process(BufferedImage input) {
        if (mode == Mode.OPENING) {
            BufferedImage erased = new MorphologyOperation(MorphologyOperation.Type.EROSION, radius).process(input);
            return new MorphologyOperation(MorphologyOperation.Type.DILATION, radius).process(erased);
        } else {
            BufferedImage dilated = new MorphologyOperation(MorphologyOperation.Type.DILATION, radius).process(input);
            return new MorphologyOperation(MorphologyOperation.Type.EROSION, radius).process(dilated);
        }
    }
    @Override public String getName() { return "Morphology " + mode.name(); }
}

// --- 8. Morphological Top-Hat Transform ---
class TopHatTransformOperation implements ImageOperation {
    private final int radius;
    public TopHatTransformOperation(int radius) { this.radius = radius; }
    @Override
    public BufferedImage process(BufferedImage input) {
        BufferedImage opened = new MorphologicalOpenCloseOperation(MorphologicalOpenCloseOperation.Mode.OPENING, radius).process(input);
        int w = input.getWidth(), h = input.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int orig = input.getRGB(x, y) & 0xFF;
                int open = opened.getRGB(x, y) & 0xFF;
                int topHat = Math.max(0, orig - open);
                result.setRGB(x, y, (0xFF << 24) | (topHat << 16) | (topHat << 8) | topHat);
            }
        }
        return result;
    }
    @Override public String getName() { return "Top-Hat Transform"; }
}

// --- 9. Vignetting Polynomial Radial Decaying Correction ---
class VignettingCorrectionOperation implements ImageOperation {
    private final double k; // Radial Decay Compensation Constant
    public VignettingCorrectionOperation(double k) { this.k = k; }
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        double cx = w / 2.0, cy = h / 2.0;
        double maxDist = Math.sqrt(cx * cx + cy * cy);

        BufferedImage result = new BufferedImage(w, h, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double r = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maxDist;
                double gain = 1.0 + k * (r * r);

                int rgb = input.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int red = Math.min(255, (int) (((rgb >> 16) & 0xFF) * gain));
                int green = Math.min(255, (int) (((rgb >> 8) & 0xFF) * gain));
                int blue = Math.min(255, (int) ((rgb & 0xFF) * gain));

                result.setRGB(x, y, (a << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return result;
    }
    @Override public String getName() { return "Vignetting Correction"; }
}

// --- 10. 2D Discrete Haar Wavelet Transform (1-Level Multi-Resolution) ---
class HaarWaveletTransformOperation implements ImageOperation {
    @Override
    public BufferedImage process(BufferedImage input) {
        int w = input.getWidth(), h = input.getHeight();
        int halfW = w / 2, halfH = h / 2;
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < halfH; y++) {
            for (int x = 0; x < halfW; x++) {
                int p00 = input.getRGB(2 * x, 2 * y) & 0xFF;
                int p10 = input.getRGB(2 * x + 1, 2 * y) & 0xFF;
                int p01 = input.getRGB(2 * x, 2 * y + 1) & 0xFF;
                int p11 = input.getRGB(2 * x + 1, 2 * y + 1) & 0xFF;

                // Haar 2D Coefficients
                int ll = (p00 + p10 + p01 + p11) / 4;          // Approximation
                int lh = Math.abs((p00 - p10 + p01 - p11) / 2); // Horizontal details
                int hl = Math.abs((p00 + p10 - p01 - p11) / 2); // Vertical details
                int hh = Math.abs((p00 - p10 - p01 + p11) / 2); // Diagonal details

                // Tile into 2x2 grid subbands
                result.setRGB(x, y, (0xFF << 24) | (ll << 16) | (ll << 8) | ll);
                result.setRGB(x + halfW, y, (0xFF << 24) | (lh << 16) | (lh << 8) | lh);
                result.setRGB(x, y + halfH, (0xFF << 24) | (hl << 16) | (hl << 8) | hl);
                result.setRGB(x + halfW, y + halfH, (0xFF << 24) | (hh << 16) | (hh << 8) | hh);
            }
        }
        return result;
    }
    @Override public String getName() { return "2D Haar Wavelet DWT"; }
}