import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.List;

/**
 * 基於區塊鏈的檔案完整性驗證系統
 * Blockchain-based File Integrity Verification System
 */
public class BlockchainFileVerifier extends JFrame {

    // ==================== 區塊結構 ====================
    static class Block {
        int index;
        String timestamp;
        String fileName;
        String fileHash;
        long fileSize;
        String prevHash;
        String blockHash;

        Block(int index, String fileName, String fileHash, long fileSize, String prevHash) {
            this.index      = index;
            this.fileName   = fileName;
            this.fileHash   = fileHash;
            this.fileSize   = fileSize;
            this.prevHash   = prevHash;
            this.timestamp  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.blockHash  = calculateBlockHash();
        }

        // 計算此區塊的 hash（包含 prevHash，形成鏈式結構）
        String calculateBlockHash() {
            String data = index + timestamp + fileName + fileHash + fileSize + prevHash;
            return sha256(data);
        }

        // 驗證此區塊 hash 是否正確
        boolean isValid() {
            return blockHash.equals(calculateBlockHash());
        }

        @Override
        public String toString() {
            return String.format("Block #%d | %s | %s | Hash: %s...",
                index, fileName, timestamp, blockHash.substring(0, 12));
        }
    }

    // ==================== 區塊鏈結構 ====================
    static class Blockchain {
        List<Block> chain = new ArrayList<>();

        Blockchain() {
            // 建立創世區塊（Genesis Block）
            Block genesis = new Block(0, "GENESIS", "0", 0, "0000000000000000");
            chain.add(genesis);
        }

        // 新增檔案記錄到鏈上
        Block addFile(String fileName, String fileHash, long fileSize) {
            String prevHash = chain.get(chain.size() - 1).blockHash;
            Block newBlock = new Block(chain.size(), fileName, fileHash, fileSize, prevHash);
            chain.add(newBlock);
            return newBlock;
        }

        // 驗證整條鏈的完整性
        boolean isChainValid() {
            for (int i = 1; i < chain.size(); i++) {
                Block current  = chain.get(i);
                Block previous = chain.get(i - 1);

                // 檢查當前區塊 hash 是否正確
                if (!current.isValid()) return false;

                // 檢查 prevHash 是否對應上一個區塊
                if (!current.prevHash.equals(previous.blockHash)) return false;
            }
            return true;
        }

        // 查找某檔案名稱最新的區塊
        Block findLatestBlock(String fileName) {
            for (int i = chain.size() - 1; i >= 1; i--) {
                if (chain.get(i).fileName.equals(fileName)) {
                    return chain.get(i);
                }
            }
            return null;
        }

        int size() {
            return chain.size() - 1; // 不算創世區塊
        }
    }

