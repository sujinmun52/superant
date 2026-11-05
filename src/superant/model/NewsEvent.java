package superant.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * 게임에 등장하는 뉴스 6종과 각 뉴스가 가격에 주는 영향 범위.
 * <p>
 * 영향은 "대상 종목들:최솟값:최댓값" 형식(0.01% 단위)으로 정의한다.
 * 같은 묶음의 종목은 한 번 추첨한 효과를 공유한다.
 */
public enum NewsEvent {
    /** 기술 개발 성공: S01 +2~6%, S02 -1~-3%. */
    TECH_SUCCESS("기술 개발 성공", "S01 새빛테크 / S02 미래칩", "S01:200:600", "S02:-300:-100"),
    /** 기술 제품 결함: S02 -2~-6%, S01 +1~3%. */
    TECH_DEFECT("기술 제품 결함", "S02 미래칩 / S01 새빛테크", "S02:-600:-200", "S01:100:300"),
    /** 에너지 가격 상승: 에너지 +2~5%, 운송 -2~-5%. */
    ENERGY_PRICE_UP("에너지 가격 상승", "에너지 업종 / 운송 업종", "S03,S04:200:500", "S05,S06:-500:-200"),
    /** 친환경 전력 지원: S04 +2~6%. */
    GREEN_POWER("친환경 전력 지원", "S04 푸른전력", "S04:200:600"),
    /** 물류 수요 증가: 운송 +2~5%. */
    LOGISTICS_DEMAND("물류 수요 증가", "운송 업종", "S05,S06:200:500"),
    /** 경기 둔화: 전체 -1~-4%. */
    ECONOMY_SLOWDOWN("경기 둔화", "전체 시장", "S01,S02,S03,S04,S05,S06:-400:-100");

    private final String title;
    private final String targets;
    private final String[] groups;

    NewsEvent(String title, String targets, String... groups) {
        this.title = title;
        this.targets = targets;
        this.groups = groups;
    }

    /**
     * 뉴스 제목을 반환한다.
     *
     * @return 뉴스 제목
     */
    public String getTitle() {
        return title;
    }

    /**
     * 관련 대상(기업 또는 업종) 설명을 반환한다.
     *
     * @return 관련 대상 문구
     */
    public String getTargets() {
        return targets;
    }

    /**
     * 이 뉴스의 종목별 효과를 정해진 범위 안에서 0.01% 단위로 추첨한다.
     *
     * @param random 게임 시드로 만든 난수 생성기
     * @return 종목 ID 별 변동률 (예: 0.0350 은 +3.5%)
     */
    public Map<String, BigDecimal> drawEffects(Random random) {
        Map<String, BigDecimal> effects = new LinkedHashMap<>();
        for (String group : groups) {
            String[] parts = group.split(":");
            int min = Integer.parseInt(parts[1]);
            int max = Integer.parseInt(parts[2]);
            int bp = min + random.nextInt(max - min + 1);
            BigDecimal rate = BigDecimal.valueOf(bp, 4);
            for (String id : parts[0].split(",")) {
                effects.merge(id, rate, BigDecimal::add);
            }
        }
        return effects;
    }
}
