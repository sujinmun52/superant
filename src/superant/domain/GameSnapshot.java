package superant.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import superant.model.Difficulty;
import superant.model.GameState;

/**
 * 어느 시점의 게임 상태를 복사해 둔 읽기 전용 객체.
 * 화면 표시와 결과 판정은 이 스냅샷만 사용하므로 진행 중인 세션을 건드리지 않는다.
 */
public class GameSnapshot {
    private final UUID sessionId;
    private final String playerName;
    private final Difficulty difficulty;
    private final GameState state;
    private final int completedTurns;
    private final BigDecimal cash;
    private final List<Stock> stocks;
    private final List<Holding> holdings;
    private final List<Trade> trades;
    private final List<BigDecimal> assetHistory;
    private final List<Map<String, BigDecimal>> weightHistory;

    /**
     * 스냅샷을 만든다. 전달받은 목록은 복사해서 보관한다.
     *
     * @param sessionId      세션 ID
     * @param playerName     플레이어 이름
     * @param difficulty     난이도
     * @param state          상태
     * @param completedTurns 마감한 턴 수
     * @param cash           현금
     * @param stocks         종목 복사본
     * @param holdings       보유 현황 복사본
     * @param trades         거래 내역
     * @param assetHistory   턴 마감 자산 이력
     * @param weightHistory  턴 마감 비중 이력
     */
    public GameSnapshot(UUID sessionId, String playerName, Difficulty difficulty, GameState state,
                        int completedTurns, BigDecimal cash, List<Stock> stocks, List<Holding> holdings,
                        List<Trade> trades, List<BigDecimal> assetHistory,
                        List<Map<String, BigDecimal>> weightHistory) {
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.state = state;
        this.completedTurns = completedTurns;
        this.cash = cash;
        this.stocks = Collections.unmodifiableList(new ArrayList<>(stocks));
        this.holdings = Collections.unmodifiableList(new ArrayList<>(holdings));
        this.trades = Collections.unmodifiableList(new ArrayList<>(trades));
        this.assetHistory = Collections.unmodifiableList(new ArrayList<>(assetHistory));
        List<Map<String, BigDecimal>> w = new ArrayList<>();
        for (Map<String, BigDecimal> m : weightHistory) {
            w.add(Collections.unmodifiableMap(new LinkedHashMap<>(m)));
        }
        this.weightHistory = Collections.unmodifiableList(w);
    }

    /**
     * 30턴을 모두 마쳤는지 확인한다.
     *
     * @return 완료했으면 true
     */
    public boolean isCompleted() {
        return completedTurns >= GameSession.TOTAL_TURNS;
    }

    /**
     * ID 로 종목을 찾는다.
     *
     * @param id 종목 ID
     * @return 종목 복사본, 없으면 null
     */
    public Stock findStock(String id) {
        for (Stock s : stocks) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 종목의 보유 수량을 반환한다.
     *
     * @param id 종목 ID
     * @return 보유 수량, 없으면 0
     */
    public int quantityOf(String id) {
        for (Holding h : holdings) {
            if (h.getStockId().equals(id)) {
                return h.getQuantity();
            }
        }
        return 0;
    }

    /** @return 세션 ID */
    public UUID getSessionId() {
        return sessionId;
    }

    /** @return 플레이어 이름 */
    public String getPlayerName() {
        return playerName;
    }

    /** @return 난이도 */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return 상태 */
    public GameState getState() {
        return state;
    }

    /** @return 마감한 턴 수 (0 ~ 30) */
    public int getCompletedTurns() {
        return completedTurns;
    }

    /** @return 현금 */
    public BigDecimal getCash() {
        return cash;
    }

    /** @return 종목 목록 */
    public List<Stock> getStocks() {
        return stocks;
    }

    /** @return 보유 현황 목록 (수량 0 포함) */
    public List<Holding> getHoldings() {
        return holdings;
    }

    /** @return 거래 내역 */
    public List<Trade> getTrades() {
        return trades;
    }

    /** @return 턴 마감 총자산 이력 (1턴부터) */
    public List<BigDecimal> getAssetHistory() {
        return assetHistory;
    }

    /** @return 턴 마감 비중 이력 (1턴부터) */
    public List<Map<String, BigDecimal>> getWeightHistory() {
        return weightHistory;
    }
}
