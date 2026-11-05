package superant.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import superant.model.Difficulty;
import superant.model.GameState;

/**
 * 게임 한 판. 시장, 포트폴리오, 뉴스 서비스를 소유하고 턴 진행과 상태를 관리한다.
 * <p>
 * 상태가 바뀔 때마다 version 이 1 씩 오르며, 컨트롤러는 화면이 가진 version 과 비교해
 * 오래된 화면에서 온 요청을 거절한다.
 */
public class GameSession {
    /** 한 게임의 총 턴 수. */
    public static final int TOTAL_TURNS = 30;
    /** 초기 현금(원). */
    public static final BigDecimal INITIAL_CASH = BigDecimal.valueOf(1_000_000);
    /** 목표 자산(원). */
    public static final BigDecimal GOAL_ASSETS = BigDecimal.valueOf(1_200_000);

    private final UUID sessionId;
    private final String playerName;
    private final Difficulty difficulty;
    private GameState state;
    private int completedTurns;
    private long version;
    private final Portfolio portfolio;
    private final Market market;
    private final NewsService newsService;
    private final List<BigDecimal> assetHistory = new ArrayList<>();
    private final List<Map<String, BigDecimal>> weightHistory = new ArrayList<>();

    /**
     * 새 게임을 시작한다. 1턴 뉴스를 바로 발표한다.
     *
     * @param playerName 플레이어 이름
     * @param difficulty 난이도
     * @param seed       변동 값을 미리 추첨할 시드
     */
    public GameSession(String playerName, Difficulty difficulty, long seed) {
        this.sessionId = UUID.randomUUID();
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.state = GameState.READY;
        this.portfolio = new Portfolio(INITIAL_CASH);
        this.market = new Market();
        this.newsService = new NewsService(seed, difficulty);
        newsService.publish(1);
    }

    /**
     * 진행 중인 턴을 마감한다.
     * 가격을 갱신하고 자산과 비중을 기록한 뒤, 30턴이면 ENDED, 아니면 다음 턴 뉴스를 발표한다.
     *
     * @throws IllegalStateException READY 상태가 아닐 때
     */
    public void closeTurn() {
        if (state != GameState.READY) {
            throw new IllegalStateException("턴을 마감할 수 없는 상태입니다: " + state);
        }
        state = GameState.UPDATING;
        try {
            int turn = completedTurns + 1;
            market.applyTurn(newsService.drawFor(turn), newsService.effectsFor(turn), difficulty.getVolatilityFactor());
            assetHistory.add(portfolio.totalAssets(market));
            weightHistory.add(portfolio.weights(market));
            completedTurns = turn;
            if (turn >= TOTAL_TURNS) {
                state = GameState.ENDED;
            } else {
                newsService.publish(turn + 1);
                state = GameState.READY;
            }
        } finally {
            if (state == GameState.UPDATING) {
                state = GameState.READY;
            }
        }
        nextVersion();
    }

    /**
     * 상태 버전을 1 올린다.
     *
     * @return 새 버전
     */
    public long nextVersion() {
        return ++version;
    }

    /**
     * 진행 중인 턴 번호를 반환한다. 종료 후에는 30 이다.
     *
     * @return 현재 턴 (1 ~ 30)
     */
    public int getCurrentTurn() {
        return Math.min(completedTurns + 1, TOTAL_TURNS);
    }

    /**
     * 30턴을 모두 마쳤는지 확인한다.
     *
     * @return 종료했으면 true
     */
    public boolean isEnded() {
        return state == GameState.ENDED;
    }

    /**
     * 현재 상태를 복사한 스냅샷을 만든다.
     *
     * @return 읽기 전용 스냅샷
     */
    public GameSnapshot snapshot() {
        List<Stock> stocks = new ArrayList<>();
        for (Stock s : market.getStocks()) {
            stocks.add(s.copy());
        }
        List<Holding> holdings = new ArrayList<>();
        for (Holding h : portfolio.getHoldings()) {
            holdings.add(h.copy());
        }
        return new GameSnapshot(sessionId, playerName, difficulty, state, completedTurns, portfolio.getCash(),
                stocks, holdings, portfolio.getTrades(), assetHistory, weightHistory);
    }

    /** @return 포트폴리오 */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /** @return 시장 */
    public Market getMarket() {
        return market;
    }

    /** @return 뉴스 서비스 */
    public NewsService getNewsService() {
        return newsService;
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

    /** @return 마감한 턴 수 */
    public int getCompletedTurns() {
        return completedTurns;
    }

    /** @return 상태 버전 */
    public long getVersion() {
        return version;
    }
}
