package superant.controller;

import java.math.BigDecimal;
import java.util.List;

import superant.domain.GameSnapshot;
import superant.domain.News;

/**
 * 화면이 한 번에 그릴 데이터를 묶은 읽기 전용 객체.
 * 화면은 이 객체의 version 을 다음 요청에 함께 보내야 한다.
 */
public class ViewData {
    private final GameSnapshot snapshot;
    private final BigDecimal totalAssets;
    private final BigDecimal returnRate;
    private final News currentNews;
    private final List<News> newsFeed;
    private final int currentTurn;
    private final long version;

    /**
     * 화면 데이터를 만든다.
     *
     * @param snapshot    게임 스냅샷
     * @param totalAssets 총자산
     * @param returnRate  수익률(%)
     * @param currentNews 이번 턴 뉴스, 없으면 null
     * @param newsFeed    발표된 뉴스 (최신순)
     * @param currentTurn 진행 중인 턴
     * @param version     상태 버전
     */
    public ViewData(GameSnapshot snapshot, BigDecimal totalAssets, BigDecimal returnRate, News currentNews,
                    List<News> newsFeed, int currentTurn, long version) {
        this.snapshot = snapshot;
        this.totalAssets = totalAssets;
        this.returnRate = returnRate;
        this.currentNews = currentNews;
        this.newsFeed = newsFeed;
        this.currentTurn = currentTurn;
        this.version = version;
    }

    /** @return 게임 스냅샷 */
    public GameSnapshot getSnapshot() {
        return snapshot;
    }

    /** @return 총자산 */
    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    /** @return 수익률(%) */
    public BigDecimal getReturnRate() {
        return returnRate;
    }

    /** @return 이번 턴 뉴스, 없으면 null */
    public News getCurrentNews() {
        return currentNews;
    }

    /** @return 발표된 뉴스 (최신순) */
    public List<News> getNewsFeed() {
        return newsFeed;
    }

    /** @return 진행 중인 턴 */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /** @return 상태 버전 */
    public long getVersion() {
        return version;
    }
}
