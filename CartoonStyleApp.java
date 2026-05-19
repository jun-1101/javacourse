import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Traditional Image Processing Cartoon Style App
 * 單一 Java 檔案版本，可直接在 VS Code 執行。
 *
 * 功能：
 * 1. Java Swing GUI
 * 2. 載入圖片
 * 3. Gaussian Blur 平滑化
 * 4. Sobel Edge Detection 邊緣偵測
 * 5. Adaptive Thresholding 自適應門檻
 * 6. Color Quantization 色彩量化
 * 7. 即時預覽 cartoon filter
 * 8. 匯出處理後圖片
 * 9. 顯示處理流程圖片（Original -> Edge -> Gray -> Quantized -> Cartoon）
 */
public class CartoonStyleApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CartoonFrame frame = new CartoonFrame();
            frame.setVisible(true);
        });
    }
}

class CartoonFrame extends JFrame {
    private final ImageLoader imageLoader = new ImageLoader();
    private final CartoonRenderer renderer = new CartoonRenderer(
            new GaussianBlur(),
            new GrayConverter(),
            new EdgeDetector(),
            new ColorQuantizer()
    );

    private BufferedImage originalImage;
    private PipelineResult pipelineResult;

    private final ImagePanel originalPanel = new ImagePanel("Original Image");
    private final ImagePanel resultPanel = new ImagePanel("Cartoon Result");

    private final StagePanel stageOriginalPanel = new StagePanel("1. Original");
    private final StagePanel stageEdgePanel = new StagePanel("2. Edge");
    private final StagePanel stageGrayPanel = new StagePanel("3. Gray");
    private final StagePanel stageQuantizedPanel = new StagePanel("4. Quantized");
    private final StagePanel stageCartoonPanel = new StagePanel("5. Cartoon");

    private final JButton loadButton = new JButton("Load Image");
    private final JButton saveButton = new JButton("Save Result");
    private final JButton resetButton = new JButton("Reset");

    private final JSlider colorSlider = new JSlider(2, 12, 6);
    private final JSlider edgeSlider = new JSlider(20, 160, 85);
    private final JSlider blurSlider = new JSlider(0, 4, 1);

    private final JLabel statusLabel = new JLabel("Please load an image first.");

    private final Timer previewTimer;
    private SwingWorker<PipelineResult, Void> worker;
    private int renderVersion = 0;

