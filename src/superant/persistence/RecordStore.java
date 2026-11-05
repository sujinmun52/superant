package superant.persistence;

import java.io.IOException;

import superant.controller.SaveResult;
import superant.model.Difficulty;
import superant.service.GameResult;

/**
 * 게임 기록 저장소 인터페이스.
 * 컨트롤러는 이 인터페이스에만 의존하므로 파일 대신 다른 저장 방식으로 바꿀 수 있다.
 */
public interface RecordStore {
    /**
     * 게임 결과를 기록에 반영한다. 같은 세션은 한 번만 반영한다.
     *
     * @param result 저장할 게임 결과
     * @return 저장 결과
     * @throws IOException 파일을 읽거나 쓸 수 없을 때
     */
    SaveResult saveResult(GameResult result) throws IOException;

    /**
     * 이름과 난이도로 누적 기록을 조회한다.
     *
     * @param playerName 플레이어 이름
     * @param difficulty 난이도
     * @return 누적 기록, 없으면 null
     * @throws IOException 파일을 읽을 수 없거나 형식이 잘못되었을 때
     */
    RecordEntry readRecords(String playerName, Difficulty difficulty) throws IOException;
}
