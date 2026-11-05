package superant;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import superant.controller.GameController;
import superant.controller.SaveResult;
import superant.controller.TradeResult;
import superant.controller.TurnResult;
import superant.controller.ViewData;
import superant.domain.Holding;
import superant.domain.Market;
import superant.domain.NewsService;
import superant.domain.Portfolio;
import superant.domain.Stock;
import superant.domain.TurnDraw;
import superant.model.AntRank;
import superant.model.Difficulty;
import superant.model.Ending;
import superant.model.OrderSide;
import superant.persistence.RecordEntry;
import superant.persistence.RecordRepository;
import superant.service.GameResult;

/**
 * 외부 라이브러리 없이 실행하는 규칙 테스트. 실패하면 종료 코드 1 로 끝난다.
 */
public final class RuleTest {
    private static int passed;
    private static int failed;

    private RuleTest() {
    }

    /**
     * 모든 테스트를 실행한다.
     *
     * @param args 사용하지 않음
     * @throws Exception 예기치 못한 오류
     */
    public static void main(String[] args) throws Exception {
        priceRounding();
        clampAt20Percent();
        averageCostAllocation();
        newsDrawIsDeterministic();
        nameValidation();
        tradeRejections();
        duplicateRequestIgnored();
        fullGameAndRecords();
        rankLadder();
        System.out.printf("%nPASSED %d, FAILED %d%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("  ok   " + name);
        } else {
            failed++;
            System.out.println("  FAIL " + name);
        }
    }

    private static void rankLadder() {
        check("칭호 -10.01% 개미굴", AntRank.of(new BigDecimal("-10.01")) == AntRank.COLONY);
        check("칭호 -10% 새내기", AntRank.of(new BigDecimal("-10")) == AntRank.ROOKIE);
        check("칭호 0% 일개미", AntRank.of(BigDecimal.ZERO) == AntRank.WORKER);
        check("칭호 19.99% 병정개미", AntRank.of(new BigDecimal("19.99")) == AntRank.SOLDIER);
        check("칭호 20% 여왕개미", AntRank.of(new BigDecimal("20")) == AntRank.QUEEN);
        check("칭호 50% 슈퍼개미", AntRank.of(new BigDecimal("50")) == AntRank.SUPER);
        GameResult queen = result(new BigDecimal("30.82"), Ending.PROFIT);
        GameResult sup = result(new BigDecimal("50.00"), Ending.PROFIT);
        check("수익 달성 30% 결과 칭호 여왕개미", "여왕개미 등극!".equals(queen.getTitle()));
        check("수익 달성 50% 결과 칭호 슈퍼개미", "슈퍼개미 등극!".equals(sup.getTitle()));
    }

    private static GameResult result(BigDecimal rate, Ending ending) {
        return new GameResult(UUID.randomUUID(), "t", Difficulty.EASY, BigDecimal.ZERO, rate, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null, 0, ending, superant.model.InvestorType.BALANCED,
                Collections.emptyList());
    }

    private static void priceRounding() {
        Stock s = new Stock("T", "t", null, null, "", 10000);
        check("가격 반올림 10000 x 1.03455 = 10346", s.updatePrice(new BigDecimal("0.03455")) == 10346);
        Stock one = new Stock("T", "t", null, null, "", 1);
        check("최소 가격 1원", one.updatePrice(new BigDecimal("-0.20")) == 1);
    }

    private static void clampAt20Percent() {
        Market m = new Market();
        TurnDraw d = new TurnDraw(1, new BigDecimal("0.15"), Collections.emptyMap(), Collections.emptyMap(), null);
        m.applyTurn(d, Map.of("S01", new BigDecimal("0.10")), BigDecimal.ONE);
        check("변동률 +20% 상한", m.getStock("S01").currentPrice() == 12000);
        check("다른 종목은 +15%", m.getStock("S04").currentPrice() == 11500);
    }

    private static void averageCostAllocation() {
        Market m = new Market();
        Portfolio p = new Portfolio(BigDecimal.valueOf(1_000_000));
        Stock s = m.getStock("S05");
        p.buy(s, 3, 1);
        s.updatePrice(new BigDecimal("0.10"));
        p.buy(s, 1, 2);
        Holding h = p.getHolding("S05");
        check("누적 원가 15000 + 5500", h.getCostBasis().compareTo(BigDecimal.valueOf(20500)) == 0);
        BigDecimal pnl = p.sell(s, 1, 2).getRealizedPnl();
        check("평균 원가 배분 실현 손익 375", pnl.compareTo(new BigDecimal("375")) == 0);
        p.sell(s, 3, 2);
        check("전량 매도 후 원가 0", h.getCostBasis().signum() == 0 && h.getQuantity() == 0);
        check("현금 보존", p.getCash().compareTo(BigDecimal.valueOf(1_001_500)) == 0);
    }

    private static void newsDrawIsDeterministic() {
        NewsService a = new NewsService(42L, Difficulty.HARD);
        NewsService b = new NewsService(42L, Difficulty.HARD);
        boolean same = true;
        int newsCount = 0;
        for (int t = 1; t <= 30; t++) {
            same &= a.drawFor(t).getMarketRate().equals(b.drawFor(t).getMarketRate());
            same &= a.drawFor(t).getCompanyRates().equals(b.drawFor(t).getCompanyRates());
            if (a.drawFor(t).getNews() != null) {
                newsCount++;
            }
            same &= a.drawFor(t) == a.drawFor(t);
        }
        check("같은 시드는 같은 추첨", same);
        check("뉴스 발생 수 합리적(10~30)", newsCount >= 10 && newsCount <= 30);
    }

