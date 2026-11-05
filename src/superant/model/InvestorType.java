package superant.model;

/**
 * 30회 턴 마감 시점의 자산 비중 평균으로 판정하는 투자 성향.
 * <p>
 * 판정 순서: 집중형, 안정형, 공격형, 분산형, 균형형.
 */
public enum InvestorType {
    /** 최대 단일 종목 비중 평균 70% 이상. */
    CONCENTRATED("집중형"),
    /** 현금 비중 평균 50% 이상. */
    STABLE("안정형"),
    /** 고위험 종목 비중 합계 평균 50% 이상. */
    AGGRESSIVE("공격형"),
    /** 최대 종목 비중 평균 40% 이하이고 3종목 이상 매수. */
    DIVERSIFIED("분산형"),
    /** 그 밖의 경우. */
    BALANCED("균형형");

    private final String label;

    InvestorType(String label) {
        this.label = label;
    }

    /**
     * 화면에 표시할 한글 이름을 반환한다.
     *
     * @return 성향 이름
     */
    public String getLabel() {
        return label;
    }
}
