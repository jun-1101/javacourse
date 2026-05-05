import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MultiThreshold {

    // ── 1. Grayscale conversion ──────────────────────────────────────────────
    static int[][] toGrayscale(BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        int[][] gray = new int[H][W];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                Color c = new Color(img.getRGB(x, y));
                gray[y][x] = (int)(0.299 * c.getRed()
                                 + 0.587 * c.getGreen()
                                 + 0.114 * c.getBlue());
            }
        return gray;
    }

    // ── 2. Histogram (256 bins, normalised) ──────────────────────────────────
    static double[] histogram(int[][] gray) {
        int H = gray.length, W = gray[0].length;
        double[] hist = new double[256];
        for (int[] row : gray)
            for (int v : row)
                hist[v]++;
        double total = H * W;
        for (int i = 0; i < 256; i++) hist[i] /= total;
        return hist;
    }

    // ── 3. Otsu multi-threshold (exhaustive 2-threshold search) ─────────────
    //  Maximise η = ω0·(μ0−μT)² + ω1·(μ1−μT)² + ω2·(μ2−μT)²
    static int[] otsu2(double[] p) {
        // Pre-compute prefix sums for fast range statistics
        double[] cumP  = new double[257]; // cumulative probability
        double[] cumM  = new double[257]; // cumulative mean (unnormalised)
        for (int i = 0; i < 256; i++) {
            cumP[i+1] = cumP[i] + p[i];
            cumM[i+1] = cumM[i] + i * p[i];
        }
        double muT = cumM[256]; // overall mean

        double bestVar = -1;
        int bestT1 = 0, bestT2 = 128;

        for (int t1 = 1; t1 < 255; t1++) {
            double w0 = cumP[t1];
            if (w0 < 1e-10) continue;
            double mu0 = cumM[t1] / w0;

            for (int t2 = t1 + 1; t2 < 256; t2++) {
                double w1 = cumP[t2] - cumP[t1];
                if (w1 < 1e-10) continue;
                double mu1 = (cumM[t2] - cumM[t1]) / w1;

                double w2 = cumP[256] - cumP[t2];
                if (w2 < 1e-10) continue;
                double mu2 = (cumM[256] - cumM[t2]) / w2;

                double var = w0*(mu0-muT)*(mu0-muT)
                           + w1*(mu1-muT)*(mu1-muT)
                           + w2*(mu2-muT)*(mu2-muT);
                if (var > bestVar) {
                    bestVar = var;
                    bestT1 = t1;
                    bestT2 = t2;
                }
            }
        }
        System.out.printf("Optimal thresholds: T1=%d, T2=%d  (η=%.6f)%n",
                          bestT1, bestT2, bestVar);
        return new int[]{bestT1, bestT2};
    }

    // ── 4. Save grayscale image ──────────────────────────────────────────────
    static BufferedImage grayToImage(int[][] gray) {
        int H = gray.length, W = gray[0].length;
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int v = gray[y][x];
                out.setRGB(x, y, new Color(v, v, v).getRGB());
            }
        return out;
    }

    // ── 5. Segmented image (3 regions → 3 grey levels) ──────────────────────
    static BufferedImage segment(int[][] gray, int t1, int t2) {
        int H = gray.length, W = gray[0].length;
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int v = gray[y][x];
                int level = (v < t1) ? 0 : (v < t2) ? 128 : 255;
                out.setRGB(x, y, new Color(level, level, level).getRGB());
            }
        return out;
    }

    // ── 6. Coloured overlay (background=blue, mid=green, foreground=red) ─────
    static BufferedImage colourOverlay(int[][] gray, int t1, int t2) {
        int H = gray.length, W = gray[0].length;
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Color[] colours = {
            new Color(30,  80, 180),   // region 0 → blue  (background/snow)
            new Color(80, 180,  60),   // region 1 → green (mid / transition)
            new Color(220, 80,  40)    // region 2 → red   (penguin foreground)
        };
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int v = gray[y][x];
                int region = (v < t1) ? 0 : (v < t2) ? 1 : 2;
                out.setRGB(x, y, colours[region].getRGB());
            }
        return out;
    }

    // ── 7. Histogram chart ───────────────────────────────────────────────────
    static BufferedImage drawHistogram(double[] p, int t1, int t2) {
        int CW = 900, CH = 400;
        int PAD = 60, CHART_H = CH - 2*PAD, CHART_W = CW - 2*PAD;
        BufferedImage img = new BufferedImage(CW, CH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, CW, CH);

        // Find max for scaling
        double maxP = 0;
        for (double v : p) if (v > maxP) maxP = v;

        // Draw bars
        for (int i = 0; i < 256; i++) {
            int barH = (int)(p[i] / maxP * CHART_H);
            int bx = PAD + (int)(i / 255.0 * CHART_W);
            // Colour bars by region
            if (i < t1)       g.setColor(new Color(30, 80, 180, 180));
            else if (i < t2)  g.setColor(new Color(80, 180, 60, 180));
            else               g.setColor(new Color(220, 80, 40, 180));
            g.fillRect(bx, PAD + CHART_H - barH, Math.max(1, CHART_W/256), barH);
        }

        // Threshold lines
        g.setStroke(new BasicStroke(2.5f));
        int x1 = PAD + (int)(t1 / 255.0 * CHART_W);
        int x2 = PAD + (int)(t2 / 255.0 * CHART_W);
        g.setColor(Color.RED);
        g.drawLine(x1, PAD, x1, PAD + CHART_H);
        g.setColor(new Color(180, 0, 180));
        g.drawLine(x2, PAD, x2, PAD + CHART_H);

        // Axes
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(PAD, PAD, CHART_W, CHART_H);

        // Labels
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Grayscale Histogram with Otsu Multi-threshold", PAD, PAD - 10);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("0", PAD - 5, PAD + CHART_H + 15);
        g.drawString("255", PAD + CHART_W - 15, PAD + CHART_H + 15);
        g.drawString("Intensity", PAD + CHART_W/2 - 20, PAD + CHART_H + 30);

        g.setColor(Color.RED);
        g.drawString("T1=" + t1, x1 + 4, PAD + 20);
        g.setColor(new Color(180, 0, 180));
        g.drawString("T2=" + t2, x2 + 4, PAD + 20);

        // Legend
        int lx = CW - 200, ly = PAD + 10;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        Color[] lc = {new Color(30,80,180), new Color(80,180,60), new Color(220,80,40)};
        String[] ll = {"Background (< T1)", "Mid-ground", "Foreground (≥ T2)"};
        for (int i = 0; i < 3; i++) {
            g.setColor(lc[i]);
            g.fillRect(lx, ly + i*18, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString(ll[i], lx + 16, ly + i*18 + 11);
        }

        g.dispose();
        return img;
    }

    // ── Main ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        String inputPath  = args.length > 0 ? args[0] : "animal.jpg";
        String outDir     = args.length > 1 ? args[1] : ".";

        System.out.println("=== Multi-threshold Segmentation (Assignment 1) ===");
        System.out.println("Input : " + inputPath);

        // Load
        BufferedImage original = ImageIO.read(new File(inputPath));
        System.out.printf("Image size: %d × %d%n", original.getWidth(), original.getHeight());

        // Grayscale
        int[][] gray = toGrayscale(original);
        double[] hist = histogram(gray);

        // Otsu 2-threshold
        int[] thresholds = otsu2(hist);
        int T1 = thresholds[0], T2 = thresholds[1];

        // Save outputs
        new File(outDir).mkdirs();

        ImageIO.write(grayToImage(gray),            "PNG",
                      new File(outDir + "/1_grayscale.png"));
        ImageIO.write(drawHistogram(hist, T1, T2),  "PNG",
                      new File(outDir + "/2_histogram.png"));
        ImageIO.write(segment(gray, T1, T2),        "PNG",
                      new File(outDir + "/3_segmented.png"));
        ImageIO.write(colourOverlay(gray, T1, T2),  "PNG",
                      new File(outDir + "/4_colour_overlay.png"));

        System.out.println("Output files saved to: " + outDir);
        System.out.println("  1_grayscale.png    – greyscale conversion");
        System.out.println("  2_histogram.png    – histogram with T1, T2 marked");
        System.out.println("  3_segmented.png    – 3-level segmented (0/128/255)");
        System.out.println("  4_colour_overlay.png – coloured region map");
        System.out.println("Done.");
    }
}
