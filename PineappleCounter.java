import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PineappleCounter {

    public static void main(String[] args) {
        try {
            // 讀取圖片
            File file = new File("0001.jpg"); 
            BufferedImage image = ImageIO.read(file);

            // 轉灰階
            int width = image.getWidth();
            int height = image.getHeight();

            int[][] gray = new int[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int rgb = image.getRGB(x, y);

                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;

                    int grayValue = (r + g + b) / 3;

                    gray[y][x] = grayValue;
                }
            }

            // 簡單邊緣偵測（水平掃描）
            int count = 0;

            for (int y = 0; y < height; y++) {
                int peaks = 0;

                for (int x = 1; x < width; x++) {

                    int diff = Math.abs(gray[y][x] - gray[y][x - 1]);

                    if (diff > 40) {
                        peaks++;
                    }
                }

                if (peaks > 50) {
                    count++;
                }
            }

            // 最終估算
            int finalCount = count / 20;

            System.out.println("Pineapple count: " + finalCount);

        } catch (IOException e) {
            System.out.println("Error loading image.");
        }
    }
}