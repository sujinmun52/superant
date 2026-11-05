package superant.controller;

import java.nio.file.Path;

/**
 * 기록 저장 요청의 결과 (FN-05).
 */
public class SaveResult extends ActionResult {
    private final boolean alreadySaved;
    private final Path location;

    private SaveResult(boolean success, String errorCode, String message, boolean alreadySaved, Path location) {
        super(success, errorCode, message);
        this.alreadySaved = alreadySaved;
        this.location = location;
    }

    /**
     * 저장 성공(또는 이미 저장됨) 결과를 만든다.
     *
     * @param alreadySaved 이미 저장한 세션이었는지
     * @param location     기록 파일 경로
     * @param message      안내 문구
     * @return 성공 결과
     */
    public static SaveResult saved(boolean alreadySaved, Path location, String message) {
        return new SaveResult(true, null, message, alreadySaved, location);
    }

    /**
     * 실패 결과를 만든다.
     *
     * @param code    실패 코드
     * @param message 안내 문구
     * @return 실패 결과
     */
    public static SaveResult fail(String code, String message) {
        return new SaveResult(false, code, message, false, null);
    }

    /** @return 이미 저장한 세션이었으면 true */
    public boolean isAlreadySaved() {
        return alreadySaved;
    }

    /** @return 기록 파일 경로, 실패하면 null */
    public Path getLocation() {
        return location;
    }
}
