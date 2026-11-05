package superant.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import superant.model.Difficulty;
import superant.model.NewsEvent;

/**
 * 발표된 뉴스 한 건.
 * <p>
 * 정확한 효과 수치는 화면에 공개하지 않는다. Easy 에서만 영향 방향 힌트를 보여 준다.
 * Hard 에서는 효과가 한 턴 늦게 적용될 수 있다.
 */
public class News {
    private final NewsEvent event;
    private final int publishedTurn;
    private final int applyTurn;
    private final Map<String, BigDecimal> effects;

    /**
     * 뉴스를 만든다.
     *
     * @param event         뉴스 종류
     * @param publishedTurn 발표 턴
     * @param applyTurn     효과가 가격에 반영되는 턴
     * @param effects       종목 ID 별 효과 변동률
     */
    public News(NewsEvent event, int publishedTurn, int applyTurn, Map<String, BigDecimal> effects) {
        this.event = event;
        this.publishedTurn = publishedTurn;
        this.applyTurn = applyTurn;
        this.effects = Collections.unmodifiableMap(new LinkedHashMap<>(effects));
    }

    /**
     * 효과가 발표 턴보다 늦게 반영되는지 확인한다.
     *
     * @return 지연 뉴스이면 true
     */
    public boolean isDelayed() {
        return applyTurn != publishedTurn;
    }

    /**
     * 난이도에 맞는 안내 문구를 만든다.
     * Easy 는 종목별 상승/하락 방향, Normal 은 관련 대상, Hard 는 관련 대상과 지연 가능성만 알려 준다.
     *
     * @param difficulty 게임 난이도
     * @return 안내 문구
     */
    public String hint(Difficulty difficulty) {
        String base = "관련: " + event.getTargets();
        if (difficulty == Difficulty.EASY) {
            List<String> up = new ArrayList<>();
            List<String> down = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : effects.entrySet()) {
                if (e.getValue().signum() > 0) {
                    up.add(e.getKey());
                } else if (e.getValue().signum() < 0) {
                    down.add(e.getKey());
                }
            }
            StringBuilder sb = new StringBuilder(base).append("  |  힌트:");
            if (!up.isEmpty()) {
                sb.append(" ▲ ").append(String.join(", ", up));
            }
            if (!down.isEmpty()) {
                sb.append(" ▼ ").append(String.join(", ", down));
            }
            return sb.toString();
        }
        if (difficulty == Difficulty.HARD) {
            return base + "  |  영향이 늦게 나타날 수 있음";
        }
        return base;
    }

    /** @return 뉴스 종류 */
    public NewsEvent getEvent() {
        return event;
    }

    /** @return 발표 턴 */
    public int getPublishedTurn() {
        return publishedTurn;
    }

    /** @return 효과 반영 턴 */
    public int getApplyTurn() {
        return applyTurn;
    }

    /** @return 종목 ID 별 효과 변동률 (읽기 전용) */
    public Map<String, BigDecimal> getEffects() {
        return effects;
    }
}
