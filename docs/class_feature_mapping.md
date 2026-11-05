# 클래스 다이어그램과 기능 연결 (CP4 보고서 X장 초안)

## 1. 구조 개요

6개 패키지로 책임을 나눈다. 화면(view)은 입력과 표시만 담당하고, 모든 요청은 GameController 하나로 들어간다. 게임 규칙은 domain, 결과 판정은 service, 파일 저장은 persistence가 맡는다. 열거형은 model에 모은다.

| 패키지 | 클래스 | 역할 |
|---|---|---|
| view | StockGameApp, MainFrame, StartPanel, PriceChartPanel, OrderDialog, ResultDialog, RecordDialog | Swing 화면 (UI-01~05) |
| controller | GameController, ViewData, ActionResult 및 하위 4개 | 입력 검증, 처리 순서, 결과 반환 |
| domain | GameSession, Portfolio, Holding, Trade, Market, Stock, NewsService, TurnDraw, News, GameSnapshot | 게임 상태와 규칙 |
| service | ResultEvaluator, GameResult | 30턴 결과 평가 |
| persistence | RecordStore, RecordRepository, RecordEntry | 기록 파일 저장과 조회 |
| model | Difficulty, GameState, OrderSide, Sector, RiskLevel, NewsEvent, Ending, InvestorType, Achievement | 열거형 |

## 2. 기능별 연결

| 기능 | 화면 | 진입 메서드 (FN) | 처리 클래스 |
|---|---|---|---|
| F-01 게임 시작 | StartPanel, MainFrame | GameController.startGame (FN-01) | GameSession 생성자가 Portfolio, Market, NewsService를 합성으로 생성 |
| F-02 종목 정보 | MainFrame 종목 표 | GameController.getView | Market.getStocks, Stock.changeRate |
| F-03 턴 진행 | MainFrame 다음 턴 | GameController.advanceTurn (FN-03) | NewsService.drawFor, effectsFor → Market.applyTurn → GameSession.closeTurn |
| F-04 매수 | OrderDialog | GameController.placeOrder (FN-02) | Portfolio.buy → Holding.add, Trade 생성 |
| F-05 매도 | OrderDialog | GameController.placeOrder (FN-02) | Portfolio.sell → Holding.remove, Trade 생성 |
| F-06 자산 조회 | MainFrame 포트폴리오 탭 | GameController.getView | Portfolio.totalAssets, weights |
| F-07 뉴스 조회 | MainFrame 뉴스 피드 | GameController.getView | NewsService.currentNews, pastNews, News.hint |
| F-08 이력 조회 | PriceChartPanel, 거래 내역 탭 | GameController.getView | Stock.getPriceHistory, Portfolio의 Trade 목록 |
| F-09 결과 평가 | ResultDialog | GameController.evaluateResult | ResultEvaluator.evaluate (FN-04) → GameResult |
| F-10 기록 저장·조회 | ResultDialog, RecordDialog | GameController.saveResult, readRecords | RecordStore를 구현한 RecordRepository.saveResult (FN-05), RecordEntry.merge |

## 3. 관계 설명 (강의자료 표기 기준)

- **일반화**: MainFrame은 JFrame, StartPanel과 PriceChartPanel은 JPanel, 세 대화상자는 JDialog를 상속한다. StartResult, TradeResult, TurnResult, SaveResult는 추상 클래스 ActionResult를 상속하여 성공 여부, 오류 코드, 메시지를 공통으로 가진다 (CP3 함수 명세의 공통 반환값).
- **실체화**: RecordRepository가 인터페이스 RecordStore를 구현한다. GameController는 인터페이스에만 의존하므로 테스트에서 저장소를 바꿔 끼울 수 있다.
- **합성**: GameSession이 사라지면 Portfolio, Market, NewsService도 함께 사라진다. Portfolio는 Holding과 Trade를, Market은 종목 6개를, NewsService는 30턴 추첨 자료(TurnDraw 30개)를 소유한다.
- **집합**: NewsService가 공개한 News 목록은 추첨 자료와 별개로 조회용으로 유지된다.
- **연관**: MainFrame은 GameController를, GameController는 GameSession(0..1), ResultEvaluator, RecordStore를 참조한다 (단방향).
- **의존**: GameController는 ViewData와 ActionResult를 생성해 반환하고, GameSession은 GameSnapshot을 만든다. ResultEvaluator는 GameSnapshot을 받아 GameResult를 만든다.
- **제약**: 현금 `{cash >= 0}`, 가격 `{price >= 1}`, 완료 턴 `{0..30}`.
- **static**: GameSession.TOTAL_TURNS = 30, INITIAL_CASH = 1000000, StockGameApp.main.

## 4. CP3 대비 변경

- CP3 시퀀스의 GameView는 Swing 구현에 맞게 MainFrame과 대화상자 4개로 나눴다.
- 공통 반환값을 ActionResult 상속 구조로 구체화했다.
- 저장소를 RecordStore 인터페이스와 RecordRepository 구현으로 분리했다.
