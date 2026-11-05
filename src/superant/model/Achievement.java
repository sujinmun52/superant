package superant.model;

/**
 * 게임 종료 시 판정하는 업적. 여러 게임에서 중복 획득할 수 있다.
 */
public enum Achievement {
    /** 거래 1회 이상이고 모든 마감 자산이 초기 자금 이상. */
    NO_LOSS("손실 없는 완주"),
    /** 첫 턴에 매수한 종목을 한 번도 팔지 않고 끝까지 보유. */
    LONG_HOLD("장기 보유"),
    /** 3종목 이상 매수. */
    DIVERSIFIED("분산 투자 경험");

    private final String label;

    Achievement(String label) {
        this.label = label;
    }

    /**
     * 화면에 표시할 한글 이름을 반환한다.
     *
     * @return 업적 이름
     */
    public String getLabel() {
        return label;
    }
}
