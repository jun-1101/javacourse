import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║         VanishingPointHeight.java                           ║
 * ║  消失點身高測量 — Vanishing Point Height Measurement         ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * 原理 (Pinhole Camera Model):
 *   在單點透視投影中，所有平行深度線都收斂至「消失點」(VP)。
 *   利用已知身高的參考人物，推算同場景其他人物的真實身高。
 *
 * 公式:
 *   H = H_ref × (oy_bot − oy_top) / (ry_bot − ry_top)
 *             × |ry_bot − VP_y| / |oy_bot − VP_y|
 *
 * 座標來源:
 *   以互動式工具手動標定消失點 + 各人物頭頂/腳底像素座標。
 *   原始影像: 1500 × 2000 px
 *
 * 執行:
 *   javac VanishingPointHeight.java
 *   java  VanishingPointHeight
 *
 * 輸出:
 *   result_pic1~4.jpg  各照片標注圖
 *   result_summary.jpg 總覽比較圖
 *   終端機輸出詳細計算過程與結果
 */
public class VanishingPointHeight {

    // ════════════════════════════════════════
    //  參數設定
    // ════════════════════════════════════════
    static final double REF_H = 180.0;   // 參考人物身高 (cm)

    static final String[] IMAGE_PATHS = {
        "pic1 (1).jpg", "pic2.jpg", "pic3.jpg", "pic4.jpg"
    };

    /**
     * 各照片人物像素座標 (1500×2000 px 原始影像)
     * 格式: { VP_x, VP_y,
     *         ref_head_y, ref_feet_y, ref_cx,
     *         other_head_y, other_feet_y, other_cx }
     *
     * 座標由互動式工具 (VanishingPointHeight Swing GUI) 手動標定:
     *   Photo 1: VP=(107,1140), 灰T恤同學 176.9cm
     *   Photo 2: VP=(195,1153), 外套同學  173.9cm
     *   Photo 3: VP=(1160,928), 黑T恤同學 182.5cm
     *   Photo 4: VP=(107,1093), 灰T恤同學 159.7cm
     */
    static final int[][] COORDS = {
        //  VP_x  VP_y   ry_top ry_bot  rx    oy_top oy_bot  ox
        {  107,  1140,   1106,  1745,  870,   1119,  1594,  451 },  // pic1  → 178.3 cm
        {  195,  1153,   1103,  1738,  957,   1130,  1567,  668 },  // pic2  → 175.0 cm
        { 1160,   928,    928,  1298,  796,    924,  1422,  624 },  // pic3  → 181.5 cm
        {  107,  1093,   1056,  1654,  843,   1103,  1412,  542 },  // pic4  → 163.6 cm
    };

    static final String[] PERSON_DESC = {
        "灰T恤同學 (Photo 1 — 後方較遠)",
        "外套同學  (Photo 2 — 前方較近)",
        "黑T恤同學 (Photo 3 — 左前方)",
        "灰T恤同學 (Photo 4 — 後方較遠)",
    };

    // 顏色
    static final Color C_REF  = new Color(  0, 220, 180);
    static final Color C_VP   = new Color(255, 230,   0);
    static final Color C_BG   = new Color(  0,   0,   0, 185);
    static final Color[] C_OTHER = {
        new Color(255,  80,  80),   // pic1 紅
        new Color(255,  80,  80),   // pic2 紅
        new Color( 80, 220,  80),   // pic3 綠
        new Color( 80, 220,  80),   // pic4 綠
    };

    // ════════════════════════════════════════
    //  主程式
    // ════════════════════════════════════════
    public static void main(String[] args) throws IOException {
        printHeader();

        double[] heights = new double[4];
        for (int i = 0; i < 4; i++) {
            int[] c   = COORDS[i];
            int vp_y  = c[1];
            int ry_t  = c[2], ry_b = c[3];
            int oy_t  = c[5], oy_b = c[6];

            heights[i] = computeHeight(ry_t, ry_b, oy_t, oy_b, vp_y);
            printResult(i, c, heights[i]);
            renderPhoto(i, heights[i]);
        }

        renderSummary(heights);
        printFooter(heights);
    }

