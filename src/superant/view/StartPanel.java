package superant.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import superant.model.Difficulty;

/**
 * 시작 화면 (UI-01). 이름 입력과 난이도 카드 3장을 보여 준다.
 */
public class StartPanel extends JPanel {
    private final JTextField nameField = new JTextField(14);
    private final ButtonGroup difficultyGroup = new ButtonGroup();
    private final Widgets.CardToggle[] cards = new Widgets.CardToggle[3];

    /**
     * 시작 화면을 만든다.
     *
     * @param onStart   게임 시작 버튼 동작
     * @param onRecords 기록 조회 버튼 동작
     */
    public StartPanel(Runnable onStart, Runnable onRecords) {
        super(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);

        JLabel badge = Theme.label("30 TURN INVESTMENT CHALLENGE", 13, Theme.MINT, true);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(badge, c);

        JLabel title = new JLabel("슈퍼개미", SwingConstants.CENTER);
        title.setFont(Theme.title(76));
        title.setForeground(Theme.TEXT);
        box.add(title, c);

        JLabel sub = Theme.label("100만 원으로 30턴 안에 120만 원을 만들어라!", 17, Theme.MUTED, false);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        c.insets = new Insets(0, 0, 30, 0);
        box.add(sub, c);

        JPanel nameRow = new JPanel(new BorderLayout(0, 6));
        nameRow.setOpaque(false);
        nameRow.add(Theme.label("플레이어 이름 (1~12자, 한글/영문/숫자/_)", 13, Theme.MUTED, true), BorderLayout.NORTH);
        nameField.setFont(Theme.font(18, false));
        nameField.setBackground(Theme.PANEL);
        nameField.setForeground(Theme.TEXT);
        nameField.setCaretColor(Theme.MINT);
        nameField.setBorder(Theme.cardBorder(10));
        nameField.addActionListener(e -> onStart.run());
        nameRow.add(nameField, BorderLayout.CENTER);
        c.insets = new Insets(0, 0, 22, 0);
        box.add(nameRow, c);

        c.insets = new Insets(0, 0, 8, 0);
        box.add(Theme.label("난이도 선택", 13, Theme.MUTED, true), c);
        JPanel cardRow = new JPanel(new GridLayout(1, 3, 14, 0));
        cardRow.setOpaque(false);
        cards[0] = card(Difficulty.EASY, "쉬움", Theme.RISK_LOW,
                "변동 폭 x0.75", "뉴스 영향 방향 힌트");
        cards[1] = card(Difficulty.NORMAL, "보통", Theme.MINT,
                "변동 폭 x1.0", "관련 기업/업종만 공개");
        cards[2] = card(Difficulty.HARD, "어려움", Theme.UP,
                "변동 폭 x1.25", "뉴스 40%가 한 턴 늦게 반영");
        for (Widgets.CardToggle t : cards) {
            difficultyGroup.add(t);
            cardRow.add(t);
        }
        cards[1].setSelected(true);
        c.insets = new Insets(0, 0, 28, 0);
        box.add(cardRow, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        Widgets.FlatButton start = new Widgets.FlatButton("게임 시작 ▶", Theme.MINT, Theme.BG);
        start.setFont(Theme.font(17, true));
        start.setPreferredSize(new Dimension(220, 50));
        start.addActionListener(e -> onStart.run());
        Widgets.FlatButton records = new Widgets.FlatButton("기록 조회", Theme.PANEL2, Theme.TEXT);
        records.setFont(Theme.font(15, true));
        records.setPreferredSize(new Dimension(140, 50));
        records.addActionListener(e -> onRecords.run());
        buttons.add(start);
        buttons.add(records);
        box.add(buttons, c);

        JLabel rules = Theme.label("<html><div style='text-align:center'>종목 6개 · 수수료 없음 · 턴마다 75% 확률로 뉴스 발생<br>"
                + "수익률 20% 이상이면 슈퍼개미 등극!</div></html>", 12, Theme.MUTED, false);
        rules.setHorizontalAlignment(SwingConstants.CENTER);
        c.insets = new Insets(4, 0, 0, 0);
        box.add(rules, c);

        box.setPreferredSize(new Dimension(720, box.getPreferredSize().height));
        add(box);
    }

    private Widgets.CardToggle card(Difficulty d, String kor, Color accent, String line1, String line2) {
        String hex = String.format("#%06X", accent.getRGB() & 0xFFFFFF);
        String html = "<html><div style='text-align:center'>"
                + "<span style='font-size:15px;color:" + hex + "'><b>" + d.name() + "</b></span><br>"
                + "<span style='font-size:13px'><b>" + kor + "</b></span><br><br>"
                + "<span style='color:#8C96A8'>" + line1 + "<br>" + line2 + "</span></div></html>";
        Widgets.CardToggle t = new Widgets.CardToggle(html, accent);
        t.setActionCommand(d.name());
        t.setPreferredSize(new Dimension(200, 130));
        return t;
    }

    /**
     * 입력한 이름을 반환한다.
     *
     * @return 앞뒤 공백을 지운 이름
     */
    public String getPlayerName() {
        return nameField.getText().trim();
    }

    /**
     * 선택한 난이도를 반환한다.
     *
     * @return 난이도, 선택하지 않았으면 null
     */
    public Difficulty getDifficulty() {
        ButtonModel m = difficultyGroup.getSelection();
        return m == null ? null : Difficulty.valueOf(m.getActionCommand());
    }

    /**
     * 이름 입력칸에 포커스를 준다.
     */
    public void focusName() {
        nameField.requestFocusInWindow();
        nameField.selectAll();
    }

    /**
     * 이름과 난이도를 미리 채운다.
     *
     * @param name       이름
     * @param difficulty 난이도
     */
    public void prefill(String name, Difficulty difficulty) {
        nameField.setText(name);
        if (difficulty != null) {
            cards[difficulty.ordinal()].setSelected(true);
        }
    }

    static JPanel spacer(int h) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(h, 0, 0, 0));
        return p;
    }
}
