package superant.persistence;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import superant.model.Achievement;
import superant.model.Difficulty;
import superant.model.Ending;
import superant.service.GameResult;

/**
 * 이름과 난이도 한 쌍의 누적 기록.
 * <p>
 * 최고 자산은 더 클 때만 바꾸며 같으면 기존 기록을 유지한다.
 * 이미 반영한 세션 ID 는 다시 반영하지 않는다.
 */
public class RecordEntry {
    private final String playerName;
    private final Difficulty difficulty;
    private BigDecimal bestAssets;
    private BigDecimal bestReturnRate;
    private int playCount;
    private final Set<Ending> endings = EnumSet.noneOf(Ending.class);
    private final Map<Achievement, Integer> achievements = new EnumMap<>(Achievement.class);
    private final Set<UUID> savedSessions = new LinkedHashSet<>();

    /**
     * 빈 기록을 만든다.
     *
     * @param playerName 플레이어 이름
     * @param difficulty 난이도
     */
    public RecordEntry(String playerName, Difficulty difficulty) {
        this.playerName = playerName;
        this.difficulty = difficulty;
    }

    /**
     * 게임 결과를 이 기록에 합친다.
     *
     * @param result 게임 결과
     * @return 새로 반영했으면 true, 이미 저장한 세션이면 false
     */
    public boolean merge(GameResult result) {
        if (savedSessions.contains(result.getSessionId())) {
            return false;
        }
        savedSessions.add(result.getSessionId());
        playCount++;
        endings.add(result.getEnding());
        for (Achievement a : result.getAchievements()) {
            achievements.merge(a, 1, Integer::sum);
        }
        if (bestAssets == null || result.getFinalAssets().compareTo(bestAssets) > 0) {
            bestAssets = result.getFinalAssets();
            bestReturnRate = result.getReturnRate();
        }
        return true;
    }

    /**
     * 같은 이름과 난이도의 기록인지 확인한다.
     *
     * @param name       플레이어 이름
     * @param difficulty 난이도
     * @return 같으면 true
     */
    public boolean matches(String name, Difficulty difficulty) {
        return playerName.equals(name) && this.difficulty == difficulty;
    }

    void restore(BigDecimal bestAssets, BigDecimal bestReturnRate, int playCount) {
        this.bestAssets = bestAssets;
        this.bestReturnRate = bestReturnRate;
        this.playCount = playCount;
    }

    /** @return 플레이어 이름 */
    public String getPlayerName() {
        return playerName;
    }

    /** @return 난이도 */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return 최고 자산, 기록이 없으면 null */
    public BigDecimal getBestAssets() {
        return bestAssets;
    }

    /** @return 최고 자산을 낸 게임의 수익률(%) */
    public BigDecimal getBestReturnRate() {
        return bestReturnRate;
    }

    /** @return 저장한 게임 수 */
    public int getPlayCount() {
        return playCount;
    }

    /** @return 달성한 엔딩 */
    public Set<Ending> getEndings() {
        return endings;
    }

    /** @return 업적별 획득 횟수 */
    public Map<Achievement, Integer> getAchievements() {
        return achievements;
    }

    /** @return 반영한 세션 ID */
    public Set<UUID> getSavedSessions() {
        return Collections.unmodifiableSet(savedSessions);
    }

    void addSession(UUID id) {
        savedSessions.add(id);
    }
}
