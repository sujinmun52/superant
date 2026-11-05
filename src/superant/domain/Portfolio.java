package superant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import superant.model.OrderSide;

/**
 * 플레이어의 현금, 보유 종목, 거래 내역.
 * <p>
 * 현금은 0 이상을 유지한다. 수수료는 없다.
 */
public class Portfolio {
    /** 비중 맵에서 현금을 나타내는 키. */
    public static final String CASH_KEY = "CASH";
    /** 한 번에 주문할 수 있는 최대 수량. */
    public static final int MAX_QUANTITY = 99_999;

    private BigDecimal cash;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    /**
     * 초기 현금으로 포트폴리오를 만든다.
     *
     * @param initialCash 초기 현금
     */
    public Portfolio(BigDecimal initialCash) {
        this.cash = initialCash;
    }

    /**
     * 현재 가격으로 매수한다.
     *
     * @param stock 종목
     * @param qty   수량 (1 ~ 99,999)
     * @param turn  체결 턴
     * @return 체결 거래
     * @throws IllegalArgumentException 수량이 범위를 벗어나거나 현금이 부족할 때
     */
    public Trade buy(Stock stock, int qty, int turn) {
        checkQuantity(qty);
        long price = stock.currentPrice();
        BigDecimal amount = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(qty));
        if (amount.compareTo(cash) > 0) {
            throw new IllegalArgumentException("현금이 부족합니다.");
        }
        cash = cash.subtract(amount);
        holdings.computeIfAbsent(stock.getId(), Holding::new).add(qty, amount);
        Trade trade = new Trade(turn, stock.getId(), OrderSide.BUY, qty, price, BigDecimal.ZERO);
        trades.add(trade);
        return trade;
    }

    /**
     * 현재 가격으로 매도한다. 실현 손익 = 매도 금액 - 배분 원가.
     *
     * @param stock 종목
     * @param qty   수량 (1 ~ 보유 수량)
     * @param turn  체결 턴
     * @return 체결 거래
     * @throws IllegalArgumentException 수량이 범위를 벗어나거나 보유 수량이 부족할 때
     */
    public Trade sell(Stock stock, int qty, int turn) {
        checkQuantity(qty);
        Holding h = holdings.get(stock.getId());
        if (h == null || h.getQuantity() < qty) {
            throw new IllegalArgumentException("보유 수량이 부족합니다.");
        }
        long price = stock.currentPrice();
        BigDecimal amount = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(qty));
        BigDecimal allocated = h.remove(qty);
        BigDecimal pnl = amount.subtract(allocated);
        h.addRealized(pnl);
        cash = cash.add(amount);
        Trade trade = new Trade(turn, stock.getId(), OrderSide.SELL, qty, price, pnl);
        trades.add(trade);
        return trade;
    }

    private static void checkQuantity(int qty) {
        if (qty < 1 || qty > MAX_QUANTITY) {
            throw new IllegalArgumentException("수량은 1 ~ 99,999 사이여야 합니다.");
        }
    }

    /**
     * 현재 현금을 반환한다.
     *
     * @return 현금
     */
    public BigDecimal getCash() {
        return cash;
    }

    /**
     * 총자산(현금 + 보유 종목 평가액)을 계산한다.
     *
     * @param market 현재 가격을 가진 시장
     * @return 총자산
     */
    public BigDecimal totalAssets(Market market) {
        BigDecimal total = cash;
        for (Holding h : holdings.values()) {
            long price = market.getStock(h.getStockId()).currentPrice();
            total = total.add(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(h.getQuantity())));
        }
        return total;
    }

    /**
     * 총자산 대비 비중을 계산한다. 현금은 {@link #CASH_KEY} 키로 들어간다.
     *
     * @param market 현재 가격을 가진 시장
     * @return 키별 비중 (0 ~ 1, 소수 8자리)
     */
    public Map<String, BigDecimal> weights(Market market) {
        BigDecimal total = totalAssets(market);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (total.signum() == 0) {
            map.put(CASH_KEY, BigDecimal.ONE);
            return map;
        }
        map.put(CASH_KEY, cash.divide(total, 8, RoundingMode.HALF_UP));
        for (Holding h : holdings.values()) {
            if (h.getQuantity() == 0) {
                continue;
            }
            long price = market.getStock(h.getStockId()).currentPrice();
            BigDecimal value = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(h.getQuantity()));
            map.put(h.getStockId(), value.divide(total, 8, RoundingMode.HALF_UP));
        }
        return map;
    }

    /**
     * 종목의 보유 현황을 반환한다.
     *
     * @param stockId 종목 ID
     * @return 보유 현황, 거래한 적 없으면 null
     */
    public Holding getHolding(String stockId) {
        return holdings.get(stockId);
    }

    /** @return 거래한 모든 종목의 보유 현황 (읽기 전용) */
    public List<Holding> getHoldings() {
        return Collections.unmodifiableList(new ArrayList<>(holdings.values()));
    }

    /** @return 전체 거래 내역 (읽기 전용) */
    public List<Trade> getTrades() {
        return Collections.unmodifiableList(trades);
    }
}
