package superant.persistence;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import superant.controller.SaveResult;
import superant.model.Achievement;
import superant.model.Difficulty;
import superant.model.Ending;
import superant.service.GameResult;

/**
 * JSON 파일에 기록을 저장하는 저장소 (FN-05).
 * <p>
 * 파일 형식은 {@code {"schemaVersion":1,"records":[...]}} 이다.
 * 저장할 때는 임시 파일에 먼저 쓰고 원래 파일을 교체해서, 중간에 실패해도 기존 기록이 깨지지 않게 한다.
 */
public class RecordRepository implements RecordStore {
    /** 현재 파일 형식 버전. */
    public static final int SCHEMA_VERSION = 1;

    private final Path file;

    /**
     * 지정한 파일을 쓰는 저장소를 만든다.
     *
     * @param file 기록 파일 경로
     */
    public RecordRepository(Path file) {
        this.file = file;
    }

    /**
     * 사용자 데이터 폴더의 stock-game/records.json 경로를 반환한다.
     * Windows 는 %APPDATA%, 그 밖에는 홈 폴더를 쓴다.
     *
     * @return 기본 기록 파일 경로
     */
    public static Path defaultPath() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isEmpty()
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"));
        return base.resolve("stock-game").resolve("records.json");
    }

    @Override
    public synchronized SaveResult saveResult(GameResult result) throws IOException {
        List<RecordEntry> all = loadAll();
        RecordEntry entry = null;
        for (RecordEntry e : all) {
            if (e.matches(result.getPlayerName(), result.getDifficulty())) {
                entry = e;
                break;
            }
        }
        if (entry == null) {
            entry = new RecordEntry(result.getPlayerName(), result.getDifficulty());
            all.add(entry);
        }
        BigDecimal oldBest = entry.getBestAssets();
        if (!entry.merge(result)) {
            return SaveResult.saved(true, file, "이미 저장한 게임입니다.");
        }
        writeAtomically(all);
        boolean newBest = oldBest == null || result.getFinalAssets().compareTo(oldBest) > 0;
        return SaveResult.saved(false, file, newBest ? "저장 완료! 최고 기록을 갱신했습니다." : "저장 완료!");
    }

    @Override
    public synchronized RecordEntry readRecords(String playerName, Difficulty difficulty) throws IOException {
        for (RecordEntry e : loadAll()) {
            if (e.matches(playerName, difficulty)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 파일 전체를 읽는다. 파일이 없으면 빈 목록이다.
     */
    @SuppressWarnings("unchecked")
    private List<RecordEntry> loadAll() throws IOException {
        List<RecordEntry> list = new ArrayList<>();
        if (!Files.exists(file)) {
            return list;
        }
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        try {
            Map<String, Object> root = (Map<String, Object>) Json.parse(text);
            int schema = Integer.parseInt(String.valueOf(root.get("schemaVersion")));
            if (schema > SCHEMA_VERSION) {
                throw new IOException("더 새로운 버전의 기록 파일입니다 (schemaVersion " + schema + ").");
            }
            for (Object o : (List<Object>) root.get("records")) {
                Map<String, Object> m = (Map<String, Object>) o;
                RecordEntry e = new RecordEntry((String) m.get("playerName"),
                        Difficulty.valueOf((String) m.get("difficulty")));
                Object best = m.get("bestAssets");
                Object rate = m.get("bestReturnRate");
                e.restore(best == null ? null : new BigDecimal((String) best),
                        rate == null ? null : new BigDecimal((String) rate),
                        Integer.parseInt((String) m.get("playCount")));
                for (Object end : (List<Object>) m.get("endings")) {
                    e.getEndings().add(Ending.valueOf((String) end));
                }
                for (Map.Entry<String, Object> a : ((Map<String, Object>) m.get("achievements")).entrySet()) {
                    e.getAchievements().put(Achievement.valueOf(a.getKey()), Integer.parseInt((String) a.getValue()));
                }
                for (Object id : (List<Object>) m.get("savedSessions")) {
                    e.addSession(UUID.fromString((String) id));
                }
                list.add(e);
            }
        } catch (ClassCastException | NullPointerException | IllegalArgumentException ex) {
            throw new IOException("기록 파일 형식이 올바르지 않습니다: " + file, ex);
        }
        return list;
    }

    /**
     * 임시 파일에 쓴 뒤 원래 파일을 교체한다.
     */
    private void writeAtomically(List<RecordEntry> entries) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n  \"records\": [");
        for (int i = 0; i < entries.size(); i++) {
            RecordEntry e = entries.get(i);
            sb.append(i == 0 ? "\n" : ",\n").append("    {");
            sb.append("\"playerName\": ").append(Json.quote(e.getPlayerName()));
            sb.append(", \"difficulty\": ").append(Json.quote(e.getDifficulty().name()));
            sb.append(", \"bestAssets\": ").append(e.getBestAssets() == null ? "null" : e.getBestAssets().toPlainString());
            sb.append(", \"bestReturnRate\": ").append(e.getBestReturnRate() == null ? "null" : e.getBestReturnRate().toPlainString());
            sb.append(", \"playCount\": ").append(e.getPlayCount());
            sb.append(", \"endings\": [");
            boolean first = true;
            for (Ending end : e.getEndings()) {
                sb.append(first ? "" : ", ").append(Json.quote(end.name()));
                first = false;
            }
            sb.append("], \"achievements\": {");
            first = true;
            for (Map.Entry<Achievement, Integer> a : e.getAchievements().entrySet()) {
                sb.append(first ? "" : ", ").append(Json.quote(a.getKey().name())).append(": ").append(a.getValue());
                first = false;
            }
            sb.append("}, \"savedSessions\": [");
            first = true;
            for (UUID id : e.getSavedSessions()) {
                sb.append(first ? "" : ", ").append(Json.quote(id.toString()));
                first = false;
            }
            sb.append("]}");
        }
        sb.append("\n  ]\n}\n");

        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "records", ".tmp");
        try {
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** @return 기록 파일 경로 */
    public Path getFile() {
        return file;
    }
}
