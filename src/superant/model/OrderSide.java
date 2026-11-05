package superant.model;

/**
 * 주문 종류.
 */
public enum OrderSide {
    /** 매수. */
    BUY("매수"),
    /** 매도. */
    SELL("매도");

    private final String label;

    OrderSide(String label) {
        this.label = label;
    }

    /**
     * 화면에 표시할 한글 이름을 반환한다.
     *
     * @return "매수" 또는 "매도"
     */
    public String getLabel() {
        return label;
    }
}
