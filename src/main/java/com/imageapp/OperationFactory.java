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
            case "clahe" -> createClahe(parseInt(ctx, "tileSize", 8), parseDouble(ctx, "clipLimit", 2.0));
            case "unsharp_mask" -> createUnsharpMask(parseDouble(ctx, "amount", 1.5), parseDouble(ctx, "sigma", 1.5));
            case "gaussian_blur" -> createGaussianBlur(parseDouble(ctx, "sigma", 2.0));
            case "median_filter" -> createMedianFilter(parseInt(ctx, "radius", 3));
            case "bilateral_filter" -> createBilateralFilter(
                    parseDouble(ctx, "sigmaColor", 75.0), parseDouble(ctx, "sigmaSpace", 75.0));
            case "otsu" -> createOtsu();
            case "sauvola" -> createSauvola(parseInt(ctx, "window", 15), parseDouble(ctx, "k", 0.2));
            case "resize" -> createResize(parseInt(ctx, "width", 256), parseInt(ctx, "height", 256));
            case "rotate" -> createRotate(parseDouble(ctx, "angle", 90.0));
            case "flip" -> createFlip(parseBoolean(ctx, "horizontal", true));
            case "crop" -> createCrop(
                    parseInt(ctx, "x", 0), parseInt(ctx, "y", 0),
                    parseInt(ctx, "width", 256), parseInt(ctx, "height", 256));
            case "watermark" -> createWatermark(getParam(ctx, "text", "CILab"), parseFloat(ctx, "opacity", 0.5f));
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
            case "wiener_deconv" -> createWienerDeconv(parseDouble(ctx, "noise", 0.01), parseDouble(ctx, "sigma", 2.0));
            case "vignetting" -> createVignetting(parseDouble(ctx, "alpha", 0.5));
            case "haar_wavelet" -> createHaarWavelet();
            case "spc" -> createSpcSimulator(parseDouble(ctx, "scaling", 1000.0), parseInt(ctx, "frames", 50));
            case "frame_diff" -> createFrameDifference(parseInt(ctx, "threshold", 30));
            case "frame_avg" -> createFrameAveraging(parseInt(ctx, "window", 5));
            case "bg_subtraction" -> createBackgroundSubtraction(parseDouble(ctx, "alpha", 0.05));
            default -> null;
        };
    }

        /**
         * Create an ImageOperation from a simple op key and params map.
         * This is a convenience for programmatic imports and the GUI.
         */
        public static ImageOperation createFromSpec(String opKey, java.util.Map<String, Object> params) {
        if (opKey == null) return null;
        String key = opKey.toLowerCase().trim();

        return switch (key) {
            case "grayscale" -> createGrayscale();
            case "invert" -> createInvert();
            case "sobel" -> createSobel();
            case "global_hist_eq" -> createGlobalHistEq();
            case "clahe" -> createClahe(
                parseMapInt(params, "tileSize", 8),
                parseMapDouble(params, "clipLimit", 2.0));
            case "unsharp_mask" -> createUnsharpMask(parseMapDouble(params, "amount", 1.5), parseMapDouble(params, "sigma", 1.5));
            case "gaussian_blur" -> createGaussianBlur(parseMapDouble(params, "sigma", 2.0));
            case "median_filter" -> createMedianFilter(parseMapInt(params, "radius", 3));
            case "bilateral_filter" -> createBilateralFilter(
                parseMapDouble(params, "sigmaColor", 75.0),
                parseMapDouble(params, "sigmaSpace", 75.0));
            case "otsu" -> createOtsu();
            case "sauvola" -> createSauvola(
                parseMapInt(params, "window", 15),
                parseMapDouble(params, "k", 0.2));
            case "resize" -> createResize(
                parseMapInt(params, "width", 256),
                parseMapInt(params, "height", 256));
            case "rotate" -> createRotate(parseMapDouble(params, "angle", 90.0));
            case "flip" -> createFlip(parseMapBoolean(params, "horizontal", true));
            case "crop" -> createCrop(
                parseMapInt(params, "x", 0), parseMapInt(params, "y", 0),
                parseMapInt(params, "width", 256), parseMapInt(params, "height", 256));
            case "watermark" -> createWatermark(
                parseMapString(params, "text", "CILab"),
                (float) parseMapDouble(params, "opacity", 0.5));
            case "morphology_dilation" -> createDilation(parseMapInt(params, "size", 3));
            case "morphology_erosion" -> createErosion(parseMapInt(params, "size", 3));
            case "morph_open" -> createMorphOpen(parseMapInt(params, "size", 3));
            case "morph_close" -> createMorphClose(parseMapInt(params, "size", 3));
            case "top_hat" -> createTopHat(parseMapInt(params, "size", 3));
            case "box_filter" -> createBoxFilter(parseMapInt(params, "size", 3));
            case "fft_spectrum" -> createFFTSpectrum();
            case "retinex" -> createRetinex(parseMapDouble(params, "sigma", 15.0));
            case "reinhard_tone" -> createReinhardTone(parseMapDouble(params, "key", 0.18));
            case "demosaic_mhc" -> createDemosaicMHC();
            case "wiener_deconv" -> createWienerDeconv(parseMapDouble(params, "noise", 0.01), parseMapDouble(params, "sigma", 2.0));
            case "vignetting" -> createVignetting(parseMapDouble(params, "alpha", 0.5));
            case "haar_wavelet" -> createHaarWavelet();
            case "spc" -> createSpcSimulator(
                parseMapDouble(params, "scaling", 1000.0),
                parseMapInt(params, "frames", 50));
            case "frame_diff" -> createFrameDifference(parseMapInt(params, "threshold", 30));
            case "frame_avg" -> createFrameAveraging(parseMapInt(params, "window", 5));
            case "bg_subtraction" -> createBackgroundSubtraction(parseMapDouble(params, "alpha", 0.05));
            default -> null;
        };
        }

        private static int parseMapInt(java.util.Map<String, Object> m, String k, int def) {
        if (m == null || !m.containsKey(k)) return def;
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
        }

        private static double parseMapDouble(java.util.Map<String, Object> m, String k, double def) {
        if (m == null || !m.containsKey(k)) return def;
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
        }

        private static boolean parseMapBoolean(java.util.Map<String, Object> m, String k, boolean def) {
        if (m == null || !m.containsKey(k)) return def;
        Object v = m.get(k);
        if (v instanceof Boolean) return (Boolean) v;
        try { return Boolean.parseBoolean(v.toString()); } catch (Exception e) { return def; }
        }

        private static String parseMapString(java.util.Map<String, Object> m, String k, String def) {
        if (m == null || !m.containsKey(k)) return def;
        Object v = m.get(k);
        return v == null ? def : v.toString();
        }

    // =====================================================================
    // Shared helpers
    // =====================================================================

    private static int clampIdx(int v, int n) { return v < 0 ? 0 : (v >= n ? n - 1 : v); }

    /** Mirror index (preserves parity, needed for Bayer demosaicing). */
    private static int mirrorIdx(int v, int n) {
        if (v < 0) v = -v;
        if (v >= n) v = 2 * (n - 1) - v;
        return clampIdx(v, n);
    }

    private static int clamp255(float v) {
        int i = Math.round(v);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    private static int[] pixels(BufferedImage in) {
        return in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
    }

    private static BufferedImage fromPixels(int[] px, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    private static float[][] toPlanes(BufferedImage in) {
        int w = in.getWidth(), h = in.getHeight();
        int[] px = pixels(in);
        float[][] p = new float[3][w * h];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            p[0][i] = (c >> 16) & 0xFF;
            p[1][i] = (c >> 8) & 0xFF;
            p[2][i] = c & 0xFF;
        }
        return p;
    }

    private static BufferedImage fromPlanes(float[][] p, int w, int h) {
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            px[i] = (clamp255(p[0][i]) << 16) | (clamp255(p[1][i]) << 8) | clamp255(p[2][i]);
        }
        return fromPixels(px, w, h);
    }

    private static int[] grayArray(BufferedImage in) {
        int[] px = pixels(in);
        int[] g = new int[px.length];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            g[i] = (int) (0.299 * ((c >> 16) & 0xFF) + 0.587 * ((c >> 8) & 0xFF) + 0.114 * (c & 0xFF));
        }
        return g;
    }

    private static BufferedImage grayToImage(float[] g, int w, int h) {
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            int v = clamp255(g[i]);
            px[i] = (v << 16) | (v << 8) | v;
        }
        return fromPixels(px, w, h);
    }

    private static float[] gaussianBlurPlane(float[] src, int w, int h, double sigma) {
        if (sigma <= 0) return src.clone();
        int r = Math.max(1, (int) Math.ceil(3 * sigma));
        float[] k = new float[2 * r + 1];
        float sum = 0;
        for (int i = -r; i <= r; i++) {
            k[i + r] = (float) Math.exp(-(i * (double) i) / (2 * sigma * sigma));
            sum += k[i + r];
        }
        for (int i = 0; i < k.length; i++) k[i] /= sum;

        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float acc = 0;
                for (int i = -r; i <= r; i++) acc += src[y * w + clampIdx(x + i, w)] * k[i + r];
                tmp[y * w + x] = acc;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float acc = 0;
                for (int i = -r; i <= r; i++) acc += tmp[clampIdx(y + i, h) * w + x] * k[i + r];
                out[y * w + x] = acc;
            }
        }
        return out;
    }

    private static float[] boxBlurPlane(float[] src, int w, int h, int r) {
        float[] tmp = new float[w * h], out = new float[w * h];
        double inv = 1.0 / (2 * r + 1);
        for (int y = 0; y < h; y++) {
            double acc = 0;
            for (int i = -r; i <= r; i++) acc += src[y * w + clampIdx(i, w)];
            for (int x = 0; x < w; x++) {
                tmp[y * w + x] = (float) (acc * inv);
                acc += src[y * w + clampIdx(x + r + 1, w)] - src[y * w + clampIdx(x - r, w)];
            }
        }
        for (int x = 0; x < w; x++) {
            double acc = 0;
            for (int i = -r; i <= r; i++) acc += tmp[clampIdx(i, h) * w + x];
            for (int y = 0; y < h; y++) {
                out[y * w + x] = (float) (acc * inv);
                acc += tmp[clampIdx(y + r + 1, h) * w + x] - tmp[clampIdx(y - r, h) * w + x];
            }
        }
        return out;
    }

    private static float[] morphPlane(float[] src, int w, int h, int r, boolean max) {
        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float best = max ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
                for (int i = -r; i <= r; i++) {
                    float v = src[y * w + clampIdx(x + i, w)];
                    best = max ? Math.max(best, v) : Math.min(best, v);
                }
                tmp[y * w + x] = best;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float best = max ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
                for (int i = -r; i <= r; i++) {
                    float v = tmp[clampIdx(y + i, h) * w + x];
                    best = max ? Math.max(best, v) : Math.min(best, v);
                }
                out[y * w + x] = best;
            }
        }
        return out;
    }

    private static BufferedImage morphImage(BufferedImage in, int size, boolean max) {
        int w = in.getWidth(), h = in.getHeight();
        int r = Math.max(1, size / 2);
        float[][] p = toPlanes(in);
        for (int c = 0; c < 3; c++) p[c] = morphPlane(p[c], w, h, r, max);
        return fromPlanes(p, w, h);
    }

    // ---- FFT (radix-2, in place) ----

    private static int nextPow2(int v) {
        int n = 1;
        while (n < v) n <<= 1;
        return n;
    }

    private static void fft1d(double[] re, double[] im, boolean inverse) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = 2 * Math.PI / len * (inverse ? 1 : -1);
            double wr = Math.cos(ang), wi = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double cr = 1, cim = 0;
                for (int j = 0; j < len / 2; j++) {
                    int a = i + j, b = i + j + len / 2;
                    double xr = re[b] * cr - im[b] * cim;
                    double xi = re[b] * cim + im[b] * cr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr; im[a] += xi;
                    double ncr = cr * wr - cim * wi;
                    cim = cr * wi + cim * wr;
                    cr = ncr;
                }
            }
        }
        if (inverse) {
            for (int i = 0; i < n; i++) { re[i] /= n; im[i] /= n; }
        }
    }

    /** 2D FFT on a row-major W x H array (both powers of two). */
    private static void fft2d(double[] re, double[] im, int W, int H, boolean inverse) {
        double[] rr = new double[W], ri = new double[W];
        for (int y = 0; y < H; y++) {
            System.arraycopy(re, y * W, rr, 0, W);
            System.arraycopy(im, y * W, ri, 0, W);
            fft1d(rr, ri, inverse);
            System.arraycopy(rr, 0, re, y * W, W);
            System.arraycopy(ri, 0, im, y * W, W);
        }
        double[] cr = new double[H], ci = new double[H];
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) { cr[y] = re[y * W + x]; ci[y] = im[y * W + x]; }
            fft1d(cr, ci, inverse);
            for (int y = 0; y < H; y++) { re[y * W + x] = cr[y]; im[y * W + x] = ci[y]; }
        }
    }

    // =====================================================================
    // Basic & geometry
    // =====================================================================

    private static ImageOperation createGrayscale() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] g = grayArray(in);
                int[] px = new int[w * h];
                for (int i = 0; i < px.length; i++) px[i] = (g[i] << 16) | (g[i] << 8) | g[i];
                return fromPixels(px, w, h);
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
                int[] px = pixels(in);
                for (int i = 0; i < px.length; i++) px[i] = ~px[i] & 0xFFFFFF;
                return fromPixels(px, w, h);
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
                int newW = Math.max(1, (int) Math.round(w * cos + h * sin));
                int newH = Math.max(1, (int) Math.round(h * cos + w * sin));
                BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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

                BufferedImage out = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_RGB);
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
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, Math.max(0.0f, opacity))));
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, w / 15)));
                g.drawString(text, w / 10, h / 2);
                g.dispose();
                return out;
            }
            @Override public String getName() { return "Watermark ('" + text + "')"; }
        };
    }

    // =====================================================================
    // Contrast / enhancement
    // =====================================================================

    private static ImageOperation createGlobalHistEq() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int total = w * h;
                int[] px = pixels(in);
                int[] lum = grayArray(in);
                int[] hist = new int[256];
                for (int v : lum) hist[v]++;

                int[] cdf = new int[256];
                int sum = 0, minCdf = -1;
                for (int i = 0; i < 256; i++) {
                    sum += hist[i];
                    cdf[i] = sum;
                    if (minCdf == -1 && cdf[i] > 0) minCdf = cdf[i];
                }
                if (minCdf == -1 || total == minCdf) return fromPixels(px, w, h);

                int[] lut = new int[256];
                for (int i = 0; i < 256; i++) {
                    lut[i] = Math.min(255, Math.max(0, Math.round((float) (cdf[i] - minCdf) / (total - minCdf) * 255.0f)));
                }

                // Equalise luminance, scale RGB by the luminance ratio (keeps hue)
                int[] res = new int[total];
                for (int i = 0; i < total; i++) {
                    int oldL = lum[i], newL = lut[oldL];
                    int c = px[i];
                    if (oldL == 0) { res[i] = (newL << 16) | (newL << 8) | newL; continue; }
                    double s = (double) newL / oldL;
                    int r = Math.min(255, (int) (((c >> 16) & 0xFF) * s));
                    int g = Math.min(255, (int) (((c >> 8) & 0xFF) * s));
                    int b = Math.min(255, (int) ((c & 0xFF) * s));
                    res[i] = (r << 16) | (g << 8) | b;
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "Global Histogram Equalization"; }
        };
    }

    /** CLAHE: tileSize = number of tiles per dimension (grid, like OpenCV's tileGridSize). */
    private static ImageOperation createClahe(int tileSize, double clipLimit) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] px = pixels(in);
                int[] gray = grayArray(in);
                int tilesX = Math.max(1, Math.min(tileSize, w));
                int tilesY = Math.max(1, Math.min(tileSize, h));
                double tw = (double) w / tilesX, th = (double) h / tilesY;

                float[][][] lut = new float[tilesY][tilesX][256];
                for (int ty = 0; ty < tilesY; ty++) {
                    for (int tx = 0; tx < tilesX; tx++) {
                        int x0 = (int) Math.round(tx * tw), x1 = (int) Math.round((tx + 1) * tw);
                        int y0 = (int) Math.round(ty * th), y1 = (int) Math.round((ty + 1) * th);
                        int n = Math.max(1, (x1 - x0) * (y1 - y0));
                        int[] hist = new int[256];
                        for (int y = y0; y < y1; y++)
                            for (int x = x0; x < x1; x++) hist[gray[y * w + x]]++;

                        int clip = Math.max(1, (int) (clipLimit * n / 256.0));
                        long excess = 0;
                        for (int i = 0; i < 256; i++) {
                            if (hist[i] > clip) { excess += hist[i] - clip; hist[i] = clip; }
                        }
                        int add = (int) (excess / 256), rem = (int) (excess % 256);
                        for (int i = 0; i < 256; i++) hist[i] += add;
                        for (int i = 0; i < rem; i++) hist[i * 256 / rem]++;

                        long sum = 0;
                        for (int i = 0; i < 256; i++) {
                            sum += hist[i];
                            lut[ty][tx][i] = (float) sum * 255f / n;
                        }
                    }
                }

                int[] res = new int[w * h];
                for (int y = 0; y < h; y++) {
                    double fy = (y + 0.5) / th - 0.5;
                    int ty0 = (int) Math.floor(fy);
                    float wy = (float) (fy - ty0);
                    int ty1 = clampIdx(ty0 + 1, tilesY);
                    ty0 = clampIdx(ty0, tilesY);
                    for (int x = 0; x < w; x++) {
                        double fx = (x + 0.5) / tw - 0.5;
                        int tx0 = (int) Math.floor(fx);
                        float wx = (float) (fx - tx0);
                        int tx1 = clampIdx(tx0 + 1, tilesX);
                        int txc = clampIdx(tx0, tilesX);

                        int i = y * w + x;
                        int g = gray[i];
                        float top = lut[ty0][txc][g] * (1 - wx) + lut[ty0][tx1][g] * wx;
                        float bot = lut[ty1][txc][g] * (1 - wx) + lut[ty1][tx1][g] * wx;
                        float v = top * (1 - wy) + bot * wy;

                        int c = px[i];
                        if (g == 0) {
                            int vi = clamp255(v);
                            res[i] = (vi << 16) | (vi << 8) | vi;
                        } else {
                            float ratio = v / g;
                            res[i] = (clamp255(((c >> 16) & 0xFF) * ratio) << 16)
                                   | (clamp255(((c >> 8) & 0xFF) * ratio) << 8)
                                   | clamp255((c & 0xFF) * ratio);
                        }
                    }
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "CLAHE (grid=" + tileSize + ", clip=" + clipLimit + ")"; }
        };
    }

    private static ImageOperation createUnsharpMask(double amount, double sigma) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                float[][] p = toPlanes(in);
                for (int c = 0; c < 3; c++) {
                    float[] blur = gaussianBlurPlane(p[c], w, h, sigma);
                    for (int i = 0; i < p[c].length; i++) {
                        p[c][i] = (float) (p[c][i] + amount * (p[c][i] - blur[i]));
                    }
                }
                return fromPlanes(p, w, h);
            }
            @Override public String getName() { return "Unsharp Mask (amount=" + amount + ", sigma=" + sigma + ")"; }
        };
    }

    // =====================================================================
    // Smoothing / denoising
    // =====================================================================

    private static ImageOperation createGaussianBlur(double sigma) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                float[][] p = toPlanes(in);
                for (int c = 0; c < 3; c++) p[c] = gaussianBlurPlane(p[c], w, h, sigma);
                return fromPlanes(p, w, h);
            }
            @Override public String getName() { return "Gaussian Blur (sigma=" + sigma + ")"; }
        };
    }

    private static ImageOperation createBoxFilter(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int r = Math.max(1, size / 2);
                float[][] p = toPlanes(in);
                for (int c = 0; c < 3; c++) p[c] = boxBlurPlane(p[c], w, h, r);
                return fromPlanes(p, w, h);
            }
            @Override public String getName() { return "Box Filter (size=" + (2 * Math.max(1, size / 2) + 1) + ")"; }
        };
    }

    private static ImageOperation createMedianFilter(int radius) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int r = Math.max(1, radius);
                int size = (2 * r + 1) * (2 * r + 1), mid = size / 2;
                int[] px = pixels(in), res = new int[w * h];
                int[] rA = new int[size], gA = new int[size], bA = new int[size];

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int idx = 0;
                        for (int ky = -r; ky <= r; ky++) {
                            int yy = clampIdx(y + ky, h);
                            for (int kx = -r; kx <= r; kx++) {
                                int c = px[yy * w + clampIdx(x + kx, w)];
                                rA[idx] = (c >> 16) & 0xFF;
                                gA[idx] = (c >> 8) & 0xFF;
                                bA[idx] = c & 0xFF;
                                idx++;
                            }
                        }
                        Arrays.sort(rA); Arrays.sort(gA); Arrays.sort(bA);
                        res[y * w + x] = (rA[mid] << 16) | (gA[mid] << 8) | bA[mid];
                    }
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "Median Filter (r=" + radius + ")"; }
        };
    }

    /**
     * Real bilateral filter (spatial Gaussian x colour-distance Gaussian).
     * Window radius is capped at 6 px (13x13) to keep it fast; with large sigmaSpace
     * the spatial term is then almost flat inside that window.
     */
    private static ImageOperation createBilateralFilter(double sigmaColor, double sigmaSpace) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int r = (int) Math.max(1, Math.min(6, Math.ceil(1.5 * sigmaSpace)));
                double ss = Math.max(sigmaSpace, 1e-3), sc = Math.max(sigmaColor, 1e-3);
                int d = 2 * r + 1;

                float[] sw = new float[d * d];
                for (int dy = -r; dy <= r; dy++)
                    for (int dx = -r; dx <= r; dx++)
                        sw[(dy + r) * d + dx + r] = (float) Math.exp(-(dx * dx + dy * dy) / (2 * ss * ss));

                float[] cw = new float[3 * 255 * 255 + 1];
                for (int i = 0; i < cw.length; i++) cw[i] = (float) Math.exp(-i / (2 * sc * sc));

                int[] px = pixels(in), res = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int c0 = px[y * w + x];
                        int r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
                        float aR = 0, aG = 0, aB = 0, wsum = 0;
                        for (int ky = -r; ky <= r; ky++) {
                            int yy = clampIdx(y + ky, h);
                            for (int kx = -r; kx <= r; kx++) {
                                int c = px[yy * w + clampIdx(x + kx, w)];
                                int r1 = (c >> 16) & 0xFF, g1 = (c >> 8) & 0xFF, b1 = c & 0xFF;
                                int dr = r1 - r0, dg = g1 - g0, db = b1 - b0;
                                float wgt = sw[(ky + r) * d + kx + r] * cw[dr * dr + dg * dg + db * db];
                                aR += wgt * r1; aG += wgt * g1; aB += wgt * b1; wsum += wgt;
                            }
                        }
                        res[y * w + x] = (clamp255(aR / wsum) << 16) | (clamp255(aG / wsum) << 8) | clamp255(aB / wsum);
                    }
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "Bilateral Filter (sc=" + sigmaColor + ", ss=" + sigmaSpace + ")"; }
        };
    }

    // =====================================================================
    // Edges / thresholding
    // =====================================================================

    private static ImageOperation createSobel() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] g = grayArray(in);
                float[] mag = new float[w * h];
                for (int y = 0; y < h; y++) {
                    int ym = clampIdx(y - 1, h) * w, y0 = y * w, yp = clampIdx(y + 1, h) * w;
                    for (int x = 0; x < w; x++) {
                        int xm = clampIdx(x - 1, w), xp = clampIdx(x + 1, w);
                        int gx = -g[ym + xm] + g[ym + xp] - 2 * g[y0 + xm] + 2 * g[y0 + xp] - g[yp + xm] + g[yp + xp];
                        int gy = -g[ym + xm] - 2 * g[ym + x] - g[ym + xp] + g[yp + xm] + 2 * g[yp + x] + g[yp + xp];
                        mag[y0 + x] = (float) Math.hypot(gx, gy);
                    }
                }
                return grayToImage(mag, w, h);
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
                int[] gray = grayArray(in);
                long[] hist = new long[256];
                for (int v : gray) hist[v]++;

                double sum = 0;
                for (int t = 0; t < 256; t++) sum += (double) t * hist[t];

                double sumB = 0, varMax = -1;
                long wB = 0;
                int threshold = 128;
                for (int t = 0; t < 256; t++) {
                    wB += hist[t];
                    if (wB == 0) continue;
                    long wF = total - wB;
                    if (wF == 0) break;
                    sumB += (double) t * hist[t];
                    double mB = sumB / wB, mF = (sum - sumB) / wF;
                    double varBetween = (double) wB * wF * (mB - mF) * (mB - mF);
                    if (varBetween > varMax) { varMax = varBetween; threshold = t; }
                }

                int[] res = new int[total];
                for (int i = 0; i < total; i++) {
                    int val = gray[i] > threshold ? 255 : 0;   // class B is <= threshold
                    res[i] = (val << 16) | (val << 8) | val;
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "Otsu Thresholding"; }
        };
    }

    /** Sauvola local threshold: T = m * (1 + k * (s / R - 1)), R = 128, via integral images. */
    private static ImageOperation createSauvola(int window, double k) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] gray = grayArray(in);
                int W1 = w + 1;
                long[] ii = new long[W1 * (h + 1)], iq = new long[W1 * (h + 1)];
                for (int y = 0; y < h; y++) {
                    long rs = 0, rq = 0;
                    for (int x = 0; x < w; x++) {
                        long g = gray[y * w + x];
                        rs += g; rq += g * g;
                        ii[(y + 1) * W1 + x + 1] = ii[y * W1 + x + 1] + rs;
                        iq[(y + 1) * W1 + x + 1] = iq[y * W1 + x + 1] + rq;
                    }
                }
                int half = Math.max(1, window / 2);
                int[] res = new int[w * h];
                for (int y = 0; y < h; y++) {
                    int y0 = Math.max(0, y - half), y1 = Math.min(h, y + half + 1);
                    for (int x = 0; x < w; x++) {
                        int x0 = Math.max(0, x - half), x1 = Math.min(w, x + half + 1);
                        double area = (double) (x1 - x0) * (y1 - y0);
                        double s1 = ii[y1 * W1 + x1] - ii[y0 * W1 + x1] - ii[y1 * W1 + x0] + ii[y0 * W1 + x0];
                        double s2 = iq[y1 * W1 + x1] - iq[y0 * W1 + x1] - iq[y1 * W1 + x0] + iq[y0 * W1 + x0];
                        double mean = s1 / area;
                        double std = Math.sqrt(Math.max(0, s2 / area - mean * mean));
                        double t = mean * (1 + k * (std / 128.0 - 1));
                        int val = gray[y * w + x] > t ? 255 : 0;
                        res[y * w + x] = (val << 16) | (val << 8) | val;
                    }
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "Sauvola Threshold (w=" + window + ", k=" + k + ")"; }
        };
    }

    // =====================================================================
    // Morphology
    // =====================================================================

    private static ImageOperation createDilation(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) { return in == null ? null : morphImage(in, size, true); }
            @Override public String getName() { return "Morphology Dilation (size=" + size + ")"; }
        };
    }

    private static ImageOperation createErosion(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) { return in == null ? null : morphImage(in, size, false); }
            @Override public String getName() { return "Morphology Erosion (size=" + size + ")"; }
        };
    }

    private static ImageOperation createMorphOpen(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                return in == null ? null : morphImage(morphImage(in, size, false), size, true);
            }
            @Override public String getName() { return "Morphology Open (size=" + size + ")"; }
        };
    }

    private static ImageOperation createMorphClose(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                return in == null ? null : morphImage(morphImage(in, size, true), size, false);
            }
            @Override public String getName() { return "Morphology Close (size=" + size + ")"; }
        };
    }

    /** White top-hat = image - opening(image), on luminance. */
    private static ImageOperation createTopHat(int size) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] orig = grayArray(in);
                int[] open = grayArray(morphImage(morphImage(in, size, false), size, true));
                float[] d = new float[w * h];
                for (int i = 0; i < d.length; i++) d[i] = Math.max(0, orig[i] - open[i]);
                return grayToImage(d, w, h);
            }
            @Override public String getName() { return "Top Hat Filter (size=" + size + ")"; }
        };
    }

    // =====================================================================
    // Frequency / scientific operators
    // =====================================================================

    /** Log-magnitude 2D FFT spectrum (Hann-windowed, zero-padded to 2^n, fftshifted). */
    private static ImageOperation createFFTSpectrum() {
        return new ImageOperation() {
            private double hann(int i, int n) {
                return n > 1 ? 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1))) : 1.0;
            }
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int W = nextPow2(w), H = nextPow2(h);
                int[] g = grayArray(in);
                double[] re = new double[W * H], im = new double[W * H];
                for (int y = 0; y < h; y++) {
                    double wy = hann(y, h);
                    for (int x = 0; x < w; x++) re[y * W + x] = g[y * w + x] * wy * hann(x, w);
                }
                fft2d(re, im, W, H, false);

                float[] mag = new float[W * H];
                double max = 1e-9;
                for (int y = 0; y < H; y++) {
                    for (int x = 0; x < W; x++) {
                        double m = Math.log1p(Math.hypot(re[y * W + x], im[y * W + x]));
                        mag[((y + H / 2) % H) * W + (x + W / 2) % W] = (float) m;
                        if (m > max) max = m;
                    }
                }
                for (int i = 0; i < mag.length; i++) mag[i] = (float) (mag[i] / max * 255.0);

                BufferedImage spec = grayToImage(mag, W, H);
                if (W == w && H == h) return spec;
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = out.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(spec, 0, 0, w, h, null);
                g2.dispose();
                return out;
            }
            @Override public String getName() { return "FFT Spectrum (log magnitude)"; }
        };
    }

    /** Single-scale Retinex: log(I) - log(G_sigma * I), then per-channel mean +/- 2 std stretch. */
    private static ImageOperation createRetinex(double sigma) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int n = w * h;
                float[][] p = toPlanes(in);
                for (int c = 0; c < 3; c++) {
                    float[] blur = gaussianBlurPlane(p[c], w, h, sigma);
                    double mean = 0;
                    float[] r = new float[n];
                    for (int i = 0; i < n; i++) {
                        r[i] = (float) (Math.log(p[c][i] + 1.0) - Math.log(blur[i] + 1.0));
                        mean += r[i];
                    }
                    mean /= n;
                    double var = 0;
                    for (int i = 0; i < n; i++) var += (r[i] - mean) * (r[i] - mean);
                    double std = Math.sqrt(var / n);
                    double lo = mean - 2 * std, hi = mean + 2 * std;
                    for (int i = 0; i < n; i++) {
                        p[c][i] = (hi - lo) < 1e-9 ? 128f : (float) ((r[i] - lo) / (hi - lo) * 255.0);
                    }
                }
                return fromPlanes(p, w, h);
            }
            @Override public String getName() { return "Retinex SSR (sigma=" + sigma + ")"; }
        };
    }

    /** Reinhard global tone mapping (extended form with L_white), applied in linear light (gamma 2.2). */
    private static ImageOperation createReinhardTone(double key) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int n = w * h;
                int[] px = pixels(in);
                double[] lin = new double[256];
                for (int i = 0; i < 256; i++) lin[i] = Math.pow(i / 255.0, 2.2);

                double[] lw = new double[n];
                double logSum = 0;
                for (int i = 0; i < n; i++) {
                    int c = px[i];
                    lw[i] = 0.2126 * lin[(c >> 16) & 0xFF] + 0.7152 * lin[(c >> 8) & 0xFF] + 0.0722 * lin[c & 0xFF];
                    logSum += Math.log(1e-4 + lw[i]);
                }
                double scale = key / Math.exp(logSum / n);
                double lmax = 0;
                for (int i = 0; i < n; i++) lmax = Math.max(lmax, lw[i] * scale);
                double lwhite2 = Math.max(lmax * lmax, 1e-6);

                int[] res = new int[n];
                for (int i = 0; i < n; i++) {
                    double lm = lw[i] * scale;
                    double ld = lm * (1 + lm / lwhite2) / (1 + lm);
                    double s = lw[i] > 1e-8 ? ld / lw[i] : 0;
                    int c = px[i];
                    res[i] = (enc(lin[(c >> 16) & 0xFF] * s) << 16)
                           | (enc(lin[(c >> 8) & 0xFF] * s) << 8)
                           | enc(lin[c & 0xFF] * s);
                }
                return fromPixels(res, w, h);
            }
            private int enc(double v) {
                v = Math.max(0, Math.min(1, v));
                return (int) Math.round(Math.pow(v, 1 / 2.2) * 255.0);
            }
            @Override public String getName() { return "Reinhard Tone Mapping (key=" + key + ")"; }
        };
    }

    private static final float[][] MHC_G_AT_RB = {
        {0, 0, -1, 0, 0}, {0, 0, 2, 0, 0}, {-1, 2, 4, 2, -1}, {0, 0, 2, 0, 0}, {0, 0, -1, 0, 0}};
    private static final float[][] MHC_HORIZ = {   // R at G in R row / B at G in B row
        {0, 0, 0.5f, 0, 0}, {0, -1, 0, -1, 0}, {-1, 4, 5, 4, -1}, {0, -1, 0, -1, 0}, {0, 0, 0.5f, 0, 0}};
    private static final float[][] MHC_VERT = {    // R at G in B row / B at G in R row
        {0, 0, -1, 0, 0}, {0, -1, 4, -1, 0}, {0.5f, 0, 5, 0, 0.5f}, {0, -1, 4, -1, 0}, {0, 0, -1, 0, 0}};
    private static final float[][] MHC_DIAG = {    // R at B / B at R
        {0, 0, -1.5f, 0, 0}, {0, 2, 0, 2, 0}, {-1.5f, 0, 6, 0, -1.5f}, {0, 2, 0, 2, 0}, {0, 0, -1.5f, 0, 0}};

    private static float conv5(float[] raw, int w, int h, int x, int y, float[][] k) {
        float s = 0;
        for (int j = -2; j <= 2; j++) {
            int yy = mirrorIdx(y + j, h);
            for (int i = -2; i <= 2; i++) {
                float kv = k[j + 2][i + 2];
                if (kv != 0) s += kv * raw[yy * w + mirrorIdx(x + i, w)];
            }
        }
        return s / 8f;
    }

    /**
     * Malvar-He-Cutler demosaicing. INPUT MUST BE A RAW BAYER MOSAIC (RGGB) stored as a
     * grayscale image; on a normal colour photo the result is meaningless.
     */
    private static ImageOperation createDemosaicMHC() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] px = pixels(in);
                float[] raw = new float[w * h];
                for (int i = 0; i < raw.length; i++) raw[i] = px[i] & 0xFF;

                float[][] p = new float[3][w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = y * w + x;
                        float v = raw[i];
                        boolean evenRow = (y & 1) == 0, evenCol = (x & 1) == 0;
                        if (evenRow && evenCol) {                 // R site
                            p[0][i] = v;
                            p[1][i] = conv5(raw, w, h, x, y, MHC_G_AT_RB);
                            p[2][i] = conv5(raw, w, h, x, y, MHC_DIAG);
                        } else if (evenRow) {                     // G in R row
                            p[1][i] = v;
                            p[0][i] = conv5(raw, w, h, x, y, MHC_HORIZ);
                            p[2][i] = conv5(raw, w, h, x, y, MHC_VERT);
                        } else if (evenCol) {                     // G in B row
                            p[1][i] = v;
                            p[0][i] = conv5(raw, w, h, x, y, MHC_VERT);
                            p[2][i] = conv5(raw, w, h, x, y, MHC_HORIZ);
                        } else {                                  // B site
                            p[2][i] = v;
                            p[1][i] = conv5(raw, w, h, x, y, MHC_G_AT_RB);
                            p[0][i] = conv5(raw, w, h, x, y, MHC_DIAG);
                        }
                    }
                }
                return fromPlanes(p, w, h);
            }
            @Override public String getName() { return "Demosaic (Malvar-He-Cutler, RGGB)"; }
        };
    }

    /**
     * Frequency-domain Wiener deconvolution assuming a Gaussian blur PSF of the given sigma.
     * noise = noise-to-signal power ratio K.
     */
    private static ImageOperation createWienerDeconv(double noise, double sigma) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int W = nextPow2(w), H = nextPow2(h);
                double sg = Math.max(sigma, 0.1), K = Math.max(noise, 1e-8);

                // PSF centred at origin (wrap-around), normalised
                double[] hr = new double[W * H], hi = new double[W * H];
                int R = Math.max(0, (int) Math.min(Math.ceil(3 * sg), Math.min(W, H) / 2 - 1));
                double psum = 0;
                for (int dy = -R; dy <= R; dy++) {
                    for (int dx = -R; dx <= R; dx++) {
                        double v = Math.exp(-(dx * dx + dy * dy) / (2 * sg * sg));
                        hr[((dy + H) % H) * W + (dx + W) % W] += v;
                        psum += v;
                    }
                }
                for (int i = 0; i < hr.length; i++) hr[i] /= psum;
                fft2d(hr, hi, W, H, false);

                float[][] p = toPlanes(in);
                for (int c = 0; c < 3; c++) {
                    double[] re = new double[W * H], im = new double[W * H];
                    for (int y = 0; y < H; y++) {
                        int sy = clampIdx(y, h);
                        for (int x = 0; x < W; x++) re[y * W + x] = p[c][sy * w + clampIdx(x, w)];
                    }
                    fft2d(re, im, W, H, false);
                    for (int i = 0; i < re.length; i++) {
                        double den = hr[i] * hr[i] + hi[i] * hi[i] + K;
                        double yr = re[i], yi = im[i];
                        re[i] = (hr[i] * yr + hi[i] * yi) / den;
                        im[i] = (hr[i] * yi - hi[i] * yr) / den;
                    }
                    fft2d(re, im, W, H, true);
                    for (int y = 0; y < h; y++)
                        for (int x = 0; x < w; x++) p[c][y * w + x] = (float) re[y * W + x];
                }
                return fromPlanes(p, w, h);
            }
            @Override public String getName() { return "Wiener Deconvolution (K=" + noise + ", sigma=" + sigma + ")"; }
        };
    }

    /** Radial vignette: gain = 1 - alpha * (r / r_max)^2. */
    private static ImageOperation createVignetting(double alpha) {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] px = pixels(in), res = new int[w * h];
                double maxDist = Math.hypot(w / 2.0, h / 2.0);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        double r = Math.hypot(x - w / 2.0, y - h / 2.0) / maxDist;
                        double f = Math.max(0.0, Math.min(1.0, 1.0 - alpha * r * r));
                        int c = px[y * w + x];
                        res[y * w + x] = ((int) (((c >> 16) & 0xFF) * f) << 16)
                                       | ((int) (((c >> 8) & 0xFF) * f) << 8)
                                       | (int) ((c & 0xFF) * f);
                    }
                }
                return fromPixels(res, w, h);
            }
            @Override public String getName() { return "Vignetting (alpha=" + alpha + ")"; }
        };
    }

    /** One-level 2D Haar DWT, shown in the standard quadrant layout (LL | HL / LH | HH). */
    private static ImageOperation createHaarWavelet() {
        return new ImageOperation() {
            @Override
            public BufferedImage process(BufferedImage in) {
                if (in == null) return null;
                int w = in.getWidth(), h = in.getHeight();
                int[] g = grayArray(in);
                int w2 = w / 2, h2 = h / 2;
                float[] out = new float[w * h];
                for (int y = 0; y < h2; y++) {
                    for (int x = 0; x < w2; x++) {
                        float a = g[2 * y * w + 2 * x], b = g[2 * y * w + 2 * x + 1];
                        float c = g[(2 * y + 1) * w + 2 * x], d = g[(2 * y + 1) * w + 2 * x + 1];
                        out[y * w + x] = (a + b + c + d) / 4f;                              // LL
                        out[y * w + x + w2] = Math.abs(a - b + c - d) / 4f * 4f;            // HL
                        out[(y + h2) * w + x] = Math.abs(a + b - c - d) / 4f * 4f;          // LH
                        out[(y + h2) * w + x + w2] = Math.abs(a - b - c + d) / 4f * 4f;     // HH
                    }
                }
                return grayToImage(out, w, h);
            }
            @Override public String getName() { return "Haar Wavelet (1 level)"; }
        };
    }

    private static ImageOperation createSpcSimulator(double scaling, int frames) {
        return new SpcSimulatorOperation(scaling, frames);
    }

    // =====================================================================
    // Temporal video operations (logic unchanged, verified correct)
    // =====================================================================

    private static BufferedImage copyRgb(BufferedImage src) {
        return fromPixels(pixels(src), src.getWidth(), src.getHeight());
    }

    private static ImageOperation createFrameDifference(int threshold) {
        return new TemporalVideoOperation() {
            @Override
            public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
                BufferedImage curr = currentFrame.image();
                if (frameHistory == null || frameHistory.isEmpty()) return copyRgb(curr);
                BufferedImage prev = frameHistory.get(frameHistory.size() - 1).image();

                int w = curr.getWidth(), h = curr.getHeight();
                int[] a = pixels(curr), b = pixels(prev), res = new int[w * h];
                for (int i = 0; i < res.length; i++) {
                    int diff = Math.max(Math.abs(((a[i] >> 16) & 0xFF) - ((b[i] >> 16) & 0xFF)),
                               Math.max(Math.abs(((a[i] >> 8) & 0xFF) - ((b[i] >> 8) & 0xFF)),
                                        Math.abs((a[i] & 0xFF) - (b[i] & 0xFF))));
                    int val = diff > threshold ? 255 : 0;
                    res[i] = (val << 16) | (val << 8) | val;
                }
                return fromPixels(res, w, h);
            }
            @Override public void resetState() { }
            @Override public String getName() { return "Frame Difference (thresh=" + threshold + ")"; }
        };
    }

    private static ImageOperation createFrameAveraging(int window) {
        return new TemporalVideoOperation() {
            @Override
            public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
                BufferedImage curr = currentFrame.image();
                if (frameHistory == null || frameHistory.isEmpty()) return copyRgb(curr);

                int w = curr.getWidth(), h = curr.getHeight();
                int start = Math.max(0, frameHistory.size() - Math.max(0, window - 1));
                List<Frame> sub = frameHistory.subList(start, frameHistory.size());
                int count = sub.size() + 1;

                int[] c0 = pixels(curr);
                int[] rS = new int[w * h], gS = new int[w * h], bS = new int[w * h];
                for (int i = 0; i < c0.length; i++) {
                    rS[i] = (c0[i] >> 16) & 0xFF; gS[i] = (c0[i] >> 8) & 0xFF; bS[i] = c0[i] & 0xFF;
                }
                for (Frame f : sub) {
                    int[] p = pixels(f.image());
                    for (int i = 0; i < p.length; i++) {
                        rS[i] += (p[i] >> 16) & 0xFF; gS[i] += (p[i] >> 8) & 0xFF; bS[i] += p[i] & 0xFF;
                    }
                }
                int[] res = new int[w * h];
                for (int i = 0; i < res.length; i++) {
                    res[i] = ((rS[i] / count) << 16) | ((gS[i] / count) << 8) | (bS[i] / count);
                }
                return fromPixels(res, w, h);
            }
            @Override public void resetState() { }
            @Override public String getName() { return "Frame Averaging (win=" + window + ")"; }
        };
    }

    private static ImageOperation createBackgroundSubtraction(double alpha) {
        return new TemporalVideoOperation() {
            private double[] bgR, bgG, bgB;

            @Override
            public BufferedImage processTemporal(Frame currentFrame, List<Frame> frameHistory) {
                BufferedImage curr = currentFrame.image();
                int w = curr.getWidth(), h = curr.getHeight();
                int[] px = pixels(curr);

                if (bgR == null || bgR.length != w * h) {
                    bgR = new double[w * h]; bgG = new double[w * h]; bgB = new double[w * h];
                    for (int i = 0; i < px.length; i++) {
                        bgR[i] = (px[i] >> 16) & 0xFF; bgG[i] = (px[i] >> 8) & 0xFF; bgB[i] = px[i] & 0xFF;
                    }
                    return fromPixels(new int[w * h], w, h);   // first frame: no foreground yet
                }

                int[] res = new int[w * h];
                for (int i = 0; i < px.length; i++) {
                    int r = (px[i] >> 16) & 0xFF, g = (px[i] >> 8) & 0xFF, b = px[i] & 0xFF;
                    int diff = (int) Math.min(255, Math.max(Math.abs(r - bgR[i]),
                               Math.max(Math.abs(g - bgG[i]), Math.abs(b - bgB[i]))));
                    bgR[i] = (1 - alpha) * bgR[i] + alpha * r;
                    bgG[i] = (1 - alpha) * bgG[i] + alpha * g;
                    bgB[i] = (1 - alpha) * bgB[i] + alpha * b;
                    res[i] = (diff << 16) | (diff << 8) | diff;
                }
                return fromPixels(res, w, h);
            }

            @Override public void resetState() { bgR = null; bgG = null; bgB = null; }
            @Override public String getName() { return "Background Subtraction (alpha=" + alpha + ")"; }
        };
    }

    // =====================================================================
    // Param helpers
    // =====================================================================

    private static String getParam(Context ctx, String name, String def) {
        String val = ctx == null ? null : ctx.queryParam(name);
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