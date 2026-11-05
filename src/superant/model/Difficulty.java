package superant.model;

import java.math.BigDecimal;

/**
 * 게임 난이도.
 * <p>
 * 난이도에 따라 기업 고유 변동 폭 배수와 뉴스 표시 방식이 달라진다.
 * Easy 는 뉴스 영향 방향 힌트를 제공하고, Hard 는 뉴스의 40% 가 한 턴 늦게 적용된다.
 */
public enum Difficulty {
    /** 쉬움: 변동 폭 x0.75, 뉴스 힌트 제공. */
    EASY("0.75", "힌트 제공"),
    /** 보통: 변동 폭 x1.0. */
    NORMAL("1.00", "기본 정보"),
    /** 어려움: 변동 폭 x1.25, 지연 뉴스 40%. */
    HARD("1.25", "지연 뉴스");

    private final BigDecimal volatilityFactor;
    private final String description;

    Difficulty(String factor, String description) {
        this.volatilityFactor = new BigDecimal(factor);
        this.description = description;
    }

    /**
     * 기업 고유 변동 폭에 곱하는 배수를 반환한다.
     *
     * @return 변동 폭 배수 (Easy 0.75, Normal 1.0, Hard 1.25)
     */
    public BigDecimal getVolatilityFactor() {
        return volatilityFactor;
    }

    /**
     * 시작 화면에 표시할 짧은 설명을 반환한다.
     *
     * @return 난이도 설명
     */
    public String getDescription() {
        return description;
    }
}
