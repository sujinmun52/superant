package superant.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

/**
 * 가격(또는 자산) 이력을 꺾은선으로 그리는 차트.
 * 가로축은 항상 0 ~ 30턴이며, 시작값보다 높으면 빨강, 낮으면 파랑으로 그린다.
 */
public class PriceChartPanel extends JPanel {
    private static final int TURNS = 30;

    private List<Long> prices = new ArrayList<>();
    private boolean live;

    /**
     * 빈 차트를 만든다.
     */
    public PriceChartPanel() {
        setBackground(Theme.PANEL);
        setPreferredSize(new Dimension(420, 220));
    }

    /**
     * 그릴 값을 바꾼다. 첫 값은 0턴 값이다.
     *
     * @param prices 0턴부터의 값 목록
     */
    public void setPrices(List<Long> prices) {
        this.prices = new ArrayList<>(prices);
        repaint();
    }

    /**
     * 장중 애니메이션 중임을 표시하는 LIVE 배지를 켜거나 끈다.
     *
     * @param live 애니메이션 중이면 true
     */
    public void setLive(boolean live) {
        this.live = live;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Widgets.smooth(g);
        int left = 70;
        int right = 16;
        int top = 14;
        int bottom = 26;
        int w = getWidth() - left - right;
        int h = getHeight() - top - bottom;
        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }
        g2.setFont(Theme.font(11, false));
        FontMetrics fm = g2.getFontMetrics();

        if (prices.isEmpty()) {
            g2.setColor(Theme.MUTED);
            g2.drawString("데이터 없음", left + w / 2 - 25, top + h / 2);
            g2.dispose();
            return;
        }
        long min = Collections.min(prices);
        long max = Collections.max(prices);
        long pad = Math.max(1, (max - min) / 8);
        if (max == min) {
            pad = Math.max(1, max / 20);
        }
        double lo = min - pad;
        double hi = max + pad;

        for (int i = 0; i <= 4; i++) {
            int y = top + (int) Math.round(h * i / 4.0);
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(left, y, left + w, y);
            long val = Math.round(hi - (hi - lo) * i / 4.0);
            String s = String.format("%,d", val);
            g2.setColor(Theme.MUTED);
            g2.drawString(s, left - 8 - fm.stringWidth(s), y + fm.getAscent() / 2 - 1);
        }
        for (int t = 0; t <= TURNS; t += 5) {
            int x = left + (int) Math.round(w * t / (double) TURNS);
            String s = t + "턴";
            g2.setColor(Theme.MUTED);
            g2.drawString(s, x - fm.stringWidth(s) / 2, top + h + fm.getAscent() + 6);
        }

        long first = prices.get(0);
        long last = prices.get(prices.size() - 1);
        Color line = last > first ? Theme.UP : last < first ? Theme.DOWN : Theme.MINT;

        int baseY = yOf(first, lo, hi, top, h);
        g2.setColor(Theme.MUTED);
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g2.drawLine(left, baseY, left + w, baseY);

        Path2D path = new Path2D.Double();
        Path2D area = new Path2D.Double();
        double lastX = left;
        for (int i = 0; i < prices.size(); i++) {
            double x = left + w * i / (double) TURNS;
            double y = yOf(prices.get(i), lo, hi, top, h);
            if (i == 0) {
                path.moveTo(x, y);
                area.moveTo(x, top + h);
                area.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                area.lineTo(x, y);
            }
            lastX = x;
        }
        area.lineTo(lastX, top + h);
        area.closePath();
        g2.setPaint(new GradientPaint(0, top, new Color(line.getRed(), line.getGreen(), line.getBlue(), 90),
                0, top + h, new Color(line.getRed(), line.getGreen(), line.getBlue(), 0)));
        g2.fill(area);
        g2.setColor(line);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
        int ly = yOf(last, lo, hi, top, h);
        g2.fillOval((int) lastX - 4, ly - 4, 8, 8);
        if (live) {
            g2.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), 70));
            g2.fillOval((int) lastX - 9, ly - 9, 18, 18);
            g2.setFont(Theme.font(12, true));
            String badge = "● LIVE";
            int bw = g2.getFontMetrics().stringWidth(badge) + 14;
            g2.setColor(Theme.UP);
            g2.fillRoundRect(left + w - bw, top, bw, 20, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(badge, left + w - bw + 7, top + 15);
        }
        g2.dispose();
    }

    private static int yOf(long v, double lo, double hi, int top, int h) {
        return top + (int) Math.round(h * (hi - v) / (hi - lo));
    }
}
