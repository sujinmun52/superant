package superant.model;

/**
 * 30턴 종료 후 수익률로 정해지는 엔딩.
 * <p>
 * 수익률 20% 이상은 수익 달성, 0% 이상 20% 미만은 자산 유지, 음수는 재도전이다.
 */
public enum Ending {
    /** 수익률 20% 이상. 50% 이상이면 결과 칭호가 "슈퍼개미 등극!" 으로 올라간다. */
    PROFIT("여왕개미 등극!", "수익 달성"),
    /** 수익률 0% 이상 20% 미만. */
    MAINTAIN("일개미 생존", "자산 유지"),
    /** 수익률 음수. */
    RETRY("개미굴로 귀환", "재도전");

    private final String title;
    private final String label;

    Ending(String title, String label) {
        this.title = title;
        this.label = label;
    }

    /**
     * 결과 화면 배너에 표시할 칭호를 반환한다.
     *
     * @return 칭호 (예: "여왕개미 등극!")
     */
    public String getTitle() {
        return title;
    }

    /**
     * 요구사항에서 정의한 엔딩 이름을 반환한다.
     *
     * @return 엔딩 이름 (예: "수익 달성")
     */
    public String getLabel() {
        return label;
    }
}
