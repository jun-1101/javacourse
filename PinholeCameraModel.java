import java.awt.geom.Point2D;

public class PinholeCameraModel {

    /**
     * 計算兩條直線的交點（消失點）
     * 直線 1 由 p1, p2 定義，直線 2 由 p3, p4 定義
     */
    public static Point2D.Double getIntersection(Point2D.Double p1, Point2D.Double p2, Point2D.Double p3, Point2D.Double p4) {
        double x1 = p1.getX(), y1 = p1.getY();
        double x2 = p2.getX(), y2 = p2.getY();
        double x3 = p3.getX(), y3 = p3.getY();
        double x4 = p4.getX(), y4 = p4.getY();

        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0) {
            // 平行線，理論上不會有交點
            return null;
        }

        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        return new Point2D.Double(x1 + ua * (x2 - x1), y1 + ua * (y2 - y1));
    }

    /**
     * 根據消失點和基準人物計算目標人物的身高
     * 
     * @param vp 消失點 (Vanishing Point)
     * @param baseFoot 基準人物腳部像素座標
     * @param baseHead 基準人物頭部像素座標
     * @param baseHeight 基準人物實際身高 (cm)
     * @param targetFoot 目標人物腳部像素座標
     * @param targetHead 目標人物頭部像素座標
     * @return 目標人物估算身高 (cm)
     */
    public static double estimateHeight(Point2D.Double vp, Point2D.Double baseFoot, Point2D.Double baseHead, double baseHeight, Point2D.Double targetFoot, Point2D.Double targetHead) {
        double h_base_pixel = baseFoot.getY() - baseHead.getY();
        double h_target_pixel = targetFoot.getY() - targetHead.getY();

        if (h_base_pixel == 0 || (targetFoot.getY() - vp.getY()) == 0) {
            return 0.0; // 避免除以零
        }
        
        double ratio_pixel_height = h_target_pixel / h_base_pixel;
        double ratio_distance_to_vp = (baseFoot.getY() - vp.getY()) / (targetFoot.getY() - vp.getY());
        
        return baseHeight * ratio_pixel_height * ratio_distance_to_vp;
    }

    public static void main(String[] args) {

        // --- 圖片 pic1(1).jpg 的身高測量 ---
        System.out.println("\n--- 圖片 pic1(1).jpg 身高估算 ---");
        Point2D.Double vp1 = new Point2D.Double(850, 880); 

        Point2D.Double base1Foot = new Point2D.Double(900, 1700);
        Point2D.Double base1Head = new Point2D.Double(900, 1100);
        double baseHeight = 180.0; 

        Point2D.Double target1Foot = new Point2D.Double(320, 1650);
        Point2D.Double target1Head = new Point2D.Double(320, 1090);

        double estimatedHeight1 = estimateHeight(vp1, base1Foot, base1Head, baseHeight, target1Foot, target1Head);
        System.out.printf("pic1(1).jpg 中，左側學生估計身高: %.2f cm\n", estimatedHeight1);

        // --- 圖片 pic3.jpg 的身高測量 ---
        System.out.println("\n--- 圖片 pic3.jpg 身高估算 ---");
        Point2D.Double vp3 = new Point2D.Double(800, 900); 

        Point2D.Double base3Foot = new Point2D.Double(900, 1700);
        Point2D.Double base3Head = new Point2D.Double(900, 1100);

        // 修改目標人物頭部座標從 1110 改為 1085
        Point2D.Double target3Foot = new Point2D.Double(420, 1680);
        Point2D.Double target3Head = new Point2D.Double(420, 1085); 

        double estimatedHeight3 = estimateHeight(vp3, base3Foot, base3Head, baseHeight, target3Foot, target3Head);
        System.out.printf("pic3.jpg 中，左側學生估計身高: %.2f cm\n", estimatedHeight3);
    }
}