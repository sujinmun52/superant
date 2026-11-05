package superant.view;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import superant.controller.GameController;
import superant.controller.ViewData;
import superant.model.Difficulty;
import superant.model.Ending;
import superant.model.OrderSide;
import superant.persistence.RecordRepository;
import superant.service.GameResult;

/**
 * 보고서용 베타 화면을 자동으로 진행하며 PNG 로 저장하는 개발 도구.
 * 사용법: java superant.view.ScreenshotTool 출력폴더
 */
public final class ScreenshotTool {
    private static MainFrame frame;
    private static GameController controller;
    private static Path out;

    private ScreenshotTool() {
    }

    /**
     * 화면을 캡처한다.
     *
     * @param args [0] 출력 폴더
     * @throws Exception 실패 시
     */
    public static void main(String[] args) throws Exception {
        out = Paths.get(args.length > 0 ? args[0] : "shots");
        Files.createDirectories(out);
        long seed = findProfitSeed();
        System.out.println("seed=" + seed);

        Path records = Files.createTempDirectory("superant-shot").resolve("records.json");
        controller = new GameController(new RecordRepository(records), new Random(seed));
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            frame = new MainFrame(controller);
            frame.setSize(1400, 900);
            frame.setVisible(true);
        });
        Thread.sleep(600);

        SwingUtilities.invokeAndWait(() -> {
            StartPanel sp = (StartPanel) get(frame, "startPanel");
            sp.prefill("개미왕", Difficulty.EASY);
        });
        Thread.sleep(300);
        shot(frame.getContentPane(), "beta_1_start", "시작 화면: 이름 입력과 난이도 카드 3장 (EASY 선택)");

        SwingUtilities.invokeAndWait(() -> call(frame, "onStart"));
        ViewData v = controller.getView();
        v = buy(v, "S01", 30);
        v = buy(v, "S03", 30);
        v = buy(v, "S04", 20);
        v = buy(v, "S06", 20);
        final ViewData v1 = v;
        SwingUtilities.invokeAndWait(() -> {
            frame.refresh(v1);
            set(frame, "selectedId", "S01");
            frame.refresh(v1);
            call(frame, "status", "바른물류 매수 20주 체결 (120,000원)", Theme.UP);
        });
        Thread.sleep(300);
        shot(frame.getContentPane(), "beta_2_game_turn1", "게임 화면 1턴: 4종목 매수 후 보유 종목 강조(민트 막대), 내 보유 정보, EASY 뉴스 힌트");

        for (int i = 0; i < 11; i++) {
            v = controller.advanceTurn(v.getVersion(), UUID.randomUUID()).getView();
        }
        v = controller.placeOrder("S01", OrderSide.SELL, 10, v.getVersion(), UUID.randomUUID()).getView();
        final ViewData v2 = v;
        SwingUtilities.invokeAndWait(() -> {
            frame.refresh(v2);
            call(frame, "selectTab", 1);
            call(frame, "status", "새빛테크 매도 10주 체결", Theme.DOWN);
        });
        Thread.sleep(300);
        shot(frame.getContentPane(), "beta_3_game_turn12", "12턴: 가격 차트와 거래 내역 탭, 매도 실현 손익 표시");

        SwingUtilities.invokeAndWait(() -> call(frame, "selectTab", 0));
        SwingUtilities.invokeAndWait(() -> call(frame, "onNextTurn"));
        Thread.sleep(1250);
        shot(frame.getContentPane(), "beta_4_live", "턴 마감 장중 애니메이션: 가격이 틱 단위로 오르내리고(빨강/파랑 깜빡임) 차트에 LIVE 표시");
        Thread.sleep(2600);
        v = controller.getView();
        final ViewData v2b = v;
        captureOrderDialog(v2b);

        for (int i = 0; i < 18; i++) {
            v = controller.advanceTurn(v.getVersion(), UUID.randomUUID()).getView();
        }
        final ViewData v3 = v;
        SwingUtilities.invokeAndWait(() -> {
            frame.refresh(v3);
            call(frame, "selectTab", 2);
        });
        Thread.sleep(300);
        shot(frame.getContentPane(), "beta_6_game_end", "30턴 종료: 거래 버튼 비활성화, 결과 보기 버튼, 뉴스 피드");

        AtomicReference<ResultDialog> rd = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> rd.set(new ResultDialog(frame, controller, controller.evaluateResult())));
        SwingUtilities.invokeAndWait(() -> call(rd.get(), "onSave"));
        shot(rd.get().getContentPane(), "beta_7_result", "결과 화면: 엔딩 칭호, 성과 지표 8개, 업적, 자산 추이, 저장 완료");

        AtomicReference<RecordDialog> rec = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> rec.set(new RecordDialog(frame, controller, "개미왕", Difficulty.EASY)));
        shot(rec.get().getContentPane(), "beta_8_records", "기록 조회 화면: 최고 자산, 달성 엔딩, 업적 횟수");

        GameController hard = new GameController(new RecordRepository(records), new Random(99));
        SwingUtilities.invokeAndWait(() -> {
            frame.dispose();
            frame = new MainFrame(hard);
            frame.setSize(1400, 900);
            frame.setVisible(true);
            ((StartPanel) get(frame, "startPanel")).prefill("고수개미", Difficulty.HARD);
            call(frame, "onStart");
        });
        ViewData h = hard.getView();
        h = hard.placeOrder("S05", OrderSide.BUY, 80, h.getVersion(), UUID.randomUUID()).getView();
        h = hard.placeOrder("S02", OrderSide.BUY, 25, h.getVersion(), UUID.randomUUID()).getView();
        for (int i = 0; i < 20; i++) {
            h = hard.advanceTurn(h.getVersion(), UUID.randomUUID()).getView();
            if (i >= 8 && h.getCurrentNews() != null) {
                break;
            }
        }
        final ViewData hv = h;
        SwingUtilities.invokeAndWait(() -> {
            set(frame, "selectedId", "S05");
            frame.refresh(hv);
            call(frame, "status", hv.getCurrentTurn() - 1 + "턴 마감 완료", Theme.MINT);
        });
        Thread.sleep(300);
        shot(frame.getContentPane(), "beta_9_hard", "HARD 모드: 수치 대신 지연 가능성만 안내하는 뉴스");

        SwingUtilities.invokeAndWait(() -> frame.dispose());
        System.exit(0);
    }

    private static void captureOrderDialog(ViewData v) throws Exception {
        Timer t = new Timer(700, e -> {
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w instanceof OrderDialog && w.isShowing()) {
                    try {
                        shot(((JDialog) w).getContentPane(), "beta_5_order", "주문 확인 창: 체결 가격, 주문 금액, 주문 후 현금");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    w.dispose();
                }
            }
        });
        t.setRepeats(false);
        t.start();
        SwingUtilities.invokeAndWait(() -> new OrderDialog(frame).showDialog(OrderSide.BUY,
                v.getSnapshot().findStock("S02"), 15, v.getSnapshot().getCash()));
    }

    private static ViewData buy(ViewData v, String id, int qty) {
        return controller.placeOrder(id, OrderSide.BUY, qty, v.getVersion(), UUID.randomUUID()).getView();
    }

    private static long findProfitSeed() {
        for (long seed = 1; seed < 500; seed++) {
            GameController c = new GameController(null, new Random(seed));
            ViewData v = c.startGame("t", Difficulty.EASY).getView();
            for (String[] b : new String[][]{{"S01", "30"}, {"S03", "30"}, {"S04", "20"}, {"S06", "20"}}) {
                v = c.placeOrder(b[0], OrderSide.BUY, Integer.parseInt(b[1]), v.getVersion(), UUID.randomUUID()).getView();
            }
            for (int i = 0; i < 11; i++) {
                v = c.advanceTurn(v.getVersion(), UUID.randomUUID()).getView();
            }
            v = c.placeOrder("S01", OrderSide.SELL, 10, v.getVersion(), UUID.randomUUID()).getView();
            for (int i = 0; i < 19; i++) {
                v = c.advanceTurn(v.getVersion(), UUID.randomUUID()).getView();
            }
            GameResult r = c.evaluateResult();
            if (r.getEnding() == Ending.PROFIT && r.getAchievements().size() >= 2
                    && r.getReturnRate().compareTo(BigDecimal.valueOf(40)) < 0) {
                return seed;
            }
        }
        return 1;
    }

    static void shot(Component c, String name, String caption) throws Exception {
        AtomicReference<BufferedImage> img = new AtomicReference<>();
        Runnable r = () -> {
            c.validate();
            BufferedImage bi = new BufferedImage(c.getWidth() * 2, c.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.scale(2, 2);
            c.printAll(g);
            g.dispose();
            img.set(bi);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeAndWait(r);
        }
        ImageIO.write(img.get(), "png", new File(out.toFile(), name + ".png"));
        Files.write(out.resolve(name + ".txt"), caption.getBytes(StandardCharsets.UTF_8));
        System.out.println("saved " + name + " " + c.getWidth() + "x" + c.getHeight());
    }

    private static Object get(Object o, String field) {
        try {
            Field f = o.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(Object o, String field, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(o, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void call(Object o, String name, Object... args) {
        try {
            for (Method m : o.getClass().getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    m.invoke(o, args);
                    return;
                }
            }
            throw new NoSuchMethodException(name);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
