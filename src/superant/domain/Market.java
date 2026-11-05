package superant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import superant.model.RiskLevel;
import superant.model.Sector;

/**
 * 6개 가상 종목으로 이루어진 시장.
 * <p>
 * 턴 마감 때 변동률 = 기업 고유 변동 x 난이도 배수 + 시장 변동 + 업종 변동 + 뉴스 효과 로 계산하고,
 * 결과를 -20% ~ +20% 로 제한한 뒤 각 종목 가격에 적용한다.
 */
public class Market {
    /** 한 턴 변동률의 절댓값 상한(20%). */
    public static final BigDecimal MAX_RATE = new BigDecimal("0.20");

    private final Map<String, Stock> stocks = new LinkedHashMap<>();

    /**
     * 기본 6종목으로 시장을 만든다.
     */
    public Market() {
        for (Stock s : createStocks()) {
            stocks.put(s.getId(), s);
        }
    }

    /**
     * 게임에 쓰는 기본 6종목을 새로 만들어 반환한다.
     *
     * @return S01 ~ S06 종목 목록
     */
    public static List<Stock> createStocks() {
        List<Stock> list = new ArrayList<>();
        list.add(new Stock("S01", "새빛테크", Sector.TECH, RiskLevel.HIGH, "차세대 디스플레이를 만드는 기술 기업", 10000));
        list.add(new Stock("S02", "미래칩", Sector.TECH, RiskLevel.MEDIUM, "반도체를 설계하는 기술 기업", 12000));
        list.add(new Stock("S03", "한울에너지", Sector.ENERGY, RiskLevel.MEDIUM, "석유와 가스를 공급하는 에너지 기업", 8000));
        list.add(new Stock("S04", "푸른전력", Sector.ENERGY, RiskLevel.LOW, "친환경 전력을 생산하는 에너지 기업", 10000));
        list.add(new Stock("S05", "누리운송", Sector.TRANSPORT, RiskLevel.HIGH, "항공과 해운을 운영하는 운송 기업", 5000));
        list.add(new Stock("S06", "바른물류", Sector.TRANSPORT, RiskLevel.LOW, "국내 택배를 담당하는 물류 기업", 6000));
        return list;
    }

    /**
     * ID 로 종목을 찾는다.
     *
     * @param id 종목 ID
     * @return 종목, 없으면 null
     */
    public Stock getStock(String id) {
        return stocks.get(id);
    }

    /**
     * 전체 종목을 ID 순서로 반환한다.
     *
     * @return 읽기 전용 종목 목록
     */
    public List<Stock> getStocks() {
        return Collections.unmodifiableList(new ArrayList<>(stocks.values()));
    }

    /**
     * 한 턴의 가격 변동을 모든 종목에 적용한다.
     *
     * @param draw        이번 턴에 미리 추첨해 둔 시장, 업종, 기업 변동
     * @param newsEffects 이번 턴에 적용되는 뉴스 효과 (종목 ID 별)
     * @param factor      난이도 변동 폭 배수
     */
    public void applyTurn(TurnDraw draw, Map<String, BigDecimal> newsEffects, BigDecimal factor) {
        for (Stock stock : stocks.values()) {
            BigDecimal company = draw.getCompanyRates().getOrDefault(stock.getId(), BigDecimal.ZERO).multiply(factor);
            BigDecimal rate = company
                    .add(draw.getMarketRate())
                    .add(draw.getSectorRates().getOrDefault(stock.getSector(), BigDecimal.ZERO))
                    .add(newsEffects.getOrDefault(stock.getId(), BigDecimal.ZERO))
                    .setScale(4, RoundingMode.HALF_UP);
            stock.updatePrice(clamp(rate));
        }
    }

    static BigDecimal clamp(BigDecimal rate) {
        if (rate.compareTo(MAX_RATE) > 0) {
            return MAX_RATE;
        }
        if (rate.compareTo(MAX_RATE.negate()) < 0) {
            return MAX_RATE.negate();
        }
        return rate;
    }
}
