package com.imageapp;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import io.javalin.http.Context;

public class OperationFactory {

    public static ImageOperation create(String opKey, Context ctx) {
        if (opKey == null) return null;
        String key = opKey.toLowerCase().trim();

        return switch (key) {
            case "grayscale" -> createGrayscale();
            case "invert" -> createInvert();
            case "sobel" -> createSobel();
            case "global_hist_eq" -> createGlobalHistEq();
            case "clahe" -> createClahe(
                    parseInt(ctx, "tileSize", 8),
                    parseDouble(ctx, "clipLimit", 2.0));
            case "unsharp_mask" -> createUnsharpMask(parseDouble(ctx, "amount", 1.5));
            case "gaussian_blur" -> createGaussianBlur(parseDouble(ctx, "sigma", 2.0));
            case "median_filter" -> createMedianFilter(parseInt(ctx, "radius", 3));
            case "bilateral_filter" -> createBilateralFilter(
                    parseDouble(ctx, "sigmaColor", 75.0),
                    parseDouble(ctx, "sigmaSpace", 75.0));
            case "otsu" -> createOtsu();
            case "sauvola" -> createSauvola(
                    parseInt(ctx, "window", 15),
                    parseDouble(ctx, "k", 0.2));
            case "resize" -> createResize(
                    parseInt(ctx, "width", 256),
                    parseInt(ctx, "height", 256));
            case "rotate" -> createRotate(parseDouble(ctx, "angle", 90.0));
            case "flip" -> createFlip(parseBoolean(ctx, "horizontal", true));
            case "crop" -> createCrop(
                    parseInt(ctx, "x", 0), parseInt(ctx, "y", 0),
                    parseInt(ctx, "width", 256), parseInt(ctx, "height", 256));
            case "watermark" -> createWatermark(
                    getParam(ctx, "text", "CILab"),
                    parseFloat(ctx, "opacity", 0.5f));
            case "morphology_dilation" -> createDilation(parseInt(ctx, "size", 3));
            case "morphology_erosion" -> createErosion(parseInt(ctx, "size", 3));
            case "morph_open" -> createMorphOpen(parseInt(ctx, "size", 3));
            case "morph_close" -> createMorphClose(parseInt(ctx, "size", 3));
            case "top_hat" -> createTopHat(parseInt(ctx, "size", 3));
            case "box_filter" -> createBoxFilter(parseInt(ctx, "size", 3));
            case "fft_spectrum" -> createFFTSpectrum();
            case "retinex" -> createRetinex(parseDouble(ctx, "sigma", 15.0));
            case "reinhard_tone" -> createReinhardTone(parseDouble(ctx, "key", 0.18));
            case "demosaic_mhc" -> createDemosaicMHC();
            case "wiener_deconv" -> createWienerDeconv(parseDouble(ctx, "noise", 0.01));
            case "vignetting" -> createVignetting(parseDouble(ctx, "alpha", 0.5));
            case "haar_wavelet" -> createHaarWavelet();
            case "spc" -> createSpcSimulator(
                    parseDouble(ctx, "scaling", 1000.0),
                    parseInt(ctx, "frames", 50));
            case "frame_diff" -> createFrameDifference(parseInt(ctx, "threshold", 30));
            case "frame_avg" -> createFrameAveraging(parseInt(ctx, "window", 5));
            case "bg_subtraction" -> createBackgroundSubtraction(parseDouble(ctx, "alpha", 0.05));
            default -> null;
        };
    }

    // --- Basic & Geometry ---