    public CartoonFrame() {
        super("Traditional Cartoon Filter App - Java OOP Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        saveButton.setEnabled(false);
        resetButton.setEnabled(false);

        previewTimer = new Timer(350, e -> renderPreview());
        previewTimer.setRepeats(false);

        setLayout(new BorderLayout(10, 10));
        add(createTopPanel(), BorderLayout.NORTH);
        add(createMainCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setupEvents();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel titleLabel = new JLabel("Traditional Image Processing Cartoon Style App");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createMainCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        center.add(createMainPreviewPanel(), BorderLayout.CENTER);
        center.add(createPipelinePanel(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel createMainPreviewPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(originalPanel);
        panel.add(resultPanel);
        return panel;
    }

    private JPanel createPipelinePanel() {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setBorder(BorderFactory.createTitledBorder("Traditional Cartoon Processing Pipeline"));

        JPanel stages = new JPanel(new GridLayout(1, 5, 10, 10));
        stages.add(stageOriginalPanel);
        stages.add(stageEdgePanel);
        stages.add(stageGrayPanel);
        stages.add(stageQuantizedPanel);
        stages.add(stageCartoonPanel);

        JLabel flowLabel = new JLabel(
                "Original  →  Edge Detection  →  Grayscale  →  Color Quantization  →  Final Cartoon",
                SwingConstants.CENTER);
        flowLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        container.add(flowLabel, BorderLayout.NORTH);
        container.add(stages, BorderLayout.CENTER);
        return container;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel controls = new JPanel(new GridLayout(3, 1, 6, 6));
        controls.add(createSliderRow("Color Quantization Levels", colorSlider));
        controls.add(createSliderRow("Edge Threshold", edgeSlider));
        controls.add(createSliderRow("Gaussian Blur Strength", blurSlider));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        bottomPanel.add(controls, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        return bottomPanel;
    }

    private JPanel createSliderRow(String name, JSlider slider) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        JLabel label = new JLabel(name + ": " + slider.getValue());
        label.setPreferredSize(new Dimension(230, 25));

        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(Math.max(1, (slider.getMaximum() - slider.getMinimum()) / 4));
        slider.setMinorTickSpacing(1);

        slider.addChangeListener(e -> {
            label.setText(name + ": " + slider.getValue());
            schedulePreview();
        });

        row.add(label, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        return row;
    }

    private void setupEvents() {
        loadButton.addActionListener(e -> loadImage());
        saveButton.addActionListener(e -> saveResult());
        resetButton.addActionListener(e -> resetImage());
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose an image file");
        int option = chooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                originalImage = imageLoader.load(file);
                pipelineResult = null;

                originalPanel.setImage(originalImage);
                resultPanel.setImage(null);
                clearPipelineStages();
                stageOriginalPanel.setImage(originalImage);

                saveButton.setEnabled(false);
                resetButton.setEnabled(true);
                statusLabel.setText("Loaded: " + file.getName());

                schedulePreview();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Cannot load image: " + ex.getMessage(),
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveResult() {
        if (pipelineResult == null || pipelineResult.cartoon == null) {
            JOptionPane.showMessageDialog(this, "No result to save yet.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save cartoon result");
        chooser.setSelectedFile(new File("cartoon_result.png"));
        int option = chooser.showSaveDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                String path = file.getAbsolutePath().toLowerCase();
                if (!path.endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                imageLoader.save(pipelineResult.cartoon, file);
                statusLabel.setText("Saved result: " + file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Cannot save image: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resetImage() {
        pipelineResult = null;
        resultPanel.setImage(null);
        clearPipelineStages();
        if (originalImage != null) {
            stageOriginalPanel.setImage(originalImage);
        }
        saveButton.setEnabled(false);
        statusLabel.setText("Result cleared. Adjust sliders or load another image.");
    }

    private void clearPipelineStages() {
        stageOriginalPanel.setImage(null);
        stageEdgePanel.setImage(null);
        stageGrayPanel.setImage(null);
        stageQuantizedPanel.setImage(null);
        stageCartoonPanel.setImage(null);
    }

    private void schedulePreview() {
        if (originalImage == null) {
            return;
        }
        previewTimer.restart();
    }

    private void renderPreview() {
        if (originalImage == null) {
            return;
        }

        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }

        final int currentVersion = ++renderVersion;
        final int colorLevels = colorSlider.getValue();
        final int edgeThreshold = edgeSlider.getValue();
        final int blurStrength = blurSlider.getValue();

        setControlsEnabled(false);
        statusLabel.setText("Processing cartoon filter and pipeline preview...");

        worker = new SwingWorker<>() {
            @Override
            protected PipelineResult doInBackground() {
                return renderer.renderPipeline(originalImage, colorLevels, edgeThreshold, blurStrength);
            }

            @Override
            protected void done() {
                if (currentVersion != renderVersion || isCancelled()) {
                    return;
                }

                try {
                    pipelineResult = get();
                    updatePreviewPanels(pipelineResult);
                    saveButton.setEnabled(true);
                    statusLabel.setText("Preview updated. Pipeline images are shown below.");
                } catch (Exception ex) {
                    statusLabel.setText("Processing failed: " + ex.getMessage());
                } finally {
                    setControlsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void updatePreviewPanels(PipelineResult result) {
        originalPanel.setImage(result.original);
        resultPanel.setImage(result.cartoon);

        stageOriginalPanel.setImage(result.original);
        stageEdgePanel.setImage(result.edgePreview);
        stageGrayPanel.setImage(result.grayPreview);
        stageQuantizedPanel.setImage(result.quantized);
        stageCartoonPanel.setImage(result.cartoon);
    }

    private void setControlsEnabled(boolean enabled) {
        loadButton.setEnabled(enabled);
        resetButton.setEnabled(enabled && originalImage != null);
        colorSlider.setEnabled(enabled);
        edgeSlider.setEnabled(enabled);
        blurSlider.setEnabled(enabled);
        saveButton.setEnabled(enabled && pipelineResult != null && pipelineResult.cartoon != null);
    }
}

class ImagePanel extends JPanel {
    private BufferedImage image;
    private final String title;

    public ImagePanel(String title) {
        this.title = title;
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2.setColor(new Color(30, 30, 30));
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString(title, 15, 25);

        if (image == null) {
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g2.drawString("No image", 15, 55);
            g2.dispose();
            return;
        }

        drawScaledImage(g2, image, 15, 40, getWidth() - 30, getHeight() - 55);
        g2.dispose();
    }

    protected void drawScaledImage(Graphics2D g2, BufferedImage image, int xArea, int yArea, int areaW, int areaH) {
        int panelW = Math.max(1, areaW);
        int panelH = Math.max(1, areaH);
        double scale = Math.min(panelW / (double) image.getWidth(), panelH / (double) image.getHeight());
        int drawW = Math.max(1, (int) (image.getWidth() * scale));
        int drawH = Math.max(1, (int) (image.getHeight() * scale));
        int x = xArea + (panelW - drawW) / 2;
        int y = yArea + (panelH - drawH) / 2;

        g2.drawImage(image, x, y, drawW, drawH, null);
        g2.setColor(Color.GRAY);
        g2.drawRect(x, y, drawW, drawH);
    }
}

class StagePanel extends JPanel {
    private BufferedImage image;
    private final String stageName;

    public StagePanel(String stageName) {
        this.stageName = stageName;
        setPreferredSize(new Dimension(220, 180));
        setBackground(new Color(250, 250, 250));
        setBorder(BorderFactory.createLineBorder(new Color(170, 170, 170)));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2.setColor(new Color(30, 30, 30));
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(stageName, 10, 20);

        if (image == null) {
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.drawString("No preview", 10, 42);
            g2.dispose();
            return;
        }

        int panelW = getWidth() - 20;
        int panelH = getHeight() - 35;
        double scale = Math.min(panelW / (double) image.getWidth(), panelH / (double) image.getHeight());
        int drawW = Math.max(1, (int) (image.getWidth() * scale));
        int drawH = Math.max(1, (int) (image.getHeight() * scale));
        int x = (getWidth() - drawW) / 2;
        int y = 28 + (panelH - drawH) / 2;

        g2.drawImage(image, x, y, drawW, drawH, null);
        g2.setColor(Color.GRAY);
        g2.drawRect(x, y, drawW, drawH);
        g2.dispose();
    }
}

class ImageLoader {
    public BufferedImage load(File file) throws IOException {
        BufferedImage input = ImageIO.read(file);
        if (input == null) {
            throw new IOException("Unsupported image format.");
        }
        return ImageUtils.toARGB(input);
    }

    public void save(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }
}

class CartoonRenderer {
    private final GaussianBlur blur;
    private final GrayConverter grayConverter;
    private final EdgeDetector edgeDetector;
    private final ColorQuantizer colorQuantizer;

    public CartoonRenderer(GaussianBlur blur,
                           GrayConverter grayConverter,
                           EdgeDetector edgeDetector,
                           ColorQuantizer colorQuantizer) {
        this.blur = blur;
        this.grayConverter = grayConverter;
        this.edgeDetector = edgeDetector;
        this.colorQuantizer = colorQuantizer;
    }

    public PipelineResult renderPipeline(BufferedImage original, int colorLevels, int edgeThreshold, int blurStrength) {
        BufferedImage source = ImageUtils.toARGB(original);
        BufferedImage smoothed = blur.apply(source, blurStrength);
        BufferedImage quantized = colorQuantizer.quantize(smoothed, colorLevels);
        double[][] gray = grayConverter.toGray(smoothed);
        BufferedImage grayPreview = grayConverter.toGrayImage(smoothed);
        boolean[][] edges = edgeDetector.detect(gray, edgeThreshold);
        edges = edgeDetector.thicken(edges, 1);
        BufferedImage edgePreview = edgeDetector.toPreviewImage(edges);
        BufferedImage cartoon = combineColorAndEdges(quantized, edges);
        return new PipelineResult(source, grayPreview, edgePreview, quantized, cartoon);
    }

    private BufferedImage combineColorAndEdges(BufferedImage colorImage, boolean[][] edges) {
        int width = colorImage.getWidth();
        int height = colorImage.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = colorImage.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;

                if (edges[y][x]) {
                    output.setRGB(x, y, (alpha << 24));
                } else {
                    output.setRGB(x, y, argb);
                }
            }
        }
        return output;
    }
}

class PipelineResult {
    public final BufferedImage original;
    public final BufferedImage grayPreview;
    public final BufferedImage edgePreview;
    public final BufferedImage quantized;
    public final BufferedImage cartoon;

    public PipelineResult(BufferedImage original,
                          BufferedImage grayPreview,
                          BufferedImage edgePreview,
                          BufferedImage quantized,
                          BufferedImage cartoon) {
        this.original = original;
        this.grayPreview = grayPreview;
        this.edgePreview = edgePreview;
        this.quantized = quantized;
        this.cartoon = cartoon;
    }
}

class GaussianBlur {
    private static final int[][] KERNEL = {
            {1, 4, 6, 4, 1},
            {4, 16, 24, 16, 4},
            {6, 24, 36, 24, 6},
            {4, 16, 24, 16, 4},
            {1, 4, 6, 4, 1}
    };
    private static final int KERNEL_SUM = 256;

    public BufferedImage apply(BufferedImage input, int strength) {
        BufferedImage current = ImageUtils.toARGB(input);
        for (int i = 0; i < strength; i++) {
            current = blurOnce(current);
        }
        return current;
    }

    private BufferedImage blurOnce(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int aSum = 0;
                int rSum = 0;
                int gSum = 0;
                int bSum = 0;

                for (int ky = -2; ky <= 2; ky++) {
                    for (int kx = -2; kx <= 2; kx++) {
                        int px = ImageUtils.clamp(x + kx, 0, width - 1);
                        int py = ImageUtils.clamp(y + ky, 0, height - 1);
                        int weight = KERNEL[ky + 2][kx + 2];
                        int argb = input.getRGB(px, py);

                        int a = (argb >>> 24) & 0xff;
                        int r = (argb >>> 16) & 0xff;
                        int g = (argb >>> 8) & 0xff;
                        int b = argb & 0xff;

                        aSum += a * weight;
                        rSum += r * weight;
                        gSum += g * weight;
                        bSum += b * weight;
                    }
                }

                int a = aSum / KERNEL_SUM;
                int r = rSum / KERNEL_SUM;
                int g = gSum / KERNEL_SUM;
                int b = bSum / KERNEL_SUM;
                output.setRGB(x, y, ImageUtils.argb(a, r, g, b));
            }
        }
        return output;
    }
}

class GrayConverter {
    public double[][] toGray(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        double[][] gray = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = input.getRGB(x, y);
                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;
                gray[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return gray;
    }

    public BufferedImage toGrayImage(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double[][] gray = toGray(input);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = ImageUtils.clamp((int) Math.round(gray[y][x]), 0, 255);
                output.setRGB(x, y, ImageUtils.argb(255, value, value, value));
            }
        }
        return output;
    }
}

class EdgeDetector {
    private static final int[][] SOBEL_X = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };

    private static final int[][] SOBEL_Y = {
            {-1, -2, -1},
            {0, 0, 0},
            {1, 2, 1}
    };

    public boolean[][] detect(double[][] gray, int thresholdSliderValue) {
        int height = gray.length;
        int width = gray[0].length;
        double[][] magnitude = new double[height][width];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double gx = 0;
                double gy = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        double pixel = gray[y + ky][x + kx];
                        gx += pixel * SOBEL_X[ky + 1][kx + 1];
                        gy += pixel * SOBEL_Y[ky + 1][kx + 1];
                    }
                }
                magnitude[y][x] = Math.sqrt(gx * gx + gy * gy);
            }
        }

        return adaptiveThreshold(magnitude, thresholdSliderValue);
    }

    private boolean[][] adaptiveThreshold(double[][] magnitude, int thresholdSliderValue) {
        int height = magnitude.length;
        int width = magnitude[0].length;
        boolean[][] edges = new boolean[height][width];
        double[][] integral = buildIntegralImage(magnitude);

        int radius = 8;
        double multiplier = 0.45 + thresholdSliderValue / 100.0;
        double minimumThreshold = 28.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int x1 = ImageUtils.clamp(x - radius, 0, width - 1);
                int y1 = ImageUtils.clamp(y - radius, 0, height - 1);
                int x2 = ImageUtils.clamp(x + radius, 0, width - 1);
                int y2 = ImageUtils.clamp(y + radius, 0, height - 1);

                double localAverage = getAreaSum(integral, x1, y1, x2, y2)
                        / ((x2 - x1 + 1) * (y2 - y1 + 1));
                double threshold = Math.max(minimumThreshold, localAverage * multiplier);
                edges[y][x] = magnitude[y][x] > threshold;
            }
        }
        return edges;
    }

    private double[][] buildIntegralImage(double[][] values) {
        int height = values.length;
        int width = values[0].length;
        double[][] integral = new double[height + 1][width + 1];

        for (int y = 1; y <= height; y++) {
            double rowSum = 0;
            for (int x = 1; x <= width; x++) {
                rowSum += values[y - 1][x - 1];
                integral[y][x] = integral[y - 1][x] + rowSum;
            }
        }
        return integral;
    }

    private double getAreaSum(double[][] integral, int x1, int y1, int x2, int y2) {
        int ax1 = x1;
        int ay1 = y1;
        int ax2 = x2 + 1;
        int ay2 = y2 + 1;

        return integral[ay2][ax2]
                - integral[ay1][ax2]
                - integral[ay2][ax1]
                + integral[ay1][ax1];
    }

    public boolean[][] thicken(boolean[][] input, int radius) {
        int height = input.length;
        int width = input[0].length;
        boolean[][] output = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!input[y][x]) {
                    continue;
                }
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            output[ny][nx] = true;
                        }
                    }
                }
            }
        }
        return output;
    }

    public BufferedImage toPreviewImage(boolean[][] edges) {
        int height = edges.length;
        int width = edges[0].length;
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (edges[y][x]) {
                    output.setRGB(x, y, ImageUtils.argb(255, 255, 255, 255));
                } else {
                    output.setRGB(x, y, ImageUtils.argb(255, 20, 20, 20));
                }
            }
        }
        return output;
    }
}

