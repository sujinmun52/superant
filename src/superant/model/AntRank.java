package superant.model;

import java.math.BigDecimal;

/**
 * 수익률에 따라 게임 중 실시간으로 바뀌는 칭호 사다리.
 * <p>
 * 엔딩 3종(수익 달성, 자산 유지, 재도전)의 판정 기준은 그대로이고,
 * 칭호는 화면 연출과 결과 칭호 표시에만 쓴다.
 * 수익률 20% 이상은 여왕개미, 50% 이상이어야 슈퍼개미다.
 */
public enum AntRank {
    /** 수익률 -10% 미만. */
    COLONY("개미굴 개미", null),
    /** 수익률 -10% 이상 0% 미만. */
    ROOKIE("새내기 개미", "-10"),
    /** 수익률 0% 이상 10% 미만. */
    WORKER("일개미", "0"),
    /** 수익률 10% 이상 20% 미만. */
    SOLDIER("병정개미", "10"),
    /** 수익률 20% 이상 50% 미만 (목표 달성). */
    QUEEN("여왕개미", "20"),
    /** 수익률 50% 이상. */
    SUPER("슈퍼개미", "50");

    private final String label;
    private final BigDecimal minRate;

    AntRank(String label, String minRate) {
        this.label = label;
        this.minRate = minRate == null ? null : new BigDecimal(minRate);
    }

    /**
     * 수익률에 맞는 칭호를 찾는다.
     *
     * @param returnRate 수익률(%)
     * @return 칭호
     */
    public static AntRank of(BigDecimal returnRate) {
        AntRank rank = COLONY;
        for (AntRank r : values()) {
            if (r.minRate != null && returnRate.compareTo(r.minRate) >= 0) {
                rank = r;
            }
        }
        return rank;
    }

    /**
     * 바로 위 칭호를 반환한다.
     *
     * @return 다음 칭호, 최고 칭호이면 null
     */
    public AntRank next() {
        AntRank[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    /**
     * 화면에 표시할 이름을 반환한다.
     *
     * @return 칭호 이름
     */
    public String getLabel() {
        return label;
    }

    /**
     * 이 칭호의 최소 수익률을 반환한다.
     *
     * @return 최소 수익률(%), 하한이 없으면 null
     */
    public BigDecimal getMinRate() {
        return minRate;
    }
}
