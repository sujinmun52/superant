package superant.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import superant.domain.Stock;
import superant.model.OrderSide;

/**
 * 주문 확인 대화상자 (UI-03). 체결 예상 금액과 주문 후 현금을 보여 주고 확인을 받는다.
 */
public class OrderDialog extends JDialog {
    private boolean confirmed;

    /**
     * 대화상자를 만든다.
     *
     * @param owner 부모 창
     */
    public OrderDialog(JFrame owner) {
        super(owner, "주문 확인", true);
    }

    /**
     * 주문 내용을 보여 주고 사용자의 확인 여부를 반환한다.
     *
     * @param side  매수 또는 매도
     * @param stock 대상 종목
     * @param qty   수량
     * @param cash  현재 현금
     * @return 확인을 눌렀으면 true
     */
    public boolean showDialog(OrderSide side, Stock stock, int qty, BigDecimal cash) {
        confirmed = false;
        Color accent = side == OrderSide.BUY ? Theme.UP : Theme.DOWN;
        BigDecimal amount = BigDecimal.valueOf(stock.currentPrice()).multiply(BigDecimal.valueOf(qty));
        BigDecimal after = side == OrderSide.BUY ? cash.subtract(amount) : cash.add(amount);
        boolean enough = after.signum() >= 0;

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));

        JLabel head = Theme.label(side.getLabel() + " 주문", 22, accent, true);
        root.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 18, 10));
        grid.setOpaque(false);
        row(grid, "종목", stock.getName() + " (" + stock.getId() + ")", Theme.TEXT);
        row(grid, "체결 가격", Theme.won(stock.currentPrice()), Theme.TEXT);
        row(grid, "수량", String.format("%,d주", qty), Theme.TEXT);
        row(grid, "주문 금액", Theme.won(amount), accent);
        row(grid, "주문 후 현금", enough ? Theme.won(after) : "현금 부족", enough ? Theme.TEXT : Theme.UP);
        root.add(grid, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        Widgets.FlatButton cancel = new Widgets.FlatButton("취소", Theme.PANEL2, Theme.TEXT);
        Widgets.FlatButton ok = new Widgets.FlatButton(side.getLabel() + " 확정", accent, Color.WHITE);
        ok.setEnabled(enough);
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        buttons.add(cancel);
        buttons.add(ok);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(enough ? ok : cancel);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return confirmed;
    }

    private static void row(JPanel grid, String key, String value, Color color) {
        grid.add(Theme.label(key, 14, Theme.MUTED, false));
        JLabel v = Theme.label(value, 15, color, true);
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        grid.add(v);
    }
}
