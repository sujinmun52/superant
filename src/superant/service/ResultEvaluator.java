package superant.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import superant.domain.GameSession;
import superant.domain.GameSnapshot;
import superant.domain.Holding;
import superant.domain.Portfolio;
import superant.domain.Stock;
import superant.domain.Trade;
import superant.model.Achievement;
import superant.model.Ending;
import superant.model.InvestorType;
import superant.model.OrderSide;
import superant.model.RiskLevel;

/**
 * 종료된 게임의 스냅샷으로 결과를 판정한다 (FN-04).
 * 같은 스냅샷이면 언제 불러도 같은 결과를 낸다.
 */
public class ResultEvaluator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal PROFIT_LINE = BigDecimal.valueOf(20);

    /**
     * 결과를 판정한다.
     *
     * @param snapshot 30턴을 마친 게임의 스냅샷
     * @return 게임 결과
     * @throws IllegalStateException 게임이 끝나지 않았을 때
     */
    public GameResult evaluate(GameSnapshot snapshot) {
        if (!snapshot.isCompleted()) {
            throw new IllegalStateException("30턴을 모두 마쳐야 결과를 볼 수 있습니다.");
        }
        BigDecimal initial = GameSession.INITIAL_CASH;
        List<BigDecimal> assets = snapshot.getAssetHistory();
        BigDecimal finalAssets = assets.get(assets.size() - 1);
        BigDecimal returnRate = finalAssets.subtract(initial).multiply(HUNDRED)
                .divide(initial, 2, RoundingMode.HALF_UP);

        BigDecimal realized = BigDecimal.ZERO;
        BigDecimal unrealized = BigDecimal.ZERO;
        String bestId = null;
        BigDecimal bestPnl = null;
        for (Holding h : snapshot.getHoldings()) {
            long price = snapshot.findStock(h.getStockId()).currentPrice();
            BigDecimal u = h.unrealizedPnl(price);
            realized = realized.add(h.getRealizedPnl());
            unrealized = unrealized.add(u);
            BigDecimal pnl = h.getRealizedPnl().add(u);
            if (bestPnl == null || pnl.compareTo(bestPnl) > 0
                    || (pnl.compareTo(bestPnl) == 0 && h.getStockId().compareTo(bestId) < 0)) {
                bestPnl = pnl;
                bestId = h.getStockId();
            }
        }

        return new GameResult(snapshot.getSessionId(), snapshot.getPlayerName(), snapshot.getDifficulty(),
                finalAssets, returnRate, realized.setScale(0, RoundingMode.HALF_UP),
                unrealized.setScale(0, RoundingMode.HALF_UP), maxDrawdown(assets), bestId,
                snapshot.getTrades().size(), decideEnding(returnRate), decideType(snapshot),
                findAchievements(snapshot));
    }

    private Ending decideEnding(BigDecimal returnRate) {
        if (returnRate.compareTo(PROFIT_LINE) >= 0) {
            return Ending.PROFIT;
        }
        if (returnRate.signum() >= 0) {
            return Ending.MAINTAIN;
        }
        return Ending.RETRY;
    }

    private InvestorType decideType(GameSnapshot snapshot) {
        List<Map<String, BigDecimal>> history = snapshot.getWeightHistory();
        BigDecimal maxSum = BigDecimal.ZERO;
        BigDecimal cashSum = BigDecimal.ZERO;
        BigDecimal highSum = BigDecimal.ZERO;
        for (Map<String, BigDecimal> w : history) {
            BigDecimal max = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> e : w.entrySet()) {
                if (Portfolio.CASH_KEY.equals(e.getKey())) {
                    cashSum = cashSum.add(e.getValue());
                    continue;
                }
                max = max.max(e.getValue());
                Stock s = snapshot.findStock(e.getKey());
                if (s != null && s.getRisk() == RiskLevel.HIGH) {
                    highSum = highSum.add(e.getValue());
                }
            }
            maxSum = maxSum.add(max);
        }
        BigDecimal n = BigDecimal.valueOf(Math.max(1, history.size()));
        BigDecimal maxAvg = maxSum.divide(n, 8, RoundingMode.HALF_UP);
        BigDecimal cashAvg = cashSum.divide(n, 8, RoundingMode.HALF_UP);
        BigDecimal highAvg = highSum.divide(n, 8, RoundingMode.HALF_UP);

        if (maxAvg.compareTo(new BigDecimal("0.70")) >= 0) {
            return InvestorType.CONCENTRATED;
        }
        if (cashAvg.compareTo(new BigDecimal("0.50")) >= 0) {
            return InvestorType.STABLE;
        }
        if (highAvg.compareTo(new BigDecimal("0.50")) >= 0) {
            return InvestorType.AGGRESSIVE;
        }
        if (maxAvg.compareTo(new BigDecimal("0.40")) <= 0 && boughtStocks(snapshot).size() >= 3) {
            return InvestorType.DIVERSIFIED;
        }
        return InvestorType.BALANCED;
    }

    private List<Achievement> findAchievements(GameSnapshot snapshot) {
        List<Achievement> list = new ArrayList<>();
        boolean noLoss = !snapshot.getTrades().isEmpty();
        for (BigDecimal a : snapshot.getAssetHistory()) {
            if (a.compareTo(GameSession.INITIAL_CASH) < 0) {
                noLoss = false;
                break;
            }
        }
        if (noLoss) {
            list.add(Achievement.NO_LOSS);
        }

        Set<String> firstTurnBuys = new HashSet<>();
        Set<String> sold = new HashSet<>();
        for (Trade t : snapshot.getTrades()) {
            if (t.getSide() == OrderSide.BUY && t.getTurn() == 1) {
                firstTurnBuys.add(t.getStockId());
            }
            if (t.getSide() == OrderSide.SELL) {
                sold.add(t.getStockId());
            }
        }
        firstTurnBuys.removeAll(sold);
        if (!firstTurnBuys.isEmpty()) {
            list.add(Achievement.LONG_HOLD);
        }

        if (boughtStocks(snapshot).size() >= 3) {
            list.add(Achievement.DIVERSIFIED);
        }
        return list;
    }

    private static Set<String> boughtStocks(GameSnapshot snapshot) {
        Set<String> ids = new HashSet<>();
        for (Trade t : snapshot.getTrades()) {
            if (t.getSide() == OrderSide.BUY) {
                ids.add(t.getStockId());
            }
        }
        return ids;
    }

    /**
     * 초기 자금을 시작점으로 한 최고점 대비 최대 낙폭(%)을 계산한다.
     */
    private BigDecimal maxDrawdown(List<BigDecimal> assets) {
        BigDecimal peak = GameSession.INITIAL_CASH;
        BigDecimal worst = BigDecimal.ZERO;
        for (BigDecimal a : assets) {
            if (a.compareTo(peak) > 0) {
                peak = a;
            }
            BigDecimal dd = peak.subtract(a).multiply(HUNDRED).divide(peak, 2, RoundingMode.HALF_UP);
            worst = worst.max(dd);
        }
        return worst.setScale(2, RoundingMode.HALF_UP);
    }
}
