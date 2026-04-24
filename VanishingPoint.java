import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class VanishingPoint {

    public static void main(String[] args) throws Exception {
        // 載入如月車站原始照片（與程式放在同一資料夾）
        BufferedImage img = ImageIO.read(new File("station.jpg"));
        int W = img.getWidth();   // 575
        int H = img.getHeight();  // 431

        // 建立可繪圖的複本
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();

        // 抗鋸齒
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 複製原始影像
        g.drawImage(img, 0, 0, null);

        // ── 消失點座標 (Vanishing Point) ──
        // 依鐵軌延伸方向手動定出消失點
        int vpX = 175;
        int vpY = 248;

        // ══════════════════════════════════════════════════════
        // 1. 紅色收斂線 (平行線在消失點交會 → Pinhole Camera Model)
        //    模擬鐵軌左右兩軌與月台邊緣
        // ══════════════════════════════════════════════════════
        g.setColor(new Color(255, 30, 30));
        g.setStroke(new BasicStroke(2.0f));

        // 左軌：從左下角 → 消失點
        g.drawLine(60, H, vpX, vpY);
        // 右軌：從右下偏右 → 消失點
        g.drawLine(W - 30, H - 30, vpX, vpY);
        // 中間月台邊緣線：從底部中右 → 消失點
        g.drawLine(330, H, vpX, vpY);

        // ══════════════════════════════════════════════════════
        // 2. 洋紅色水平線 (horizon line / 地平線)
        // ══════════════════════════════════════════════════════
        g.setColor(new Color(255, 0, 255));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(0, vpY, W, vpY);

        // ══════════════════════════════════════════════════════
        // 3. 綠色虛線 (垂直方向參考線，說明消失點高度)
        // ══════════════════════════════════════════════════════
        float[] dash = {8f, 6f};
        g.setColor(new Color(50, 220, 50));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, dash, 0f));
        // 從消失點向右上延伸（仿第一張圖的綠色斜虛線）
        g.drawLine(vpX, vpY, vpX + 130, 0);

        // ══════════════════════════════════════════════════════
        // 4. 黃色 X 標記消失點
        // ══════════════════════════════════════════════════════
        int sz = 10;
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(3.0f));
        g.drawLine(vpX - sz, vpY - sz, vpX + sz, vpY + sz);
        g.drawLine(vpX + sz, vpY - sz, vpX - sz, vpY + sz);



        g.dispose();

        // 輸出結果（與程式放在同一資料夾）
        File outFile = new File("kisaragi_vanishing_point.jpg");
        ImageIO.write(out, "jpg", outFile);
        System.out.println("Done! Output: " + outFile.getAbsolutePath());
        System.out.println("Image size: " + W + " x " + H);
        System.out.println("Vanishing Point: (" + vpX + ", " + vpY + ")");
    }
}