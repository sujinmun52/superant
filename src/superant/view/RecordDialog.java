package superant.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.UncheckedIOException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import superant.controller.GameController;
import superant.model.Achievement;
import superant.model.Difficulty;
import superant.model.Ending;
import superant.persistence.RecordEntry;

/**
 * 기록 조회 화면 (UI-05). 이름과 난이도로 누적 기록을 찾는다.
 */
public class RecordDialog extends JDialog {
    private final GameController controller;
    private final JTextField nameField = new JTextField(12);
    private final JComboBox<Difficulty> difficultyBox = new JComboBox<>(Difficulty.values());
    private final JPanel body = new JPanel(new BorderLayout());

    /**
     * 기록 조회 화면을 만든다.
     *
     * @param owner      부모 창
     * @param controller 게임 컨트롤러
     */
    public RecordDialog(JFrame owner, GameController controller) {
        this(owner, controller, "", Difficulty.NORMAL);
    }

    /**
     * 이름과 난이도를 미리 채운 기록 조회 화면을 만든다. 이름이 있으면 바로 조회한다.
     *
     * @param owner      부모 창
     * @param controller 게임 컨트롤러
     * @param name       미리 채울 이름
     * @param difficulty 미리 고를 난이도
     */
    public RecordDialog(JFrame owner, GameController controller, String name, Difficulty difficulty) {
        super(owner, "기록 조회", true);
        this.controller = controller;

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(Theme.label("이름", 13, Theme.MUTED, true));
        nameField.setFont(Theme.font(14, false));
        nameField.setBackground(Theme.PANEL);
        nameField.setForeground(Theme.TEXT);
        nameField.setCaretColor(Theme.MINT);
        nameField.setBorder(Theme.cardBorder(6));
        nameField.setText(name == null ? "" : name);
        nameField.addActionListener(e -> onSearch());
        top.add(nameField);
        top.add(Theme.label("  난이도", 13, Theme.MUTED, true));
        difficultyBox.setSelectedItem(difficulty == null ? Difficulty.NORMAL : difficulty);
        top.add(difficultyBox);
        Widgets.FlatButton search = new Widgets.FlatButton("조회", Theme.MINT, Theme.BG);
        search.addActionListener(e -> onSearch());
        top.add(search);
        root.add(top, BorderLayout.NORTH);

        body.setBackground(Theme.PANEL);
        body.setBorder(Theme.cardBorder(16));
        body.setPreferredSize(new Dimension(560, 290));
        root.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        Widgets.FlatButton close = new Widgets.FlatButton("닫기", Theme.PANEL2, Theme.TEXT);
        close.addActionListener(e -> dispose());
        bottom.add(close);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        if (name != null && !name.trim().isEmpty()) {
            onSearch();
        } else {
            message("이름과 난이도를 입력하고 조회를 누르세요.", Theme.MUTED);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 조회 버튼 동작.
     */
    private void onSearch() {
        String name = nameField.getText().trim();
        if (!GameController.NAME_PATTERN.matcher(name).matches()) {
            message("이름은 1~12자의 한글, 영문, 숫자, 밑줄(_)만 쓸 수 있습니다.", Theme.UP);
            return;
        }
        Difficulty d = (Difficulty) difficultyBox.getSelectedItem();
        RecordEntry e;
        try {
            e = controller.readRecords(name, d);
        } catch (UncheckedIOException ex) {
            message("기록을 읽지 못했습니다: " + ex.getCause().getMessage(), Theme.UP);
            return;
        }
        if (e == null) {
            message(name + " / " + d.name() + " 기록이 없습니다.", Theme.MUTED);
            return;
        }
        show(e);
    }

    private void message(String text, java.awt.Color color) {
        body.removeAll();
        JLabel l = Theme.label(text, 14, color, false);
        l.setHorizontalAlignment(JLabel.CENTER);
        body.add(l, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    private void show(RecordEntry e) {
        body.removeAll();
        JPanel grid = new JPanel();
        grid.setLayout(new javax.swing.BoxLayout(grid, javax.swing.BoxLayout.Y_AXIS));
        grid.setOpaque(false);
        row(grid, "플레이어", e.getPlayerName() + " · " + e.getDifficulty().name());
        row(grid, "저장한 게임 수", e.getPlayCount() + "판");
        row(grid, "최고 자산", e.getBestAssets() == null ? "-" : Theme.won(e.getBestAssets()));
        row(grid, "최고 기록 수익률", e.getBestReturnRate() == null ? "-" : Theme.pct(e.getBestReturnRate()));

        StringBuilder endings = new StringBuilder("<html>");
        for (Ending end : Ending.values()) {
            boolean got = e.getEndings().contains(end);
            endings.append("<span style='color:").append(got ? "#FFC24B" : "#4A5468").append("'>")
                    .append(got ? "● " : "○ ").append(end.getTitle()).append(" (").append(end.getLabel())
                    .append(")</span><br>");
        }
        row(grid, "달성 엔딩", endings.append("</html>").toString());

        StringBuilder ach = new StringBuilder("<html>");
        for (Achievement a : Achievement.values()) {
            int n = e.getAchievements().getOrDefault(a, 0);
            ach.append("<span style='color:").append(n > 0 ? "#2EE6A6" : "#4A5468").append("'>")
                    .append(a.getLabel()).append(" x").append(n).append("</span><br>");
        }
        row(grid, "업적", ach.append("</html>").toString());
        body.add(grid, BorderLayout.NORTH);
        body.revalidate();
        body.repaint();
    }

    private static void row(JPanel grid, String key, String value) {
        JPanel r = new JPanel(new BorderLayout(16, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel k = Theme.label(key, 14, Theme.MUTED, true);
        k.setPreferredSize(new Dimension(140, 22));
        k.setVerticalAlignment(JLabel.TOP);
        r.add(k, BorderLayout.WEST);
        r.add(Theme.label(value, 14, Theme.TEXT, true), BorderLayout.CENTER);
        r.setAlignmentX(0f);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
        grid.add(r);
    }
}