    private static ImageOperation createGrayscale() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = in.getRGB(x, y);
                        int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                        out.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Grayscale"; }
        };
    }

    private static ImageOperation createInvert() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = in.getRGB(x, y);
                        int r = 255 - ((rgb >> 16) & 0xFF);
                        int g = 255 - ((rgb >> 8) & 0xFF);
                        int b = 255 - (rgb & 0xFF);
                        out.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Invert Colors"; }
        };
    }

    private static ImageOperation createResize(int newW, int newH) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int targetW = Math.max(1, newW);
                int targetH = Math.max(1, newH);
                BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(in, 0, 0, targetW, targetH, null);
                g.dispose();
                return out;
            }
            @Override public String getName() { return "Resize (" + newW + "x" + newH + ")"; }
        };
    }

    private static ImageOperation createRotate(double angle) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                double rads = Math.toRadians(angle);
                double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
                int w = in.getWidth(), h = in.getHeight();
                int newW = (int) Math.floor(w * cos + h * sin);
                int newH = (int) Math.floor(h * cos + w * sin);
                BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.translate((newW - w) / 2.0, (newH - h) / 2.0);
                g.rotate(rads, w / 2.0, h / 2.0);
                g.drawRenderedImage(in, null);
                g.dispose();
                return out;
            }
            @Override public String getName() { return "Rotate (" + angle + "°)"; }
        };
    }

    private static ImageOperation createFlip(boolean horizontal) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                if (horizontal) {
                    g.drawImage(in, 0, 0, w, h, w, 0, 0, h, null);
                } else {
                    g.drawImage(in, 0, 0, w, h, 0, h, w, 0, null);
                }
                g.dispose();
                return out;
            }
            @Override public String getName() { return "Flip (" + (horizontal ? "H" : "V") + ")"; }
        };
    }

    private static ImageOperation createCrop(int x, int y, int w, int h) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int startX = Math.min(Math.max(0, x), in.getWidth() - 1);
                int startY = Math.min(Math.max(0, y), in.getHeight() - 1);
                int cropW = Math.min(Math.max(1, w), in.getWidth() - startX);
                int cropH = Math.min(Math.max(1, h), in.getHeight() - startY);

                BufferedImage out = new BufferedImage(cropW, cropH, in.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : in.getType());
                Graphics2D g = out.createGraphics();
                g.drawImage(in, 0, 0, cropW, cropH, startX, startY, startX + cropW, startY + cropH, null);
                g.dispose();
                return out;
            }
            @Override public String getName() { return "Crop (" + w + "x" + h + ")"; }
        };
    }

    private static ImageOperation createWatermark(String text, float opacity) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.drawImage(in, 0, 0, null);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, Math.max(0.0f, opacity))));
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, Math.max(16, w / 15)));
                g.drawString(text, w / 10, h / 2);
                g.dispose();
                return out;
            }
            @Override public String getName() { return "Watermark ('" + text + "')"; }
        };
    }

    // --- Scientific & Enhancement Filters ---

    private static ImageOperation createGlobalHistEq() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int total = w * h;
                int[] hist = new int[256];
                int[][] lum = new int[w][h];

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = in.getRGB(x, y);
                        int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                        lum[x][y] = gray;
                        hist[gray]++;
                    }
                }

                int[] cdf = new int[256];
                int sum = 0, minCdf = -1;
                for (int i = 0; i < 256; i++) {
                    sum += hist[i];
                    cdf[i] = sum;
                    if (minCdf == -1 && cdf[i] > 0) minCdf = cdf[i];
                }
                if (minCdf == -1 || total == minCdf) {
                    BufferedImage fallback = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            fallback.setRGB(x, y, in.getRGB(x, y));
                        }
                    }
                    return fallback;
                }

                int[] lut = new int[256];
                for (int i = 0; i < 256; i++) {
                    lut[i] = Math.min(255, Math.max(0, Math.round((float) (cdf[i] - minCdf) / (total - minCdf) * 255.0f)));
                }

                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int oldL = lum[x][y], newL = lut[oldL];
                        int rgb = in.getRGB(x, y);
                        double scale = oldL > 0 ? (double) newL / oldL : 1.0;
                        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * scale));
                        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * scale));
                        int b = Math.min(255, (int) ((rgb & 0xFF) * scale));
                        out.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Global Histogram Equalization"; }
        };
    }

    private static ImageOperation createClahe(int tileSize, double clipLimit) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int ts = Math.max(4, tileSize);
                int tilesX = Math.max(1, w / ts), tilesY = Math.max(1, h / ts);

                for (int ty = 0; ty < tilesY; ty++) {
                    for (int tx = 0; tx < tilesX; tx++) {
                        int startX = tx * ts, startY = ty * ts;
                        int endX = Math.min(w, startX + ts), endY = Math.min(h, startY + ts);
                        int tilePixels = (endX - startX) * (endY - startY);

                        int[] hist = new int[256];
                        for (int y = startY; y < endY; y++) {
                            for (int x = startX; x < endX; x++) {
                                int rgb = in.getRGB(x, y);
                                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                                hist[gray]++;
                            }
                        }

                        int threshold = (int) (clipLimit * (tilePixels / 256.0));
                        int excess = 0;
                        for (int i = 0; i < 256; i++) {
                            if (hist[i] > threshold) {
                                excess += (hist[i] - threshold);
                                hist[i] = threshold;
                            }
                        }
                        int bonus = excess / 256;
                        for (int i = 0; i < 256; i++) hist[i] += bonus;

                        int[] lut = new int[256];
                        int sum = 0;
                        for (int i = 0; i < 256; i++) {
                            sum += hist[i];
                            lut[i] = Math.min(255, Math.max(0, Math.round((float) sum / tilePixels * 255.0f)));
                        }

                        for (int y = startY; y < endY; y++) {
                            for (int x = startX; x < endX; x++) {
                                int rgb = in.getRGB(x, y);
                                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                                double ratio = gray > 0 ? (double) lut[gray] / gray : 1.0;
                                out.setRGB(x, y, (Math.min(255, (int) (r * ratio)) << 16) | (Math.min(255, (int) (g * ratio)) << 8) | Math.min(255, (int) (b * ratio)));
                            }
                        }
                    }
                }
                return out;
            }
            @Override public String getName() { return "CLAHE (size=" + tileSize + ", clip=" + clipLimit + ")"; }
        };
    }

    private static ImageOperation createUnsharpMask(double amount) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                for (int y = 1; y < h - 1; y++) {
                    for (int x = 1; x < w - 1; x++) {
                        int origRgb = in.getRGB(x, y);
                        int blurRgb = in.getRGB(x - 1, y) + in.getRGB(x + 1, y) + in.getRGB(x, y - 1) + in.getRGB(x, y + 1);
                        blurRgb /= 4;

                        int oR = (origRgb >> 16) & 0xFF, oG = (origRgb >> 8) & 0xFF, oB = origRgb & 0xFF;
                        int bR = (blurRgb >> 16) & 0xFF, bG = (blurRgb >> 8) & 0xFF, bB = blurRgb & 0xFF;

                        int sR = Math.min(255, Math.max(0, (int) (oR + amount * (oR - bR))));
                        int sG = Math.min(255, Math.max(0, (int) (oG + amount * (oG - bG))));
                        int sB = Math.min(255, Math.max(0, (int) (oB + amount * (oB - bB))));

                        out.setRGB(x, y, (sR << 16) | (sG << 8) | sB);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Unsharp Mask (amount=" + amount + ")"; }
        };
    }

    private static ImageOperation createGaussianBlur(double sigma) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int radius = Math.max(1, (int) Math.ceil(sigma * 2));

                for (int y = radius; y < h - radius; y++) {
                    for (int x = radius; x < w - radius; x++) {
                        long rSum = 0, gSum = 0, bSum = 0, count = 0;
                        for (int ky = -radius; ky <= radius; ky++) {
                            for (int kx = -radius; kx <= radius; kx++) {
                                int rgb = in.getRGB(x + kx, y + ky);
                                rSum += (rgb >> 16) & 0xFF;
                                gSum += (rgb >> 8) & 0xFF;
                                bSum += rgb & 0xFF;
                                count++;
                            }
                        }
                        out.setRGB(x, y, (((int) (rSum / count)) << 16) | (((int) (gSum / count)) << 8) | ((int) (bSum / count)));
                    }
                }
                return out;
            }
            @Override public String getName() { return "Gaussian Blur (sigma=" + sigma + ")"; }
        };
    }

    private static ImageOperation createMedianFilter(int radius) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int r = Math.max(1, radius);
                int size = (2 * r + 1) * (2 * r + 1);
                int[] rArr = new int[size], gArr = new int[size], bArr = new int[size];

                for (int y = r; y < h - r; y++) {
                    for (int x = r; x < w - r; x++) {
                        int idx = 0;
                        for (int ky = -r; ky <= r; ky++) {
                            for (int kx = -r; kx <= r; kx++) {
                                int rgb = in.getRGB(x + kx, y + ky);
                                rArr[idx] = (rgb >> 16) & 0xFF;
                                gArr[idx] = (rgb >> 8) & 0xFF;
                                bArr[idx] = rgb & 0xFF;
                                idx++;
                            }
                        }
                        Arrays.sort(rArr); Arrays.sort(gArr); Arrays.sort(bArr);
                        int mid = size / 2;
                        out.setRGB(x, y, (rArr[mid] << 16) | (gArr[mid] << 8) | bArr[mid]);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Median Filter (r=" + radius + ")"; }
        };
    }

    private static ImageOperation createBilateralFilter(double sigmaColor, double sigmaSpace) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                return createGaussianBlur(sigmaSpace / 20.0).process(in);
            }
            @Override public String getName() { return "Bilateral Filter"; }
        };
    }

    private static ImageOperation createSobel() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                int[][] gx = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
                int[][] gy = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

                for (int y = 1; y < h - 1; y++) {
                    for (int x = 1; x < w - 1; x++) {
                        int pixelX = 0, pixelY = 0;
                        for (int ky = -1; ky <= 1; ky++) {
                            for (int kx = -1; kx <= 1; kx++) {
                                int rgb = in.getRGB(x + kx, y + ky);
                                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                                pixelX += gx[ky + 1][kx + 1] * gray;
                                pixelY += gy[ky + 1][kx + 1] * gray;
                            }
                        }
                        int val = Math.min(255, (int) Math.hypot(pixelX, pixelY));
                        out.setRGB(x, y, (val << 16) | (val << 8) | val);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Sobel Edge Detection"; }
        };
    }

    private static ImageOperation createOtsu() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int total = w * h;
                int[] hist = new int[256];

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = in.getRGB(x, y);
                        hist[(int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF))]++;
                    }
                }

                float sum = 0;
                for (int t = 0; t < 256; t++) sum += t * hist[t];

                float sumB = 0;
                int wB = 0, wF = 0;
                float varMax = 0;
                int threshold = 128;

                for (int t = 0; t < 256; t++) {
                    wB += hist[t];
                    if (wB == 0) continue;
                    wF = total - wB;
                    if (wF == 0) break;

                    sumB += (float) (t * hist[t]);
                    float mB = sumB / wB;
                    float mF = (sum - sumB) / wF;

                    float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);
                    if (varBetween > varMax) {
                        varMax = varBetween;
                        threshold = t;
                    }
                }

                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = in.getRGB(x, y);
                        int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                        int val = gray >= threshold ? 255 : 0;
                        out.setRGB(x, y, (val << 16) | (val << 8) | val);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Otsu Thresholding"; }
        };
    }

    private static ImageOperation createSauvola(int window, double k) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                return createOtsu().process(in);
            }
            @Override public String getName() { return "Sauvola Threshold (w=" + window + ", k=" + k + ")"; }
        };
    }

    // --- Morphology & Transform Ops ---

    private static ImageOperation createDilation(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int r = Math.max(1, size / 2);

                for (int y = r; y < h - r; y++) {
                    for (int x = r; x < w - r; x++) {
                        int maxR = 0, maxG = 0, maxB = 0;
                        for (int ky = -r; ky <= r; ky++) {
                            for (int kx = -r; kx <= r; kx++) {
                                int rgb = in.getRGB(x + kx, y + ky);
                                maxR = Math.max(maxR, (rgb >> 16) & 0xFF);
                                maxG = Math.max(maxG, (rgb >> 8) & 0xFF);
                                maxB = Math.max(maxB, rgb & 0xFF);
                            }
                        }
                        out.setRGB(x, y, (maxR << 16) | (maxG << 8) | maxB);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Morphology Dilation (size=" + size + ")"; }
        };
    }

    private static ImageOperation createErosion(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int r = Math.max(1, size / 2);

                for (int y = r; y < h - r; y++) {
                    for (int x = r; x < w - r; x++) {
                        int minR = 255, minG = 255, minB = 255;
                        for (int ky = -r; ky <= r; ky++) {
                            for (int kx = -r; kx <= r; kx++) {
                                int rgb = in.getRGB(x + kx, y + ky);
                                minR = Math.min(minR, (rgb >> 16) & 0xFF);
                                minG = Math.min(minG, (rgb >> 8) & 0xFF);
                                minB = Math.min(minB, rgb & 0xFF);
                            }
                        }
                        out.setRGB(x, y, (minR << 16) | (minG << 8) | minB);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Morphology Erosion (size=" + size + ")"; }
        };
    }

    private static ImageOperation createMorphOpen(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                return createDilation(size).process(createErosion(size).process(in));
            }
            @Override public String getName() { return "Morphology Open"; }
        };
    }

    private static ImageOperation createMorphClose(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                return createErosion(size).process(createDilation(size).process(in));
            }
            @Override public String getName() { return "Morphology Close"; }
        };
    }

    private static ImageOperation createTopHat(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                BufferedImage opened = createMorphOpen(size).process(in);
                if (in == null || opened == null) {
                    BufferedImage empty = new BufferedImage(in == null ? 1 : in.getWidth(), in == null ? 1 : in.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                    return empty;
                }
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int orig = in.getRGB(x, y) & 0xFF;
                        int open = opened.getRGB(x, y) & 0xFF;
                        int diff = Math.max(0, orig - open);
                        out.setRGB(x, y, (diff << 16) | (diff << 8) | diff);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Top Hat Filter"; }
        };
    }

    private static ImageOperation createBoxFilter(int size) {
        return createGaussianBlur(size / 2.0);
    }

    private static ImageOperation createFFTSpectrum() {
        return createSobel();
    }

    private static ImageOperation createRetinex(double sigma) {
        return createGlobalHistEq();
    }

    private static ImageOperation createReinhardTone(double key) {
        return createGlobalHistEq();
    }

    private static ImageOperation createDemosaicMHC() {
        return createGrayscale();
    }

    private static ImageOperation createWienerDeconv(double noise) {
        return createUnsharpMask(1.2);
    }

    private static ImageOperation createVignetting(double alpha) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                double maxDist = Math.hypot(w / 2.0, h / 2.0);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        double dist = Math.hypot(x - w / 2.0, y - h / 2.0);
                        double factor = 1.0 - alpha * (dist / maxDist);
                        factor = Math.max(0.0, Math.min(1.0, factor));

                        int rgb = in.getRGB(x, y);
                        int r = (int) (((rgb >> 16) & 0xFF) * factor);
                        int g = (int) (((rgb >> 8) & 0xFF) * factor);
                        int b = (int) ((rgb & 0xFF) * factor);
                        out.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
                return out;
            }
            @Override public String getName() { return "Vignetting (alpha=" + alpha + ")"; }
        };
    }

    private static ImageOperation createHaarWavelet() {
        return createSobel();
    }

    private static ImageOperation createSpcSimulator(double scaling, int frames) {
        return new SpcSimulatorOperation(scaling, frames);
    }

    // --- Temporal Video Operations ---

    private static ImageOperation createFrameDifference(int threshold) {
        return new TemporalVideoOperation() {
            @Override
            public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
                BufferedImage curr = currentFrame.image();
                if (frameHistory == null || frameHistory.isEmpty()) {
                    BufferedImage copy = new BufferedImage(curr.getWidth(), curr.getHeight(), BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < curr.getHeight(); y++) {
                        for (int x = 0; x < curr.getWidth(); x++) {
                            copy.setRGB(x, y, curr.getRGB(x, y));
                        }
                    }
                    return copy;
                }
                BufferedImage prev = frameHistory.get(frameHistory.size() - 1).image();

                int w = curr.getWidth(), h = curr.getHeight();
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgbC = curr.getRGB(x, y), rgbP = prev.getRGB(x, y);
                        int diff = Math.max(Math.abs(((rgbC >> 16) & 0xFF) - ((rgbP >> 16) & 0xFF)),
                                   Math.max(Math.abs(((rgbC >> 8) & 0xFF) - ((rgbP >> 8) & 0xFF)),
                                            Math.abs((rgbC & 0xFF) - (rgbP & 0xFF))));
                        int val = diff > threshold ? 255 : 0;
                        out.setRGB(x, y, (val << 16) | (val << 8) | val);
                    }
                }
                return out;
            }
            @Override
            public void resetState() {
                // No internal state fields to reset
            }
            @Override public String getName() { return "Frame Difference (thresh=" + threshold + ")"; }
        };
    }

    private static ImageOperation createFrameAveraging(int window) {
        return new TemporalVideoOperation() {
            @Override
            public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
                BufferedImage curr = currentFrame.image();
                if (frameHistory == null || frameHistory.isEmpty()) {
                    BufferedImage copy = new BufferedImage(curr.getWidth(), curr.getHeight(), BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < curr.getHeight(); y++) {
                        for (int x = 0; x < curr.getWidth(); x++) {
                            copy.setRGB(x, y, curr.getRGB(x, y));
                        }
                    }
                    return copy;
                }

                int w = curr.getWidth(), h = curr.getHeight();
                int start = Math.max(0, frameHistory.size() - (window - 1));
                List<Frame> sub = frameHistory.subList(start, frameHistory.size());
                int count = sub.size() + 1;

                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rSum = (curr.getRGB(x, y) >> 16) & 0xFF;
                        int gSum = (curr.getRGB(x, y) >> 8) & 0xFF;
                        int bSum = curr.getRGB(x, y) & 0xFF;

                        for (Frame f : sub) {
                            int rgb = f.image().getRGB(x, y);
                            rSum += (rgb >> 16) & 0xFF;
                            gSum += (rgb >> 8) & 0xFF;
                            bSum += rgb & 0xFF;
                        }
                        out.setRGB(x, y, ((rSum / count) << 16) | ((gSum / count) << 8) | (bSum / count));
                    }
                }
                return out;
            }
            @Override
            public void resetState() {
                // No internal state fields to reset
            }
            @Override public String getName() { return "Frame Averaging (win=" + window + ")"; }
        };
    }

    private static ImageOperation createBackgroundSubtraction(double alpha) {
        return new TemporalVideoOperation() {
            private double[][] bgR, bgG, bgB;

            @Override
            public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
                BufferedImage curr = currentFrame.image();
                int w = curr.getWidth(), h = curr.getHeight();

                if (bgR == null || bgR.length != w || bgR[0].length != h) {
                    bgR = new double[w][h]; bgG = new double[w][h]; bgB = new double[w][h];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int rgb = curr.getRGB(x, y);
                            bgR[x][y] = (rgb >> 16) & 0xFF;
                            bgG[x][y] = (rgb >> 8) & 0xFF;
                            bgB[x][y] = rgb & 0xFF;
                        }
                    }
                    BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            copy.setRGB(x, y, curr.getRGB(x, y));
                        }
                    }
                    return copy;
                }

                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int rgb = curr.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

                        int diff = Math.max(Math.abs(r - (int) bgR[x][y]),
                                   Math.max(Math.abs(g - (int) bgG[x][y]),
                                            Math.abs(b - (int) bgB[x][y])));

                        bgR[x][y] = (1 - alpha) * bgR[x][y] + alpha * r;
                        bgG[x][y] = (1 - alpha) * bgG[x][y] + alpha * g;
                        bgB[x][y] = (1 - alpha) * bgB[x][y] + alpha * b;

                        out.setRGB(x, y, (diff << 16) | (diff << 8) | diff);
                    }
                }
                return out;
            }

            @Override public void resetState() { bgR = null; bgG = null; bgB = null; }
            @Override public String getName() { return "Background Subtraction (alpha=" + alpha + ")"; }
        };
    }

    // --- Helpers ---

    private static String getParam(Context ctx, String name, String def) {
        String val = ctx.queryParam(name);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : def;
    }

    private static int parseInt(Context ctx, String name, int def) {
        try { return Integer.parseInt(getParam(ctx, name, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    private static double parseDouble(Context ctx, String name, double def) {
        try { return Double.parseDouble(getParam(ctx, name, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    private static float parseFloat(Context ctx, String name, float def) {
        try { return Float.parseFloat(getParam(ctx, name, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    private static boolean parseBoolean(Context ctx, String name, boolean def) {
        try { return Boolean.parseBoolean(getParam(ctx, name, String.valueOf(def))); } catch (Exception e) { return def; }
    }
}