package superant.controller;

/**
 * 게임 시작 요청의 결과 (FN-01).
 */
public class StartResult extends ActionResult {
    private final ViewData view;

    private StartResult(boolean success, String errorCode, String message, ViewData view) {
        super(success, errorCode, message);
        this.view = view;
    }

    /**
     * 성공 결과를 만든다.
     *
     * @param view 첫 화면 데이터
     * @return 성공 결과
     */
    public static StartResult ok(ViewData view) {
        return new StartResult(true, null, "게임을 시작했습니다.", view);
    }

    /**
     * 실패 결과를 만든다.
     *
     * @param code    실패 코드
     * @param message 안내 문구
     * @return 실패 결과
     */
    public static StartResult fail(String code, String message) {
        return new StartResult(false, code, message, null);
    }

    /** @return 화면 데이터, 실패하면 null */
    public ViewData getView() {
        return view;
    }
}
