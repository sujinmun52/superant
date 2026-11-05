package superant.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import superant.domain.GameSession;
import superant.domain.Holding;
import superant.domain.Portfolio;
import superant.domain.Stock;
import superant.domain.Trade;
import superant.model.Difficulty;
import superant.model.OrderSide;
import superant.persistence.RecordEntry;
import superant.persistence.RecordStore;
import superant.service.GameResult;
import superant.service.ResultEvaluator;

/**
 * 화면 요청을 받아 규칙을 검사하고 도메인 객체에 위임하는 컨트롤러.
 * <p>
 * 거래와 턴 마감은 화면이 가진 version 이 현재 version 과 같을 때만 처리한다.
 * 같은 requestId 로 다시 요청하면 처음 결과를 그대로 돌려줘 중복 처리를 막는다.
 */
public class GameController {
    /** 이름 규칙: 1 ~ 12자, 한글/영문/숫자/밑줄. */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9_]{1,12}$");
    private static final int MAX_HANDLED = 200;

    private GameSession session;
    private final ResultEvaluator evaluator = new ResultEvaluator();
    private final RecordStore store;
    private final Random seedSource;
    private GameResult lastResult;
    private final Map<UUID, ActionResult> handled = new LinkedHashMap<UUID, ActionResult>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, ActionResult> eldest) {
            return size() > MAX_HANDLED;
        }
    };

    /**
     * 기록 저장소를 받아 컨트롤러를 만든다.
     *
     * @param store 기록 저장소
     */
    public GameController(RecordStore store) {
        this(store, new Random());
    }

    /**
     * 시드 생성기를 지정해 컨트롤러를 만든다. 테스트에서 같은 게임을 재현할 때 쓴다.
     *
     * @param store      기록 저장소
     * @param seedSource 게임 시드를 만들 난수 생성기
     */
    public GameController(RecordStore store, Random seedSource) {
        this.store = store;
        this.seedSource = seedSource;
    }

    /**
     * 새 게임을 시작한다 (FN-01). 진행 중인 게임이 있으면 버린다.
     *
     * @param name       플레이어 이름
     * @param difficulty 난이도
     * @return 시작 결과
     */
    public synchronized StartResult startGame(String name, Difficulty difficulty) {
        String trimmed = name == null ? "" : name.trim();
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            return StartResult.fail("INVALID_NAME", "이름은 1~12자의 한글, 영문, 숫자, 밑줄(_)만 쓸 수 있습니다.");
        }
        if (difficulty == null) {
            return StartResult.fail("NO_DIFFICULTY", "난이도를 선택하세요.");
        }
        session = new GameSession(trimmed, difficulty, seedSource.nextLong());
        lastResult = null;
        handled.clear();
        return StartResult.ok(getView());
    }

    /**
     * 매수 또는 매도 주문을 처리한다 (FN-02).
     *
     * @param stockId         종목 ID
     * @param side            매수 또는 매도
     * @param qty             수량
     * @param expectedVersion 화면이 가진 상태 버전
     * @param requestId       요청 ID (중복 방지)
     * @return 거래 결과
     */
    public synchronized TradeResult placeOrder(String stockId, OrderSide side, int qty, long expectedVersion,
                                               UUID requestId) {
        ActionResult prev = handled.get(requestId);
        if (prev instanceof TradeResult) {
            return (TradeResult) prev;
        }
        TradeResult result = doOrder(stockId, side, qty, expectedVersion);
        if (requestId != null) {
            handled.put(requestId, result);
        }
        return result;
    }

    private TradeResult doOrder(String stockId, OrderSide side, int qty, long expectedVersion) {
        if (session == null) {
            return TradeResult.fail("NO_GAME", "진행 중인 게임이 없습니다.");
        }
        if (session.isEnded()) {
            return TradeResult.fail("GAME_ENDED", "게임이 끝나 거래할 수 없습니다.");
        }
        if (expectedVersion != session.getVersion()) {
            return TradeResult.fail("STALE_VIEW", "화면 정보가 오래되었습니다. 최신 정보로 다시 시도하세요.");
        }
        Stock stock = session.getMarket().getStock(stockId);
        if (stock == null) {
            return TradeResult.fail("NO_STOCK", "종목을 선택하세요.");
        }
        if (side == null) {
            return TradeResult.fail("NO_SIDE", "매수 또는 매도를 선택하세요.");
        }
        if (qty < 1 || qty > Portfolio.MAX_QUANTITY) {
            return TradeResult.fail("INVALID_QTY", "수량은 1 ~ 99,999 사이의 정수여야 합니다.");
        }
        Portfolio pf = session.getPortfolio();
        BigDecimal amount = BigDecimal.valueOf(stock.currentPrice()).multiply(BigDecimal.valueOf(qty));
        if (side == OrderSide.BUY && amount.compareTo(pf.getCash()) > 0) {
            return TradeResult.fail("NO_CASH", "현금이 부족합니다.");
        }
        Holding h = pf.getHolding(stockId);
        if (side == OrderSide.SELL && (h == null || h.getQuantity() < qty)) {
            return TradeResult.fail("NO_HOLDING", "보유 수량이 부족합니다.");
        }
        int turn = session.getCurrentTurn();
        Trade trade = side == OrderSide.BUY ? pf.buy(stock, qty, turn) : pf.sell(stock, qty, turn);
        session.nextVersion();
        String msg = String.format("%s %s %,d주 체결 (%,d원)", stock.getName(), side.getLabel(), qty,
                trade.getAmount());
        return TradeResult.ok(msg, trade.getPrice(), trade.getQuantity(), trade.getRealizedPnl(), getView());
    }

    /**
     * 진행 중인 턴을 마감하고 다음 턴으로 넘어간다 (FN-03).
     * 30턴을 마치면 결과를 판정해 둔다.
     *
     * @param expectedVersion 화면이 가진 상태 버전
     * @param requestId       요청 ID (중복 방지)
     * @return 턴 결과
     */
    public synchronized TurnResult advanceTurn(long expectedVersion, UUID requestId) {
        ActionResult prev = handled.get(requestId);
        if (prev instanceof TurnResult) {
            return (TurnResult) prev;
        }
        TurnResult result;
        if (session == null) {
            result = TurnResult.fail("NO_GAME", "진행 중인 게임이 없습니다.");
        } else if (session.isEnded()) {
            result = TurnResult.fail("GAME_ENDED", "이미 30턴을 마쳤습니다.");
        } else if (expectedVersion != session.getVersion()) {
            result = TurnResult.fail("STALE_VIEW", "화면 정보가 오래되었습니다. 최신 정보로 다시 시도하세요.");
        } else {
            session.closeTurn();
            if (session.isEnded()) {
                lastResult = evaluator.evaluate(session.snapshot());
            }
            result = TurnResult.ok(session.getCompletedTurns(), session.isEnded(), getView());
        }
        if (requestId != null) {
            handled.put(requestId, result);
        }
        return result;
    }

    /**
     * 현재 화면 데이터를 만든다.
     *
     * @return 화면 데이터, 게임이 없으면 null
     */
    public synchronized ViewData getView() {
        if (session == null) {
            return null;
        }
        BigDecimal total = session.getPortfolio().totalAssets(session.getMarket());
        BigDecimal rate = total.subtract(GameSession.INITIAL_CASH).multiply(BigDecimal.valueOf(100))
                .divide(GameSession.INITIAL_CASH, 2, RoundingMode.HALF_UP);
        return new ViewData(session.snapshot(), total, rate, session.getNewsService().currentNews(),
                session.getNewsService().pastNews(), session.getCurrentTurn(), session.getVersion());
    }

    /**
     * 종료된 게임의 결과를 반환한다 (FN-04).
     *
     * @return 게임 결과, 게임이 끝나지 않았으면 null
     */
    public synchronized GameResult evaluateResult() {
        if (session == null || !session.isEnded()) {
            return null;
        }
        if (lastResult == null) {
            lastResult = evaluator.evaluate(session.snapshot());
        }
        return lastResult;
    }

    /**
     * 마지막 게임 결과를 기록에 저장한다 (FN-05).
     *
     * @return 저장 결과
     */
    public synchronized SaveResult saveResult() {
        GameResult result = evaluateResult();
        if (result == null) {
            return SaveResult.fail("NOT_ENDED", "30턴을 마친 뒤에 저장할 수 있습니다.");
        }
        try {
            return store.saveResult(result);
        } catch (IOException e) {
            return SaveResult.fail("IO_ERROR", "저장하지 못했습니다: " + e.getMessage());
        }
    }

    /**
     * 이름과 난이도로 누적 기록을 조회한다 (FN-05).
     *
     * @param name       플레이어 이름
     * @param difficulty 난이도
     * @return 누적 기록, 없으면 null
     * @throws UncheckedIOException 기록 파일을 읽을 수 없을 때
     */
    public RecordEntry readRecords(String name, Difficulty difficulty) {
        try {
            return store.readRecords(name == null ? "" : name.trim(), difficulty);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
