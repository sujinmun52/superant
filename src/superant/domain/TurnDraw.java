package superant.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import superant.model.Sector;

/**
 * 게임 시작 때 시드로 미리 추첨해 둔 한 턴 분량의 변동 값.
 * 같은 턴을 다시 조회해도 다시 추첨하지 않는다.
 */
public class TurnDraw {
    private final int turn;
    private final BigDecimal marketRate;
    private final Map<Sector, BigDecimal> sectorRates;
    private final Map<String, BigDecimal> companyRates;
    private final News news;

    /**
     * 추첨 결과를 묶는다.
     *
     * @param turn         턴 번호 (1 ~ 30)
     * @param marketRate   시장 공통 변동률
     * @param sectorRates  업종별 변동률
     * @param companyRates 종목별 기업 고유 변동률 (난이도 배수 적용 전)
     * @param news         이번 턴에 발표되는 뉴스, 없으면 null
     */
    public TurnDraw(int turn, BigDecimal marketRate, Map<Sector, BigDecimal> sectorRates,
                    Map<String, BigDecimal> companyRates, News news) {
        this.turn = turn;
        this.marketRate = marketRate;
        this.sectorRates = Collections.unmodifiableMap(new LinkedHashMap<>(sectorRates));
        this.companyRates = Collections.unmodifiableMap(new LinkedHashMap<>(companyRates));
        this.news = news;
    }

    /** @return 턴 번호 */
    public int getTurn() {
        return turn;
    }

    /** @return 시장 공통 변동률 */
    public BigDecimal getMarketRate() {
        return marketRate;
    }

    /** @return 업종별 변동률 */
    public Map<Sector, BigDecimal> getSectorRates() {
        return sectorRates;
    }

    /** @return 종목별 기업 고유 변동률 */
    public Map<String, BigDecimal> getCompanyRates() {
        return companyRates;
    }

    /**
     * 이번 턴에 발표되는 뉴스를 반환한다.
     *
     * @return 뉴스, 없으면 null
     */
    public News getNews() {
        return news;
    }
}