    private static void nameValidation() {
        GameController c = new GameController(null, new Random(1));
        check("빈 이름 거절", !c.startGame("  ", Difficulty.EASY).isSuccess());
        check("13자 거절", !c.startGame("abcdefghijklm", Difficulty.EASY).isSuccess());
        check("특수문자 거절", !c.startGame("ant!", Difficulty.EASY).isSuccess());
        check("한글 이름 허용", c.startGame("슈퍼개미_1", Difficulty.EASY).isSuccess());
    }

    private static void tradeRejections() {
        GameController c = new GameController(null, new Random(1));
        ViewData v = c.startGame("tester", Difficulty.NORMAL).getView();
        long ver = v.getVersion();
        check("현금 부족 거절", "NO_CASH".equals(c.placeOrder("S01", OrderSide.BUY, 99_999, ver, UUID.randomUUID()).getErrorCode()));
        check("보유 부족 거절", "NO_HOLDING".equals(c.placeOrder("S01", OrderSide.SELL, 1, ver, UUID.randomUUID()).getErrorCode()));
        check("수량 0 거절", "INVALID_QTY".equals(c.placeOrder("S01", OrderSide.BUY, 0, ver, UUID.randomUUID()).getErrorCode()));
        check("거절 후 버전 유지", c.getView().getVersion() == ver);
        check("오래된 버전 거절", "STALE_VIEW".equals(c.advanceTurn(ver + 5, UUID.randomUUID()).getErrorCode()));
    }

    private static void duplicateRequestIgnored() {
        GameController c = new GameController(null, new Random(1));
        long ver = c.startGame("tester", Difficulty.NORMAL).getView().getVersion();
        UUID id = UUID.randomUUID();
        TradeResult r1 = c.placeOrder("S04", OrderSide.BUY, 10, ver, id);
        TradeResult r2 = c.placeOrder("S04", OrderSide.BUY, 10, ver, id);
        check("같은 requestId 는 한 번만 체결", r1 == r2 && c.getView().getSnapshot().quantityOf("S04") == 10);
    }

    private static void fullGameAndRecords() throws Exception {
        Path dir = Files.createTempDirectory("superant-test");
        Path file = dir.resolve("stock-game").resolve("records.json");
        GameController c = new GameController(new RecordRepository(file), new Random(7));
        ViewData v = c.startGame("개미", Difficulty.HARD).getView();
        v = c.placeOrder("S01", OrderSide.BUY, 20, v.getVersion(), UUID.randomUUID()).getView();
        v = c.placeOrder("S03", OrderSide.BUY, 20, v.getVersion(), UUID.randomUUID()).getView();
        v = c.placeOrder("S06", OrderSide.BUY, 20, v.getVersion(), UUID.randomUUID()).getView();
        check("결과는 종료 전 null", c.evaluateResult() == null);
        TurnResult tr = null;
        for (int i = 0; i < 30; i++) {
            tr = c.advanceTurn(v.getVersion(), UUID.randomUUID());
            v = tr.getView();
        }
        check("30턴 후 종료", tr.isEnded() && v.getSnapshot().isCompleted());
        check("종료 후 턴 진행 거절", "GAME_ENDED".equals(c.advanceTurn(v.getVersion(), UUID.randomUUID()).getErrorCode()));
        check("종료 후 거래 거절", "GAME_ENDED".equals(c.placeOrder("S01", OrderSide.BUY, 1, v.getVersion(), UUID.randomUUID()).getErrorCode()));
        check("자산 이력 30개", v.getSnapshot().getAssetHistory().size() == 30);
        check("가격 이력 31개", v.getSnapshot().getStocks().get(0).getPriceHistory().size() == 31);

        GameResult r = c.evaluateResult();
        check("결과 판정 재현성", r == c.evaluateResult());
        check("분산 투자 업적", r.getAchievements().contains(superant.model.Achievement.DIVERSIFIED));
        check("장기 보유 업적", r.getAchievements().contains(superant.model.Achievement.LONG_HOLD));
        System.out.printf("       결과: %s / 최종 %s원 / 수익률 %s%% / 성향 %s / MDD %s%% / 최고 %s%n",
                r.getTitle(), r.getFinalAssets(), r.getReturnRate(), r.getInvestorType().getLabel(),
                r.getMaxDrawdown(), r.getBestStockId());

        SaveResult s1 = c.saveResult();
        SaveResult s2 = c.saveResult();
        check("저장 성공", s1.isSuccess() && !s1.isAlreadySaved() && Files.exists(file));
        check("중복 저장 방지", s2.isSuccess() && s2.isAlreadySaved());
        RecordEntry e = new RecordRepository(file).readRecords("개미", Difficulty.HARD);
        check("기록 다시 읽기", e != null && e.getPlayCount() == 1
                && e.getBestAssets().compareTo(r.getFinalAssets()) == 0
                && e.getEndings().contains(r.getEnding()));
        check("다른 난이도 기록 없음", new RecordRepository(file).readRecords("개미", Difficulty.EASY) == null);
        check("임시 파일 남지 않음", Files.list(file.getParent()).count() == 1);
    }
}
