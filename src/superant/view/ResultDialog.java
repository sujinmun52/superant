package superant.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import superant.controller.GameController;
import superant.controller.SaveResult;
import superant.controller.ViewData;
import superant.domain.GameSession;
import superant.domain.Stock;
import superant.model.Achievement;
import superant.model.Ending;
import superant.service.GameResult;

/**
 * 결과 화면 (UI-04). 엔딩 칭호, 성과 지표, 업적, 자산 추이를 보여 주고 기록을 저장한다.
 */
public class ResultDialog extends JDialog {
    private final GameResult result;
    private final GameController controller;
    private final JLabel saveStatus = Theme.label(" ", 13, Theme.MUTED, false);
    private final Widgets.FlatButton saveButton;

    /**
     * 결과 화면을 만든다.
     *
     * @param owner      부모 창
     * @param controller 게임 컨트롤러
     * @param result     표시할 게임 결과
     */
    public ResultDialog(JFrame owner, GameController controller, GameResult result) {
        super(owner, "게임 결과", true);
        this.controller = controller;
        this.result = result;
        ViewData view = controller.getView();

        Color accent = result.getEnding() == Ending.PROFIT ? Theme.GOLD
                : result.getEnding() == Ending.MAINTAIN ? Theme.MINT : Theme.DOWN;

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));

        JPanel banner = new JPanel();
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBackground(Theme.PANEL);
        banner.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 2),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel small = Theme.label("GAME OVER · " + result.getEnding().getLabel() + " · 최종 칭호 "
                + result.getRank().getLabel() + " ", 13, accent, true);
        JLabel title = new JLabel(result.getTitle());
        title.setFont(Theme.title(44));
        title.setForeground(accent);
        JLabel sub = Theme.label(result.getPlayerName() + " · " + result.getDifficulty().name()
                + " · 최종 자산 " + Theme.won(result.getFinalAssets()) + " (" + Theme.pct(result.getReturnRate()) + ")",
                15, Theme.TEXT, false);
        for (JLabel l : new JLabel[]{small, title, sub}) {
            l.setAlignmentX(0.5f);
            banner.add(l);
        }
        root.add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel stats = new JPanel(new GridLayout(2, 4, 10, 10));
        stats.setOpaque(false);
        String best = "없음";
        if (result.getBestStockId() != null && view != null) {
            Stock s = view.getSnapshot().findStock(result.getBestStockId());
            best = s == null ? result.getBestStockId() : s.getName();
        }
        stat(stats, "최종 자산", Theme.won(result.getFinalAssets()), Theme.TEXT);
        stat(stats, "수익률", Theme.pct(result.getReturnRate()), Theme.trend(result.getReturnRate()));
        stat(stats, "실현 손익", Theme.signedWon(result.getRealizedPnl()), Theme.trend(result.getRealizedPnl()));
        stat(stats, "평가 손익", Theme.signedWon(result.getUnrealizedPnl()), Theme.trend(result.getUnrealizedPnl()));
        stat(stats, "최대 낙폭", "-" + result.getMaxDrawdown().toPlainString() + "%", Theme.DOWN);
        stat(stats, "최고 수익 종목", best, Theme.TEXT);
        stat(stats, "거래 횟수", result.getTradeCount() + "회", Theme.TEXT);
        stat(stats, "투자 성향", result.getInvestorType().getLabel(), Theme.MINT);
        center.add(stats, BorderLayout.NORTH);

        PriceChartPanel chart = new PriceChartPanel();
        chart.setPreferredSize(new Dimension(640, 200));
        if (view != null) {
            List<Long> assets = new ArrayList<>();
            assets.add(GameSession.INITIAL_CASH.longValue());
            for (BigDecimal a : view.getSnapshot().getAssetHistory()) {
                assets.add(a.setScale(0, RoundingMode.HALF_UP).longValue());
            }
            chart.setPrices(assets);
        }
        JPanel chartBox = new JPanel(new BorderLayout(0, 6));
        chartBox.setBackground(Theme.PANEL);
        chartBox.setBorder(Theme.cardBorder(10));
        chartBox.add(Theme.label("총자산 추이", 13, Theme.MUTED, true), BorderLayout.NORTH);
        chartBox.add(chart, BorderLayout.CENTER);
        center.add(chartBox, BorderLayout.CENTER);

        JPanel ach = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ach.setOpaque(false);
        ach.add(Theme.label("업적", 13, Theme.MUTED, true));
        if (result.getAchievements().isEmpty()) {
            ach.add(Theme.label("이번 게임에서 얻은 업적이 없습니다.", 13, Theme.MUTED, false));
        }
        for (Achievement a : result.getAchievements()) {
            JLabel chip = Theme.label("★ " + a.getLabel(), 13, Theme.GOLD, true);
            chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.GOLD),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)));
            ach.add(chip);
        }
        center.add(ach, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(saveStatus, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        saveButton = new Widgets.FlatButton("기록 저장", Theme.MINT, Theme.BG);
        saveButton.addActionListener(e -> onSave());
        Widgets.FlatButton records = new Widgets.FlatButton("기록 조회", Theme.PANEL2, Theme.TEXT);
        records.addActionListener(e -> new RecordDialog(owner, controller, result.getPlayerName(),
                result.getDifficulty()).setVisible(true));
        Widgets.FlatButton close = new Widgets.FlatButton("닫기", Theme.PANEL2, Theme.TEXT);
        close.addActionListener(e -> dispose());
        buttons.add(saveButton);
        buttons.add(records);
        buttons.add(close);
        bottom.add(buttons, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    private static void stat(JPanel grid, String key, String value, Color color) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Theme.PANEL);
        p.setBorder(Theme.cardBorder(10));
        p.add(Theme.label(key, 12, Theme.MUTED, true), BorderLayout.NORTH);
        JLabel v = Theme.label(value, 17, color, true);
        v.setHorizontalAlignment(SwingConstants.LEFT);
        p.add(v, BorderLayout.CENTER);
        grid.add(p);
    }

    /**
     * 기록 저장 버튼 동작. 같은 게임은 한 번만 저장된다.
     */
    private void onSave() {
        SaveResult r = controller.saveResult();
        if (r.isSuccess()) {
            saveStatus.setForeground(r.isAlreadySaved() ? Theme.GOLD : Theme.MINT);
            saveStatus.setText(r.getMessage());
            saveStatus.setToolTipText(String.valueOf(r.getLocation()));
            saveButton.setEnabled(false);
        } else {
            saveStatus.setForeground(Theme.UP);
            saveStatus.setText(r.getMessage());
        }
    }

    /**
     * 표시 중인 결과를 반환한다.
     *
     * @return 게임 결과
     */
    public GameResult getResult() {
        return result;
    }
}
