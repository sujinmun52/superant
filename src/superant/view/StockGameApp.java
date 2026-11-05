package superant.view;

import javax.swing.SwingUtilities;

import superant.controller.GameController;
import superant.persistence.RecordRepository;

/**
 * 슈퍼개미 실행 진입점.
 */
public final class StockGameApp {
    private StockGameApp() {
    }

    /**
     * 게임 창을 띄운다. 기록은 사용자 데이터 폴더의 stock-game/records.json 에 저장한다.
     *
     * @param args 사용하지 않음
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            GameController controller = new GameController(new RecordRepository(RecordRepository.defaultPath()));
            new MainFrame(controller).setVisible(true);
        });
    }
}
