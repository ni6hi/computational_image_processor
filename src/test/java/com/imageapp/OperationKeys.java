package com.imageapp;

import java.util.List;

/** Every op key OperationFactory supports; update when adding an operator. */
final class OperationKeys {
    static final List<String> ALL = List.of(
            "grayscale", "invert", "sobel", "global_hist_eq", "clahe", "unsharp_mask", "gaussian_blur",
            "median_filter", "bilateral_filter", "otsu", "sauvola", "resize", "rotate", "flip", "crop",
            "watermark", "morphology_dilation", "morphology_erosion", "morph_open", "morph_close", "top_hat",
            "box_filter", "fft_spectrum", "retinex", "reinhard_tone", "demosaic_mhc", "wiener_deconv",
            "vignetting", "haar_wavelet", "spc", "frame_diff", "frame_avg", "bg_subtraction");

    private OperationKeys() {}
}
