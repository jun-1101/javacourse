import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import javax.swing.*;

// ============================================================
//  AI SHOOTER CHALLENGE  —  Single-file Java Swing version
//  Controls: WASD / Arrow Keys = Move | SPACE = Shoot
//            P = Pause | R = Restart
// ============================================================

public class AIShooter extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AIShooter());
    }

    public AIShooter() {
        setTitle("AI Shooter Challenge");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        // 視窗完全顯示後再啟動，確保 focus 正常
        SwingUtilities.invokeLater(() -> panel.startGame());
    }

    // =========================================================
    //  CONSTANTS
    // =========================================================
    static final int TILE    = 40;
    static final int COLS    = 20;
    static final int ROWS    = 15;
    static final int MW      = COLS * TILE;  // 800
    static final int MH      = ROWS * TILE;  // 600
    static final int PANEL_W = MW + 180;

    // =========================================================
    //  MAP
    // =========================================================
    static class GameMap {
        boolean[][] obs = new boolean[COLS][ROWS];

        GameMap() { generate(); }

        void generate() {
            obs = new boolean[COLS][ROWS];
            Random r = new Random();
            int placed = 0;
            while (placed < 20) {
                int gx = r.nextInt(COLS), gy = r.nextInt(ROWS);
                if (gx < 1 || gx > COLS - 2 || gy < 1 || gy > ROWS - 2) continue;
                if (gx >= 7 && gx <= 12 && gy >= 5 && gy <= 10) continue; // keep center clear
                if (gy <= 1) continue;
                if (!obs[gx][gy]) { obs[gx][gy] = true; placed++; }
            }
        }

        boolean isObs(int gx, int gy) {
            if (gx < 0 || gx >= COLS || gy < 0 || gy >= ROWS) return true;
            return obs[gx][gy];
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(8, 15, 35));
            g.fillRect(0, 0, MW, MH);
            // Stars
            Random sr = new Random(777);
            g.setColor(new Color(200, 210, 255, 140));
            for (int i = 0; i < 90; i++) {
                int sx = sr.nextInt(MW), sy = sr.nextInt(MH), ss = sr.nextInt(2) + 1;
                g.fillOval(sx, sy, ss, ss);
            }
            // Grid
            g.setColor(new Color(30, 55, 100, 60));
            for (int x = 0; x <= COLS; x++) g.drawLine(x * TILE, 0, x * TILE, MH);
            for (int y = 0; y <= ROWS; y++) g.drawLine(0, y * TILE, MW, y * TILE);
            // Asteroids
            for (int gx = 0; gx < COLS; gx++)
                for (int gy = 0; gy < ROWS; gy++)
                    if (obs[gx][gy]) drawAsteroid(g, gx * TILE, gy * TILE);
        }

        void drawAsteroid(Graphics2D g, int x, int y) {
            int s = TILE;
            int[] px = {x+4, x+s-6, x+s-3, x+s-9, x+s-5, x+5, x+2};
            int[] py = {y+7, y+3,   y+13,  y+s-5, y+s-2, y+s-7, y+17};
            g.setColor(new Color(70, 62, 55));
            g.fillPolygon(px, py, px.length);
            g.setColor(new Color(110, 98, 85));
            g.fillOval(x + 8, y + 7, 13, 9);
            g.setColor(new Color(35, 30, 25));
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolygon(px, py, px.length);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // =========================================================
    //  BFS Node
    // =========================================================
    static class Node {
        int x, y; Node parent;
        Node(int x, int y, Node p) { this.x = x; this.y = y; this.parent = p; }
    }

    // =========================================================
    //  BFS Pathfinding
    // =========================================================
    static List<Node> bfs(GameMap map, int sx, int sy, int gx, int gy) {
        if (sx == gx && sy == gy) return Collections.emptyList();
        int[] dx = {0, 0, -1, 1}, dy = {-1, 1, 0, 0};
        Queue<Node> q = new LinkedList<>();
        boolean[][] vis = new boolean[COLS][ROWS];
        if (sx >= 0 && sx < COLS && sy >= 0 && sy < ROWS) {
            vis[sx][sy] = true;
            q.add(new Node(sx, sy, null));
        }
        while (!q.isEmpty()) {
            Node cur = q.poll();
            if (cur.x == gx && cur.y == gy) {
                List<Node> path = new ArrayList<>();
                for (Node n = cur; n != null; n = n.parent) path.add(0, n);
                return path;
            }
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i], ny = cur.y + dy[i];
                if (nx >= 0 && nx < COLS && ny >= 0 && ny < ROWS && !vis[nx][ny] && !map.isObs(nx, ny)) {
                    vis[nx][ny] = true;
                    q.add(new Node(nx, ny, cur));
                }
            }
        }
        return Collections.emptyList();
    }

    // =========================================================
    //  PLAYER
    // =========================================================
    static class Player {
        static final int SZ = 36, SPD = 3, MAX_HP = 5;
        int x, y, hp = MAX_HP;
        long lastHit = 0;
        Set<Integer> keys = new HashSet<>();

        Player(int x, int y) { this.x = x; this.y = y; }

        void keyDown(int k) { keys.add(k); }
        void keyUp(int k)   { keys.remove(k); }

        void update(GameMap map) {
            int nx = x, ny = y;
            if (keys.contains(KeyEvent.VK_LEFT)  || keys.contains(KeyEvent.VK_A)) nx -= SPD;
            if (keys.contains(KeyEvent.VK_RIGHT) || keys.contains(KeyEvent.VK_D)) nx += SPD;
            if (keys.contains(KeyEvent.VK_UP)    || keys.contains(KeyEvent.VK_W)) ny -= SPD;
            if (keys.contains(KeyEvent.VK_DOWN)  || keys.contains(KeyEvent.VK_S)) ny += SPD;
            nx = Math.max(0, Math.min(nx, MW - SZ));
            ny = Math.max(0, Math.min(ny, MH - SZ));
            if (!hitsObs(map, nx, y)) x = nx;
            if (!hitsObs(map, x, ny)) y = ny;
        }

        boolean hitsObs(GameMap map, int px, int py) {
            int m = 4;
            for (int gx = (px + m) / TILE; gx <= (px + SZ - m) / TILE; gx++)
                for (int gy = (py + m) / TILE; gy <= (py + SZ - m) / TILE; gy++)
                    if (map.isObs(gx, gy)) return true;
            return false;
        }

        boolean takeDamage() {
            long now = System.currentTimeMillis();
            if (now - lastHit >= 1000) { hp--; lastHit = now; return true; }
            return false;
        }

        boolean invincible() { return System.currentTimeMillis() - lastHit < 1000; }

        void draw(Graphics2D g) {
            if (invincible() && (System.currentTimeMillis() / 120) % 2 == 0) return;
            int cx = x + SZ / 2;
            g.setColor(new Color(255, 150, 0, 180));
            g.fillOval(cx - 7, y + SZ - 5, 14, 11);
            int[] bx = {cx, cx-12, cx-8, cx, cx+8, cx+12};
            int[] by = {y,  y+SZ-10, y+SZ, y+SZ-6, y+SZ, y+SZ-10};
            g.setColor(new Color(55, 125, 210));
            g.fillPolygon(bx, by, 6);
            int[] cpx = {cx, cx-5, cx+5}, cpy = {y+4, y+20, y+20};
            g.setColor(new Color(150, 230, 255));
            g.fillPolygon(cpx, cpy, 3);
            g.setColor(new Color(35, 95, 175));
            g.fillPolygon(new int[]{cx-8, cx-22, cx-14}, new int[]{y+22, y+SZ-4, y+SZ-4}, 3);
            g.fillPolygon(new int[]{cx+8, cx+22, cx+14}, new int[]{y+22, y+SZ-4, y+SZ-4}, 3);
            g.setColor(new Color(130, 210, 255));
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolygon(bx, by, 6);
            g.setStroke(new BasicStroke(1f));
        }

        void reset(int sx, int sy) { x = sx; y = sy; hp = MAX_HP; keys.clear(); lastHit = 0; }
        Rectangle bounds() { return new Rectangle(x + 4, y + 4, SZ - 8, SZ - 8); }
        int gx() { return (x + SZ / 2) / TILE; }
        int gy() { return (y + SZ / 2) / TILE; }
        int cx() { return x + SZ / 2; }
        int cy() { return y + SZ / 2; }
    }

    // =========================================================
    //  BULLET
    // =========================================================
    static class Bullet {
        static final int W = 5, H = 14, SPD = 10;
        int x, y; boolean active = true;

        Bullet(int cx, int ty) { x = cx - W / 2; y = ty - H; }

        void update(GameMap map) {
            y -= SPD;
            if (y + H < 0) active = false;
            if (map.isObs((x + W / 2) / TILE, y / TILE)) active = false;
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(100, 200, 255, 90));
            g.fillOval(x - 2, y - 2, W + 4, H + 4);
            GradientPaint gp = new GradientPaint(x, y, new Color(220, 245, 255), x, y + H, new Color(0, 130, 255));
            g.setPaint(gp);
            g.fillRoundRect(x, y, W, H, 3, 3);
        }

        Rectangle bounds() { return new Rectangle(x, y, W, H); }
    }


    // =========================================================
    //  ENEMY BULLET
    // =========================================================
    static class EnemyBullet {
        static final int W = 6, H = 12, SPD = 6;
        float x, y; boolean active = true; Color color;

        EnemyBullet(float cx, float cy, Color c) {
            x = cx - W / 2f; y = cy; color = c;
        }

        void update(GameMap map) {
            y += SPD;
            if (y > MH) active = false;
            int gx = (int)(x + W / 2) / TILE, gy = (int)y / TILE;
            if (gy >= 0 && gy < ROWS && map.isObs(gx, gy)) active = false;
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g.fillOval((int)x - 3, (int)y - 3, W + 6, H + 6);
            GradientPaint gp = new GradientPaint(x, y, color.brighter(), x, y + H, color.darker());
            g.setPaint(gp);
            g.fillRoundRect((int)x, (int)y, W, H, 3, 3);
        }

        Rectangle bounds() { return new Rectangle((int)x, (int)y, W, H); }
    }

    // =========================================================
    //  ENEMY
    // =========================================================
    static class Enemy {
        static final int SZ = 34;
        float x, y, speed;
        boolean alive = true;
        int type, hp, maxHp;
        float tpx, tpy;
        long lastBfs = 0, lastShot = 0;
        static final long BFS_CD = 450;
        long SHOT_CD;

        Enemy(int sx, int sy, float spd, int type) {
            x = sx; y = sy; speed = spd; tpx = sx; tpy = sy;
            this.type = type;
            this.maxHp = (type == 2) ? 2 : 1;
            this.hp = this.maxHp;
            this.SHOT_CD = (type == 0) ? 1800 : (type == 1) ? 2500 : 3000;
            this.lastShot = System.currentTimeMillis() + (long)(Math.random() * 1500);
        }

        void update(GameMap map, Player player) {
            if (!alive) return;
            long now = System.currentTimeMillis();
            if (now - lastBfs >= BFS_CD) {
                lastBfs = now;
                int mgx = (int)(x + SZ / 2) / TILE, mgy = (int)(y + SZ / 2) / TILE;
                List<Node> path = bfs(map, mgx, mgy, player.gx(), player.gy());
                if (path.size() > 1) {
                    Node nxt = path.get(1);
                    tpx = nxt.x * TILE + TILE / 2f - SZ / 2f;
                    tpy = nxt.y * TILE + TILE / 2f - SZ / 2f;
                }
            }
            float dx = tpx - x, dy = tpy - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > speed) { x += speed * dx / d; y += speed * dy / d; }
            else { x = tpx; y = tpy; }
        }

        EnemyBullet tryShoot() {
            if (!alive) return null;
            long now = System.currentTimeMillis();
            if (now - lastShot < SHOT_CD) return null;
            lastShot = now;
            Color c = (type % 3 == 0) ? new Color(255, 80, 80)
                    : (type % 3 == 1) ? new Color(210, 85, 255)
                    : new Color(85, 225, 85);
            return new EnemyBullet(x + SZ / 2f, y + SZ, c);
        }

        boolean hit() { hp--; return hp <= 0; }

        void draw(Graphics2D g) {
            if (!alive) return;
            int ix = (int) x, iy = (int) y, cx = ix + SZ / 2;
            Color body, accent, glow;
            switch (type % 3) {
                case 0 -> { body = new Color(190, 35, 35);  accent = new Color(255, 90, 90);  glow = new Color(255, 90, 90, 160); }
                case 1 -> { body = new Color(145, 35, 185); accent = new Color(210, 85, 255); glow = new Color(210, 85, 255, 160); }
                default -> { body = new Color(35, 145, 35); accent = new Color(85, 225, 85);  glow = new Color(85, 225, 85, 160); }
            }
            g.setColor(glow);
            g.fillOval(cx - 7, iy - 6, 14, 11);
            int[] bx = {cx, cx-12, cx-8, cx, cx+8, cx+12};
            int[] by = {iy+SZ, iy+10, iy, iy+6, iy, iy+10};
            g.setColor(body); g.fillPolygon(bx, by, 6);
            int[] cpx = {cx, cx-5, cx+5}, cpy = {iy+SZ-4, iy+SZ-18, iy+SZ-18};
            g.setColor(accent); g.fillPolygon(cpx, cpy, 3);
            g.setColor(body.darker());
            g.fillPolygon(new int[]{cx-8, cx-22, cx-14}, new int[]{iy+SZ-22, iy+10, iy+10}, 3);
            g.fillPolygon(new int[]{cx+8, cx+22, cx+14}, new int[]{iy+SZ-22, iy+10, iy+10}, 3);
            g.setColor(accent);
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolygon(bx, by, 6);
            g.setStroke(new BasicStroke(1f));
            // HP bar（只對多血量敵人顯示）
            if (maxHp > 1) {
                g.setColor(new Color(80, 0, 0));
                g.fillRect(ix, iy - 8, SZ, 4);
                g.setColor(new Color(50, 200, 50));
                g.fillRect(ix, iy - 8, SZ * hp / maxHp, 4);
            }
        }

        Rectangle bounds() { return new Rectangle((int)x + 4, (int)y + 4, SZ - 8, SZ - 8); }
    }

    // =========================================================
    //  PARTICLE
    // =========================================================
    static class Particle {
        float x, y, vx, vy; int life, maxLife; Color color;
        Particle(float x, float y, Color c) {
            this.x = x; this.y = y;
            Random r = new Random();
            float ang = (float)(r.nextDouble() * Math.PI * 2);
            float spd = 1 + r.nextFloat() * 4;
            vx = (float)Math.cos(ang) * spd; vy = (float)Math.sin(ang) * spd;
            maxLife = life = 20 + r.nextInt(20); color = c;
        }
        void update() { x += vx; y += vy; vx *= 0.92f; vy *= 0.92f; life--; }
        boolean dead() { return life <= 0; }
        void draw(Graphics2D g) {
            float alpha = (float)life / maxLife;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 220)));
            int sz = Math.max(1, (int)(alpha * 5));
            g.fillOval((int)x - sz/2, (int)y - sz/2, sz, sz);
        }
    }

    // =========================================================
    //  SCOREBOARD
    // =========================================================
    static class ScoreBoard {
        int score = 0, level = 1;
        long levelTimer = System.currentTimeMillis();
        long survivalTimer = System.currentTimeMillis();
        long pausedElapsed = -1; // 暫停時記錄已過時間，-1=未暫停

        void addKill() { score += 100; }
        void updateSurvival() {
            long now = System.currentTimeMillis();
            if (now - survivalTimer >= 3000) { score += 10; survivalTimer = now; }
            if (now - levelTimer >= 15000)   { level++;     levelTimer = now; }
        }
        // 暫停：記錄目前已累積的時間
        void pause() {
            if (pausedElapsed < 0)
                pausedElapsed = System.currentTimeMillis() - levelTimer;
        }
        // 繼續：把 levelTimer 往前撥，使累積時間不變
        void resume() {
            if (pausedElapsed >= 0) {
                levelTimer = System.currentTimeMillis() - pausedElapsed;
                survivalTimer = System.currentTimeMillis(); // 存活計時也重置起點
                pausedElapsed = -1;
            }
        }
        // Game Over 時直接凍結（不再 resume）
        void freeze() { pause(); }
        void reset() { score = 0; level = 1; pausedElapsed = -1; levelTimer = survivalTimer = System.currentTimeMillis(); }

        void draw(Graphics2D g, int ox, int hp, int maxHp) {
            g.setColor(new Color(5, 10, 25));
            g.fillRect(ox, 0, 180, MH);
            g.setColor(new Color(30, 80, 150));
            g.drawLine(ox, 0, ox, MH);
            int y = 30;
            g.setFont(new Font("Courier New", Font.BOLD, 13));
            g.setColor(new Color(100, 180, 255));
            g.drawString("SCORE", ox + 20, y); y += 28;
            g.setFont(new Font("Courier New", Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(score), ox + 20, y); y += 40;
            g.setFont(new Font("Courier New", Font.BOLD, 13));
            g.setColor(new Color(100, 180, 255));
            g.drawString("HP", ox + 20, y); y += 26;
            for (int i = 0; i < maxHp; i++) {
                g.setColor(i < hp ? new Color(230, 50, 50) : new Color(60, 60, 60));
                g.fillOval(ox + 16 + i * 24, y - 16, 18, 18);
            }
            y += 30;
            g.setFont(new Font("Courier New", Font.BOLD, 13));
            g.setColor(new Color(100, 180, 255));
            g.drawString("LEVEL", ox + 20, y); y += 26;
            g.setFont(new Font("Courier New", Font.BOLD, 26));
            g.setColor(new Color(255, 210, 50));
            g.drawString(String.valueOf(level), ox + 20, y); y += 40;
            int barW = 140;
            long elapsed = (pausedElapsed >= 0) ? pausedElapsed : System.currentTimeMillis() - levelTimer;
            int fill = (int)(barW * Math.min(elapsed / 15000.0, 1.0));
            g.setColor(new Color(30, 50, 80));
            g.fillRoundRect(ox + 18, y, barW, 6, 3, 3);
            g.setColor(new Color(50, 150, 255));
            g.fillRoundRect(ox + 18, y, fill, 6, 3, 3);
            y += 30;
            g.setFont(new Font("Courier New", Font.BOLD, 11));
            g.setColor(new Color(80, 120, 180));
            g.drawString("CONTROLS", ox + 20, y); y += 20;
            g.setFont(new Font("Courier New", Font.PLAIN, 11));
            g.setColor(new Color(150, 170, 200));
            for (String l : new String[]{"WASD/↑↓←→ Move", "SPACE  Shoot", "P      Pause", "R      Restart"}) {
                g.drawString(l, ox + 14, y); y += 17;
            }
        }
    }

    // =========================================================
    //  GAME PANEL
    // =========================================================
    static class GamePanel extends JPanel implements ActionListener {
        static final int FPS = 60;
        Timer timer = new Timer(1000 / FPS, this);

        GameMap  map     = new GameMap();
        Player   player  = new Player(MW / 2 - 18, MH - 80);
        List<Bullet>      bullets      = new ArrayList<>();
        List<EnemyBullet> enemyBullets = new ArrayList<>();
        List<Enemy>       enemies      = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();
        ScoreBoard score = new ScoreBoard();

        boolean paused = false, gameOver = false;
        long lastShot = 0, lastSpawn = 0;
        static final long SHOOT_CD = 250, SPAWN_CD_BASE = 3500;
        Random rand = new Random();

        Image offscreen; Graphics2D og;

        GamePanel() {
            setPreferredSize(new Dimension(PANEL_W, MH));
            setBackground(Color.BLACK);
            setFocusable(true);

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    int k = e.getKeyCode();
                    if (k == KeyEvent.VK_P && !gameOver) {
                        paused = !paused;
                        if (paused) score.pause(); else score.resume();
                    }
                    if (k == KeyEvent.VK_R) restartGame();
                    if (!paused && !gameOver) player.keyDown(k);
                }
                @Override public void keyReleased(KeyEvent e) { player.keyUp(e.getKeyCode()); }
            });
        }

        void startGame() {
            requestFocusInWindow();
            spawnWave();
            timer.start();
        }

        void restartGame() {
            player.reset(MW / 2 - 18, MH - 80);
            bullets.clear(); enemyBullets.clear(); enemies.clear(); particles.clear();
            score.reset(); map.generate();
            paused = false; gameOver = false;
            lastSpawn = System.currentTimeMillis();
            spawnWave();
            requestFocusInWindow();
        }

        void tryShoot() {
            long now = System.currentTimeMillis();
            if (now - lastShot >= SHOOT_CD) {
                bullets.add(new Bullet(player.cx(), player.y));
                lastShot = now;
            }
        }

        void spawnWave() {
            int count = 1 + score.level / 2;
            for (int i = 0; i < count; i++) {
                int gx = rand.nextInt(COLS - 2) + 1;
                int gy = rand.nextInt(3);
                if (map.isObs(gx, gy)) continue;
                float spd = 0.8f + score.level * 0.18f + rand.nextFloat() * 0.4f;
                enemies.add(new Enemy(gx * TILE, gy * TILE, spd, rand.nextInt(3)));
            }
            lastSpawn = System.currentTimeMillis();
        }

        void spawnSingle() {
            int gx = rand.nextInt(COLS - 2) + 1, gy = rand.nextInt(2);
            if (!map.isObs(gx, gy)) {
                float spd = 0.8f + score.level * 0.2f + rand.nextFloat() * 0.3f;
                enemies.add(new Enemy(gx * TILE, gy * TILE, spd, rand.nextInt(3)));
            }
        }

        void explosion(float cx, float cy, Color c, int n) {
            for (int i = 0; i < n; i++) particles.add(new Particle(cx, cy, c));
        }

        @Override public void actionPerformed(ActionEvent e) {
            if (!paused && !gameOver) update();
            repaint();
        }

        void update() {
            score.updateSurvival();

            // Auto spawn
            long spawnCd = Math.max(1500, SPAWN_CD_BASE - (long)(score.level * 100));
            if (System.currentTimeMillis() - lastSpawn > spawnCd) {
                spawnSingle();
                lastSpawn = System.currentTimeMillis();
            }

            // Player move
            player.update(map);

            // Player shoot (held SPACE = continuous fire with cooldown)
            if (player.keys.contains(KeyEvent.VK_SPACE)) tryShoot();

            // Bullets
            bullets.forEach(b -> b.update(map));
            bullets.removeIf(b -> !b.active);

            // Enemies move
            for (Enemy en : enemies) en.update(map, player);

            // 敵人射擊
            for (Enemy en : enemies) {
                EnemyBullet eb = en.tryShoot();
                if (eb != null) enemyBullets.add(eb);
            }

            // 敵人子彈移動
            enemyBullets.forEach(eb -> eb.update(map));
            enemyBullets.removeIf(eb -> !eb.active);

            // 敵人子彈 vs 玩家
            for (EnemyBullet eb : enemyBullets) {
                if (eb.active && eb.bounds().intersects(player.bounds())) {
                    eb.active = false;
                    if (player.takeDamage()) {
                        explosion(player.cx(), player.cy(), new Color(255, 80, 80), 10);
                    }
                }
            }

            // Bullet vs Enemy
            for (Bullet b : bullets) {
                for (Enemy en : enemies) {
                    if (en.alive && b.active && b.bounds().intersects(en.bounds())) {
                        b.active = false;
                        if (en.hit()) {
                            en.alive = false;
                            score.addKill();
                            explosion(en.x + Enemy.SZ / 2f, en.y + Enemy.SZ / 2f, new Color(255, 160, 30), 22);
                        } else {
                            explosion(en.x + Enemy.SZ / 2f, en.y + Enemy.SZ / 2f, new Color(255, 80, 80), 8);
                        }
                    }
                }
            }

            // Enemy vs Player
            for (Enemy en : enemies) {
                if (en.alive && en.bounds().intersects(player.bounds())) {
                    if (player.takeDamage()) {
                        explosion(player.cx(), player.cy(), new Color(255, 80, 80), 14);
                        en.alive = false;
                    }
                }
            }

            enemies.removeIf(en -> !en.alive);
            if (enemies.isEmpty()) spawnWave();

            particles.forEach(Particle::update);
            particles.removeIf(Particle::dead);

            if (player.hp <= 0) { gameOver = true; score.freeze(); }
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (offscreen == null || offscreen.getWidth(null) != PANEL_W) {
                offscreen = createImage(PANEL_W, MH);
                og = (Graphics2D) offscreen.getGraphics();
            }
            Graphics2D g = og;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            map.draw(g);
            particles.forEach(p -> p.draw(g));
            bullets.forEach(b -> b.draw(g));
            enemyBullets.forEach(eb -> eb.draw(g));
            enemies.forEach(en -> en.draw(g));
            player.draw(g);
            score.draw(g, MW, player.hp, Player.MAX_HP);

            g.setColor(new Color(40, 100, 200, 80));
            g.fillRect(MW - 2, 0, 4, MH);

            if (paused) {
                g.setColor(new Color(0, 0, 0, 140));
                g.fillRect(0, 0, MW, MH);
                drawCenter(g, "PAUSED", MW/2, MH/2-20, new Font("Courier New", Font.BOLD, 52), new Color(255, 210, 50));
                drawCenter(g, "Press P to resume", MW/2, MH/2+40, new Font("Courier New", Font.PLAIN, 20), Color.WHITE);
            }
            if (gameOver) {
                g.setColor(new Color(0, 0, 0, 170));
                g.fillRect(0, 0, MW, MH);
                drawCenter(g, "GAME OVER", MW/2, MH/2-50, new Font("Courier New", Font.BOLD, 56), new Color(220, 40, 40));
                drawCenter(g, "SCORE: " + score.score, MW/2, MH/2+10, new Font("Courier New", Font.BOLD, 28), new Color(255, 210, 50));
                drawCenter(g, "Press R to Restart", MW/2, MH/2+60, new Font("Courier New", Font.PLAIN, 20), Color.WHITE);
            }

            g0.drawImage(offscreen, 0, 0, null);
        }

        void drawCenter(Graphics2D g, String text, int cx, int cy, Font font, Color color) {
            g.setFont(font); g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, cx - fm.stringWidth(text) / 2, cy);
        }
    }
}