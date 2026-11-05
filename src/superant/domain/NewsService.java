package superant.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import superant.model.Difficulty;
import superant.model.NewsEvent;
import superant.model.Sector;

/**
 * 30턴 분량의 변동 값과 뉴스를 게임 시작 때 한 번에 추첨하고, 턴마다 발표한다.
 * <p>
 * 각 턴은 75% 확률로 뉴스 6종 중 하나를 같은 확률로 고른다.
 * Hard 에서는 뉴스의 40% 가 한 턴 늦게 반영되며, 30턴 이후로 밀린 효과는 적용되지 않는다.
 */
public class NewsService {
    /** 뉴스가 발생할 확률(%). */
    public static final int NEWS_PERCENT = 75;
    /** Hard 에서 뉴스가 지연될 확률(%). */
    public static final int DELAY_PERCENT = 40;

    private final Difficulty difficulty;
    private final List<TurnDraw> draws = new ArrayList<>();
    private final List<News> published = new ArrayList<>();
    private int currentTurn;

    /**
     * 시드로 30턴 분량을 미리 추첨한다.
     *
     * @param seed       난수 시드
     * @param difficulty 게임 난이도
     */
    public NewsService(long seed, Difficulty difficulty) {
        this.difficulty = difficulty;
        Random random = new Random(seed);
        List<Stock> defs = Market.createStocks();
        NewsEvent[] events = NewsEvent.values();
        for (int turn = 1; turn <= GameSession.TOTAL_TURNS; turn++) {
            BigDecimal market = basisPoints(random, -200, 200);
            Map<Sector, BigDecimal> sectors = new EnumMap<>(Sector.class);
            for (Sector s : Sector.values()) {
                sectors.put(s, basisPoints(random, -150, 150));
            }
            Map<String, BigDecimal> company = new LinkedHashMap<>();
            for (Stock s : defs) {
                int max = s.getRisk().getMaxBasisPoints();
                company.put(s.getId(), basisPoints(random, -max, max));
            }
            News news = null;
            if (random.nextInt(100) < NEWS_PERCENT) {
                NewsEvent event = events[random.nextInt(events.length)];
                Map<String, BigDecimal> effects = event.drawEffects(random);
                int applyTurn = turn;
                if (difficulty == Difficulty.HARD && random.nextInt(100) < DELAY_PERCENT) {
                    applyTurn = turn + 1;
                }
                news = new News(event, turn, applyTurn, effects);
            }
            draws.add(new TurnDraw(turn, market, sectors, company, news));
        }
    }

    private static BigDecimal basisPoints(Random random, int min, int max) {
        return BigDecimal.valueOf(min + random.nextInt(max - min + 1), 4);
    }

    /**
     * 미리 추첨한 턴 값을 반환한다. 다시 불러도 같은 값이다.
     *
     * @param turn 턴 번호 (1 ~ 30)
     * @return 그 턴의 추첨 값
     */
    public TurnDraw drawFor(int turn) {
        return draws.get(turn - 1);
    }

    /**
     * 해당 턴의 뉴스를 발표한다. 뉴스가 없는 턴이면 발표 목록은 바뀌지 않는다.
     *
     * @param turn 진행 중인 턴 번호
     */
    public void publish(int turn) {
        currentTurn = turn;
        News news = drawFor(turn).getNews();
        if (news != null) {
            published.add(news);
        }
    }

    /**
     * 진행 중인 턴에 발표된 뉴스를 반환한다.
     *
     * @return 이번 턴 뉴스, 없으면 null
     */
    public News currentNews() {
        if (published.isEmpty()) {
            return null;
        }
        News last = published.get(published.size() - 1);
        return last.getPublishedTurn() == currentTurn ? last : null;
    }

    /**
     * 지금까지 발표된 뉴스를 최신순으로 반환한다.
     *
     * @return 발표 뉴스 목록 (읽기 전용)
     */
    public List<News> pastNews() {
        List<News> list = new ArrayList<>(published);
        Collections.reverse(list);
        return Collections.unmodifiableList(list);
    }

    /**
     * 해당 턴에 반영할 뉴스 효과를 종목별로 합산한다.
     * 지연 뉴스는 발표 다음 턴에 반영된다.
     *
     * @param turn 마감하는 턴 번호
     * @return 종목 ID 별 효과 합계
     */
    public Map<String, BigDecimal> effectsFor(int turn) {
        Map<String, BigDecimal> sum = new LinkedHashMap<>();
        if (turn < 1 || turn > GameSession.TOTAL_TURNS) {
            return sum;
        }
        for (TurnDraw d : draws) {
            News n = d.getNews();
            if (n != null && n.getApplyTurn() == turn) {
                n.getEffects().forEach((id, rate) -> sum.merge(id, rate, BigDecimal::add));
            }
        }
        return sum;
    }

    /** @return 게임 난이도 */
    public Difficulty getDifficulty() {
        return difficulty;
    }
}
