package ex12;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CalculatorLayoutBuilder extends JFrame {

    private JTextField tf;
    private JPanel panel;
    private String[][] labels;

    public static void main(String[] args) {
        new CalculatorLayoutBuilder("電卓");
    }

    public CalculatorLayoutBuilder(String title) {
        super(title);
        
        labels = new String[][] {
            {"TF","TF","TF","TF","TF"},
            {"7", "8", "9", "DEL", "AC"},
            {"4", "5", "6", "*", "/"},
            {"1", "2", "3", "+", "-"},
            {"0", ".", " ", "=", "="}
        };
        
        JPanel pane = (JPanel)getContentPane();
        panel = new JPanel(new GridBagLayout());
        pane.add(panel, BorderLayout.CENTER);

        JButton editBtn = new JButton("編集ウィンドウを開く");
        editBtn.setFont(new Font("Meiryo", Font.BOLD, 20));
        editBtn.addActionListener(e -> new EditorWindow(this, labels));
        pane.add(editBtn, BorderLayout.SOUTH);

        setSize(500, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        rebuildUI();
    }

       //TF の結合サイズを確定
     
    private Rectangle decideTfSpan() {
        int rows = labels.length;
        int cols = labels[0].length;

        boolean[][] tfMap = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tfMap[r][c] = labels[r][c].trim().equalsIgnoreCase("TF");
            }
        }

        // M×N ここが一番優先となる
        for (int h = rows; h >= 2; h--) {
            for (int w = cols; w >= 2; w--) {
                boolean allCellsAreTF = true;
                for (int r = 0; r < h; r++) {
                    for (int c = 0; c < w; c++) {
                        if (!tfMap[r][c]) {
                        	allCellsAreTF = false;
                            break;
                        }
                    }
                    if (!allCellsAreTF) break;
                }
                if (allCellsAreTF) return new Rectangle(0, 0, w, h);
            }
        }

        //横 M×1
        int w = 0;
        while (w < cols && tfMap[0][w]) w++;
        if (w >= 2) return new Rectangle(0, 0, w, 1);

        // 縦 1×N
        int h = 0;
        while (h < rows && tfMap[h][0]) h++;
        if (h >= 2) return new Rectangle(0, 0, 1, h);

        return new Rectangle(0, 0, 1, 1);
    }

  
       //ボタン結合（最大矩形）
    private Rectangle findButtonSpan(int sr, int sc, boolean[][] used) {
        String label = labels[sr][sc].trim();
        if (label.isEmpty() || label.equalsIgnoreCase("TF")) return null;

        int rows = labels.length;
        int cols = labels[0].length;

        int maxW = 1;
        int maxH = 1;

        // 横方向
        for (int w = 1; sc + w < cols; w++) {
            if (!label.equals(labels[sr][sc + w].trim()) || used[sr][sc + w]) break;
            maxW++;
        }

        // 縦方向
        for (int h = 1; sr + h < rows; h++) {
            boolean ok = true;
            for (int x = 0; x < maxW; x++) {
                if (!label.equals(labels[sr + h][sc + x].trim()) || used[sr + h][sc + x]) {
                    ok = false;
                    break;
                }
            }
            if (!ok) break;
            maxH++;
        }

        return new Rectangle(sc, sr, maxW, maxH);
    }

       //UI 再構築
    public void updateLabels(String[][] newLabels) {
        this.labels = newLabels;
        rebuildUI();
    }

    private void rebuildUI() {
        panel.removeAll();

        int rows = labels.length;
        int cols = labels[0].length;

        tf = new JTextField();
        tf.setFont(new Font("Arial", Font.PLAIN, 24));
        tf.setHorizontalAlignment(JTextField.RIGHT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        boolean[][] used = new boolean[rows][cols];

        // TF を先に配置
        Rectangle tfRect = decideTfSpan();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = tfRect.width;
        gbc.gridheight = tfRect.height;
        panel.add(tf, gbc);

        for (int y = 0; y < tfRect.height; y++) {
            for (int x = 0; x < tfRect.width; x++) {
                used[y][x] = true;
            }
        }

        // ボタン配置（結合あり）
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                if (used[r][c]) continue;

                String label = labels[r][c].trim();
                if (label.isEmpty() || label.equalsIgnoreCase("TF")) continue;

                Rectangle span = findButtonSpan(r, c, used);

                gbc.gridx = span.x;
                gbc.gridy = span.y;
                gbc.gridwidth = span.width;
                gbc.gridheight = span.height;

                panel.add(createButton(label), gbc);

                for (int y = span.y; y < span.y + span.height; y++) {
                    for (int x = span.x; x < span.x + span.width; x++) {
                        used[y][x] = true;
                    }
                }
            }
        }

        panel.revalidate();
        panel.repaint();
    }

   
       //ボタン生成
    private JButton createButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Arial", Font.BOLD, 20));

        switch (label) {
            case "AC":
                btn.addActionListener(e -> tf.setText(""));
                break;

            case "DEL":
                btn.addActionListener(e -> {
                    if (tf.getText().equals("ERROR")) {
                        tf.setText("");
                        return;
                    }
                    String s = tf.getText();
                    if (!s.isEmpty())
                        tf.setText(s.substring(0, s.length() - 1));
                });
                break;

            case "=":
                btn.addActionListener(e -> evaluate());
                break;

            default:
                btn.addActionListener(e -> {
                    // ERROR のときはクリアしてから入力
                    if (tf.getText().equals("ERROR")) tf.setText("");
                    tf.setText(tf.getText() + label);
                });
        }
        return btn;
    }
    
    
      // 計算処理
    private void evaluate() {
        try {
            tf.setText(eval(tf.getText()).toPlainString());
        } catch (Exception e) {
            tf.setText("ERROR");
        }
    }
    private BigDecimal eval(String expr) {
        expr = expr.replaceAll("\\s+", "");

        ArrayList<BigDecimal> nums = new ArrayList<>();
        ArrayList<Character> ops = new ArrayList<>();

        StringBuilder buf = new StringBuilder();

        for (char c : expr.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                buf.append(c);
            } else {
                nums.add(new BigDecimal(buf.toString()));
                buf.setLength(0);
                ops.add(c);
            }
        }
        nums.add(new BigDecimal(buf.toString()));

        // * と / を先に処理
        for (int i = 0; i < ops.size();) {
            char op = ops.get(i);
            if (op == '*' || op == '/') {
                BigDecimal a = nums.get(i);
                BigDecimal b = nums.get(i + 1);

                BigDecimal r = (op == '*')
                        ? a.multiply(b)
                        : a.divide(b, MathContext.DECIMAL64); // 割り算対策

                nums.set(i, r);
                nums.remove(i + 1);
                ops.remove(i);
            } else {
                i++;
            }
        }

        // + と -
        BigDecimal res = nums.get(0);
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i) == '+') {
                res = res.add(nums.get(i + 1));
            } else {
                res = res.subtract(nums.get(i + 1));
            }
        }

        return res.stripTrailingZeros();
    }

}