class ColorQuantizer {
    public BufferedImage quantize(BufferedImage input, int levels) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int safeLevels = Math.max(2, levels);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = input.getRGB(x, y);
                int a = (argb >>> 24) & 0xff;
                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;

                r = quantizeChannel(r, safeLevels);
                g = quantizeChannel(g, safeLevels);
                b = quantizeChannel(b, safeLevels);

                int[] boosted = boostSaturation(r, g, b, 1.10);
                output.setRGB(x, y, ImageUtils.argb(a, boosted[0], boosted[1], boosted[2]));
            }
        }
        return output;
    }

    private int quantizeChannel(int value, int levels) {
        double normalized = value / 255.0;
        int index = (int) Math.round(normalized * (levels - 1));
        return ImageUtils.clamp((int) Math.round(index * (255.0 / (levels - 1))), 0, 255);
    }

    private int[] boostSaturation(int r, int g, int b, double factor) {
        double gray = 0.299 * r + 0.587 * g + 0.114 * b;
        int nr = ImageUtils.clamp((int) Math.round(gray + (r - gray) * factor), 0, 255);
        int ng = ImageUtils.clamp((int) Math.round(gray + (g - gray) * factor), 0, 255);
        int nb = ImageUtils.clamp((int) Math.round(gray + (b - gray) * factor), 0, 255);
        return new int[]{nr, ng, nb};
    }
}

class ImageUtils {
    public static BufferedImage toARGB(BufferedImage input) {
        if (input.getType() == BufferedImage.TYPE_INT_ARGB) {
            BufferedImage copy = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            g.drawImage(input, 0, 0, null);
            g.dispose();
            return copy;
        }

        BufferedImage converted = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = converted.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return converted;
    }

    public static int argb(int a, int r, int g, int b) {
        return ((clamp(a, 0, 255) & 0xff) << 24)
                | ((clamp(r, 0, 255) & 0xff) << 16)
                | ((clamp(g, 0, 255) & 0xff) << 8)
                | (clamp(b, 0, 255) & 0xff);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}