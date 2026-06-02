import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * 基於 Merkle Tree 的檔案完整性驗證系統
 * Merkle Tree-based File Integrity Verification System
 *
 * 核心演算法：
 *  - Merkle Tree 建構：O(n)
 *  - Merkle Proof 生成：O(log n)
 *  - Merkle Proof 驗證：O(log n)
 */
public class MerkleFileVerifier extends JFrame {

    // ================================================================
    //  SHA-256 工具
    // ================================================================
    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "ERR"; }
    }

    static String sha256File(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(f)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        byte[] h = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ================================================================
    //  Merkle Tree 節點
    // ================================================================
    static class MerkleNode {
        String hash;
        MerkleNode left, right;
        String fileName;   // 僅葉節點有值
        boolean isLeaf;

        // 葉節點
        MerkleNode(String fileName, String fileHash) {
            this.fileName = fileName;
            this.hash     = fileHash;
            this.isLeaf   = true;
        }

        // 內部節點
        MerkleNode(MerkleNode left, MerkleNode right) {
            this.left   = left;
            this.right  = right;
            this.isLeaf = false;
            this.hash   = sha256(left.hash + right.hash);
        }
    }

    // ================================================================
    //  Merkle Tree
    // ================================================================
    static class MerkleTree {
        MerkleNode root;
        List<MerkleNode> leaves = new ArrayList<>();
        List<String> fileNames  = new ArrayList<>();

        // 建構樹 O(n)
        void build(List<String> names, List<String> hashes) {
            leaves.clear();
            fileNames.clear();
            if (names.isEmpty()) { root = null; return; }

            // 建立葉節點
            List<MerkleNode> level = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                MerkleNode leaf = new MerkleNode(names.get(i), hashes.get(i));
                leaves.add(leaf);
                fileNames.add(names.get(i));
                level.add(leaf);
            }

            // 若葉節點數為奇數，複製最後一個（標準做法）
            while (level.size() > 1) {
                List<MerkleNode> parent = new ArrayList<>();
                if (level.size() % 2 == 1) level.add(level.get(level.size() - 1));
                for (int i = 0; i < level.size(); i += 2) {
                    parent.add(new MerkleNode(level.get(i), level.get(i + 1)));
                }
                level = parent;
            }
            root = level.get(0);
        }

        String getRootHash() {
            return root == null ? "（空）" : root.hash;
        }

        // Merkle Proof：生成某葉節點的驗證路徑 O(log n)
        // 回傳：List of [sibling_hash, position("L"/"R")]
        List<String[]> generateProof(int leafIndex) {
            List<String[]> proof = new ArrayList<>();
            if (root == null || leafIndex >= leaves.size()) return proof;
            collectProof(root, leaves.get(leafIndex).hash, proof);
            return proof;
        }

        private boolean collectProof(MerkleNode node, String target, List<String[]> proof) {
            if (node == null) return false;
            if (node.isLeaf) return node.hash.equals(target);

            if (collectProof(node.left, target, proof)) {
                proof.add(new String[]{ node.right.hash, "R" });
                return true;
            }
            if (collectProof(node.right, target, proof)) {
                proof.add(new String[]{ node.left.hash, "L" });
                return true;
            }
            return false;
        }

        // Merkle Proof 驗證 O(log n)
        static boolean verifyProof(String leafHash, List<String[]> proof, String rootHash) {
            String current = leafHash;
            for (String[] step : proof) {
                String siblingHash = step[0];
                String pos         = step[1];
                if (pos.equals("R")) {
                    current = sha256(current + siblingHash);
                } else {
                    current = sha256(siblingHash + current);
                }
            }
            return current.equals(rootHash);
        }

        int size() { return leaves.size(); }
    }

    // ================================================================
    //  Block（含 Merkle Tree）
    // ================================================================
    static class Block {
        int index;
        String timestamp;
        String prevHash;
        String blockHash;
        MerkleTree merkleTree = new MerkleTree();
        List<String> fileNames  = new ArrayList<>();
        List<String> fileHashes = new ArrayList<>();
        List<Long>   fileSizes  = new ArrayList<>();

        Block(int index, String prevHash) {
            this.index     = index;
            this.prevHash  = prevHash;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            rebuild();
        }

        void addFile(String name, String hash, long size) {
            fileNames.add(name);
            fileHashes.add(hash);
            fileSizes.add(size);
            rebuild();
        }

        void rebuild() {
            merkleTree.build(fileNames, fileHashes);
            blockHash = sha256(index + timestamp + prevHash + merkleTree.getRootHash());
        }

        boolean verifyFile(String name, String currentHash) {
            int idx = fileNames.indexOf(name);
            if (idx < 0) return false;
            // 1. 比對 leaf hash
            if (!fileHashes.get(idx).equals(currentHash)) return false;
            // 2. 用 Merkle Proof 驗證（O(log n)）
            List<String[]> proof = merkleTree.generateProof(idx);
            return MerkleTree.verifyProof(currentHash, proof, merkleTree.getRootHash());
        }
    }

    // ================================================================
    //  Blockchain
    // ================================================================
    static class Blockchain {
        List<Block> chain = new ArrayList<>();

        Blockchain() {
            Block genesis = new Block(0, "0000000000000000");
            chain.add(genesis);
        }

        Block latestBlock() { return chain.get(chain.size() - 1); }

        Block newBlock() {
            Block b = new Block(chain.size(), latestBlock().blockHash);
            chain.add(b);
            return b;
        }

        boolean isChainValid() {
            for (int i = 1; i < chain.size(); i++) {
                Block cur = chain.get(i), prev = chain.get(i - 1);
                String expected = sha256(cur.index + cur.timestamp + cur.prevHash + cur.merkleTree.getRootHash());
                if (!cur.blockHash.equals(expected)) return false;
                if (!cur.prevHash.equals(prev.blockHash)) return false;
            }
            return true;
        }
    }

    // ================================================================
    //  Merkle Tree 視覺化面板
    // ================================================================
    static class MerkleTreePanel extends JPanel {
        MerkleTree tree;
        int highlightLeaf = -1;
        List<String[]> proofPath = new ArrayList<>();

        // 顏色
        static final Color COL_BG      = new Color(14, 20, 34);
        static final Color COL_INNER   = new Color(35, 70, 120);
        static final Color COL_LEAF    = new Color(30, 100, 70);
        static final Color COL_PROOF   = new Color(200, 140, 30);
        static final Color COL_TARGET  = new Color(60, 200, 120);
        static final Color COL_ROOT    = new Color(80, 140, 220);
        static final Color COL_EDGE    = new Color(50, 80, 130);
        static final Color COL_PROOF_E = new Color(200, 140, 30);
        static final Color COL_TEXT    = new Color(180, 210, 255);
        static final Color COL_GENESIS = new Color(60, 60, 80);

        void setTree(MerkleTree t, int highlight, List<String[]> proof) {
            this.tree = t;
            this.highlightLeaf = highlight;
            this.proofPath = proof != null ? proof : new ArrayList<>();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(COL_BG);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (tree == null || tree.root == null) {
                g.setColor(new Color(80, 100, 140));
                g.setFont(new Font("Monospaced", Font.PLAIN, 13));
                g.drawString("尚未建立 Merkle Tree", getWidth() / 2 - 90, getHeight() / 2);
                return;
            }

            // 計算樹高
            int depth = treeDepth(tree.root);
            int nodeW = 100, nodeH = 28;
            int hGap = 14, vGap = 52;
            int totalW = (int) Math.pow(2, depth - 1) * (nodeW + hGap);
            int startX = Math.max(getWidth() / 2, totalW / 2);
            int startY = 30;

            drawNode(g, tree.root, startX, startY, totalW / 2, nodeW, nodeH, hGap, vGap, depth, 0);
        }

        private void drawNode(Graphics2D g, MerkleNode node, int x, int y,
                              int spread, int nW, int nH, int hGap, int vGap,
                              int depth, int currentDepth) {
            if (node == null) return;

            if (!node.isLeaf) {
                int childY = y + nH + vGap;
                int leftX  = x - spread / 2;
                int rightX = x + spread / 2;
                // 畫邊
                boolean leftProof  = isProofNode(node.left,  proofPath);
                boolean rightProof = isProofNode(node.right, proofPath);
                drawEdge(g, x, y + nH, leftX,  childY, leftProof  ? COL_PROOF_E : COL_EDGE);
                drawEdge(g, x, y + nH, rightX, childY, rightProof ? COL_PROOF_E : COL_EDGE);
                drawNode(g, node.left,  leftX,  childY, spread / 2, nW, nH, hGap, vGap, depth, currentDepth + 1);
                drawNode(g, node.right, childY > 0 ? rightX : rightX, childY, spread / 2, nW, nH, hGap, vGap, depth, currentDepth + 1);
            }

            // 決定顏色
            Color bg, border;
            boolean isTarget = false, isProof = false;
            if (currentDepth == 0) {
                bg = COL_ROOT; border = new Color(100, 180, 255);
            } else if (node.isLeaf) {
                int li = leafIndex(node);
                if (li == highlightLeaf) {
                    bg = COL_TARGET; border = new Color(80, 255, 160); isTarget = true;
                } else if (isProofLeaf(node)) {
                    bg = COL_PROOF; border = new Color(255, 200, 60); isProof = true;
                } else {
                    bg = COL_LEAF; border = new Color(40, 140, 90);
                }
            } else {
                if (isProofNode(node, proofPath)) {
                    bg = new Color(100, 70, 20); border = COL_PROOF; isProof = true;
                } else {
                    bg = COL_INNER; border = new Color(50, 100, 170);
                }
            }

            int rx = x - nW / 2, ry = y;
            // 發光效果
            if (isTarget || isProof) {
                g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 60));
                g.fillRoundRect(rx - 4, ry - 4, nW + 8, nH + 8, 14, 14);
            }
            g.setColor(bg);
            g.fillRoundRect(rx, ry, nW, nH, 8, 8);
            g.setColor(border);
            g.setStroke(new BasicStroke(isTarget || isProof ? 2f : 1f));
            g.drawRoundRect(rx, ry, nW, nH, 8, 8);
            g.setStroke(new BasicStroke(1f));

            // 文字
            g.setColor(COL_TEXT);
            String label;
            if (currentDepth == 0) {
                label = "Root";
            } else if (node.isLeaf) {
                String fn = node.fileName;
                label = fn.length() > 10 ? fn.substring(0, 9) + "…" : fn;
            } else {
                label = "H" + currentDepth;
            }
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, x - fm.stringWidth(label) / 2, ry + 12);

            // hash 縮寫
            String hashShort = node.hash.substring(0, 8) + "…";
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            fm = g.getFontMetrics();
            g.setColor(new Color(140, 170, 220));
            g.drawString(hashShort, x - fm.stringWidth(hashShort) / 2, ry + 23);
        }

        private void drawEdge(Graphics2D g, int x1, int y1, int x2, int y2, Color c) {
            g.setColor(c);
            g.setStroke(new BasicStroke(c.equals(COL_PROOF_E) ? 2f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x1, y1, x2, y2);
            g.setStroke(new BasicStroke(1f));
        }

        private int treeDepth(MerkleNode n) {
            if (n == null) return 0;
            if (n.isLeaf) return 1;
            return 1 + Math.max(treeDepth(n.left), treeDepth(n.right));
        }

        private int leafIndex(MerkleNode n) {
            if (tree == null) return -1;
            return tree.leaves.indexOf(n);
        }

        private boolean isProofNode(MerkleNode n, List<String[]> proof) {
            if (n == null || proof == null) return false;
            for (String[] step : proof) {
                if (n.hash.equals(step[0])) return true;
            }
            return false;
        }

        private boolean isProofLeaf(MerkleNode n) {
            return isProofNode(n, proofPath);
        }
    }

    // ================================================================
    //  主 GUI
    // ================================================================
    private Blockchain blockchain = new Blockchain();
    private Block currentBlock;

    // 左側檔案列表
    private DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private JList<String> fileList;

    // 右側資訊
    private JTextArea infoArea;
    private MerkleTreePanel treePanel;
    private JLabel rootHashLabel;
    private JLabel chainStatusLabel;
    private JLabel statusBar;
    private JTabbedPane rightTabs;

    // 顏色常數
    static final Color C_BG      = new Color(13, 18, 30);
    static final Color C_PANEL   = new Color(20, 28, 46);
    static final Color C_ACCENT  = new Color(64, 196, 255);
    static final Color C_GREEN   = new Color(70, 210, 120);
    static final Color C_ORANGE  = new Color(255, 165, 60);
    static final Color C_RED     = new Color(255, 80, 80);
    static final Color C_TEXT    = new Color(190, 210, 245);
    static final Color C_SUBTLE  = new Color(80, 100, 140);

    public MerkleFileVerifier() {
        setTitle("🌳 Merkle Tree 檔案完整性驗證系統");
        setSize(1100, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        currentBlock = blockchain.newBlock();
        buildUI();
        refreshAll();
    }

    private void buildUI() {
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Top Bar ──
    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PANEL);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, C_ACCENT));

        JLabel title = new JLabel("  🌳  Merkle Tree File Integrity Verifier");
        title.setFont(new Font("Monospaced", Font.BOLD, 17));
        title.setForeground(C_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        chainStatusLabel = new JLabel("● Chain OK  ");
        chainStatusLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        chainStatusLabel.setForeground(C_GREEN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        right.setBackground(C_PANEL);

        // Root Hash 顯示
        rootHashLabel = new JLabel("Root: —");
        rootHashLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        rootHashLabel.setForeground(C_SUBTLE);
        right.add(rootHashLabel);
        right.add(chainStatusLabel);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── Center: Left panel + Right panel ──
    private JSplitPane buildCenter() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        sp.setDividerLocation(320);
        sp.setBackground(C_BG);
        sp.setBorder(null);
        sp.setDividerSize(4);
        return sp;
    }

    // ── Left: 檔案管理 ──
    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(C_BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        // 標題
        JLabel lbl = styledLabel("  當前區塊 #" + currentBlock.index + " — 檔案列表", C_ACCENT, true);
        lbl.setBackground(C_PANEL);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        p.add(lbl, BorderLayout.NORTH);

        // 檔案列表
        fileList = new JList<>(fileListModel);
        fileList.setBackground(C_PANEL);
        fileList.setForeground(C_TEXT);
        fileList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        fileList.setSelectionBackground(new Color(30, 70, 120));
        fileList.setSelectionForeground(Color.WHITE);
        fileList.setFixedCellHeight(26);
        fileList.setCellRenderer(new FileListRenderer());
        fileList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onFileSelect(); });

        JScrollPane scroll = new JScrollPane(fileList);
        scroll.getViewport().setBackground(C_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(35, 55, 90)));
        p.add(scroll, BorderLayout.CENTER);

        // 按鈕區
        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        btnPanel.setBackground(C_BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JButton btnAdd    = mkBtn("📂  新增檔案", C_ACCENT);
        JButton btnVerify = mkBtn("🔍  驗證選中檔案", C_GREEN);
        JButton btnProof  = mkBtn("🔗  生成 Merkle Proof", C_ORANGE);
        JButton btnNew    = mkBtn("➕  新區塊", new Color(170, 120, 255));

        btnAdd.addActionListener(e    -> addFiles());
        btnVerify.addActionListener(e -> verifySelected());
        btnProof.addActionListener(e  -> showMerkleProof());
        btnNew.addActionListener(e    -> newBlock());

        btnPanel.add(btnAdd);
        btnPanel.add(btnVerify);
        btnPanel.add(btnProof);
        btnPanel.add(btnNew);
        p.add(btnPanel, BorderLayout.SOUTH);
        return p;
    }

    // ── Right: Tabs（Tree視覺化 + 資訊 + 區塊鏈）──
    private JTabbedPane buildRight() {
        rightTabs = new JTabbedPane();
        rightTabs.setBackground(C_BG);
        rightTabs.setForeground(C_TEXT);
        rightTabs.setFont(new Font("Monospaced", Font.BOLD, 12));

        // Tab 1: Merkle Tree 視覺化
        treePanel = new MerkleTreePanel();
        treePanel.setPreferredSize(new Dimension(600, 400));
        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.setBackground(MerkleTreePanel.COL_BG);
        treeScroll.getViewport().setBackground(MerkleTreePanel.COL_BG);
        treeScroll.setBorder(null);
        rightTabs.addTab("🌳  Merkle Tree", treeScroll);

        // Tab 2: 資訊面板
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setBackground(C_PANEL);
        infoArea.setForeground(C_TEXT);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        infoArea.setText(welcomeText());
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setBorder(null);
        infoScroll.getViewport().setBackground(C_PANEL);
        rightTabs.addTab("📋  詳細資訊", infoScroll);

        // Tab 3: 區塊鏈
        rightTabs.addTab("⛓  區塊鏈", buildChainPanel());

        return rightTabs;
    }

    // 區塊鏈 Tab
    private JPanel buildChainPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);

        JTextArea chainArea = new JTextArea();
        chainArea.setEditable(false);
        chainArea.setBackground(C_PANEL);
        chainArea.setForeground(C_TEXT);
        chainArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        chainArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        chainArea.setName("chainArea");

        JScrollPane sp = new JScrollPane(chainArea);
        sp.setBorder(null);
        sp.getViewport().setBackground(C_PANEL);
        p.add(sp, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(C_BG);
        JButton refresh = mkBtn("🔄  重整", C_ACCENT);
        JButton verify  = mkBtn("🛡  驗證整條鏈", C_GREEN);
        refresh.addActionListener(e -> refreshChainTab(chainArea));
        verify.addActionListener(e  -> { verifyChain(); refreshChainTab(chainArea); });
        btnRow.add(refresh);
        btnRow.add(verify);
        p.add(btnRow, BorderLayout.SOUTH);

        // 初始刷新
        SwingUtilities.invokeLater(() -> refreshChainTab(chainArea));
        return p;
    }

    // ── Status Bar ──
    private JLabel buildStatusBar() {
        statusBar = new JLabel("  就緒");
        statusBar.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusBar.setForeground(C_SUBTLE);
        statusBar.setOpaque(true);
        statusBar.setBackground(C_PANEL);
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return statusBar;
    }

    // ================================================================
    //  功能：新增檔案
    // ================================================================
    private void addFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("選擇檔案加入 Merkle Tree（可多選）");
        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
            "圖片 (*.jpg, *.png, *.gif, *.bmp, *.webp)", "jpg", "jpeg", "png", "gif", "bmp", "webp"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
            "文字 (*.txt, *.csv, *.json, *.xml)", "txt", "csv", "json", "xml"));

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File[] files = fc.getSelectedFiles();
        int added = 0;
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (currentBlock.fileNames.contains(f.getName())) {
                sb.append("⚠ 已存在：").append(f.getName()).append("\n");
                continue;
            }
            try {
                setStatus("計算 SHA-256: " + f.getName());
                String hash = sha256File(f);
                currentBlock.addFile(f.getName(), hash, f.length());
                added++;
                sb.append("✅ 新增：").append(f.getName()).append("\n");
            } catch (Exception ex) {
                sb.append("❌ 失敗：").append(f.getName()).append(" (").append(ex.getMessage()).append(")\n");
            }
        }
        refreshAll();
        setStatus("新增 " + added + " 個檔案，Merkle Root 已更新");
        setInfo("新增結果\n" + "═".repeat(40) + "\n" + sb +
            "\n新 Merkle Root:\n" + currentBlock.merkleTree.getRootHash());
        treePanel.setTree(currentBlock.merkleTree, -1, null);
    }

    // ================================================================
    //  功能：驗證選中檔案
    // ================================================================
    private void verifySelected() {
        int idx = fileList.getSelectedIndex();
        if (idx < 0) { setStatus("請先選擇一個檔案"); return; }
        String name = currentBlock.fileNames.get(idx);

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("選擇「" + name + "」進行驗證");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            String currentHash = sha256File(file);
            String storedHash  = currentBlock.fileHashes.get(idx);
            boolean hashMatch  = currentHash.equals(storedHash);

            // 用 Merkle Proof 驗證
            List<String[]> proof = currentBlock.merkleTree.generateProof(idx);
            boolean proofOK = MerkleTree.verifyProof(currentHash, proof, currentBlock.merkleTree.getRootHash());

            boolean ok = hashMatch && proofOK;

            // 高亮樹
            treePanel.setTree(currentBlock.merkleTree, ok ? idx : idx, proof);
            rightTabs.setSelectedIndex(0); // 切到 Tree tab

            StringBuilder sb = new StringBuilder();
            sb.append(ok ? "✅ 驗證通過 — 檔案完整\n" : "❌ 驗證失敗 — 檔案已被篡改\n");
            sb.append("═".repeat(44)).append("\n");
            sb.append("檔案名稱  ：").append(name).append("\n");
            sb.append("Hash 比對  ：").append(hashMatch ? "✅ 相符" : "❌ 不符").append("\n");
            sb.append("Merkle Proof：").append(proofOK ? "✅ 有效" : "❌ 無效").append("\n\n");
            sb.append("記錄 Hash:\n").append(storedHash).append("\n\n");
            sb.append("當前 Hash:\n").append(currentHash).append("\n\n");
            sb.append("Proof 步驟（").append(proof.size()).append(" 步，O(log n) = O(").append(proof.size()).append(")）\n");
            for (int i = 0; i < proof.size(); i++) {
                sb.append(String.format("  Step %d [%s]: %s...\n", i + 1, proof.get(i)[1], proof.get(i)[0].substring(0, 16)));
            }
            sb.append("\nMerkle Root:\n").append(currentBlock.merkleTree.getRootHash());
            setInfo(sb.toString());
            setStatus(ok ? "✅ 驗證通過：" + name : "❌ 驗證失敗：" + name);
        } catch (Exception ex) {
            setStatus("❌ 錯誤：" + ex.getMessage());
        }
    }

    // ================================================================
    //  功能：顯示 Merkle Proof
    // ================================================================
    private void showMerkleProof() {
        int idx = fileList.getSelectedIndex();
        if (idx < 0) { setStatus("請先選擇一個檔案"); return; }

        String name = currentBlock.fileNames.get(idx);
        List<String[]> proof = currentBlock.merkleTree.generateProof(idx);

        treePanel.setTree(currentBlock.merkleTree, idx, proof);
        rightTabs.setSelectedIndex(0);

        StringBuilder sb = new StringBuilder();
        sb.append("🔗 Merkle Proof — " + name + "\n");
        sb.append("═".repeat(44)).append("\n\n");
        sb.append("Leaf Hash:\n").append(currentBlock.fileHashes.get(idx)).append("\n\n");
        sb.append("驗證路徑（共 ").append(proof.size()).append(" 步）：\n");
        sb.append("每步只需 1 個 sibling hash，複雜度 O(log n)\n\n");

        String cur = currentBlock.fileHashes.get(idx);
        for (int i = 0; i < proof.size(); i++) {
            String sib = proof.get(i)[0];
            String pos = proof.get(i)[1];
            String next;
            if (pos.equals("R")) {
                next = sha256(cur + sib);
                sb.append(String.format("Step %d: SHA256( current + sibling[R] )\n", i + 1));
            } else {
                next = sha256(sib + cur);
                sb.append(String.format("Step %d: SHA256( sibling[L] + current )\n", i + 1));
            }
            sb.append("  sibling: ").append(sib, 0, 16).append("...\n");
            sb.append("  result : ").append(next, 0, 16).append("...\n\n");
            cur = next;
        }
        sb.append("最終 Hash:\n").append(cur).append("\n\n");
        sb.append("Merkle Root:\n").append(currentBlock.merkleTree.getRootHash()).append("\n\n");
        sb.append(cur.equals(currentBlock.merkleTree.getRootHash())
            ? "✅ Proof 有效：路徑 Hash 與 Root 相符" : "❌ Proof 無效");

        setInfo(sb.toString());
        setStatus("Merkle Proof 已生成：" + proof.size() + " 步（O(log " + currentBlock.merkleTree.size() + ")）");
    }

    // ================================================================
    //  功能：新區塊
    // ================================================================
    private void newBlock() {
        if (currentBlock.fileNames.isEmpty()) {
            setStatus("⚠ 目前區塊無任何檔案，不需要新建");
            return;
        }
        currentBlock = blockchain.newBlock();
        fileListModel.clear();
        treePanel.setTree(null, -1, null);
        setInfo("已建立新區塊 #" + currentBlock.index + "\n\n可以開始新增檔案。");
        setStatus("✅ 新區塊 #" + currentBlock.index + " 已建立");
        refreshRootHash();
    }

    // ================================================================
    //  功能：驗證整條鏈
    // ================================================================
    private void verifyChain() {
        boolean ok = blockchain.isChainValid();
        chainStatusLabel.setText(ok ? "● Chain OK  " : "● Chain BROKEN  ");
        chainStatusLabel.setForeground(ok ? C_GREEN : C_RED);
        setStatus(ok ? "✅ 整條鏈驗證通過，共 " + blockchain.chain.size() + " 個區塊"
                     : "❌ 區塊鏈驗證失敗！");
        JOptionPane.showMessageDialog(this,
            ok ? "✅ 區塊鏈完整\n所有 Block Hash 及 prevHash 連結正確。"
               : "❌ 區塊鏈遭篡改！\n偵測到 Hash 不一致。",
            "區塊鏈驗證", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    // ================================================================
    //  UI 輔助
    // ================================================================
    private void onFileSelect() {
        int idx = fileList.getSelectedIndex();
        if (idx < 0 || idx >= currentBlock.fileNames.size()) return;
        treePanel.setTree(currentBlock.merkleTree, idx, null);
        rightTabs.setSelectedIndex(0);
        String name = currentBlock.fileNames.get(idx);
        String hash = currentBlock.fileHashes.get(idx);
        long   size = currentBlock.fileSizes.get(idx);
        List<String[]> proof = currentBlock.merkleTree.generateProof(idx);
        setInfo("檔案詳情\n" + "═".repeat(44) + "\n\n"
            + "名稱：" + name + "\n"
            + "大小：" + fmtSize(size) + "\n"
            + "Leaf Hash:\n" + hash + "\n\n"
            + "Proof 深度：" + proof.size() + " 步（O(log " + currentBlock.merkleTree.size() + ")）\n\n"
            + "Merkle Root:\n" + currentBlock.merkleTree.getRootHash());
    }

    private void refreshAll() {
        fileListModel.clear();
        for (int i = 0; i < currentBlock.fileNames.size(); i++) {
            fileListModel.addElement(currentBlock.fileNames.get(i));
        }
        treePanel.setTree(currentBlock.merkleTree, -1, null);
        refreshRootHash();
    }

    private void refreshRootHash() {
        String root = currentBlock.merkleTree.getRootHash();
        rootHashLabel.setText("Root: " + (root.equals("（空）") ? "—" : root.substring(0, 16) + "…"));
    }

    private void refreshChainTab(JTextArea area) {
        StringBuilder sb = new StringBuilder();
        sb.append("區塊鏈狀態\n").append("═".repeat(60)).append("\n\n");
        for (Block b : blockchain.chain) {
            sb.append(b.index == 0 ? "[ Genesis Block ]\n" : "[ Block #" + b.index + " ]\n");
            sb.append("  時間       : ").append(b.timestamp).append("\n");
            sb.append("  檔案數     : ").append(b.fileNames.size()).append("\n");
            sb.append("  Merkle Root: ").append(b.merkleTree.getRootHash(), 0,
                Math.min(32, b.merkleTree.getRootHash().length())).append("...\n");
            sb.append("  Block Hash : ").append(b.blockHash, 0, 32).append("...\n");
            sb.append("  Prev  Hash : ").append(b.prevHash, 0, 32).append("...\n");
            if (!b.fileNames.isEmpty()) {
                sb.append("  檔案列表   :\n");
                for (String n : b.fileNames) sb.append("    • ").append(n).append("\n");
            }
            sb.append("\n");
        }
        area.setText(sb.toString());
        area.setCaretPosition(0);
    }

    private void setInfo(String text) {
        infoArea.setText(text);
        infoArea.setCaretPosition(0);
        rightTabs.setSelectedIndex(1);
    }

    private void setStatus(String msg) { statusBar.setText("  " + msg); }

    private String welcomeText() {
        return "歡迎使用 Merkle Tree 檔案完整性驗證系統\n" +
            "═".repeat(44) + "\n\n" +
            "操作說明：\n" +
            "1. 點「新增檔案」選擇一個或多個檔案\n" +
            "   → 系統計算 SHA-256 並建構 Merkle Tree\n\n" +
            "2. 點「驗證選中檔案」\n" +
            "   → 比對 Hash 並執行 Merkle Proof 驗證\n\n" +
            "3. 點「生成 Merkle Proof」\n" +
            "   → 顯示從葉節點到 Root 的驗證路徑\n" +
            "   → 只需 O(log n) 個 Hash 即可驗證\n\n" +
            "4. 點「新區塊」→ 將當前樹封存進區塊鏈\n\n" +
            "演算法複雜度：\n" +
            "  建構 Merkle Tree : O(n)\n" +
            "  生成 Merkle Proof: O(log n)\n" +
            "  驗證 Merkle Proof: O(log n)\n";
    }

    // ── 自訂 List CellRenderer ──
    class FileListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object val,
                int idx, boolean selected, boolean focused) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, val, idx, selected, focused);
            lbl.setText("  " + (idx + 1) + ".  " + val);
            lbl.setBackground(selected ? new Color(30, 70, 120) : (idx % 2 == 0 ? C_PANEL : new Color(24, 34, 55)));
            lbl.setForeground(C_TEXT);
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            return lbl;
        }
    }

    // ── 按鈕工廠 ──
    private JButton mkBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Dialog", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(C_PANEL);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 1),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(30, 45, 70)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(C_PANEL); }
        });
        return b;
    }

    private JLabel styledLabel(String text, Color fg, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, 13));
        l.setForeground(fg);
        return l;
    }

    static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / (1024.0 * 1024));
    }

    // ================================================================
    //  Main
    // ================================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MerkleFileVerifier().setVisible(true));
    }
}
