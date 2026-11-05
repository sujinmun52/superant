package superant.controller;

/**
 * 턴 마감 요청의 결과 (FN-03).
 */
public class TurnResult extends ActionResult {
    private final int completedTurns;
    private final boolean ended;
    private final ViewData view;

    private TurnResult(boolean success, String errorCode, String message, int completedTurns, boolean ended,
                       ViewData view) {
        super(success, errorCode, message);
        this.completedTurns = completedTurns;
        this.ended = ended;
        this.view = view;
    }

    /**
     * 성공 결과를 만든다.
     *
     * @param completedTurns 마감한 턴 수
     * @param ended          30턴을 마쳤는지
     * @param view           마감 후 화면 데이터
     * @return 성공 결과
     */
    public static TurnResult ok(int completedTurns, boolean ended, ViewData view) {
        String msg = ended ? "30턴을 모두 마쳤습니다." : completedTurns + "턴을 마감했습니다.";
        return new TurnResult(true, null, msg, completedTurns, ended, view);
    }

    /**
     * 거절 결과를 만든다.
     *
     * @param code    실패 코드
     * @param message 안내 문구
     * @return 실패 결과
     */
    public static TurnResult fail(String code, String message) {
        return new TurnResult(false, code, message, 0, false, null);
    }

    /** @return 마감한 턴 수 */
    public int getCompletedTurns() {
        return completedTurns;
    }

    /** @return 게임이 끝났으면 true */
    public boolean isEnded() {
        return ended;
    }

    /** @return 화면 데이터, 실패하면 null */
    public ViewData getView() {
        return view;
    }
}
