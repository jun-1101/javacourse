import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


public class VanishingPointDetector {


    public static void main(String[] args) {
        String fileName = "C:\\Users\\User\\1113405027\\pic4.jpg";
       
        try {
            BufferedImage img = ImageIO.read(new File(fileName));
            int w = img.getWidth();
            int h = img.getHeight();


            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


            // --- 重新定位：讓 VP 落在鐵軌延伸的盡頭 ---
            int vpX = (int) (w * 0.31); // 往左移，對準鐵軌盡頭
            int vpY = (int) (h * 0.53); // 稍微下壓，拉長遠景感


            // 1. 地平線 (紫色)
            g2.setColor(new Color(255, 0, 255, 180));
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(0, vpY, w, vpY);


            // 2. 鐵軌深度線 (紅色) - 精確對準照片中的鐵軌路徑
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
           
            // 左側鐵軌線：從 VP 連到左下角
            g2.drawLine(vpX, vpY, (int)(w * 0.28), h);
            // 右側鐵軌線：從 VP 連到畫面底部中央偏右
            g2.drawLine(vpX, vpY, (int)(w * 0.82), h);
           
            // 3. 消失點標記 (黃色)
            g2.setColor(Color.YELLOW);
            int sz = 10;
            g2.drawLine(vpX - sz, vpY - sz, vpX + sz, vpY + sz);
            g2.drawLine(vpX - sz, vpY + sz, vpX + sz, vpY - sz);
            g2.drawString("軌道消失點", vpX + 15, vpY - 10);


            // 4. 月台燈光延伸線 (綠色) - 證明整體空間的一致性
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2.drawLine(vpX, vpY, (int)(w * 0.55), 0); // 指向月台燈柱頂端


            g2.dispose();


            // 輸出圖片
            ImageIO.write(img, "png", new File("Kisaragi_Tracks_Optimized.png"));
            System.out.println("鐵軌對齊圖已生成：Kisaragi_Tracks_Optimized.png");


        } catch (IOException e) {
            System.err.println("找不到照片檔案！");
        }
    }
}

