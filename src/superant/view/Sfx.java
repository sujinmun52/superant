package superant.view;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * 게임 효과음. 외부 사운드 파일 없이 사인파를 직접 합성해 재생한다.
 * 재생은 별도 스레드에서 순서대로 하며, 사운드 장치가 없으면 조용히 무시한다.
 */
public final class Sfx {
    /** 효과음 종류. 각 값은 (주파수 Hz, 길이 ms) 음표 목록이다. */
    public enum Sound {
        /** 매수 체결: 올라가는 두 음. */
        BUY(new int[][]{{880, 70}, {1320, 120}}),
        /** 매도 체결: 내려가는 두 음. */
        SELL(new int[][]{{1320, 70}, {880, 120}}),
        /** 상승 마감: 밝은 화음 아르페지오. */
        CLOSE_UP(new int[][]{{523, 80}, {659, 80}, {784, 160}}),
        /** 하락 마감: 내려가는 세 음. */
        CLOSE_DOWN(new int[][]{{392, 90}, {330, 90}, {262, 180}}),
        /** 칭호 상승, 수익 엔딩: 팡파르. */
        RANK_UP(new int[][]{{523, 90}, {659, 90}, {784, 90}, {1047, 320}}),
        /** 속보: 짧은 경고음 두 번. */
        NEWS(new int[][]{{1175, 60}, {0, 50}, {1175, 60}}),
        /** 주문 거절. */
        ERROR(new int[][]{{196, 160}});

        private final int[][] notes;

        Sound(int[][] notes) {
            this.notes = notes;
        }
    }

    private static final float RATE = 44_100f;
    private static final ExecutorService PLAYER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "superant-sfx");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean enabled = true;
    private static volatile boolean broken;

    private Sfx() {
    }

    /**
     * 소리를 켜거나 끈다.
     *
     * @param on true 이면 재생
     */
    public static void setEnabled(boolean on) {
        enabled = on;
    }

    /**
     * 소리가 켜져 있는지 반환한다.
     *
     * @return 켜져 있으면 true
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 효과음을 비동기로 재생한다. 꺼져 있거나 장치가 없으면 아무 일도 하지 않는다.
     *
     * @param sound 재생할 효과음
     */
    public static void play(Sound sound) {
        if (!enabled || broken) {
            return;
        }
        PLAYER.execute(() -> render(sound));
    }

    private static void render(Sound sound) {
        byte[] pcm = synth(sound.notes);
        try {
            AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
            line.close();
        } catch (Exception | LinkageError e) {
            broken = true;
        }
    }

    /**
     * 음표 목록을 16비트 PCM 으로 합성한다. 클릭 소리를 막기 위해 짧은 페이드 인/아웃을 준다.
     */
    static byte[] synth(int[][] notes) {
        int total = 0;
        for (int[] n : notes) {
            total += (int) (RATE * n[1] / 1000);
        }
        byte[] out = new byte[total * 2];
        int pos = 0;
        for (int[] n : notes) {
            int len = (int) (RATE * n[1] / 1000);
            int fade = Math.min(len / 4, (int) (RATE * 0.008));
            for (int i = 0; i < len; i++) {
                double env = Math.min(1.0, Math.min(i, len - 1 - i) / (double) Math.max(1, fade));
                double decay = Math.exp(-2.5 * i / len);
                double t = i / RATE;
                double v = n[0] == 0 ? 0 : (Math.sin(2 * Math.PI * n[0] * t)
                        + 0.25 * Math.sin(4 * Math.PI * n[0] * t)) * 0.22 * env * decay;
                short s = (short) (v * Short.MAX_VALUE);
                out[pos++] = (byte) s;
                out[pos++] = (byte) (s >> 8);
            }
        }
        return out;
    }
}
