package superant.model;

/**
 * 종목 위험도. 기업 고유 변동의 최대 폭을 정한다.
 */
public enum RiskLevel {
    /** 낮음: 기본 변동 ±2%. */
    LOW("낮음", 200),
    /** 중간: 기본 변동 ±4%. */
    MEDIUM("중간", 400),
    /** 높음: 기본 변동 ±6%. */
    HIGH("높음", 600);

    private final String label;
    private final int maxBasisPoints;

    RiskLevel(String label, int maxBasisPoints) {
        this.label = label;
        this.maxBasisPoints = maxBasisPoints;
    }

    /**
     * 화면에 표시할 한글 이름을 반환한다.
     *
     * @return "낮음", "중간", "높음"
     */
    public String getLabel() {
        return label;
    }

    /**
     * 기업 고유 변동의 최대 폭을 0.01% 단위로 반환한다.
     *
     * @return 최대 변동 폭 (예: 600 은 ±6%)
     */
    public int getMaxBasisPoints() {
        return maxBasisPoints;
    }
}
