package superant.model;

/**
 * 업종. 같은 업종의 종목은 턴마다 같은 업종 변동(±1.5%)을 공유한다.
 */
public enum Sector {
    /** 기술 업종. */
    TECH("기술"),
    /** 에너지 업종. */
    ENERGY("에너지"),
    /** 운송 업종. */
    TRANSPORT("운송");

    private final String label;

    Sector(String label) {
        this.label = label;
    }

    /**
     * 화면에 표시할 한글 이름을 반환한다.
     *
     * @return 업종 이름
     */
    public String getLabel() {
        return label;
    }
}
