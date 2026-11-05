package superant.controller;

/**
 * 컨트롤러 요청의 처리 결과를 담는 공통 부모 클래스.
 * 실패하면 errorCode 와 사용자에게 보여 줄 message 를 가진다.
 */
public abstract class ActionResult {
    /** 성공 여부. */
    protected final boolean success;
    /** 실패 코드 (성공이면 null). */
    protected final String errorCode;
    /** 사용자 안내 문구. */
    protected final String message;

    /**
     * 결과를 만든다.
     *
     * @param success   성공 여부
     * @param errorCode 실패 코드, 성공이면 null
     * @param message   안내 문구
     */
    protected ActionResult(boolean success, String errorCode, String message) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
    }

    /** @return 성공했으면 true */
    public boolean isSuccess() {
        return success;
    }

    /** @return 실패 코드, 성공이면 null */
    public String getErrorCode() {
        return errorCode;
    }

    /** @return 안내 문구 */
    public String getMessage() {
        return message;
    }
}
