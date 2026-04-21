import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class EdgeDetection {

    // Step 1：彩色圖片轉灰階  Gray = 0.299R + 0.587G + 0.114B
    public static int[][] toGrayscale(BufferedImage img) {
        int rows = img.getHeight(), cols = img.getWidth();
        int[][] gray = new int[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                gray[y][x] = (int)(0.299*r + 0.587*g + 0.114*b);
            }
        return gray;
    }

    // Step 2：X 方向梯度  Ix(x,y) = [f(x+1,y) - f(x-1,y)] / 2
    public static double[][] computeGradientX(int[][] image, int rows, int cols) {
        double[][] Ix = new double[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) {
                int xPrev = (x-1 >= 0)   ? x-1 : x;
                int xNext = (x+1 < cols) ? x+1 : x;
                Ix[y][x] = (image[y][xNext] - image[y][xPrev]) / 2.0;
            }
        return Ix;
    }

    // Step 3：Y 方向梯度  Iy(x,y) = [f(x,y+1) - f(x,y-1)] / 2
    public static double[][] computeGradientY(int[][] image, int rows, int cols) {
        double[][] Iy = new double[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) {
                int yPrev = (y-1 >= 0)   ? y-1 : y;
                int yNext = (y+1 < rows) ? y+1 : y;
                Iy[y][x] = (image[yNext][x] - image[yPrev][x]) / 2.0;
            }
        return Iy;
    }

    // Step 4：梯度強度  magnitude = sqrt(Ix^2 + Iy^2)
    public static double[][] computeMagnitude(double[][] Ix, double[][] Iy, int rows, int cols) {
        double[][] mag = new double[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                mag[y][x] = Math.sqrt(Ix[y][x]*Ix[y][x] + Iy[y][x]*Iy[y][x]);
        return mag;
    }

    // Step 5：正規化並儲存為灰階圖片
    public static void saveImage(double[][] data, int rows, int cols, String filename) throws Exception {
        double maxVal = 0;
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                if (data[y][x] > maxVal) maxVal = data[y][x];

        BufferedImage out = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) {
                int v = (maxVal > 0) ? (int)(data[y][x] / maxVal * 255) : 0;
                v = Math.min(255, Math.max(0, v));
                out.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        ImageIO.write(out, "jpg", new File(filename));
        System.out.println("已儲存：" + filename);
    }

    public static void main(String[] args) throws Exception {
        String inputPath = (args.length > 0) ? args[0] : "fox.jpg";
        System.out.println("讀取圖片：" + inputPath);
        BufferedImage img = ImageIO.read(new File(inputPath));
        int rows = img.getHeight(), cols = img.getWidth();
        System.out.printf("影像尺寸：%d x %d%n", cols, rows);

        int[][]    gray = toGrayscale(img);
        double[][] Ix   = computeGradientX(gray, rows, cols);
        double[][] Iy   = computeGradientY(gray, rows, cols);
        double[][] mag  = computeMagnitude(Ix, Iy, rows, cols);

        double[][] grayD = new double[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) grayD[y][x] = gray[y][x];

        saveImage(grayD, rows, cols, "output_gray.jpg");
        saveImage(Ix,    rows, cols, "output_Ix.jpg");
        saveImage(Iy,    rows, cols, "output_Iy.jpg");
        saveImage(mag,   rows, cols, "output_magnitude.jpg");

        System.out.println("\n邊緣偵測完成！輸出：output_gray / Ix / Iy / magnitude .jpg");

        // 驗證講義 3x3 範例
        System.out.println("\n=== 講義 3x3 範例驗證 ===");
        int[][] sample = {{10,20,30},{40,50,60},{70,80,90}};
        double[][] sIx  = computeGradientX(sample, 3, 3);
        double[][] sIy  = computeGradientY(sample, 3, 3);
        double[][] sMag = computeMagnitude(sIx, sIy, 3, 3);
        System.out.printf("中心點 Ix = %.1f（預期 10.0）%n", sIx[1][1]);
        System.out.printf("中心點 Iy = %.1f（預期 30.0）%n", sIy[1][1]);
        System.out.printf("中心點 Magnitude = %.2f%n", sMag[1][1]);
    }
}