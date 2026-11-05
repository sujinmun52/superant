package superant.controller;

import java.math.BigDecimal;

/**
 * 매수/매도 요청의 결과 (FN-02).
 */
public class TradeResult extends ActionResult {
    private final long price;
    private final int quantity;
    private final BigDecimal realizedPnl;
    private final ViewData view;

    private TradeResult(boolean success, String errorCode, String message, long price, int quantity,
                        BigDecimal realizedPnl, ViewData view) {
        super(success, errorCode, message);
        this.price = price;
        this.quantity = quantity;
        this.realizedPnl = realizedPnl;
        this.view = view;
    }

    /**
     * 체결 결과를 만든다.
     *
     * @param message     체결 안내 문구
     * @param price       체결 가격
     * @param quantity    체결 수량
     * @param realizedPnl 실현 손익 (매수는 0)
     * @param view        체결 후 화면 데이터
     * @return 성공 결과
     */
    public static TradeResult ok(String message, long price, int quantity, BigDecimal realizedPnl, ViewData view) {
        return new TradeResult(true, null, message, price, quantity, realizedPnl, view);
    }

    /**
     * 거절 결과를 만든다. 상태는 바뀌지 않는다.
     *
     * @param code    실패 코드
     * @param message 안내 문구
     * @return 실패 결과
     */
    public static TradeResult fail(String code, String message) {
        return new TradeResult(false, code, message, 0, 0, BigDecimal.ZERO, null);
    }

    /** @return 체결 가격 */
    public long getPrice() {
        return price;
    }

    /** @return 체결 수량 */
    public int getQuantity() {
        return quantity;
    }

    /** @return 실현 손익 */
    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    /** @return 체결 후 화면 데이터, 실패하면 null */
    public ViewData getView() {
        return view;
    }
}