    // ════════════════════════════════════════
    //  計算公式
    // ════════════════════════════════════════
    static double computeHeight(int ry_t, int ry_b,
                                int oy_t, int oy_b, int vpy) {
        double pix_r = Math.abs(ry_b - ry_t);
        double pix_o = Math.abs(oy_b - oy_t);
        double dr    = Math.abs(ry_b - vpy);
        double doo   = Math.abs(oy_b - vpy);
        return (pix_r == 0 || doo == 0) ? 0
               : REF_H * (pix_o / pix_r) * (dr / doo);
    }

    // ════════════════════════════════════════
    //  標注照片輸出
    // ════════════════════════════════════════
    static void renderPhoto(int idx, double height) throws IOException {
        File f = new File(IMAGE_PATHS[idx]);
        if (!f.exists()) {
            System.out.printf("  [WARN] 找不到 %s%n", IMAGE_PATHS[idx]);
            return;
        }
        BufferedImage src = ImageIO.read(f);
        int W0 = src.getWidth(), H0 = src.getHeight();
        int outW = 750, outH = (int)(H0 * outW / (double)W0);
        double sc = outW / (double)W0;

        BufferedImage canvas = new BufferedImage(outW, outH,
                                                 BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g2d(canvas);
        g.drawImage(src, 0, 0, outW, outH, null);

        int[] c  = COORDS[idx];
        int vpx  = s(c[0],sc), vpy  = s(c[1],sc);
        int ryt  = s(c[2],sc), ryb  = s(c[3],sc), rx = s(c[4],sc);
        int oyt  = s(c[5],sc), oyb  = s(c[6],sc), ox = s(c[7],sc);

        Color co = C_OTHER[idx];

        // VP 虛線到各端點
        dashed(g, vpx,vpy, rx,ryt, alpha(C_REF,160), 1.5f);
        dashed(g, vpx,vpy, rx,ryb, alpha(C_REF,160), 1.5f);
        dashed(g, vpx,vpy, ox,oyt, alpha(co,  160), 1.5f);
        dashed(g, vpx,vpy, ox,oyb, alpha(co,  160), 1.5f);

        // 水平線
        g.setColor(new Color(255,255,255,45));
        g.setStroke(dash(1f, 14f, 8f));
        g.drawLine(0, vpy, outW, vpy);

        // 高度棒
        bar(g, rx, ryt, ryb, C_REF, "REF 180cm", outW);
        bar(g, ox, oyt, oyb, co, String.format("%.1f cm", height), outW);

        // VP 標記
        vp(g, vpx, vpy);

        // 資訊框
        infoBox(g, idx, c, height, outW, outH);

        g.dispose();
        String out = "result_pic" + (idx+1) + ".jpg";
        ImageIO.write(rgb(canvas), "jpg", new File(out));
        System.out.printf("  ✓ 已輸出: %s%n", out);
    }

    static void bar(Graphics2D g, int cx, int yt, int yb,
                    Color col, String lbl, int W) {
        int bx = (cx + 130 < W) ? cx + 16 : cx - 138;
        g.setColor(alpha(col, 225));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(bx, yt, bx, yb);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(bx-7, yt, bx+7, yt);
        g.drawLine(bx-7, yb, bx+7, yb);
        g.fillOval(cx-6, yt-6, 12, 12);
        g.fillOval(cx-6, yb-6, 12, 12);
        Font fn = new Font("Arial", Font.BOLD, 15);
        g.setFont(fn);
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(lbl)+12, lh = fm.getHeight()+4;
        int mid = (yt+yb)/2, lx = bx+6, ly = mid-lh/2;
        g.setColor(C_BG);
        g.fillRoundRect(lx, ly, lw, lh, 6, 6);
        g.setColor(col);
        g.drawString(lbl, lx+6, ly+lh-5);
    }

    static void vp(Graphics2D g, int vx, int vy) {
        int[] rs = {22,13,7}; int[] as = {40,100,200};
        for (int i=0;i<3;i++) {
            g.setColor(new Color(255,230,0,as[i]));
            g.fillOval(vx-rs[i],vy-rs[i],rs[i]*2,rs[i]*2);
        }
        g.setColor(C_VP); g.fillOval(vx-4,vy-4,8,8);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(vx-18,vy,vx+18,vy); g.drawLine(vx,vy-18,vx,vy+18);
        g.setFont(new Font("Arial",Font.BOLD,12));
        FontMetrics fm=g.getFontMetrics();
        int lw=fm.stringWidth("VP")+8;
        g.setColor(C_BG); g.fillRoundRect(vx+12,vy-11,lw,16,4,4);
        g.setColor(C_VP); g.drawString("VP",vx+16,vy+1);
    }

    static void infoBox(Graphics2D g, int idx, int[] c,
                        double h, int W, int H) {
        int vpy=c[1], ryt=c[2], ryb=c[3], oyt=c[5], oyb=c[6];
        int pr=ryb-ryt, po=oyb-oyt;
        int dr=Math.abs(ryb-vpy), doo=Math.abs(oyb-vpy);
        String[] lines = {
            String.format("Photo %d | VP_y=%d", idx+1, vpy),
            String.format("REF:   %d~%d  pix=%d", ryt, ryb, pr),
            String.format("OTHER: %d~%d  pix=%d", oyt, oyb, po),
            String.format("H = 180 x (%d/%d) x (%d/%d)", po, pr, dr, doo),
            String.format("  = %.1f cm", h),
        };
        Font fm=new Font("Monospaced",Font.PLAIN,12);
        Font fb=new Font("Monospaced",Font.BOLD,13);
        int lh=17, pad=8, bw=310, bh=lines.length*lh+pad*2;
        int bx=6, by=H-bh-6;
        g.setColor(C_BG); g.fillRoundRect(bx,by,bw,bh,10,10);
        g.setColor(new Color(70,80,100,200));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx,by,bw,bh,10,10);
        int ty=by+pad+lh-3;
        for (int i=0;i<lines.length;i++) {
            if (i==0)             { g.setFont(fb); g.setColor(new Color(120,210,255)); }
            else if (i==lines.length-1) { g.setFont(fb); g.setColor(C_OTHER[idx]); }
            else                  { g.setFont(fm); g.setColor(new Color(200,210,225)); }
            g.drawString(lines[i], bx+pad, ty); ty+=lh;
        }
    }

