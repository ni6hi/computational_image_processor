package com.imageapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ImageProcessorGUI extends JFrame {

    private enum MorphType { EROSION, DILATION }
    private final ImagePipeline pipeline = new ImagePipeline();
    private BufferedImage currentOriginalImage;
    private BufferedImage currentPreviewImage;

    private final DefaultListModel<String> pipelineListModel = new DefaultListModel<>();
    private final JList<String> pipelineJList = new JList<>(pipelineListModel);
    private final JLabel previewLabel = new JLabel("Load an image to preview results", SwingConstants.CENTER);
    private final JProgressBar progressBar = new JProgressBar();

    public ImageProcessorGUI() {
        setTitle("Laboratory Image Processing & Analysis Tool");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // --- Left Control Panel ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Scientific Operations"));
        leftPanel.setPreferredSize(new Dimension(340, 0));

        JPanel addOpsPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        // Buttons
        JButton btnGrayscale = new JButton("Grayscale");
        JButton btnRotate = new JButton("Rotate...");
        JButton btnFlip = new JButton("Flip Horizontal");
        JButton btnCrop = new JButton("Crop Center");
        JButton btnInvert = new JButton("Invert Colors");

        JButton btnGaussian = new JButton("Gaussian Blur...");
        JButton btnMedian = new JButton("Median Filter...");
        JButton btnBilateral = new JButton("Bilateral Filter...");

        JButton btnGHE = new JButton("Global Hist Eq");
        JButton btnCLAHE = new JButton("CLAHE...");
        JButton btnUSM = new JButton("Unsharp Mask...");

        JButton btnSobel = new JButton("Sobel Edge");
        JButton btnOtsu = new JButton("Otsu Binarize");
        JButton btnSauvola = new JButton("Sauvola Binarize...");
        JButton btnMorphology = new JButton("Morphology...");
        JButton btnWatermark = new JButton("Watermark...");

        JButton btnRemoveOp = new JButton("Remove Selected Step");
        btnRemoveOp.setBackground(new Color(220, 100, 100));

        addOpsPanel.add(btnGrayscale);
        addOpsPanel.add(btnRotate);
        addOpsPanel.add(btnFlip);
        addOpsPanel.add(btnCrop);
        addOpsPanel.add(btnInvert);
        addOpsPanel.add(btnGaussian);
        addOpsPanel.add(btnMedian);
        addOpsPanel.add(btnBilateral);
        addOpsPanel.add(btnGHE);
        addOpsPanel.add(btnCLAHE);
        addOpsPanel.add(btnUSM);
        addOpsPanel.add(btnSobel);
        addOpsPanel.add(btnOtsu);
        addOpsPanel.add(btnSauvola);
        addOpsPanel.add(btnMorphology);
        addOpsPanel.add(btnWatermark);

        // --- Action Listeners with Hyperparameter Sliders ---
        btnGrayscale.addActionListener(e -> addOp("grayscale", Map.of()));
        btnFlip.addActionListener(e -> addOp("flip", Map.of("horizontal", true)));
        btnInvert.addActionListener(e -> addOp("invert", Map.of()));
        btnGHE.addActionListener(e -> addOp("global_hist_eq", Map.of()));
        btnSobel.addActionListener(e -> addOp("sobel", Map.of()));
        btnOtsu.addActionListener(e -> addOp("otsu", Map.of()));

        btnRotate.addActionListener(e -> {
            JSlider slider = createSlider(0, 360, 90, 90, 180);
            if (showSliderDialog("Rotate Image", "Angle (Degrees):", slider)) {
                addOp("rotate", Map.of("angle", slider.getValue()));
            }
        });

        btnCrop.addActionListener(e -> {
            if (currentOriginalImage != null) {
                int w = currentOriginalImage.getWidth();
                int h = currentOriginalImage.getHeight();
                addOp("crop", Map.of("x", w / 4, "y", h / 4, "width", w / 2, "height", h / 2));
            } else {
                addOp("crop", Map.of("x", 50, "y", 50, "width", 200, "height", 200));
            }
        });

        btnGaussian.addActionListener(e -> {
            JSlider slider = createSlider(1, 100, 20, 20, 50); // Represents 0.1 to 10.0
            if (showSliderDialog("Gaussian Blur", "Sigma (σ):", slider, 10.0f)) {
                addOp("gaussian_blur", Map.of("sigma", slider.getValue() / 10.0));
            }
        });

        btnMedian.addActionListener(e -> {
            JSlider slider = createSlider(1, 10, 1, 1, 2);
            if (showSliderDialog("Median Filter", "Window Radius (px):", slider)) {
                addOp("median_filter", Map.of("radius", slider.getValue()));
            }
        });

        btnBilateral.addActionListener(e -> {
            JSlider spatialSlider = createSlider(1, 20, 3, 5, 10);
            JSlider rangeSlider = createSlider(5, 100, 20, 20, 50);
            JPanel panel = new JPanel(new GridLayout(4, 1));
            JLabel lbl1 = new JLabel("Spatial Sigma: " + spatialSlider.getValue());
            JLabel lbl2 = new JLabel("Range Sigma: " + rangeSlider.getValue());

            spatialSlider.addChangeListener(ce -> lbl1.setText("Spatial Sigma: " + spatialSlider.getValue()));
            rangeSlider.addChangeListener(ce -> lbl2.setText("Range Sigma: " + rangeSlider.getValue()));

            panel.add(lbl1); panel.add(spatialSlider);
            panel.add(lbl2); panel.add(rangeSlider);

            if (JOptionPane.showConfirmDialog(this, panel, "Bilateral Filter Parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                addOp("bilateral_filter", Map.of("sigmaSpace", spatialSlider.getValue(), "sigmaColor", rangeSlider.getValue()));
            }
        });

        btnCLAHE.addActionListener(e -> {
            JSlider tileSlider = createSlider(4, 64, 16, 12, 24);
            JSlider clipSlider = createSlider(10, 100, 20, 20, 50); // 1.0 to 10.0
            JPanel panel = new JPanel(new GridLayout(4, 1));
            JLabel lbl1 = new JLabel("Tile Size: " + tileSlider.getValue() + "x" + tileSlider.getValue());
            JLabel lbl2 = new JLabel("Clip Limit: " + (clipSlider.getValue() / 10.0f));

            tileSlider.addChangeListener(ce -> lbl1.setText("Tile Size: " + tileSlider.getValue() + "x" + tileSlider.getValue()));
            clipSlider.addChangeListener(ce -> lbl2.setText("Clip Limit: " + (clipSlider.getValue() / 10.0f)));

            panel.add(lbl1); panel.add(tileSlider);
            panel.add(lbl2); panel.add(clipSlider);

            if (JOptionPane.showConfirmDialog(this, panel, "CLAHE Parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                addOp("clahe", Map.of("tileSize", tileSlider.getValue(), "clipLimit", clipSlider.getValue() / 10.0));
            }
        });

        btnUSM.addActionListener(e -> {
            JSlider slider = createSlider(1, 50, 10, 10, 20); // 0.1 to 5.0
            if (showSliderDialog("Unsharp Masking", "Sharpening Amount:", slider, 10.0f)) {
                addOp("unsharp_mask", Map.of("amount", slider.getValue() / 10.0));
            }
        });

        btnSauvola.addActionListener(e -> {
            JSlider windowSlider = createSlider(3, 51, 15, 12, 24);
            JSlider kSlider = createSlider(1, 100, 20, 20, 50); // 0.01 to 1.0
            JPanel panel = new JPanel(new GridLayout(4, 1));
            JLabel lbl1 = new JLabel("Window Size: " + windowSlider.getValue());
            JLabel lbl2 = new JLabel("k Factor: " + (kSlider.getValue() / 100.0f));

            windowSlider.addChangeListener(ce -> lbl1.setText("Window Size: " + windowSlider.getValue()));
            kSlider.addChangeListener(ce -> lbl2.setText("k Factor: " + (kSlider.getValue() / 100.0f)));

            panel.add(lbl1); panel.add(windowSlider);
            panel.add(lbl2); panel.add(kSlider);

            if (JOptionPane.showConfirmDialog(this, panel, "Sauvola Binarization Parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                addOp("sauvola", Map.of("window", windowSlider.getValue(), "k", kSlider.getValue() / 100.0));
            }
        });

        btnMorphology.addActionListener(e -> {
            JComboBox<MorphType> typeCombo = new JComboBox<>(MorphType.values());
            JSlider radiusSlider = createSlider(1, 10, 1, 1, 2);
            JPanel panel = new JPanel(new GridLayout(3, 1));
            JLabel lbl = new JLabel("Radius (px): " + radiusSlider.getValue());

            radiusSlider.addChangeListener(ce -> lbl.setText("Radius (px): " + radiusSlider.getValue()));

            panel.add(new JLabel("Morphology Operation Type:"));
            panel.add(typeCombo);
            panel.add(lbl);
            panel.add(radiusSlider);

            if (JOptionPane.showConfirmDialog(this, panel, "Morphology Parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                // Factory ops take a window size and use radius = size / 2, so radius r maps to size 2r + 1
                String key = typeCombo.getSelectedItem() == MorphType.EROSION ? "morphology_erosion" : "morphology_dilation";
                addOp(key, Map.of("size", 2 * radiusSlider.getValue() + 1));
            }
        });

        btnWatermark.addActionListener(e -> {
            JTextField textInput = new JTextField("Lab Sample");
            JSlider opacitySlider = createSlider(1, 100, 60, 20, 50); // 0.01 to 1.0
            JPanel panel = new JPanel(new GridLayout(4, 1));
            JLabel lbl = new JLabel("Opacity: " + (opacitySlider.getValue() / 100.0f));

            opacitySlider.addChangeListener(ce -> lbl.setText("Opacity: " + (opacitySlider.getValue() / 100.0f)));

            panel.add(new JLabel("Watermark Text:"));
            panel.add(textInput);
            panel.add(lbl);
            panel.add(opacitySlider);

            if (JOptionPane.showConfirmDialog(this, panel, "Watermark Parameters", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (!textInput.getText().isBlank()) {
                    addOp("watermark", Map.of("text", textInput.getText(), "opacity", opacitySlider.getValue() / 100.0));
                }
            }
        });

        btnRemoveOp.addActionListener(e -> {
            int idx = pipelineJList.getSelectedIndex();
            if (idx >= 0) {
                pipeline.removeOperation(idx);
                pipelineListModel.remove(idx);
                updatePreview();
            }
        });

        leftPanel.add(new JScrollPane(addOpsPanel), BorderLayout.CENTER);

        JPanel bottomListPanel = new JPanel(new BorderLayout());
        bottomListPanel.setBorder(BorderFactory.createTitledBorder("Active Pipeline Sequence"));
        bottomListPanel.setPreferredSize(new Dimension(340, 200));
        bottomListPanel.add(new JScrollPane(pipelineJList), BorderLayout.CENTER);
        bottomListPanel.add(btnRemoveOp, BorderLayout.SOUTH);

        leftPanel.add(bottomListPanel, BorderLayout.SOUTH);

        // --- Center Preview Panel ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Live Image Preview"));
        previewLabel.setBackground(Color.DARK_GRAY);
        previewLabel.setOpaque(true);
        previewLabel.setForeground(Color.WHITE);
        centerPanel.add(new JScrollPane(previewLabel), BorderLayout.CENTER);

        // --- Top Bar Controls ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnLoadSingle = new JButton("Load Preview Image");
        JButton btnClearPipeline = new JButton("Clear Pipeline");
        JButton btnSavePipeline = new JButton("Save Pipeline...");
        JButton btnLoadPipeline = new JButton("Load Pipeline...");

        btnLoadSingle.addActionListener(e -> loadPreviewImage());
        btnClearPipeline.addActionListener(e -> {
            pipeline.clear();
            pipelineListModel.clear();
            updatePreview();
        });

        btnSavePipeline.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Pipeline JSON");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File out = chooser.getSelectedFile();
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    om.writerWithDefaultPrettyPrinter().writeValue(out, pipeline.toConfig());
                    JOptionPane.showMessageDialog(this, "Pipeline saved to " + out.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to save pipeline: " + ex.getMessage());
                }
            }
        });

        btnLoadPipeline.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Load Pipeline JSON");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File in = chooser.getSelectedFile();
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    PipelineConfig cfg = om.readValue(in, PipelineConfig.class);
                    java.util.List<String> warnings = pipeline.loadFromConfig(cfg);
                    pipelineListModel.clear();
                    for (ImageOperation op : pipeline.getOperations()) pipelineListModel.addElement(op.getName());
                    updatePreview();
                    if (warnings.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Pipeline loaded from " + in.getAbsolutePath());
                    } else {
                        JOptionPane.showMessageDialog(this, "Pipeline loaded from " + in.getAbsolutePath()
                                + "\n\nSome steps were skipped:\n" + String.join("\n", warnings),
                                "Pipeline loaded with warnings", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load pipeline: " + ex.getMessage());
                }
            }
        });

        topPanel.add(btnLoadSingle);
        topPanel.add(btnClearPipeline);
        topPanel.add(btnSavePipeline);
        topPanel.add(btnLoadPipeline);

        // --- Bottom Batch Processor Panel ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Batch Processing Engine"));

        JButton btnRunBatch = new JButton("Start Folder Processing");
        btnRunBatch.addActionListener(e -> runBatchProcessing());

        progressBar.setStringPainted(true);

        bottomPanel.add(btnRunBatch, BorderLayout.WEST);
        bottomPanel.add(progressBar, BorderLayout.CENTER);

        // --- Main Frame Assembly ---
        add(topPanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- Helper UI Builder Methods for Sliders ---

    private JSlider createSlider(int min, int max, int value, int minorTick, int majorTick) {
        JSlider slider = new JSlider(min, max, value);
        slider.setMinorTickSpacing(minorTick);
        slider.setMajorTickSpacing(majorTick);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private boolean showSliderDialog(String title, String labelText, JSlider slider) {
        return showSliderDialog(title, labelText, slider, 1.0f);
    }

    private boolean showSliderDialog(String title, String labelText, JSlider slider, float divisor) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel valueLabel = new JLabel(labelText + " " + (slider.getValue() / divisor));

        slider.addChangeListener(e -> valueLabel.setText(labelText + " " + (slider.getValue() / divisor)));

        panel.add(valueLabel, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION);
        return result == JOptionPane.OK_OPTION;
    }

    // All GUI operations come from OperationFactory, the same implementations the web UI and batch runner use
    private void addOp(String key, Map<String, Object> params) {
        addOperation(OperationFactory.createFromSpec(key, params));
    }

    private void addOperation(ImageOperation op) {
        pipeline.addOperation(op);
        pipelineListModel.addElement(op.getName());
        updatePreview();
    }

    private void loadPreviewImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                currentOriginalImage = ImageIO.read(chooser.getSelectedFile());
                updatePreview();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image: " + ex.getMessage());
            }
        }
    }

    private void updatePreview() {
        if (currentOriginalImage == null) return;

        currentPreviewImage = pipeline.execute(currentOriginalImage);

        int maxWidth = previewLabel.getWidth() > 0 ? previewLabel.getWidth() : 600;
        int maxHeight = previewLabel.getHeight() > 0 ? previewLabel.getHeight() : 500;

        double scale = Math.min((double) maxWidth / currentPreviewImage.getWidth(), (double) maxHeight / currentPreviewImage.getHeight());
        scale = Math.min(scale, 1.0);

        int previewW = (int) (currentPreviewImage.getWidth() * scale);
        int previewH = (int) (currentPreviewImage.getHeight() * scale);

        Image scaled = currentPreviewImage.getScaledInstance(previewW, previewH, Image.SCALE_SMOOTH);
        previewLabel.setIcon(new ImageIcon(scaled));
        previewLabel.setText("");
    }

    private void runBatchProcessing() {
        if (pipeline.getOperations().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one operation to the pipeline first.");
            return;
        }

        JFileChooser srcChooser = new JFileChooser();
        srcChooser.setDialogTitle("Select Input Directory");
        srcChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (srcChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File inputDir = srcChooser.getSelectedFile();

        JFileChooser destChooser = new JFileChooser();
        destChooser.setDialogTitle("Select Output Directory");
        destChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (destChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File outputDir = destChooser.getSelectedFile();

        BatchProcessor.processDirectory(inputDir, outputDir, pipeline, "png", new BatchProcessor.ProgressListener() {
            @Override
            public void onProgress(int completed, int total, String currentFile) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setMaximum(total);
                    progressBar.setValue(completed);
                    progressBar.setString("Processing: " + completed + "/" + total + " (" + currentFile + ")");
                });
            }

            @Override
            public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("Batch process completed!");
                    JOptionPane.showMessageDialog(ImageProcessorGUI.this, "Batch processing complete!");
                });
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageProcessorGUI().setVisible(true));
    }
}