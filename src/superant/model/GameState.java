package superant.model;

/**
 * 게임 세션의 진행 상태.
 * <p>
 * READY 에서만 거래와 턴 진행을 받는다. 턴 마감 중에는 UPDATING 이며,
 * 30턴을 마치면 ENDED 가 되어 거래와 턴 진행을 차단한다.
 */
public enum GameState {
    /** 거래와 턴 진행이 가능한 상태. */
    READY,
    /** 턴 마감을 계산하는 중인 상태. */
    UPDATING,
    /** 30턴을 모두 마친 상태. */
    ENDED
}
