package superant.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import superant.model.RiskLevel;

/**
 * 다크 테마 색상, 글꼴, 숫자 형식을 모아 둔 도우미.
 * 상승은 빨강, 하락은 파랑(국내 증시 관례)으로 표시한다.
 */
final class Theme {
    static final Color BG = new Color(0x0E1117);
    static final Color PANEL = new Color(0x161B24);
    static final Color PANEL2 = new Color(0x1C2330);
    static final Color BORDER = new Color(0x273041);
    static final Color TEXT = new Color(0xE6EAF2);
    static final Color MUTED = new Color(0x8C96A8);
    static final Color MINT = new Color(0x2EE6A6);
    static final Color UP = new Color(0xFF4D5E);
    static final Color DOWN = new Color(0x3D8BFF);
    static final Color GOLD = new Color(0xFFC24B);
    static final Color RISK_LOW = new Color(0x3DDC84);
    static final Color RISK_MID = new Color(0xFF9F43);
    static final Color RISK_HIGH = new Color(0xFF4D5E);

    private static final String BODY_FAMILY;
    private static final String TITLE_FAMILY;

    static {
        Set<String> fonts = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        BODY_FAMILY = pick(fonts, "Noto Sans KR", "Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", "NanumGothic", Font.DIALOG);
        TITLE_FAMILY = pick(fonts, "Black Han Sans", BODY_FAMILY);
    }

    private Theme() {
    }

    private static String pick(Set<String> fonts, String... names) {
        for (String n : names) {
            if (fonts.contains(n)) {
                return n;
            }
        }
        return names[names.length - 1];
    }

    static Font font(float size, boolean bold) {
        return new Font(BODY_FAMILY, bold ? Font.BOLD : Font.PLAIN, Math.round(size));
    }

    static Font title(float size) {
        boolean special = !TITLE_FAMILY.equals(BODY_FAMILY);
        return new Font(TITLE_FAMILY, special ? Font.PLAIN : Font.BOLD, Math.round(size));
    }

    static JLabel label(String text, float size, Color color, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(font(size, bold));
        l.setForeground(color);
        return l;
    }

    static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(cardBorder(12));
        return p;
    }

    static Border cardBorder(int pad) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(pad, pad, pad, pad));
    }

    static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(PANEL);
        sp.setBackground(PANEL);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    static void styleTable(JTable t) {
        t.setBackground(PANEL);
        t.setForeground(TEXT);
        t.setFont(font(13, false));
        t.setRowHeight(30);
        t.setGridColor(BORDER);
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(0x23314A));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        JTableHeader h = t.getTableHeader();
        DefaultTableCellRenderer hr = new DefaultTableCellRenderer();
        hr.setBackground(PANEL2);
        hr.setForeground(MUTED);
        hr.setFont(font(12, true));
        hr.setHorizontalAlignment(SwingConstants.CENTER);
        hr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        h.setDefaultRenderer(hr);
        h.setBackground(PANEL2);
    }

    static void install() {
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", font(14, false));
        UIManager.put("OptionPane.buttonFont", font(13, true));
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL2);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.font", font(13, true));
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("ComboBox.background", PANEL2);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", new Color(0x23314A));
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.font", font(13, false));
        UIManager.put("ToolTip.background", PANEL2);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ScrollBar.background", PANEL);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.track", PANEL);
    }

    static String won(BigDecimal v) {
        return String.format("%,d원", v.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    static String won(long v) {
        return String.format("%,d원", v);
    }

    static String signedWon(BigDecimal v) {
        long n = v.setScale(0, RoundingMode.HALF_UP).longValue();
        return (n > 0 ? "+" : "") + String.format("%,d원", n);
    }

    static String pct(BigDecimal v) {
        BigDecimal s = v.setScale(2, RoundingMode.HALF_UP);
        return (s.signum() > 0 ? "+" : "") + s.toPlainString() + "%";
    }

    static Color trend(BigDecimal v) {
        int s = v.signum();
        return s > 0 ? UP : s < 0 ? DOWN : MUTED;
    }

    static Color risk(RiskLevel r) {
        switch (r) {
            case LOW: return RISK_LOW;
            case MEDIUM: return RISK_MID;
            default: return RISK_HIGH;
        }
    }
}
