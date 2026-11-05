package superant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 한 종목의 보유 현황.
 * <p>
 * 원가는 누적 매수 금액이며, 매도 때 평균 단가로 배분한다(소수 8자리, HALF_UP).
 * 전량 매도하면 남은 원가를 모두 배분한다. 수량이 0 이 되어도 실현 손익을 위해 객체는 남긴다.
 */
public class Holding {
    private static final int SCALE = 8;

    private final String stockId;
    private int quantity;
    private BigDecimal costBasis = BigDecimal.ZERO;
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    /**
     * 빈 보유 현황을 만든다.
     *
     * @param stockId 종목 ID
     */
    public Holding(String stockId) {
        this.stockId = stockId;
    }

    private Holding(Holding other) {
        this.stockId = other.stockId;
        this.quantity = other.quantity;
        this.costBasis = other.costBasis;
        this.realizedPnl = other.realizedPnl;
    }

    /**
     * 평균 단가(남은 원가 / 수량)를 반환한다.
     *
     * @return 평균 단가, 수량이 0 이면 0
     */
    public BigDecimal averagePrice() {
        if (quantity == 0) {
            return BigDecimal.ZERO;
        }
        return costBasis.divide(BigDecimal.valueOf(quantity), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 매수 수량과 금액을 더한다.
     *
     * @param qty    매수 수량
     * @param amount 매수 금액
     */
    public void add(int qty, BigDecimal amount) {
        quantity += qty;
        costBasis = costBasis.add(amount);
    }

    /**
     * 매도 수량만큼 빼고, 그 수량에 배분된 원가를 반환한다.
     *
     * @param qty 매도 수량 (보유 수량 이하)
     * @return 배분된 원가
     */
    public BigDecimal remove(int qty) {
        if (qty < 1 || qty > quantity) {
            throw new IllegalArgumentException("보유 수량보다 많이 팔 수 없습니다.");
        }
        BigDecimal allocated;
        if (qty == quantity) {
            allocated = costBasis;
        } else {
            allocated = averagePrice().multiply(BigDecimal.valueOf(qty)).setScale(SCALE, RoundingMode.HALF_UP);
        }
        quantity -= qty;
        costBasis = quantity == 0 ? BigDecimal.ZERO : costBasis.subtract(allocated);
        return allocated;
    }

    /**
     * 실현 손익을 누적한다.
     *
     * @param pnl 이번 매도의 실현 손익
     */
    void addRealized(BigDecimal pnl) {
        realizedPnl = realizedPnl.add(pnl);
    }

    /**
     * 현재 가격 기준 평가 손익(보유 가치 - 남은 원가)을 반환한다.
     *
     * @param price 현재 가격
     * @return 평가 손익
     */
    public BigDecimal unrealizedPnl(long price) {
        return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity)).subtract(costBasis);
    }

    /**
     * 스냅샷용 복사본을 만든다.
     *
     * @return 같은 값을 가진 새 Holding
     */
    public Holding copy() {
        return new Holding(this);
    }

    /** @return 종목 ID */
    public String getStockId() {
        return stockId;
    }

    /** @return 보유 수량 */
    public int getQuantity() {
        return quantity;
    }

    /** @return 남은 원가 */
    public BigDecimal getCostBasis() {
        return costBasis;
    }

    /** @return 누적 실현 손익 */
    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }
}
