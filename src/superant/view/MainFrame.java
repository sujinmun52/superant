package superant.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import superant.controller.GameController;
import superant.controller.StartResult;
import superant.controller.TradeResult;
import superant.controller.TurnResult;
import superant.controller.ViewData;
import superant.domain.GameSession;
import superant.domain.GameSnapshot;
import superant.domain.Holding;
import superant.domain.News;
import superant.domain.Portfolio;
import superant.domain.Stock;
import superant.domain.Trade;
import superant.model.AntRank;
import superant.model.Difficulty;
import superant.model.OrderSide;
import superant.model.RiskLevel;

/**
 * 메인 창. 시작 화면(UI-01)과 게임 화면(UI-02)을 카드로 전환한다.
 * <p>
 * 화면은 컨트롤러가 돌려준 {@link ViewData} 만으로 다시 그리며,
 * 요청을 보낼 때 그 version 을 함께 보낸다.
 */
public class MainFrame extends JFrame {
    private static final String START = "start";
    private static final String GAME = "game";

    private final GameController controller;
    private final StartPanel startPanel;
    private final PriceChartPanel chartPanel = new PriceChartPanel();
    private final JTable stockTable;
    private final JTable holdingTable;
    private final JTable tradeTable;
    private final JTextArea newsArea = new JTextArea();
    private final DefaultTableModel stockModel;
    private final DefaultTableModel holdingModel;
    private final DefaultTableModel tradeModel;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final JLabel playerLabel = Theme.label("", 13, Theme.MUTED, true);
    private final JLabel turnLabel = new JLabel();
    private final Widgets.TurnPips pips = new Widgets.TurnPips();
    private final JLabel assetLabel = Theme.label("", 24, Theme.TEXT, true);
    private final JLabel rateLabel = Theme.label("", 14, Theme.MUTED, true);
    private final JLabel cashLabel = Theme.label("", 20, Theme.TEXT, true);
    private final JLabel goalLabel = Theme.label("", 12, Theme.MUTED, false);
    private final Widgets.GoalGauge gauge = new Widgets.GoalGauge();
    private final JLabel newsBadge = Theme.label(" BREAKING ", 12, Theme.BG, true);
    private final JLabel newsTitle = Theme.label("", 16, Theme.TEXT, true);
    private final JLabel newsHint = Theme.label("", 13, Theme.MUTED, false);