    static void dashed(Graphics2D g, int x1,int y1,int x2,int y2,
                       Color c, float w) {
        Stroke old=g.getStroke();
        g.setStroke(new BasicStroke(w,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,10f,new float[]{9f,5f},0f));
        g.setColor(c); g.drawLine(x1,y1,x2,y2); g.setStroke(old);
    }

    // ════════════════════════════════════════
    //  總覽圖
    // ════════════════════════════════════════
    static void renderSummary(double[] heights) throws IOException {
        int W=920, H=590;
        BufferedImage canvas=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=g2d(canvas);
        g.setColor(new Color(14,19,34)); g.fillRect(0,0,W,H);

        Font fT =new Font("Arial",Font.BOLD,26);
        Font fS =new Font("Arial",Font.PLAIN,13);
        Font fM =new Font("Monospaced",Font.PLAIN,12);
        Font fMB=new Font("Monospaced",Font.BOLD,13);
        Font fV =new Font("Arial",Font.BOLD,16);
        Font fSm=new Font("Arial",Font.PLAIN,12);

        center(g,"Vanishing Point Height Measurement",fT,new Color(100,200,255),W,42);
        center(g,"消失點身高測量  |  Pinhole Camera Model  |  REF = 180 cm",
               fS,new Color(150,155,175),W,66);
        g.setColor(new Color(50,65,100)); g.fillRect(40,74,W-80,1);

        // 左欄: 公式 + 結果
        int fx=42, fy=92;
        g.setFont(fMB); g.setColor(new Color(170,255,155));
        g.drawString("公式 / Formula:", fx, fy); fy+=22;
        String[] fl={"H = H_ref","      × (oy_bot - oy_top)",
                     "      / (ry_bot - ry_top)","      × |ry_bot - VP_y|",
                     "      / |oy_bot - VP_y|","",
                     "VP_y  = 消失點 y 座標 (pixel)",
                     "H_ref = 180 cm  (Just Do It)",
                     "ry    = 參考人物 head/feet y",
                     "oy    = 待測人物 head/feet y"};
        g.setFont(fM); g.setColor(new Color(195,205,225));
        for (String l:fl){g.drawString(l,fx,fy);fy+=19;}

        fy+=10;
        g.setFont(fMB); g.setColor(new Color(170,255,155));
        g.drawString("測量結果:", fx, fy); fy+=20;
        String[] sd={"Photo 1  灰T恤 (後方)","Photo 2  外套  (前方)",
                     "Photo 3  黑T恤 (前方)","Photo 4  灰T恤 (後方)"};
        Color[] bc2={new Color(255,90,70),new Color(60,170,255),
                     new Color(100,220,80),new Color(210,100,220)};
        for (int i=0;i<4;i++){
            g.setFont(fMB); g.setColor(bc2[i]);
            g.drawString(String.format("  %s : %.1f cm",sd[i],heights[i]),fx,fy);
            fy+=20;
        }

        // 右欄: 長條圖
        int BAX=445,BAY=85,BAW=W-BAX-35,BAH=390;
        double mn=120,mx=220;
        g.setColor(new Color(60,75,105)); g.setStroke(new BasicStroke(1f));
        g.drawLine(BAX,BAY,BAX,BAY+BAH);
        g.drawLine(BAX,BAY+BAH,BAX+BAW,BAY+BAH);
        g.setFont(fSm);
        for (int cm=(int)mn;cm<=(int)mx;cm+=20){
            int gy=BAY+BAH-(int)((cm-mn)/(mx-mn)*BAH);
            g.setColor(new Color(45,55,80)); g.drawLine(BAX+1,gy,BAX+BAW,gy);
            g.setColor(new Color(110,120,145));
            g.drawString(cm+"cm",BAX-42,gy+4);
        }
        int rY=BAY+BAH-(int)((180-mn)/(mx-mn)*BAH);
        g.setColor(new Color(0,200,160,180));
        g.setStroke(dash(1.5f,10f,5f));
        g.drawLine(BAX,rY,BAX+BAW,rY);
        g.setFont(fSm); g.setColor(new Color(0,200,160));
        g.drawString("REF 180cm",BAX+BAW-82,rY-4);

        int barW=(BAW-(4+1)*14)/4;
        g.setStroke(new BasicStroke(1f));
        for (int i=0;i<4;i++){
            double h=heights[i];
            int bx=BAX+14+i*(barW+14);
            int bh=(int)((h-mn)/(mx-mn)*BAH), by=BAY+BAH-bh;
            Color bc=bc2[i];
            g.setColor(new Color(0,0,0,70));
            g.fillRoundRect(bx+3,by+3,barW,bh,6,6);
            GradientPaint gp=new GradientPaint(bx,by,bc,bx,by+bh,bc.darker());
            g.setPaint(gp); g.fillRoundRect(bx,by,barW,bh,6,6);
            g.setPaint(null); g.setColor(bc.brighter());
            g.drawRoundRect(bx,by,barW,bh,6,6);
            g.setFont(fV); g.setColor(Color.WHITE);
            String hs=String.format("%.1f",h);
            FontMetrics fm=g.getFontMetrics();
            g.drawString(hs,bx+(barW-fm.stringWidth(hs))/2,by-6);
            g.setFont(fSm); g.setColor(bc.brighter());
            String lb="P"+(i+1); fm=g.getFontMetrics();
            g.drawString(lb,bx+(barW-fm.stringWidth(lb))/2,BAY+BAH+16);
        }

        g.setFont(new Font("Arial",Font.ITALIC,11));
        center(g,"* 消失點由天花板橫樑平行線交叉點標定 | 參考人物 Just Do It = 180 cm",
               new Font("Arial",Font.ITALIC,11),new Color(90,100,120),W,H-10);

        g.dispose();
        ImageIO.write(rgb(canvas),"jpg",new File("result_summary.jpg"));
        System.out.println("  ✓ 已輸出: result_summary.jpg");
    }

