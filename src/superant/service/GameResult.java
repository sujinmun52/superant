package superant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import superant.model.Achievement;
import superant.model.AntRank;
import superant.model.Difficulty;
import superant.model.Ending;
import superant.model.InvestorType;

/**
 * 30턴 종료 후 판정한 게임 결과. 생성 후 바뀌지 않는다.
 */
public class GameResult {
    private final UUID sessionId;
    private final String playerName;
    private final Difficulty difficulty;
    private final BigDecimal finalAssets;
    private final BigDecimal returnRate;
    private final BigDecimal realizedPnl;
    private final BigDecimal unrealizedPnl;
    private final BigDecimal maxDrawdown;
    private final String bestStockId;
    private final int tradeCount;
    private final Ending ending;
    private final InvestorType investorType;
    private final List<Achievement> achievements;

    /**
     * 결과를 만든다.
     *
     * @param sessionId     세션 ID
     * @param playerName    플레이어 이름
     * @param difficulty    난이도
     * @param finalAssets   최종 자산
     * @param returnRate    수익률(%)
     * @param realizedPnl   실현 손익 합계
     * @param unrealizedPnl 평가 손익 합계
     * @param maxDrawdown   최대 낙폭(%)
     * @param bestStockId   최고 수익 종목 ID, 거래가 없으면 null
     * @param tradeCount    거래 횟수
     * @param ending        엔딩
     * @param investorType  투자 성향
     * @param achievements  이번 게임에서 얻은 업적
     */
    public GameResult(UUID sessionId, String playerName, Difficulty difficulty, BigDecimal finalAssets,
                      BigDecimal returnRate, BigDecimal realizedPnl, BigDecimal unrealizedPnl,
                      BigDecimal maxDrawdown, String bestStockId, int tradeCount, Ending ending,
                      InvestorType investorType, List<Achievement> achievements) {
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.finalAssets = finalAssets;
        this.returnRate = returnRate;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.maxDrawdown = maxDrawdown;
        this.bestStockId = bestStockId;
        this.tradeCount = tradeCount;
        this.ending = ending;
        this.investorType = investorType;
        this.achievements = Collections.unmodifiableList(new ArrayList<>(achievements));
    }

    /**
     * 결과 배너에 표시할 칭호를 반환한다.
     * 수익 달성 엔딩 중 수익률 50% 이상이면 "슈퍼개미 등극!", 그 밖에는 엔딩 칭호를 쓴다.
     *
     * @return "슈퍼개미 등극!", "여왕개미 등극!", "일개미 생존", "개미굴로 귀환" 중 하나
     */
    public String getTitle() {
        if (ending == Ending.PROFIT && getRank() == AntRank.SUPER) {
            return "슈퍼개미 등극!";
        }
        return ending.getTitle();
    }

    /**
     * 최종 수익률에 해당하는 칭호를 반환한다.
     *
     * @return 칭호
     */
    public AntRank getRank() {
        return AntRank.of(returnRate);
    }

    /** @return 세션 ID */
    public UUID getSessionId() {
        return sessionId;
    }

    /** @return 플레이어 이름 */
    public String getPlayerName() {
        return playerName;
    }

    /** @return 난이도 */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return 최종 자산 */
    public BigDecimal getFinalAssets() {
        return finalAssets;
    }

    /** @return 수익률(%) */
    public BigDecimal getReturnRate() {
        return returnRate;
    }

    /** @return 실현 손익 합계 */
    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    /** @return 평가 손익 합계 */
    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    /** @return 최대 낙폭(%) */
    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    /** @return 최고 수익 종목 ID, 없으면 null */
    public String getBestStockId() {
        return bestStockId;
    }

    /** @return 거래 횟수 */
    public int getTradeCount() {
        return tradeCount;
    }

    /** @return 엔딩 */
    public Ending getEnding() {
        return ending;
    }

    /** @return 투자 성향 */
    public InvestorType getInvestorType() {
        return investorType;
    }

    /** @return 업적 목록 */
    public List<Achievement> getAchievements() {
        return achievements;
    }
}