    private final JLabel detailName = Theme.label("", 22, Theme.TEXT, true);
    private final JLabel detailMeta = Theme.label("", 12, Theme.MUTED, false);
    private final JLabel detailPrice = Theme.label("", 26, Theme.TEXT, true);
    private final JLabel detailChange = Theme.label("", 14, Theme.MUTED, true);
    private final JLabel detailHolding = Theme.label("", 13, Theme.MUTED, false);

    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, Portfolio.MAX_QUANTITY, 1));
    private final Widgets.FlatButton buyButton = new Widgets.FlatButton("매수", Theme.UP, Color.WHITE);
    private final Widgets.FlatButton sellButton = new Widgets.FlatButton("매도", Theme.DOWN, Color.WHITE);
    private final Widgets.FlatButton maxButton = new Widgets.FlatButton("최대", Theme.PANEL2, Theme.TEXT);
    private final Widgets.FlatButton nextButton = new Widgets.FlatButton("", Theme.MINT, Theme.BG);
    private final JLabel statusLabel = Theme.label(" ", 13, Theme.MUTED, false);

    private final CardLayout tabCards = new CardLayout();
    private final JPanel tabBody = new JPanel(tabCards);
    private final List<Widgets.FlatButton> tabButtons = new ArrayList<>();

    private final JLabel holdLabel = Theme.label(" ", 13, Theme.MUTED, true);
    private final JLabel goalTitle = Theme.label(" ", 12, Theme.MUTED, true);
    private final JLabel rankLabel = new JLabel(" ");
    private final JLabel streakLabel = Theme.label(" ", 12, Theme.UP, true);
    private static final BigDecimal SUPER_ASSETS = BigDecimal.valueOf(1_500_000);
    private final SummaryPane summary = new SummaryPane();

    private static final int TICK_FRAMES = 40;
    private static final int TICK_MS = 55;
    private final Random tickRandom = new Random();
    private final Map<String, Integer> tickDir = new HashMap<>();
    private Timer ticker;
    private ViewData tickBefore;
    private TurnResult tickResult;

    private ViewData view;
    private String selectedId = "S01";
    private boolean refreshing;

    /**
     * 메인 창을 만든다.
     *
     * @param controller 게임 컨트롤러
     */
    public MainFrame(GameController controller) {
        super("슈퍼개미 - 30턴 투자 챌린지");
        this.controller = controller;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);

        startPanel = new StartPanel(this::onStart, () -> new RecordDialog(this, controller).setVisible(true));

        stockModel = readOnlyModel("ID", "종목", "업종", "위험도", "현재가", "등락률", "보유");
        stockTable = new JTable(stockModel);
        holdingModel = readOnlyModel("종목", "수량", "평균 단가", "현재가", "평가 금액", "평가 손익", "수익률");
        holdingTable = new JTable(holdingModel);
        tradeModel = readOnlyModel("턴", "종목", "구분", "수량", "체결가", "거래 금액", "실현 손익");
        tradeTable = new JTable(tradeModel);

        root.add(startPanel, START);
        root.add(buildGamePanel(), GAME);
        setContentPane(root);
        setGlassPane(summary);

        setSize(1320, 860);
        setMinimumSize(new Dimension(1180, 760));
        setLocationRelativeTo(null);
        cards.show(root, START);
    }

    private static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JComponent buildGamePanel() {
        JPanel game = new JPanel(new BorderLayout(0, 12));
        game.setBackground(Theme.BG);
        game.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(buildHud(), BorderLayout.NORTH);
        north.add(buildNewsStrip(), BorderLayout.SOUTH);
        game.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 12));
        center.setOpaque(false);
        JPanel upper = new JPanel(new GridLayout(1, 2, 12, 0));
        upper.setOpaque(false);
        upper.add(buildMarket());
        upper.add(buildDetail());
        center.add(upper);
        center.add(buildTabs());
        game.add(center, BorderLayout.CENTER);

        game.add(buildBottomBar(), BorderLayout.SOUTH);
        return game;
    }

    private JComponent buildHud() {
        JPanel hud = new JPanel(new GridLayout(1, 4, 12, 0));
        hud.setOpaque(false);

        JPanel turn = Theme.card();
        turn.setLayout(new BoxLayout(turn, BoxLayout.Y_AXIS));
        turnLabel.setFont(Theme.title(26));
        turnLabel.setForeground(Theme.MINT);
        for (JComponent c : new JComponent[]{playerLabel, turnLabel, pips, spacer(4), streakLabel}) {
            c.setAlignmentX(0f);
            turn.add(c);
        }
        pips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        hud.add(turn);

        JPanel asset = Theme.card();
        asset.setLayout(new BoxLayout(asset, BoxLayout.Y_AXIS));
        asset.add(Theme.label("총자산", 12, Theme.MUTED, true));
        asset.add(assetLabel);
        asset.add(rateLabel);
        hud.add(asset);

        JPanel goal = Theme.card();
        goal.setLayout(new BoxLayout(goal, BoxLayout.Y_AXIS));
        gauge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        rankLabel.setFont(Theme.title(22));
        for (JComponent c : new JComponent[]{goalTitle, rankLabel, spacer(6), gauge, spacer(6), goalLabel}) {
            c.setAlignmentX(0f);
            goal.add(c);
        }
        hud.add(goal);

        JPanel cash = Theme.card();
        cash.setLayout(new BoxLayout(cash, BoxLayout.Y_AXIS));
        cash.add(Theme.label("주문 가능 현금", 12, Theme.MUTED, true));
        cash.add(cashLabel);
        cash.add(holdLabel);
        hud.add(cash);
        return hud;
    }

    private static JComponent spacer(int h) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, h));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        return p;
    }

    private JComponent buildNewsStrip() {
        JPanel strip = new JPanel(new BorderLayout(12, 0));
        strip.setBackground(Theme.PANEL2);
        strip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.GOLD),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        newsBadge.setOpaque(true);
        newsBadge.setBackground(Theme.GOLD);
        newsBadge.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        JPanel badgeBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        badgeBox.setOpaque(false);
        badgeBox.add(newsBadge);
        strip.add(badgeBox, BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(newsTitle);
        text.add(newsHint);
        strip.add(text, BorderLayout.CENTER);
        return strip;
    }

    private JComponent buildMarket() {
        Theme.styleTable(stockTable);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockTable.removeColumn(stockTable.getColumnModel().getColumn(0));
        DefaultTableCellRenderer base = new MarketRenderer();
        for (int i = 0; i < stockTable.getColumnCount(); i++) {
            stockTable.getColumnModel().getColumn(i).setCellRenderer(base);
        }
        stockTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        stockTable.getSelectionModel().addListSelectionListener(e -> {
            if (refreshing || e.getValueIsAdjusting()) {
                return;
            }
            int row = stockTable.getSelectedRow();
            if (row >= 0) {
                selectedId = (String) stockModel.getValueAt(stockTable.convertRowIndexToModel(row), 0);
                updateDetail();
            }
        });
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.PANEL);
        p.setBorder(Theme.cardBorder(12));
        p.add(Theme.label("시장 · 6종목", 14, Theme.TEXT, true), BorderLayout.NORTH);
        p.add(Theme.scroll(stockTable), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildDetail() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.PANEL);
        p.setBorder(Theme.cardBorder(12));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel left = new JPanel(new GridLayout(3, 1));
        left.setOpaque(false);
        left.add(detailName);
        left.add(detailMeta);
        detailHolding.setFont(Theme.font(14, true));
        left.add(detailHolding);
        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        detailPrice.setHorizontalAlignment(SwingConstants.RIGHT);
        detailChange.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(detailPrice);
        right.add(detailChange);
        head.add(left, BorderLayout.CENTER);
        head.add(right, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);
        p.add(chartPanel, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildTabs() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.PANEL);
        p.setBorder(Theme.cardBorder(12));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        Theme.styleTable(holdingTable);
        Theme.styleTable(tradeTable);
        DefaultTableCellRenderer pnl = new PnlRenderer();
        for (int i = 0; i < holdingTable.getColumnCount(); i++) {
            holdingTable.getColumnModel().getColumn(i).setCellRenderer(pnl);
        }
        for (int i = 0; i < tradeTable.getColumnCount(); i++) {
            tradeTable.getColumnModel().getColumn(i).setCellRenderer(pnl);
        }
        newsArea.setEditable(false);
        newsArea.setBackground(Theme.PANEL);
        newsArea.setForeground(Theme.TEXT);
        newsArea.setFont(Theme.font(13, false));
        newsArea.setLineWrap(true);
        newsArea.setWrapStyleWord(true);
        newsArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        addTab(bar, "포트폴리오", Theme.scroll(holdingTable));
        addTab(bar, "거래 내역", Theme.scroll(tradeTable));
        addTab(bar, "뉴스 피드", Theme.scroll(newsArea));
        selectTab(0);
        p.add(bar, BorderLayout.NORTH);
        tabBody.setOpaque(false);
        p.add(tabBody, BorderLayout.CENTER);
        return p;
    }

    private void addTab(JPanel bar, String name, JComponent content) {
        int index = tabButtons.size();
        Widgets.FlatButton b = new Widgets.FlatButton(name, Theme.PANEL2, Theme.TEXT);
        b.setFont(Theme.font(13, true));
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.addActionListener(e -> selectTab(index));
        tabButtons.add(b);
        bar.add(b);
        tabBody.add(content, name);
    }

    private void selectTab(int index) {
        for (int i = 0; i < tabButtons.size(); i++) {
            Widgets.FlatButton b = tabButtons.get(i);
            b.setBase(i == index ? Theme.MINT : Theme.PANEL2);
            b.setForeground(i == index ? Theme.BG : Theme.TEXT);
        }
        tabCards.show(tabBody, tabButtons.get(index).getText());
    }

    private JComponent buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(Theme.PANEL);
        bar.setBorder(Theme.cardBorder(10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(Theme.label("수량", 13, Theme.MUTED, true));
        qtySpinner.setFont(Theme.font(15, true));
        qtySpinner.setPreferredSize(new Dimension(110, 38));
        JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) qtySpinner.getEditor();
        ed.getTextField().setBackground(Theme.PANEL2);
        ed.getTextField().setForeground(Theme.TEXT);
        ed.getTextField().setCaretColor(Theme.MINT);
        qtySpinner.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        left.add(qtySpinner);
        maxButton.setToolTipText("현재 현금으로 살 수 있는 최대 수량");
        maxButton.addActionListener(e -> onMax());
        left.add(maxButton);
        buyButton.setPreferredSize(new Dimension(100, 40));
        sellButton.setPreferredSize(new Dimension(100, 40));
        buyButton.addActionListener(e -> onBuy());
        sellButton.addActionListener(e -> onSell());
        left.add(buyButton);
        left.add(sellButton);
        bar.add(left, BorderLayout.WEST);

        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bar.add(statusLabel, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        Widgets.FlatButton records = new Widgets.FlatButton("기록", Theme.PANEL2, Theme.TEXT);
        records.addActionListener(e -> openRecords());
        Widgets.FlatButton newGame = new Widgets.FlatButton("새 게임", Theme.PANEL2, Theme.TEXT);
        newGame.addActionListener(e -> onNewGame());
        nextButton.setFont(Theme.font(15, true));
        nextButton.setPreferredSize(new Dimension(230, 40));
        nextButton.addActionListener(e -> onNextTurn());
        right.add(records);
        right.add(newGame);
        right.add(nextButton);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /**
     * 컨트롤러가 준 화면 데이터로 게임 화면 전체를 다시 그린다.
     *
     * @param data 화면 데이터
     */
    public void refresh(ViewData data) {
        if (data == null) {
            return;
        }
        this.view = data;
        GameSnapshot s = data.getSnapshot();
        boolean ended = s.isCompleted();

        playerLabel.setText(s.getPlayerName() + "  ·  " + s.getDifficulty().name() + " · " + s.getDifficulty().getDescription() + " ");
        turnLabel.setText(ended ? "GAME OVER" : String.format("TURN %02d / %d", data.getCurrentTurn(), GameSession.TOTAL_TURNS));
        pips.setProgress(s.getCompletedTurns(), GameSession.TOTAL_TURNS);
        assetLabel.setText(Theme.won(data.getTotalAssets()));
        rateLabel.setText("수익률 " + Theme.pct(data.getReturnRate()) + "  (" + Theme.signedWon(
                data.getTotalAssets().subtract(GameSession.INITIAL_CASH)) + ")  ");
        rateLabel.setForeground(Theme.trend(data.getReturnRate()));
        cashLabel.setText(Theme.won(s.getCash()));
        int heldCount = 0;
        BigDecimal heldPnl = BigDecimal.ZERO;
        for (Holding h : s.getHoldings()) {
            if (h.getQuantity() > 0) {
                heldCount++;
                heldPnl = heldPnl.add(h.unrealizedPnl(s.findStock(h.getStockId()).currentPrice()));
            }
        }
        holdLabel.setText(heldCount == 0 ? "보유 종목 없음" : "보유 " + heldCount + "종목 · 평가손익 " + Theme.signedWon(heldPnl) + " ");
        holdLabel.setForeground(heldCount == 0 ? Theme.MUTED : Theme.trend(heldPnl));
        updateGoal(data.getTotalAssets());
        int streak = streak(s.getAssetHistory());
        if (streak >= 2) {
            streakLabel.setText("▲ " + streak + "턴 연속 수익 중!  ");
            streakLabel.setForeground(Theme.UP);
        } else if (streak <= -2) {
            streakLabel.setText("▼ " + (-streak) + "턴 연속 하락  ");
            streakLabel.setForeground(Theme.DOWN);
        } else {
            streakLabel.setText(" ");
        }

        News news = data.getCurrentNews();
        if (ended) {
            newsTitle.setText("30턴 완료! 결과를 확인하세요.");
            newsHint.setText("오른쪽 아래 '결과 보기' 버튼을 누르면 엔딩과 투자 성향을 볼 수 있습니다.");
            newsBadge.setBackground(Theme.MINT);
            newsBadge.setText(" FINISH ");
        } else if (news != null) {
            newsTitle.setText("[" + data.getCurrentTurn() + "턴] " + news.getEvent().getTitle());
            newsHint.setText(news.hint(s.getDifficulty()));
            newsBadge.setBackground(Theme.GOLD);
            newsBadge.setText(" BREAKING ");
        } else {
            newsTitle.setText("[" + data.getCurrentTurn() + "턴] 특별한 뉴스가 없습니다.");
            newsHint.setText("시장 흐름과 업종 변동만 반영됩니다.");
            newsBadge.setBackground(Theme.MUTED);
            newsBadge.setText(" QUIET ");
        }

        refreshing = true;
        stockModel.setRowCount(0);
        int selRow = 0;
        for (int i = 0; i < s.getStocks().size(); i++) {
            Stock st = s.getStocks().get(i);
            int qty = s.quantityOf(st.getId());
            stockModel.addRow(new Object[]{st.getId(), st.getName() + "  " + st.getId(), st.getSector().getLabel(),
                    st.getRisk(), String.format("%,d", st.currentPrice()), st.changeRate(),
                    qty > 0 ? String.format("%,d주", qty) : "-"});
            if (st.getId().equals(selectedId)) {
                selRow = i;
            }
        }
        stockTable.setRowSelectionInterval(selRow, selRow);
        refreshing = false;
        updateDetail();

        holdingModel.setRowCount(0);
        for (Holding h : s.getHoldings()) {
            if (h.getQuantity() == 0) {
                continue;
            }
            Stock st = s.findStock(h.getStockId());
            BigDecimal value = BigDecimal.valueOf(st.currentPrice()).multiply(BigDecimal.valueOf(h.getQuantity()));
            BigDecimal pnl = h.unrealizedPnl(st.currentPrice());
            BigDecimal rate = h.getCostBasis().signum() == 0 ? BigDecimal.ZERO
                    : pnl.multiply(BigDecimal.valueOf(100)).divide(h.getCostBasis(), 2, RoundingMode.HALF_UP);
            holdingModel.addRow(new Object[]{st.getName(), String.format("%,d주", h.getQuantity()),
                    Theme.won(h.averagePrice()), Theme.won(st.currentPrice()), Theme.won(value), pnl, rate});
        }

        tradeModel.setRowCount(0);
        List<Trade> trades = s.getTrades();
        for (int i = trades.size() - 1; i >= 0; i--) {
            Trade t = trades.get(i);
            tradeModel.addRow(new Object[]{t.getTurn() + "턴", s.findStock(t.getStockId()).getName(),
                    t.getSide(), String.format("%,d주", t.getQuantity()), Theme.won(t.getPrice()),
                    Theme.won(t.getAmount()), t.getSide() == OrderSide.SELL ? t.getRealizedPnl() : "-"});
        }

        StringBuilder sb = new StringBuilder();
        for (News n : data.getNewsFeed()) {
            sb.append('[').append(n.getPublishedTurn()).append("턴] ").append(n.getEvent().getTitle())
                    .append("\n    ").append(n.hint(s.getDifficulty())).append("\n\n");
        }
        newsArea.setText(sb.length() == 0 ? "아직 발표된 뉴스가 없습니다." : sb.toString());
        newsArea.setCaretPosition(0);

        setTradingEnabled(!ended);
        if (ended) {
            nextButton.setText("결과 보기 ▶");
            nextButton.setBase(Theme.GOLD);
        } else {
            nextButton.setText(data.getCurrentTurn() == GameSession.TOTAL_TURNS ? "마지막 턴 마감 ▶" : "턴 마감 · 다음 턴 ▶");
            nextButton.setBase(Theme.MINT);
        }
    }

    /**
     * 칭호와 목표 게이지를 갱신한다. 여왕개미(20%) 전에는 120만 원, 그 뒤에는 150만 원(슈퍼개미)을 목표로 보여 준다.
     */
    private void updateGoal(BigDecimal total) {
        BigDecimal rate = total.subtract(GameSession.INITIAL_CASH).multiply(BigDecimal.valueOf(100))
                .divide(GameSession.INITIAL_CASH, 2, RoundingMode.HALF_UP);
        AntRank rank = AntRank.of(rate);
        rankLabel.setText(rank.getLabel() + " ");
        rankLabel.setForeground(rankColor(rank));
        boolean queen = rank.compareTo(AntRank.QUEEN) >= 0;
        BigDecimal target = queen ? SUPER_ASSETS : GameSession.GOAL_ASSETS;
        goalTitle.setText(queen ? "도전 1,500,000원 · 슈퍼개미" : "목표 1,200,000원 · 여왕개미");
        double ratio = total.doubleValue() / target.doubleValue();
        gauge.setRatio(ratio);
        if (rank == AntRank.SUPER) {
            goalLabel.setText("슈퍼개미 달성! 끝까지 지켜내세요  ");
            goalLabel.setForeground(Theme.GOLD);
        } else {
            AntRank next = rank.next();
            BigDecimal need = GameSession.INITIAL_CASH.multiply(BigDecimal.ONE.add(
                    next.getMinRate().divide(BigDecimal.valueOf(100)))).subtract(total);
            goalLabel.setText("다음 칭호 " + next.getLabel() + "까지 " + Theme.won(need.max(BigDecimal.ONE)) + "  ");
            goalLabel.setForeground(Theme.MUTED);
        }
    }

    static Color rankColor(AntRank rank) {
        switch (rank) {
            case SUPER: return Theme.GOLD;
            case QUEEN: return Theme.MINT;
            case SOLDIER: return Theme.RISK_MID;
            case WORKER: return Theme.TEXT;
            default: return Theme.DOWN;
        }
    }

    /**
     * 마감 자산 이력으로 연속 수익(양수) 또는 연속 하락(음수) 턴 수를 센다.
     */
    private static int streak(List<BigDecimal> assets) {
        int n = 0;
        for (int i = assets.size() - 1; i >= 0; i--) {
            BigDecimal prev = i == 0 ? GameSession.INITIAL_CASH : assets.get(i - 1);
            int c = assets.get(i).compareTo(prev);
            if (c == 0 || (n > 0 && c < 0) || (n < 0 && c > 0)) {
                break;
            }
            n += c;
        }
        return n;
    }

    private void updateDetail() {
        if (view == null) {
            return;
        }
        GameSnapshot s = view.getSnapshot();
        Stock st = s.findStock(selectedId);
        if (st == null) {
            return;
        }
        detailName.setText(st.getName() + "  " + st.getId());
        detailMeta.setText(st.getSector().getLabel() + " 업종 · 위험도 " + st.getRisk().getLabel() + " · " + st.getDescription());
        detailPrice.setText(Theme.won(st.currentPrice()));
        BigDecimal ch = st.changeRate();
        detailChange.setText((ch.signum() > 0 ? "▲ " : ch.signum() < 0 ? "▼ " : "") + Theme.pct(ch) + " (직전 턴 대비) ");
        detailChange.setForeground(Theme.trend(ch));
        int qty = s.quantityOf(st.getId());
        Holding held = null;
        for (Holding h : s.getHoldings()) {
            if (h.getStockId().equals(st.getId()) && h.getQuantity() > 0) {
                held = h;
            }
        }
        if (held == null) {
            detailHolding.setText("보유하지 않은 종목");
            detailHolding.setForeground(Theme.MUTED);
        } else {
            BigDecimal pnl = held.unrealizedPnl(st.currentPrice());
            BigDecimal rate = pnl.multiply(BigDecimal.valueOf(100))
                    .divide(held.getCostBasis(), 2, RoundingMode.HALF_UP);
            detailHolding.setText(String.format("● 내 보유 %,d주 · 평균 %s · 평가손익 %s (%s) ", qty,
                    Theme.won(held.averagePrice()), Theme.signedWon(pnl), Theme.pct(rate)));
            detailHolding.setForeground(pnl.signum() == 0 ? Theme.MINT : Theme.trend(pnl));
        }
        chartPanel.setPrices(st.getPriceHistory());
    }

    /**
     * 시작 버튼 동작 (FN-01).
     */
    private void onStart() {
        Difficulty d = startPanel.getDifficulty();
        StartResult r = controller.startGame(startPanel.getPlayerName(), d);
        if (!r.isSuccess()) {
            JOptionPane.showMessageDialog(this, r.getMessage(), "시작할 수 없음", JOptionPane.WARNING_MESSAGE);
            startPanel.focusName();
            return;
        }
        selectedId = "S01";
        qtySpinner.setValue(1);
        refresh(r.getView());
        selectTab(0);
        status("게임 시작! 1턴 뉴스를 확인하고 종목을 골라 투자하세요.", Theme.MINT);
        cards.show(root, GAME);
    }

    /**
     * 매수 버튼 동작 (FN-02).
     */
    private void onBuy() {
        order(OrderSide.BUY);
    }

    /**
     * 매도 버튼 동작 (FN-02).
     */
    private void onSell() {
        order(OrderSide.SELL);
    }

    private void order(OrderSide side) {
        if (view == null) {
            return;
        }
        commitSpinner();
        int qty = (Integer) qtySpinner.getValue();
        Stock st = view.getSnapshot().findStock(selectedId);
        if (side == OrderSide.SELL && view.getSnapshot().quantityOf(selectedId) < qty) {
            status("보유 수량이 부족합니다. (보유 " + view.getSnapshot().quantityOf(selectedId) + "주)", Theme.UP);
            return;
        }
        boolean ok = new OrderDialog(this).showDialog(side, st, qty, view.getSnapshot().getCash());
        if (!ok) {
            status("주문을 취소했습니다.", Theme.MUTED);
            return;
        }
        TradeResult r = controller.placeOrder(selectedId, side, qty, view.getVersion(), UUID.randomUUID());
        if (r.isSuccess()) {
            refresh(r.getView());
            String extra = side == OrderSide.SELL ? "  실현 손익 " + Theme.signedWon(r.getRealizedPnl()) : "";
            status(r.getMessage() + extra, side == OrderSide.BUY ? Theme.UP : Theme.DOWN);
        } else {
            status(r.getMessage(), Theme.UP);
            if ("STALE_VIEW".equals(r.getErrorCode())) {
                refresh(controller.getView());
            }
        }
    }

    private void onMax() {
        if (view == null) {
            return;
        }
        Stock st = view.getSnapshot().findStock(selectedId);
        long max = view.getSnapshot().getCash().longValue() / st.currentPrice();
        int held = view.getSnapshot().quantityOf(selectedId);
        int value = (int) Math.max(1, Math.min(Portfolio.MAX_QUANTITY, max));
        qtySpinner.setValue(value);
        status(String.format("최대 매수 %,d주 · 보유 %,d주", max, held), Theme.MUTED);
    }

    private void commitSpinner() {
        try {
            qtySpinner.commitEdit();
        } catch (java.text.ParseException e) {
            qtySpinner.setValue(1);
        }
    }

    /**
     * 턴 마감 버튼 동작 (FN-03). 30턴을 마치면 결과 화면을 연다.
     */
    private void onNextTurn() {
        if (view == null) {
            return;
        }
        if (ticker != null) {
            finishTicks();
            return;
        }
        if (view.getSnapshot().isCompleted()) {
            showResult();
            return;
        }
        ViewData before = view;
        TurnResult r = controller.advanceTurn(view.getVersion(), UUID.randomUUID());
        if (!r.isSuccess()) {
            status(r.getMessage(), Theme.UP);
            refresh(controller.getView());
            return;
        }
        animateTurn(before, r);
    }

    /**
     * 턴 마감 장중 애니메이션. 확정된 종가는 이미 컨트롤러가 계산했고,
     * 화면은 이전 종가에서 새 종가까지 무작위로 흔들리는 경로를 약 2초 동안 보여 준다.
     * 경로의 마지막 값은 항상 실제 종가와 같으므로 게임 결과에는 영향이 없다.
     *
     * @param before 마감 전 화면 데이터
     * @param result 턴 마감 결과 (마감 후 화면 데이터 포함)
     */
    private void animateTurn(ViewData before, TurnResult result) {
        tickBefore = before;
        tickResult = result;
        GameSnapshot b = before.getSnapshot();
        GameSnapshot a = result.getView().getSnapshot();
        List<Stock> stocks = b.getStocks();
        long[][] paths = new long[stocks.size()][];
        for (int i = 0; i < stocks.size(); i++) {
            Stock s = stocks.get(i);
            paths[i] = tickPath(s.currentPrice(), a.findStock(s.getId()).currentPrice(), s.getRisk());
        }
        setTradingEnabled(false);
        nextButton.setText("건너뛰기 ▶▶");
        nextButton.setBase(Theme.GOLD);
        turnLabel.setText(String.format("TURN %02d · 장 진행 중", before.getCurrentTurn()));
        newsBadge.setText(" LIVE ");
        newsBadge.setBackground(Theme.UP);
        chartPanel.setLive(true);
        status("장 진행 중... 가격이 움직이고 있습니다", Theme.GOLD);
        int[] frame = {0};
        ticker = new Timer(TICK_MS, e -> {
            frame[0]++;
            if (frame[0] > TICK_FRAMES) {
                finishTicks();
            } else {
                renderTick(b, paths, frame[0]);
            }
        });
        ticker.start();
    }

    private long[] tickPath(long from, long to, RiskLevel risk) {
        int n = TICK_FRAMES;
        double total = from * risk.getMaxBasisPoints() / 10000.0 * 0.45;
        double step = total / Math.sqrt(n);
        double[] w = new double[n + 1];
        for (int k = 1; k <= n; k++) {
            w[k] = w[k - 1] + tickRandom.nextGaussian() * step;
        }
        long[] p = new long[n + 1];
        for (int k = 0; k <= n; k++) {
            double t = k / (double) n;
            double bridge = w[k] - t * w[n];
            p[k] = Math.max(1, Math.round(from + (to - from) * t + bridge));
        }
        p[0] = from;
        p[n] = to;
        return p;
    }

    private void renderTick(GameSnapshot b, long[][] paths, int k) {
        BigDecimal total = b.getCash();
        refreshing = true;
        List<Stock> stocks = b.getStocks();
        for (int i = 0; i < stocks.size(); i++) {
            Stock s = stocks.get(i);
            long p = paths[i][k];
            tickDir.put(s.getId(), Long.compare(p, paths[i][k - 1]));
            BigDecimal ch = BigDecimal.valueOf(p - s.currentPrice()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(s.currentPrice()), 2, RoundingMode.HALF_UP);
            stockModel.setValueAt(String.format("%,d", p), i, 4);
            stockModel.setValueAt(ch, i, 5);
            total = total.add(BigDecimal.valueOf(p).multiply(BigDecimal.valueOf(b.quantityOf(s.getId()))));
            if (s.getId().equals(selectedId)) {
                detailPrice.setText(Theme.won(p));
                detailChange.setText((ch.signum() > 0 ? "▲ " : ch.signum() < 0 ? "▼ " : "") + Theme.pct(ch) + " (장중) ");
                detailChange.setForeground(Theme.trend(ch));
                List<Long> h = new ArrayList<>(s.getPriceHistory());
                h.add(p);
                chartPanel.setPrices(h);
            }
        }
        refreshing = false;
        BigDecimal diff = total.subtract(GameSession.INITIAL_CASH);
        assetLabel.setText(Theme.won(total));
        rateLabel.setText("수익률 " + Theme.pct(diff.multiply(BigDecimal.valueOf(100))
                .divide(GameSession.INITIAL_CASH, 2, RoundingMode.HALF_UP)) + "  (" + Theme.signedWon(diff) + ")  ");
        rateLabel.setForeground(Theme.trend(diff));
        updateGoal(total);
    }

    private void finishTicks() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
        tickDir.clear();
        chartPanel.setLive(false);
        TurnResult r = tickResult;
        if (r == null) {
            return;
        }
        tickResult = null;
        refresh(r.getView());
        BigDecimal diff = r.getView().getTotalAssets().subtract(tickBefore.getTotalAssets());
        status(r.getCompletedTurns() + "턴 장 마감 · 총자산 " + Theme.signedWon(diff), Theme.trend(diff));
        if (r.isEnded()) {
            showResult();
        } else {
            showTurnSummary(tickBefore, r);
        }
    }

    /**
     * 장 마감 직후 이번 턴 손익, 최고 상승/최대 하락 종목, 칭호 변화, 연속 수익을 카드로 보여 준다.
     *
     * @param before 마감 전 화면 데이터
     * @param result 턴 마감 결과
     */
    private void showTurnSummary(ViewData before, TurnResult result) {
        ViewData after = result.getView();
        GameSnapshot s = after.getSnapshot();
        BigDecimal diff = after.getTotalAssets().subtract(before.getTotalAssets());
        BigDecimal pct = diff.multiply(BigDecimal.valueOf(100))
                .divide(before.getTotalAssets(), 2, RoundingMode.HALF_UP);
        Stock best = null;
        Stock worst = null;
        for (Stock st : s.getStocks()) {
            if (best == null || st.changeRate().compareTo(best.changeRate()) > 0) {
                best = st;
            }
            if (worst == null || st.changeRate().compareTo(worst.changeRate()) < 0) {
                worst = st;
            }
        }
        List<Object[]> lines = new ArrayList<>();
        lines.add(new Object[]{result.getCompletedTurns() + "턴 장 마감", Theme.GOLD, 15f});
        lines.add(new Object[]{Theme.signedWon(diff) + "  (" + Theme.pct(pct) + ") ", Theme.trend(diff), 34f});
        lines.add(new Object[]{"최고 상승  " + best.getName() + " " + Theme.pct(best.changeRate()) + " ", Theme.UP, 15f});
        lines.add(new Object[]{"최대 하락  " + worst.getName() + " " + Theme.pct(worst.changeRate()) + " ", Theme.DOWN, 15f});
        AntRank was = AntRank.of(before.getReturnRate());
        AntRank now = AntRank.of(after.getReturnRate());
        if (now.compareTo(was) > 0) {
            lines.add(new Object[]{"칭호 상승!  " + was.getLabel() + " → " + now.getLabel() + " ", rankColor(now), 20f});
        } else if (now.compareTo(was) < 0) {
            lines.add(new Object[]{"칭호 하락  " + was.getLabel() + " → " + now.getLabel() + " ", Theme.MUTED, 16f});
        }
        int streak = streak(s.getAssetHistory());
        if (streak >= 2) {
            lines.add(new Object[]{"▲ " + streak + "턴 연속 수익 중! ", Theme.UP, 17f});
        }
        lines.add(new Object[]{"클릭하면 닫힙니다", Theme.MUTED, 12f});
        summary.showLines(lines);
    }

    private void setTradingEnabled(boolean on) {
        buyButton.setEnabled(on);
        sellButton.setEnabled(on);
        maxButton.setEnabled(on);
        qtySpinner.setEnabled(on);
    }

    /**
     * 결과 화면을 연다 (FN-04).
     */
    private void showResult() {
        if (controller.evaluateResult() == null) {
            return;
        }
        new ResultDialog(this, controller, controller.evaluateResult()).setVisible(true);
    }

    private void onNewGame() {
        if (view != null && !view.getSnapshot().isCompleted()) {
            int c = JOptionPane.showConfirmDialog(this, "진행 중인 게임을 버리고 새로 시작할까요?", "새 게임",
                    JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (view != null) {
            startPanel.prefill(view.getSnapshot().getPlayerName(), view.getSnapshot().getDifficulty());
        }
        cards.show(root, START);
        startPanel.focusName();
    }

    private void openRecords() {
        if (view == null) {
            new RecordDialog(this, controller).setVisible(true);
        } else {
            new RecordDialog(this, controller, view.getSnapshot().getPlayerName(),
                    view.getSnapshot().getDifficulty()).setVisible(true);
        }
    }

    private void status(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    /** 시장 표 셀 렌더러: 위험도 색 점, 등락률 색, 보유 종목 강조, 장중 틱 깜빡임. */
    private final class MarketRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            int modelRow = t.convertRowIndexToModel(row);
            String id = (String) stockModel.getValueAt(modelRow, 0);
            boolean held = !"-".equals(stockModel.getValueAt(modelRow, 6));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setForeground(Theme.TEXT);
            if (col == 0 && held) {
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.MINT),
                        BorderFactory.createEmptyBorder(0, 6, 0, 10)));
            }
            setFont(Theme.font(13, col == 0));
            setHorizontalAlignment(col >= 3 ? SwingConstants.RIGHT : col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            if (v instanceof RiskLevel) {
                RiskLevel r = (RiskLevel) v;
                setText("● " + r.getLabel());
                setForeground(Theme.risk(r));
            } else if (v instanceof BigDecimal) {
                BigDecimal b = (BigDecimal) v;
                setText((b.signum() > 0 ? "▲ " : b.signum() < 0 ? "▼ " : "") + Theme.pct(b));
                setForeground(Theme.trend(b));
            } else if (col == 5 && held) {
                setText("● " + v);
                setForeground(Theme.MINT);
                setFont(Theme.font(13, true));
            }
            Color bg = sel ? t.getSelectionBackground() : Theme.PANEL;
            Integer dir = tickDir.get(id);
            if (dir != null && col == 3) {
                bg = dir > 0 ? new Color(0x4A1E28) : dir < 0 ? new Color(0x1A2C4D) : bg;
                setForeground(dir > 0 ? Theme.UP : dir < 0 ? Theme.DOWN : Theme.TEXT);
                setFont(Theme.font(13, true));
            }
            setBackground(bg);
            return this;
        }
    }

    /** 화면 위에 잠깐 떠서 턴 결과를 보여 주는 반투명 카드 (글래스 페인). */
    private static final class SummaryPane extends JComponent {
        private List<Object[]> lines = new ArrayList<>();
        private final Timer hide = new Timer(2800, e -> setVisible(false));

        SummaryPane() {
            hide.setRepeats(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    setVisible(false);
                }
            });
        }

        void showLines(List<Object[]> lines) {
            this.lines = lines;
            setVisible(true);
            repaint();
            hide.restart();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = Widgets.smooth(g);
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRect(0, 0, getWidth(), getHeight());
            int[] heights = new int[lines.size()];
            int cw = 0;
            int ch = 36;
            for (int i = 0; i < lines.size(); i++) {
                g2.setFont(Theme.font((Float) lines.get(i)[2], true));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                heights[i] = fm.getHeight() + 6;
                ch += heights[i];
                cw = Math.max(cw, fm.stringWidth((String) lines.get(i)[0]));
            }
            cw = Math.max(cw + 80, 420);
            int x = (getWidth() - cw) / 2;
            int y = (getHeight() - ch) / 2;
            g2.setColor(Theme.PANEL2);
            g2.fillRoundRect(x, y, cw, ch, 24, 24);
            g2.setColor(Theme.GOLD);
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawRoundRect(x, y, cw, ch, 24, 24);
            int ty = y + 18;
            for (int i = 0; i < lines.size(); i++) {
                Object[] l = lines.get(i);
                g2.setFont(Theme.font((Float) l[2], true));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.setColor((Color) l[1]);
                String text = (String) l[0];
                g2.drawString(text, x + (cw - fm.stringWidth(text)) / 2, ty + fm.getAscent());
                ty += heights[i];
            }
            g2.dispose();
        }
    }

    /** 손익 칸에 색을 입히는 렌더러. */
    private static final class PnlRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setForeground(Theme.TEXT);
            setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
            if (v instanceof BigDecimal) {
                BigDecimal b = (BigDecimal) v;
                boolean isRate = t.getColumnName(col).contains("률");
                setText(isRate ? Theme.pct(b) : Theme.signedWon(b));
                setForeground(Theme.trend(b));
            } else if (v instanceof OrderSide) {
                OrderSide s = (OrderSide) v;
                setText(s.getLabel());
                setForeground(s == OrderSide.BUY ? Theme.UP : Theme.DOWN);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            setBackground(sel ? t.getSelectionBackground() : Theme.PANEL);
            return this;
        }
    }
}