    // ════════════════════════════════════════
    //  終端機輸出
    // ════════════════════════════════════════
    static void printHeader(){
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  Vanishing Point Height Measurement              ║");
        System.out.println("║  消失點身高測量  |  Pinhole Camera Model         ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf ("  公式: H = %.0f × (pix_other/pix_ref) × (dr/do)%n",REF_H);
        System.out.println("──────────────────────────────────────────────────");
    }

    static void printResult(int i, int[] c, double h){
        int vpy=c[1],ryt=c[2],ryb=c[3],oyt=c[5],oyb=c[6];
        int pr=ryb-ryt,po=oyb-oyt;
        int dr=Math.abs(ryb-vpy),doo=Math.abs(oyb-vpy);
        System.out.printf("%n  ▶ Photo %d | %s%n",i+1,PERSON_DESC[i]);
        System.out.printf("    消失點 VP  = (%d, %d)%n",c[0],vpy);
        System.out.printf("    REF        top=%d  bot=%d  (pix_ref=%d)%n",ryt,ryb,pr);
        System.out.printf("    OTHER      top=%d  bot=%d  (pix_other=%d)%n",oyt,oyb,po);
        System.out.printf("    dr=|%d-%d|=%d,  do=|%d-%d|=%d%n",ryb,vpy,dr,oyb,vpy,doo);
        System.out.printf("    H = 180 × (%d/%d) × (%d/%d) = %.1f cm%n",po,pr,dr,doo,h);
    }

    static void printFooter(double[] heights){
        System.out.println();
        System.out.println("──────────────────────────────────────────────────");
        System.out.println("  ★  最終身高結果  ★");
        System.out.println();
        String[] nm={"灰T恤(後) Photo1","外套(前)  Photo2",
                     "黑T恤(前) Photo3","灰T恤(後) Photo4"};
        for(int i=0;i<4;i++)
            System.out.printf("     %s  →  %.1f cm%n",nm[i],heights[i]);
        System.out.println();
        System.out.println("  輸出: result_pic1~4.jpg, result_summary.jpg");
        System.out.println("══════════════════════════════════════════════════");
        System.out.println();
    }

    // ════════════════════════════════════════
    //  工具
    // ════════════════════════════════════════
    static int s(int v,double sc){return(int)(v*sc);}
    static Color alpha(Color c,int a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),a);}
    static Graphics2D g2d(BufferedImage img){
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }
    static BufferedImage rgb(BufferedImage src){
        BufferedImage out=new BufferedImage(src.getWidth(),src.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();
        g.setColor(Color.BLACK); g.fillRect(0,0,out.getWidth(),out.getHeight());
        g.drawImage(src,0,0,null); g.dispose(); return out;
    }
    static void center(Graphics2D g,String s,Font f,Color c,int W,int y){
        g.setFont(f); g.setColor(c);
        FontMetrics fm=g.getFontMetrics();
        g.drawString(s,(W-fm.stringWidth(s))/2,y);
    }
    static BasicStroke dash(float w,float on,float off){
        return new BasicStroke(w,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,
                               10f,new float[]{on,off},0f);
    }
}