package superant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import superant.model.RiskLevel;
import superant.model.Sector;

/**
 * 가상 종목. 턴 마감마다 가격이 하나씩 이력에 추가된다.
 * <p>
 * 이력의 첫 값은 시작 가격(0턴)이고, 이후 값은 각 턴 마감 가격이다.
 * 가격은 원 단위 정수이며 항상 1원 이상이다.
 */
public class Stock {
    private final String id;
    private final String name;
    private final Sector sector;
    private final RiskLevel risk;
    private final String description;
    private final List<Long> priceHistory = new ArrayList<>();

    /**
     * 종목을 만든다.
     *
     * @param id           종목 ID (예: "S01")
     * @param name         종목 이름
     * @param sector       업종
     * @param risk         위험도
     * @param description  한 줄 설명
     * @param initialPrice 시작 가격 (1원 이상)
     */
    public Stock(String id, String name, Sector sector, RiskLevel risk, String description, long initialPrice) {
        if (initialPrice < 1) {
            throw new IllegalArgumentException("가격은 1원 이상이어야 합니다.");
        }
        this.id = id;
        this.name = name;
        this.sector = sector;
        this.risk = risk;
        this.description = description;
        this.priceHistory.add(initialPrice);
    }

    private Stock(Stock other) {
        this.id = other.id;
        this.name = other.name;
        this.sector = other.sector;
        this.risk = other.risk;
        this.description = other.description;
        this.priceHistory.addAll(other.priceHistory);
    }

    /**
     * 현재(가장 최근) 가격을 반환한다.
     *
     * @return 현재 가격(원)
     */
    public long currentPrice() {
        return priceHistory.get(priceHistory.size() - 1);
    }

    /**
     * 직전 가격 대비 등락률을 퍼센트로 반환한다. 이력이 하나뿐이면 0 이다.
     *
     * @return 등락률(%), 소수 둘째 자리
     */
    public BigDecimal changeRate() {
        if (priceHistory.size() < 2) {
            return BigDecimal.ZERO.setScale(2);
        }
        long prev = priceHistory.get(priceHistory.size() - 2);
        long cur = currentPrice();
        return BigDecimal.valueOf(cur - prev).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prev), 2, RoundingMode.HALF_UP);
    }

    /**
     * 변동률을 적용해 새 가격을 이력에 추가한다.
     * 새 가격 = 이전 가격 x (1 + 변동률), 원 단위 반올림, 최소 1원.
     *
     * @param rate 변동률 (예: 0.035 는 +3.5%)
     * @return 새 가격(원)
     */
    public long updatePrice(BigDecimal rate) {
        BigDecimal next = BigDecimal.valueOf(currentPrice())
                .multiply(BigDecimal.ONE.add(rate))
                .setScale(0, RoundingMode.HALF_UP);
        long price = Math.max(1L, next.longValueExact());
        priceHistory.add(price);
        return price;
    }

    /**
     * 가격 이력을 읽기 전용으로 반환한다.
     *
     * @return 0턴부터의 가격 목록
     */
    public List<Long> getPriceHistory() {
        return Collections.unmodifiableList(priceHistory);
    }

    /**
     * 화면 스냅샷용 복사본을 만든다.
     *
     * @return 같은 값을 가진 새 Stock
     */
    public Stock copy() {
        return new Stock(this);
    }

    /** @return 종목 ID */
    public String getId() {
        return id;
    }

    /** @return 종목 이름 */
    public String getName() {
        return name;
    }

    /** @return 업종 */
    public Sector getSector() {
        return sector;
    }

    /** @return 위험도 */
    public RiskLevel getRisk() {
        return risk;
    }

    /** @return 한 줄 설명 */
    public String getDescription() {
        return description;
    }
}
