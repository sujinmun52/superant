package superant.domain;

import java.math.BigDecimal;

import superant.model.OrderSide;

/**
 * 체결된 거래 한 건. 생성 후 바뀌지 않는다.
 */
public class Trade {
    private final int turn;
    private final String stockId;
    private final OrderSide side;
    private final int quantity;
    private final long price;
    private final BigDecimal realizedPnl;

    /**
     * 거래 기록을 만든다.
     *
     * @param turn        체결 턴
     * @param stockId     종목 ID
     * @param side        매수 또는 매도
     * @param quantity    수량
     * @param price       체결 가격(원)
     * @param realizedPnl 실현 손익 (매수는 0)
     */
    public Trade(int turn, String stockId, OrderSide side, int quantity, long price, BigDecimal realizedPnl) {
        this.turn = turn;
        this.stockId = stockId;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.realizedPnl = realizedPnl;
    }

    /**
     * 거래 금액(가격 x 수량)을 반환한다.
     *
     * @return 거래 금액(원)
     */
    public long getAmount() {
        return price * quantity;
    }

    /** @return 체결 턴 */
    public int getTurn() {
        return turn;
    }

    /** @return 종목 ID */
    public String getStockId() {
        return stockId;
    }

    /** @return 매수 또는 매도 */
    public OrderSide getSide() {
        return side;
    }

    /** @return 수량 */
    public int getQuantity() {
        return quantity;
    }

    /** @return 체결 가격(원) */
    public long getPrice() {
        return price;
    }

    /** @return 실현 손익 */
    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }
}