    // ==================== SHA-256 工具函式 ====================
    static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    static String sha256File(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        byte[] hash = md.digest();
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    // ==================== GUI 元件 ====================
    private Blockchain blockchain = new Blockchain();
    private DefaultTableModel tableModel;
    private JTable chainTable;
    private JLabel statusLabel;
    private JLabel chainStatusLabel;
    private JTextArea detailArea;

    public BlockchainFileVerifier() {
        setTitle("🔗 區塊鏈檔案完整性驗證系統");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        buildUI();
        refreshTable();
    }

    private void buildUI() {
        // 主色調
        Color darkBg    = new Color(18, 24, 38);
        Color panelBg   = new Color(28, 36, 54);
        Color accent    = new Color(64, 196, 255);
        Color accentGreen = new Color(80, 220, 130);
        Color accentRed   = new Color(255, 90, 90);
        Color textLight = new Color(200, 215, 240);

        getContentPane().setBackground(darkBg);
        setLayout(new BorderLayout(8, 8));

        // ── 頂部標題 ──
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        titlePanel.setBackground(panelBg);
        titlePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, accent));
        JLabel titleLabel = new JLabel("🔗 Blockchain File Integrity Verifier");
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        titleLabel.setForeground(accent);
        chainStatusLabel = new JLabel("● 鏈狀態：正常");
        chainStatusLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        chainStatusLabel.setForeground(accentGreen);
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalStrut(20));
        titlePanel.add(chainStatusLabel);
        add(titlePanel, BorderLayout.NORTH);

        // ── 左側：區塊鏈列表 ──
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBackground(darkBg);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        JLabel chainLabel = new JLabel("  區塊鏈記錄");
        chainLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        chainLabel.setForeground(accent);
        chainLabel.setOpaque(true);
        chainLabel.setBackground(panelBg);
        chainLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        leftPanel.add(chainLabel, BorderLayout.NORTH);

        String[] columns = {"#", "檔案名稱", "時間", "Block Hash (前16碼)"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        chainTable = new JTable(tableModel);
        chainTable.setBackground(panelBg);
        chainTable.setForeground(textLight);
        chainTable.setSelectionBackground(new Color(40, 80, 120));
        chainTable.setSelectionForeground(Color.WHITE);
        chainTable.setGridColor(new Color(40, 55, 80));
        chainTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chainTable.setRowHeight(24);
        chainTable.getTableHeader().setBackground(new Color(35, 50, 75));
        chainTable.getTableHeader().setForeground(accent);
        chainTable.getTableHeader().setFont(new Font("Monospaced", Font.BOLD, 12));
        chainTable.getColumnModel().getColumn(0).setMaxWidth(40);
        chainTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        chainTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        // 點擊列顯示詳細
        chainTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showBlockDetail();
        });

        JScrollPane tableScroll = new JScrollPane(chainTable);
        tableScroll.getViewport().setBackground(panelBg);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 90)));
        leftPanel.add(tableScroll, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(480, 0));
        add(leftPanel, BorderLayout.CENTER);

        // ── 右側：操作面板 + 詳細資訊 ──
        JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setBackground(darkBg);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        // 操作按鈕
        JPanel actionPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        actionPanel.setBackground(darkBg);

        JButton btnAdd    = makeButton("📂  新增檔案到鏈", accent, darkBg);
        JButton btnVerify = makeButton("🔍  驗證檔案", accentGreen, darkBg);
        JButton btnChain  = makeButton("🛡  驗證整條鏈", new Color(180, 140, 255), darkBg);
        JButton btnClear  = makeButton("🗑  清除區塊鏈", accentRed, darkBg);

        actionPanel.add(btnAdd);
        actionPanel.add(btnVerify);
        actionPanel.add(btnChain);
        actionPanel.add(btnClear);

        btnAdd.addActionListener(e    -> addFileToChain());
        btnVerify.addActionListener(e -> verifyFile());
        btnChain.addActionListener(e  -> verifyChain());
        btnClear.addActionListener(e  -> clearChain());

        rightPanel.add(actionPanel, BorderLayout.NORTH);

        // 詳細資訊區
        JLabel detailLabel = new JLabel("  區塊詳情");
        detailLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        detailLabel.setForeground(accent);
        detailLabel.setOpaque(true);
        detailLabel.setBackground(panelBg);
        detailLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setBackground(panelBg);
        detailArea.setForeground(textLight);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        detailArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        detailArea.setText("← 點選左側區塊查看詳情");

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 90)));

        JPanel detailContainer = new JPanel(new BorderLayout());
        detailContainer.setBackground(darkBg);
        detailContainer.add(detailLabel, BorderLayout.NORTH);
        detailContainer.add(detailScroll, BorderLayout.CENTER);
        rightPanel.add(detailContainer, BorderLayout.CENTER);

        // 狀態列
        statusLabel = new JLabel(" 就緒");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(140, 160, 200));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(panelBg);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        rightPanel.add(statusLabel, BorderLayout.SOUTH);

        rightPanel.setPreferredSize(new Dimension(300, 0));
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton makeButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(new Color(28, 36, 54));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(40, 55, 80)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(28, 36, 54)); }
        });
        return btn;
    }

    // ==================== 功能：新增檔案 ====================
    private void addFileToChain() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("選擇要加入區塊鏈的檔案");
        fc.setAcceptAllFileFilterUsed(true); // 保留「所有檔案」選項
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "PDF 文件 (*.pdf)", "pdf"));
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "圖片檔案 (*.jpg, *.jpeg, *.png, *.gif, *.bmp, *.webp)",
            "jpg", "jpeg", "png", "gif", "bmp", "webp"));
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "文字檔案 (*.txt, *.csv, *.json, *.xml)", "txt", "csv", "json", "xml"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            setStatus("計算 SHA-256...");
            String hash = sha256File(file);
            Block block = blockchain.addFile(file.getName(), hash, file.length());
            refreshTable();
            setStatus("✅ 已新增：" + file.getName() + "（Block #" + block.index + "）");
            // 自動選中新區塊
            chainTable.setRowSelectionInterval(chainTable.getRowCount() - 1, chainTable.getRowCount() - 1);
            showBlockDetail();
        } catch (Exception ex) {
            setStatus("❌ 錯誤：" + ex.getMessage());
        }
    }

    // ==================== 功能：驗證檔案 ====================
    private void verifyFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("選擇要驗證的檔案");
        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "PDF 文件 (*.pdf)", "pdf"));
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "圖片檔案 (*.jpg, *.jpeg, *.png, *.gif, *.bmp, *.webp)",
            "jpg", "jpeg", "png", "gif", "bmp", "webp"));
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "文字檔案 (*.txt, *.csv, *.json, *.xml)", "txt", "csv", "json", "xml"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        Block record = blockchain.findLatestBlock(file.getName());

        if (record == null) {
            showResult("⚠️ 未找到記錄",
                "檔案「" + file.getName() + "」\n尚未記錄在區塊鏈中。\n\n請先使用「新增檔案到鏈」功能。",
                false);
            return;
        }

        try {
            setStatus("驗證中...");
            String currentHash = sha256File(file);
            boolean match = currentHash.equals(record.fileHash);

            String msg;
            if (match) {
                msg = "✅ 驗證成功！檔案完整\n\n"
                    + "檔案名稱：" + file.getName() + "\n"
                    + "區塊編號：#" + record.index + "\n"
                    + "記錄時間：" + record.timestamp + "\n\n"
                    + "SHA-256（記錄）：\n" + record.fileHash + "\n\n"
                    + "SHA-256（當前）：\n" + currentHash + "\n\n"
                    + "→ 兩個 Hash 完全相同，檔案未被篡改。";
            } else {
                msg = "❌ 驗證失敗！檔案已被修改\n\n"
                    + "檔案名稱：" + file.getName() + "\n"
                    + "區塊編號：#" + record.index + "\n"
                    + "記錄時間：" + record.timestamp + "\n\n"
                    + "SHA-256（記錄）：\n" + record.fileHash + "\n\n"
                    + "SHA-256（當前）：\n" + currentHash + "\n\n"
                    + "→ Hash 不符，檔案已被篡改！";
            }

            setStatus(match ? "✅ 驗證成功：" + file.getName() : "❌ 驗證失敗：" + file.getName());
            showResult(match ? "驗證結果：通過" : "驗證結果：失敗", msg, match);
        } catch (Exception ex) {
            setStatus("❌ 錯誤：" + ex.getMessage());
        }
    }

    // ==================== 功能：驗證整條鏈 ====================
    private void verifyChain() {
        boolean valid = blockchain.isChainValid();
        chainStatusLabel.setText(valid ? "● 鏈狀態：正常" : "● 鏈狀態：已損毀");
        chainStatusLabel.setForeground(valid ? new Color(80, 220, 130) : new Color(255, 90, 90));

        String msg;
        if (valid) {
            msg = "✅ 區塊鏈完整性驗證通過\n\n"
                + "區塊總數：" + (blockchain.chain.size()) + "（含創世區塊）\n"
                + "記錄檔案：" + blockchain.size() + " 個\n\n"
                + "每個區塊的 Hash 均正確，\n且 prevHash 連結完整，\n鏈結構未遭竄改。";
        } else {
            msg = "❌ 區塊鏈完整性驗證失敗\n\n"
                + "偵測到區塊鏈遭到竄改！\n"
                + "某個區塊的 Hash 或 prevHash 不一致。";
        }

        setStatus(valid ? "✅ 整條鏈驗證通過" : "❌ 整條鏈驗證失敗");
        showResult(valid ? "區塊鏈驗證：通過" : "區塊鏈驗證：失敗", msg, valid);
    }

    // ==================== 功能：清除區塊鏈 ====================
    private void clearChain() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "確定要清除整條區塊鏈嗎？\n所有記錄將遺失。",
            "確認清除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            blockchain = new Blockchain();
            refreshTable();
            detailArea.setText("← 點選左側區塊查看詳情");
            chainStatusLabel.setText("● 鏈狀態：正常");
            chainStatusLabel.setForeground(new Color(80, 220, 130));
            setStatus("已清除區塊鏈，創世區塊重新建立。");
        }
    }

    // ==================== UI 更新 ====================
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Block b : blockchain.chain) {
            tableModel.addRow(new Object[]{
                b.index == 0 ? "G" : String.valueOf(b.index),
                b.index == 0 ? "[創世區塊]" : b.fileName,
                b.timestamp,
                b.blockHash.substring(0, 16) + "..."
            });
        }
    }

    private void showBlockDetail() {
        int row = chainTable.getSelectedRow();
        if (row < 0 || row >= blockchain.chain.size()) return;
        Block b = blockchain.chain.get(row);
        String detail = (b.index == 0 ? "=== 創世區塊 (Genesis) ===\n" : "=== Block #" + b.index + " ===\n")
            + "\n時間：" + b.timestamp
            + "\n\n檔案名稱：" + b.fileName
            + (b.index > 0 ? "\n檔案大小：" + formatSize(b.fileSize) : "")
            + "\n\n檔案 SHA-256：\n" + b.fileHash
            + "\n\n前一區塊 Hash：\n" + b.prevHash
            + "\n\n本區塊 Hash：\n" + b.blockHash
            + "\n\n區塊完整性：" + (b.isValid() ? "✅ 正常" : "❌ 異常");
        detailArea.setText(detail);
        detailArea.setCaretPosition(0);
    }

    private void showResult(String title, String msg, boolean success) {
        int type = success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(this, msg, title, type);
    }

    private void setStatus(String msg) {
        statusLabel.setText(" " + msg);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // ==================== 主程式 ====================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new BlockchainFileVerifier().setVisible(true));
    }
}
