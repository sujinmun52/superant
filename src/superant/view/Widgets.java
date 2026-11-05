package superant.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

/**
 * 다크 테마용 작은 커스텀 컴포넌트 모음 (둥근 버튼, 턴 진행 칸, 목표 게이지).
 */
final class Widgets {
    private Widgets() {
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** 배경색을 직접 그리는 둥근 버튼. */
    static final class FlatButton extends JButton {
        private Color base;

        FlatButton(String text, Color base, Color fg) {
            super(text);
            this.base = base;
            setForeground(fg);
            setFont(Theme.font(14, true));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        }

        void setBase(Color base) {
            this.base = base;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            ButtonModel m = getModel();
            Color c = !isEnabled() ? Theme.BORDER : m.isPressed() ? base.darker() : m.isRollover() ? base.brighter() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 선택하면 테두리가 강조되는 카드형 토글 버튼. */
    static final class CardToggle extends JToggleButton {
        private final Color accent;

        CardToggle(String html, Color accent) {
            super(html);
            this.accent = accent;
            setForeground(Theme.TEXT);
            setFont(Theme.font(13, false));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            boolean sel = isSelected();
            g2.setColor(sel ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 38) : Theme.PANEL);
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
            g2.setColor(sel ? accent : getModel().isRollover() ? Theme.MUTED : Theme.BORDER);
            g2.setStroke(new java.awt.BasicStroke(sel ? 2.5f : 1.2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 30턴 진행 상황을 작은 칸으로 보여 준다. */
    static final class TurnPips extends JComponent {
        private int completed;
        private int total = 30;

        TurnPips() {
            setPreferredSize(new Dimension(300, 10));
        }

        void setProgress(int completed, int total) {
            this.completed = completed;
            this.total = total;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            float gap = 3f;
            float w = (getWidth() - gap * (total - 1)) / total;
            for (int i = 0; i < total; i++) {
                if (i < completed) {
                    g2.setColor(Theme.MINT);
                } else if (i == completed) {
                    g2.setColor(Theme.GOLD);
                } else {
                    g2.setColor(Theme.BORDER);
                }
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(i * (w + gap), 0, w, getHeight(), 4, 4));
            }
            g2.dispose();
        }
    }

    /** 목표 자산 대비 진행률 게이지. */
    static final class GoalGauge extends JComponent {
        private double ratio;

        GoalGauge() {
            setPreferredSize(new Dimension(180, 10));
        }

        void setRatio(double ratio) {
            this.ratio = Math.max(0, Math.min(1, ratio));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int h = getHeight();
            g2.setColor(Theme.BORDER);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            g2.setColor(ratio >= 1 ? Theme.GOLD : Theme.MINT);
            g2.fillRoundRect(0, 0, (int) Math.round(getWidth() * ratio), h, h, h);
            g2.dispose();
        }
    }
}
