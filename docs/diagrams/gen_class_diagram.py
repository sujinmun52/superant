"""슈퍼개미 클래스 다이어그램 생성기.

강의자료(0. Diagrams.pdf) 표기법을 따른다.
- 클래스: 이름 / 속성 / 연산 3칸, 추상 클래스는 이탤릭, 인터페이스는 <<interface>>
- 가시성: + public, - private, # protected, static 은 밑줄
- 속성: 가시성 이름 : 타입 = 기본값, 연산: 가시성 이름(파라미터 : 타입) : 반환타입
- 관계: 일반화, 실체화, 연관(방향), 집합, 합성, 의존, 다중성, 제약 { }
- 패키지로 이름 공간 구분

출력: class_diagram.svg, class_diagram.uxf (UMLet / UMLetino 에서 열기)
"""
from __future__ import annotations

import html
import math
import sys

MODE = sys.argv[1] if len(sys.argv) > 1 else "full"
from pathlib import Path

OUT = Path(__file__).resolve().parent

CHAR_W = 7.0      # Consolas 12px 대략 폭
LINE_H = 17
PAD_X = 10
NAME_H = 24
STEREO_H = 15


class Box:
    def __init__(self, name, attrs=(), ops=(), stereo=None, abstract=False,
                 tags=None, compact=False, min_w=0):
        self.name = name
        self.attrs = list(attrs)
        self.ops = list(ops)
        self.stereo = stereo
        self.abstract = abstract
        self.tags = tags
        compact = compact or MODE == "overview"
        self.compact = compact          # 외부 라이브러리 클래스: 이름 칸만
        self.min_w = min_w
        self.x = self.y = 0
        lines = [self.name] + ([] if compact else [self._txt(a) for a in self.attrs] + [self._txt(o) for o in self.ops])
        if tags:
            lines.append(tags + "  ")
        if stereo:
            lines.append("<<" + stereo + ">>")
        self.w = max(min_w, int(max(len(s) for s in lines) * CHAR_W + PAD_X * 2))
        self.w = int(math.ceil(self.w / 10.0) * 10)
        h = NAME_H + (STEREO_H if stereo else 0)
        if not compact:
            h += max(1, len(self.attrs)) * LINE_H + 8
            h += max(1, len(self.ops)) * LINE_H + 8
        self.h = int(math.ceil(h / 10.0) * 10)

    @staticmethod
    def _txt(item):
        return item[1:] if item.startswith("_") else item

    def at(self, x, y):
        self.x, self.y = x, y
        return self

    def below(self, other, gap=40, dx=0):
        return self.at(other.x + dx, other.y + other.h + gap)

    def right(self, other, gap=40, dy=0):
        return self.at(other.x + other.w + gap, other.y + dy)

    # 앵커: side 와 0~1 위치
    def anchor(self, side, f=0.5):
        if side == "top":
            return (self.x + self.w * f, self.y)
        if side == "bottom":
            return (self.x + self.w * f, self.y + self.h)
        if side == "left":
            return (self.x, self.y + self.h * f)
        return (self.x + self.w, self.y + self.h * f)


B = {}


def box(key, *a, **k):
    B[key] = Box(*a, **k)
    return B[key]


# ---------------------------------------------------------------- view
box("App", "StockGameApp", [], ["_+main(args : String[]) : void"], tags="실행")
box("JFrame", "javax.swing::JFrame", compact=True)
box("JPanel", "javax.swing::JPanel", compact=True)
box("JDialog", "javax.swing::JDialog", compact=True)
box("MainFrame", "MainFrame", [
    "-controller : GameController",
    "-startPanel : StartPanel",
    "-chartPanel : PriceChartPanel",
    "-stockTable : JTable",
    "-holdingTable : JTable",
    "-newsArea : JTextArea",
], [
    "+MainFrame(controller : GameController)",
    "+refresh(view : ViewData) : void",
    "-onStart() : void",
    "-onBuy() : void",
    "-onSell() : void",
    "-onNextTurn() : void",
    "-animateTurn(before : ViewData, result : TurnResult) : void",
    "-showResult() : void",
], tags="UI-02 / F-02~08")
box("StartPanel", "StartPanel", [
    "-nameField : JTextField",
    "-difficultyGroup : ButtonGroup",
], [
    "+getPlayerName() : String",
    "+getDifficulty() : Difficulty",
], tags="UI-01 / F-01")
box("Chart", "PriceChartPanel", [
    "-prices : List<Long>",
], [
    "+setPrices(prices : List<Long>) : void",
    "#paintComponent(g : Graphics) : void",
], tags="F-08")
box("OrderDialog", "OrderDialog", [
    "-confirmed : boolean",
], [
    "+showDialog(side : OrderSide, stock : Stock,",
    "    qty : int, cash : BigDecimal) : boolean",
], tags="UI-03 / F-04, F-05")
box("ResultDialog", "ResultDialog", [
    "-result : GameResult",
    "-saveStatus : JLabel",
], [
    "+ResultDialog(owner : JFrame, controller :",
    "    GameController, result : GameResult)",
    "-onSave() : void",
], tags="UI-04 / F-09, F-10")
box("RecordDialog", "RecordDialog", [
    "-controller : GameController",
], [
    "+RecordDialog(owner : JFrame, controller : GameController)",
    "+RecordDialog(owner : JFrame, controller : GameController,",
    "    name : String, difficulty : Difficulty)",
    "-onSearch() : void",
], tags="UI-05 / F-10")

# ---------------------------------------------------------------- controller
box("Controller", "GameController", [
    "-session : GameSession",
    "-evaluator : ResultEvaluator",
    "-store : RecordStore",
    "-seedSource : Random",
    "-lastResult : GameResult",
    "-handled : Map<UUID, ActionResult>",
], [
    "+startGame(name : String, difficulty : Difficulty) : StartResult",
    "+placeOrder(stockId : String, side : OrderSide, qty : int,",
    "    expectedVersion : long, requestId : UUID) : TradeResult",
    "+advanceTurn(expectedVersion : long, requestId : UUID) : TurnResult",
    "+getView() : ViewData",
    "+evaluateResult() : GameResult",
    "+saveResult() : SaveResult",
    "+readRecords(name : String, difficulty : Difficulty) : RecordEntry",
], tags="FN-01~03 / F-01, F-03~05")
box("ViewData", "ViewData", [
    "-snapshot : GameSnapshot",
    "-totalAssets : BigDecimal",
    "-returnRate : BigDecimal",
    "-currentNews : News",
    "-newsFeed : List<News>",
    "-currentTurn : int",
    "-version : long",
], ["+getSnapshot() : GameSnapshot"], tags="F-02, F-06, F-07")
box("ActionResult", "ActionResult", [
    "#success : boolean",
    "#errorCode : String",
    "#message : String",
], [
    "+isSuccess() : boolean",
    "+getMessage() : String",
], abstract=True)
box("StartResult", "StartResult", ["-view : ViewData"], ["+getView() : ViewData"])
box("TradeResult", "TradeResult", [
    "-price : long",
    "-quantity : int",
    "-realizedPnl : BigDecimal",
    "-view : ViewData",
], ["+getView() : ViewData"])
box("TurnResult", "TurnResult", [
    "-completedTurns : int",
    "-ended : boolean",
    "-view : ViewData",
], ["+isEnded() : boolean"])
box("SaveResult", "SaveResult", [
    "-alreadySaved : boolean",
    "-location : Path",
], ["+isAlreadySaved() : boolean"])

# ---------------------------------------------------------------- domain
box("Session", "GameSession", [
    "_+TOTAL_TURNS : int = 30",
    "_+INITIAL_CASH : BigDecimal = 1000000",
    "_+GOAL_ASSETS : BigDecimal = 1200000",
    "-sessionId : UUID",
    "-playerName : String",
    "-difficulty : Difficulty",
    "-state : GameState",
    "-completedTurns : int {0..30}",
    "-version : long",
    "-assetHistory : List<BigDecimal>",
    "-weightHistory : List<Map<String, BigDecimal>>",
], [
    "+GameSession(name : String, difficulty : Difficulty, seed : long)",
    "+closeTurn() : void",
    "+nextVersion() : long",
    "+getCurrentTurn() : int",
    "+getPortfolio() : Portfolio",
    "+getMarket() : Market",
    "+getNewsService() : NewsService",
    "+isEnded() : boolean",
    "+snapshot() : GameSnapshot",
], tags="F-01, F-03")
box("Portfolio", "Portfolio", [
    "_+MAX_QUANTITY : int = 99999",
    "-cash : BigDecimal {cash >= 0}",
    "-holdings : Map<String, Holding>",
    "-trades : List<Trade>",
], [
    "+buy(stock : Stock, qty : int, turn : int) : Trade",
    "+sell(stock : Stock, qty : int, turn : int) : Trade",
    "+getCash() : BigDecimal",
    "+totalAssets(market : Market) : BigDecimal",
    "+weights(market : Market) : Map<String, BigDecimal>",
], tags="F-04, F-05, F-06")
box("Holding", "Holding", [
    "-stockId : String",
    "-quantity : int",
    "-costBasis : BigDecimal",
    "-realizedPnl : BigDecimal",
], [
    "+averagePrice() : BigDecimal",
    "+add(qty : int, amount : BigDecimal) : void",
    "+remove(qty : int) : BigDecimal",
    "+unrealizedPnl(price : long) : BigDecimal",
])
box("Trade", "Trade", [
    "-turn : int",
    "-stockId : String",
    "-side : OrderSide",
    "-quantity : int",
    "-price : long",
    "-realizedPnl : BigDecimal",
], ["+getAmount() : long"], tags="F-08")
box("Market", "Market", [
    "_+MAX_RATE : BigDecimal = 0.20",
    "-stocks : Map<String, Stock>",
], [
    "_+createStocks() : List<Stock>",
    "+getStock(id : String) : Stock",
    "+getStocks() : List<Stock>",
    "+applyTurn(draw : TurnDraw, newsEffects :",
    "    Map<String, BigDecimal>, factor : BigDecimal) : void",
], tags="F-02, F-03")
box("Stock", "Stock", [
    "-id : String",
    "-name : String",
    "-sector : Sector",
    "-risk : RiskLevel",
    "-description : String",
    "-priceHistory : List<Long> {price >= 1}",
], [
    "+currentPrice() : long",
    "+changeRate() : BigDecimal",
    "+updatePrice(rate : BigDecimal) : long",
    "+getPriceHistory() : List<Long>",
], tags="F-02, F-08")
box("NewsService", "NewsService", [
    "-difficulty : Difficulty",
    "-draws : List<TurnDraw>",
    "-published : List<News>",
], [
    "+NewsService(seed : long, difficulty : Difficulty)",
    "+drawFor(turn : int) : TurnDraw",
    "+publish(turn : int) : void",
    "+currentNews() : News",
    "+pastNews() : List<News>",
    "+effectsFor(turn : int) : Map<String, BigDecimal>",
], tags="F-07")
box("TurnDraw", "TurnDraw", [
    "-turn : int",
    "-marketRate : BigDecimal",
    "-sectorRates : Map<Sector, BigDecimal>",
    "-companyRates : Map<String, BigDecimal>",
    "-news : News",
], ["+getNews() : News"])
box("News", "News", [
    "-event : NewsEvent",
    "-publishedTurn : int",
    "-applyTurn : int",
    "-effects : Map<String, BigDecimal>",
], [
    "+isDelayed() : boolean",
    "+hint(difficulty : Difficulty) : String",
], tags="F-07")
box("Snapshot", "GameSnapshot", [
    "-sessionId : UUID",
    "-playerName : String",
    "-difficulty : Difficulty",
    "-state : GameState",
    "-completedTurns : int",
    "-cash : BigDecimal",
    "-stocks : List<Stock>",
    "-holdings : List<Holding>",
    "-trades : List<Trade>",
    "-assetHistory : List<BigDecimal>",
    "-weightHistory : List<Map<String, BigDecimal>>",
], [
    "+isCompleted() : boolean",
    "+findStock(id : String) : Stock",
    "+quantityOf(id : String) : int",
])

# ---------------------------------------------------------------- service
box("Evaluator", "ResultEvaluator", [], [
    "+evaluate(snapshot : GameSnapshot) : GameResult",
    "-decideEnding(rate : BigDecimal) : Ending",
    "-decideType(snapshot : GameSnapshot) : InvestorType",
    "-findAchievements(snapshot : GameSnapshot) : List<Achievement>",
    "-maxDrawdown(history : List<BigDecimal>) : BigDecimal",
], tags="FN-04 / F-09, F-10")
box("GameResult", "GameResult", [
    "-sessionId : UUID",
    "-playerName : String",
    "-difficulty : Difficulty",
    "-finalAssets : BigDecimal",
    "-returnRate : BigDecimal",
    "-realizedPnl : BigDecimal",
    "-unrealizedPnl : BigDecimal",
    "-maxDrawdown : BigDecimal",
    "-bestStockId : String",
    "-tradeCount : int",
    "-ending : Ending",
    "-investorType : InvestorType",
    "-achievements : List<Achievement>",
], ["+getTitle() : String"], tags="F-09")

# ---------------------------------------------------------------- persistence
box("RecordStore", "RecordStore", [], [
    "+saveResult(result : GameResult) : SaveResult",
    "+readRecords(name : String, difficulty : Difficulty) : RecordEntry",
], stereo="interface", compact=False, tags="F-10")
box("Repository", "RecordRepository", [
    "-file : Path",
], [
    "+RecordRepository(file : Path)",
    "_+defaultPath() : Path",
    "+saveResult(result : GameResult) : SaveResult",
    "+readRecords(name : String, difficulty : Difficulty) : RecordEntry",
    "-loadAll() : List<RecordEntry>",
    "-writeAtomically(entries : List<RecordEntry>) : void",
], tags="FN-05")
box("RecordEntry", "RecordEntry", [
    "-playerName : String",
    "-difficulty : Difficulty",
    "-bestAssets : BigDecimal",
    "-bestReturnRate : BigDecimal",
    "-playCount : int",
    "-endings : Set<Ending>",
    "-achievements : Map<Achievement, Integer>",
    "-savedSessions : Set<UUID>",
], ["+merge(result : GameResult) : boolean"])

# ---------------------------------------------------------------- model (enum)
ENUMS = [
    ("Difficulty", ["EASY", "NORMAL", "HARD", "-volatilityFactor : BigDecimal"]),
    ("GameState", ["READY", "UPDATING", "ENDED"]),
    ("OrderSide", ["BUY", "SELL"]),
    ("Sector", ["TECH", "ENERGY", "TRANSPORT"]),
    ("RiskLevel", ["LOW", "MEDIUM", "HIGH"]),
    ("NewsEvent", ["TECH_SUCCESS", "TECH_DEFECT", "ENERGY_PRICE_UP",
                   "GREEN_POWER", "LOGISTICS_DEMAND", "ECONOMY_SLOWDOWN"]),
    ("Ending", ["PROFIT", "MAINTAIN", "RETRY"]),
    ("InvestorType", ["CONCENTRATED", "STABLE", "AGGRESSIVE", "DIVERSIFIED", "BALANCED"]),
    ("Achievement", ["NO_LOSS", "LONG_HOLD", "DIVERSIFIED"]),
]
for name, vals in ENUMS:
    box("E_" + name, name, vals, [], stereo="enumeration")
    B["E_" + name].ops = []

# ================================================================ layout
G = 50
# view 패키지
TOP = 140
B["App"].at(60, TOP)
B["JFrame"].at(380, TOP)
B["MainFrame"].below(B["JFrame"], 50)
B["JPanel"].at(B["MainFrame"].x + B["MainFrame"].w + 70, TOP)
B["StartPanel"].below(B["JPanel"], 50)
B["Chart"].below(B["StartPanel"], 30)
B["JDialog"].at(B["Chart"].x + B["Chart"].w + 260, TOP)
B["OrderDialog"].at(B["JDialog"].x - 250, B["JDialog"].y + 110)
B["ResultDialog"].right(B["OrderDialog"], 30)
B["RecordDialog"].right(B["ResultDialog"], 30)
VIEW_BOTTOM = max(b.y + b.h for k, b in B.items() if k in
                  ("App", "MainFrame", "Chart", "OrderDialog", "ResultDialog", "RecordDialog"))

# controller 패키지
CY = VIEW_BOTTOM + 130
B["Controller"].at(380, CY)
B["ViewData"].right(B["Controller"], 80)
B["ActionResult"].right(B["ViewData"], 120)
SY = B["ActionResult"].y + B["ActionResult"].h + 60
B["StartResult"].at(B["ActionResult"].x - 260, SY)
B["TradeResult"].right(B["StartResult"], 30)
B["TurnResult"].right(B["TradeResult"], 30)
B["SaveResult"].right(B["TurnResult"], 30)
CTRL_BOTTOM = max(B[k].y + B[k].h for k in ("Controller", "ViewData", "StartResult", "TradeResult", "TurnResult", "SaveResult"))

# domain 패키지
DY = CTRL_BOTTOM + 130
B["Session"].at(60, DY)
B["Portfolio"].right(B["Session"], 90)
B["Holding"].below(B["Portfolio"], 70)
B["Trade"].right(B["Holding"], 40)
B["Market"].at(max(B["Portfolio"].x + B["Portfolio"].w, B["Trade"].x + B["Trade"].w) + 90, DY)
B["Stock"].below(B["Market"], 70)
B["NewsService"].right(B["Market"], 90)
B["TurnDraw"].below(B["NewsService"], 70)
B["News"].right(B["TurnDraw"], 90)
B["Snapshot"].below(B["Session"], 70)
DOM_BOTTOM = max(B[k].y + B[k].h for k in ("Session", "Snapshot", "Holding", "Trade", "Stock", "TurnDraw", "News"))

# service / persistence 패키지 (domain 아래)
PY = DOM_BOTTOM + 130
B["Evaluator"].at(60, PY)
B["GameResult"].below(B["Evaluator"], 60)
B["RecordStore"].right(B["Evaluator"], 160)
B["Repository"].below(B["RecordStore"], 70)
B["RecordEntry"].below(B["Repository"], 60)

# model 패키지 (enum)
ex = B["RecordStore"].x + max(B["RecordStore"].w, B["Repository"].w) + 160
ey = PY
col_w = 0
row_x = ex
for i, (name, _) in enumerate(ENUMS):
    b = B["E_" + name]
    if i and i % 3 == 0:
        row_x += col_w + 30
        ey = PY
        col_w = 0
    b.at(row_x, ey)
    ey += b.h + 30
    col_w = max(col_w, b.w)

# ================================================================ relations
# kind: gen, real, assoc, dep, comp, aggr
R = []


def rel(kind, a, aside, b, bside, ma=None, mb=None, label=None, af=0.5, bf=0.5, via=None):
    R.append(dict(kind=kind, a=a, aside=aside, b=b, bside=bside, ma=ma, mb=mb,
                  label=label, af=af, bf=bf, via=via or []))


# 일반화 (자식 -> 부모)
rel("gen", "MainFrame", "top", "JFrame", "bottom")
rel("gen", "StartPanel", "top", "JPanel", "bottom")
rel("gen", "OrderDialog", "top", "JDialog", "bottom", af=0.5, bf=0.3)
rel("gen", "ResultDialog", "top", "JDialog", "bottom", bf=0.5)
rel("gen", "RecordDialog", "top", "JDialog", "bottom", af=0.4, bf=0.7)
for k, f in (("StartResult", 0.2), ("TradeResult", 0.4), ("TurnResult", 0.6), ("SaveResult", 0.8)):
    rel("gen", k, "top", "ActionResult", "bottom", bf=f)
rel("gen", "Chart", "right", "JPanel", "right", via=[(B["Chart"].x + B["Chart"].w + 30, B["Chart"].y + B["Chart"].h / 2),
                                                     (B["Chart"].x + B["Chart"].w + 30, B["JPanel"].y + B["JPanel"].h / 2)])
# 실체화
rel("real", "Repository", "top", "RecordStore", "bottom")
# 합성 (전체 -> 부분, 다이아몬드는 전체 쪽)
rel("comp", "MainFrame", "right", "StartPanel", "left", ma="1", mb="1", af=0.15)
rel("comp", "MainFrame", "right", "Chart", "left", ma="1", mb="1", af=0.6)
rel("comp", "Session", "right", "Portfolio", "left", ma="1", mb="1", af=0.1, bf=0.2)
rel("comp", "Session", "top", "Market", "top", ma="1", mb="1", af=0.6, bf=0.4,
    via=[(B["Session"].x + B["Session"].w * 0.6, DY - 30), (B["Market"].x + B["Market"].w * 0.4, DY - 30)])
rel("comp", "Session", "top", "NewsService", "top", ma="1", mb="1", af=0.8, bf=0.4,
    via=[(B["Session"].x + B["Session"].w * 0.8, DY - 55), (B["NewsService"].x + B["NewsService"].w * 0.4, DY - 55)])
rel("comp", "Portfolio", "bottom", "Holding", "top", ma="1", mb="0..*", af=0.3)
rel("comp", "Portfolio", "bottom", "Trade", "top", ma="1", mb="0..*", af=0.8, bf=0.3,
    via=[(B["Portfolio"].x + B["Portfolio"].w * 0.8, B["Trade"].y - 30), (B["Trade"].x + B["Trade"].w * 0.3, B["Trade"].y - 30)])
rel("comp", "Market", "bottom", "Stock", "top", ma="1", mb="6")
rel("comp", "NewsService", "bottom", "TurnDraw", "top", ma="1", mb="30")
rel("comp", "Repository", "bottom", "RecordEntry", "top", ma="1", mb="0..*")
# 집합 (공개된 뉴스 목록: 뉴스는 추첨 자료와 별개로 유지)
rel("aggr", "NewsService", "right", "News", "top", ma="1", mb="0..*", af=0.7, bf=0.5,
    via=[(B["News"].x + B["News"].w * 0.5, B["NewsService"].y + B["NewsService"].h * 0.7)])
# 연관 (단방향)
rel("assoc", "App", "right", "MainFrame", "left", ma="1", mb="1", af=0.5, bf=0.12)
rel("assoc", "MainFrame", "bottom", "Controller", "top", ma="*", mb="1", af=0.3, bf=0.3, label="controller")
rel("assoc", "Controller", "bottom", "Session", "top", ma="1", mb="0..1", af=0.15, bf=0.3, label="session",
    via=[(B["Controller"].x + B["Controller"].w * 0.15, DY - 80), (B["Session"].x + B["Session"].w * 0.3, DY - 80)])
rel("assoc", "Controller", "left", "Evaluator", "left", ma="1", mb="1", af=0.8, bf=0.3, label=None,
    via=[(30, B["Controller"].y + B["Controller"].h * 0.8), (30, B["Evaluator"].y + B["Evaluator"].h * 0.3)])
rel("assoc", "Controller", "bottom", "RecordStore", "top", ma="1", mb="1", af=0.85, bf=0.5, label="store",
    via=[(B["Controller"].x + B["Controller"].w * 0.85, DY - 105),
         (B["News"].x + B["News"].w + 60, DY - 105),
         (B["News"].x + B["News"].w + 60, PY - 50),
         (B["RecordStore"].x + B["RecordStore"].w * 0.5, PY - 50)])
rel("assoc", "TurnDraw", "right", "News", "left", ma="1", mb="0..1", af=0.5, bf=0.5)
# 의존 (파라미터, 반환, 생성)
_cb = B["Chart"].y + B["Chart"].h + 25
rel("dep", "MainFrame", "bottom", "OrderDialog", "bottom", af=0.9, bf=0.5, label="<<create>>",
    via=[(B["MainFrame"].x + B["MainFrame"].w * 0.9, _cb),
         (B["OrderDialog"].x + B["OrderDialog"].w * 0.5, _cb)])
rel("dep", "Controller", "right", "ViewData", "left", label="<<create>>")
rel("dep", "ViewData", "right", "ActionResult", "left", label="")
rel("dep", "Session", "bottom", "Snapshot", "top", label="<<create>>")
rel("dep", "Evaluator", "bottom", "GameResult", "top", label="<<create>>")
rel("dep", "Evaluator", "top", "Snapshot", "bottom", af=0.5, bf=0.5, label="uses")
rel("dep", "Portfolio", "right", "Stock", "left", af=0.8, bf=0.3, label="uses")
rel("dep", "Repository", "left", "GameResult", "right", af=0.6, bf=0.3, label="uses")

# ActionResult 의존은 의미상 TradeResult 등이 ViewData 를 가짐: ViewData -> ActionResult 대신 결과가 ViewData 보유로 교체
R = [r for r in R if not (r["a"] == "ViewData" and r["b"] == "ActionResult")]
rel("dep", "Controller", "top", "ActionResult", "top", af=0.9, bf=0.5, label="<<create>>",
    via=[(B["Controller"].x + B["Controller"].w * 0.9, CY - 45),
         (B["ActionResult"].x + B["ActionResult"].w * 0.5, CY - 45)])

# ================================================================ packages
PKGS = [
    ("view", ["App", "JFrame", "MainFrame", "JPanel", "StartPanel", "Chart", "JDialog",
              "OrderDialog", "ResultDialog", "RecordDialog"]),
    ("controller", ["Controller", "ViewData", "ActionResult", "StartResult", "TradeResult",
                    "TurnResult", "SaveResult"]),
    ("domain", ["Session", "Portfolio", "Holding", "Trade", "Market", "Stock", "NewsService",
                "TurnDraw", "News", "Snapshot"]),
    ("service", ["Evaluator", "GameResult"]),
    ("persistence", ["RecordStore", "Repository", "RecordEntry"]),
    ("model", ["E_" + n for n, _ in ENUMS]),
]


def pkg_rect(keys, pad=24):
    xs = [B[k].x for k in keys]
    ys = [B[k].y for k in keys]
    xe = [B[k].x + B[k].w for k in keys]
    ye = [B[k].y + B[k].h for k in keys]
    return min(xs) - pad, min(ys) - pad - 10, max(xe) - min(xs) + pad * 2, max(ye) - min(ys) + pad * 2 + 10


# ================================================================ SVG
def esc(s):
    return html.escape(s, quote=True)


def svg_box(b: Box):
    out = []
    out.append(f'<rect x="{b.x}" y="{b.y}" width="{b.w}" height="{b.h}" fill="#FFFFFF" stroke="#1B2430" stroke-width="1.2"/>')
    y = b.y + 4
    if b.stereo:
        y += 12
        out.append(f'<text x="{b.x + b.w / 2}" y="{y}" class="stereo">&lt;&lt;{esc(b.stereo)}&gt;&gt;</text>')
    y += 17
    cls = "cname italic" if b.abstract else "cname"
    out.append(f'<text x="{b.x + b.w / 2}" y="{y}" class="{cls}">{esc(b.name)}</text>')
    if b.tags:
        out.append(f'<text x="{b.x + b.w}" y="{b.y - 5}" class="tag">{esc(b.tags)}</text>')
    if b.compact:
        return "\n".join(out)
    y = b.y + NAME_H + (STEREO_H if b.stereo else 0)
    out.append(f'<line x1="{b.x}" y1="{y}" x2="{b.x + b.w}" y2="{y}" stroke="#1B2430" stroke-width="1"/>')
    ty = y + 4
    for a in b.attrs:
        ty += LINE_H
        out.append(_member(b, a, ty))
    if not b.attrs:
        ty += LINE_H
    y2 = ty + 8 if b.attrs else y + LINE_H + 8
    if b.stereo == "enumeration":
        return "\n".join(out)
    out.append(f'<line x1="{b.x}" y1="{y2 - 2}" x2="{b.x + b.w}" y2="{y2 - 2}" stroke="#1B2430" stroke-width="1"/>')
    ty = y2 + 2
    for o in b.ops:
        ty += LINE_H
        out.append(_member(b, o, ty))
    return "\n".join(out)


def _member(b, s, y):
    static = s.startswith("_")
    t = s[1:] if static else s
    deco = ' text-decoration="underline"' if static else ""
    return f'<text x="{b.x + PAD_X}" y="{y - 4}" class="mem"{deco}>{esc(t)}</text>'


def _pts(r):
    a, b = B[r["a"]], B[r["b"]]
    p0 = a.anchor(r["aside"], r["af"])
    p1 = b.anchor(r["bside"], r["bf"])
    return [p0] + list(r["via"]) + [p1]


def _unit(p, q):
    dx, dy = q[0] - p[0], q[1] - p[1]
    d = math.hypot(dx, dy) or 1
    return dx / d, dy / d


def svg_rel(r):
    pts = _pts(r)
    k = r["kind"]
    dash = ' stroke-dasharray="7 5"' if k in ("dep", "real") else ""
    path = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
    out = [f'<polyline points="{path}" fill="none" stroke="#1B2430" stroke-width="1.2"{dash}/>']
    end, prev = pts[-1], pts[-2]
    ux, uy = _unit(prev, end)
    px, py = -uy, ux
    if k in ("gen", "real"):
        s = 14
        bx, by = end[0] - ux * s, end[1] - uy * s
        tri = [end, (bx + px * 8, by + py * 8), (bx - px * 8, by - py * 8)]
        out.append('<polygon points="%s" fill="#FFFFFF" stroke="#1B2430" stroke-width="1.2"/>' %
                   " ".join(f"{x:.1f},{y:.1f}" for x, y in tri))
    if k in ("assoc", "dep"):
        s = 11
        bx, by = end[0] - ux * s, end[1] - uy * s
        out.append(f'<polyline points="{bx + px * 6:.1f},{by + py * 6:.1f} {end[0]:.1f},{end[1]:.1f} {bx - px * 6:.1f},{by - py * 6:.1f}" fill="none" stroke="#1B2430" stroke-width="1.2"/>')
    if k in ("comp", "aggr"):
        st, nx = pts[0], pts[1]
        vx, vy = _unit(st, nx)
        qx, qy = -vy, vx
        L = 16
        dia = [st, (st[0] + vx * L / 2 + qx * 6, st[1] + vy * L / 2 + qy * 6),
               (st[0] + vx * L, st[1] + vy * L), (st[0] + vx * L / 2 - qx * 6, st[1] + vy * L / 2 - qy * 6)]
        fill = "#1B2430" if k == "comp" else "#FFFFFF"
        out.append('<polygon points="%s" fill="%s" stroke="#1B2430" stroke-width="1.2"/>' %
                   (" ".join(f"{x:.1f},{y:.1f}" for x, y in dia), fill))
        # 부분 쪽 방향 화살표 (탐색 방향)
        s = 11
        bx, by = end[0] - ux * s, end[1] - uy * s
        out.append(f'<polyline points="{bx + px * 6:.1f},{by + py * 6:.1f} {end[0]:.1f},{end[1]:.1f} {bx - px * 6:.1f},{by - py * 6:.1f}" fill="none" stroke="#1B2430" stroke-width="1.2"/>')
    # 다중성
    if r["ma"]:
        st, nx = pts[0], pts[1]
        vx, vy = _unit(st, nx)
        off = 26 if k in ("comp", "aggr") else 12
        out.append(f'<text x="{st[0] + vx * off - vy * 10 + 3:.1f}" y="{st[1] + vy * off + vx * 10 + 4:.1f}" class="mult">{esc(r["ma"])}</text>')
    if r["mb"]:
        anchor = ' text-anchor="end"' if ux > 0.5 else ""
        out.append(f'<text x="{end[0] - ux * 18 - uy * 10 + 3:.1f}" y="{end[1] - uy * 18 + ux * 10 + 4:.1f}" class="mult"{anchor}>{esc(r["mb"])}</text>')
    if r["label"]:
        # 가장 긴 구간 중앙
        best = max(range(len(pts) - 1), key=lambda i: math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1]))
        p, q = pts[best], pts[best + 1]
        mx, my = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2
        out.append(f'<text x="{mx + 6:.1f}" y="{my - 5:.1f}" class="lbl">{esc(r["label"])}</text>')
    return "\n".join(out)


def build_svg():
    all_x = [b.x + b.w for b in B.values()]
    all_y = [b.y + b.h for b in B.values()]
    W = int(max(all_x) + 80)
    H = int(max(all_y) + 160)
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
             '<style>'
             'text{font-family:Consolas,"Malgun Gothic",monospace;fill:#1B2430}'
             '.cname{font-family:"Segoe UI","Malgun Gothic",sans-serif;font-weight:700;font-size:14px;text-anchor:middle}'
             '.italic{font-style:italic}'
             '.stereo{font-size:11px;text-anchor:middle}'
             '.mem{font-size:12px}'
             '.mult{font-size:12px;font-weight:700}'
             '.lbl{font-size:11px;font-style:italic;fill:#3B4656}'
             '.tag{font-family:"Malgun Gothic",sans-serif;font-size:11px;font-weight:700;fill:#1F5FD1;text-anchor:end}'
             '.pkg{font-family:"Segoe UI",sans-serif;font-size:14px;font-weight:700}'
             '.title{font-family:"Segoe UI","Malgun Gothic",sans-serif;font-size:22px;font-weight:700}'
             '.legend{font-family:"Malgun Gothic",sans-serif;font-size:12px}'
             '</style>',
             f'<rect width="{W}" height="{H}" fill="#FFFFFF"/>',
             '<text x="40" y="44" class="title">슈퍼개미 Class Diagram (package superant)%s</text>' % (" - 전체 구조" if MODE == "overview" else " - 상세")]
    for name, keys in PKGS:
        x, y, w, h = pkg_rect(keys)
        tw = len(name) * 9 + 24
        parts.append(f'<rect x="{x}" y="{y - 22}" width="{tw}" height="22" fill="#F1F4F8" stroke="#6B7686" stroke-width="1"/>')
        parts.append(f'<text x="{x + 10}" y="{y - 6}" class="pkg">{esc(name)}</text>')
        parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#F7F9FC" stroke="#6B7686" stroke-width="1"/>')
    for r in R:
        parts.append(svg_rel(r))
    for b in B.values():
        parts.append(svg_box(b))
    ly = H - 70
    parts.append(f'<text x="60" y="{ly}" class="legend">파란 글씨: 연결 기능(F), 함수 명세(FN), 화면(UI) 번호 · 밑줄: static · {{ }} : 제약 · 실선 화살표: 연관 · 점선 화살표: 의존 · 빈 삼각형: 일반화 · 점선 빈 삼각형: 실체화 · 검은 마름모: 합성 · 흰 마름모: 집합</text>')
    parts.append("</svg>")
    return "\n".join(parts), W, H


# ================================================================ UMLet (.uxf)
def uxf_box(b: Box):
    lines = []
    if b.stereo:
        lines.append("&lt;&lt;" + esc(b.stereo) + "&gt;&gt;")
    nm = esc(b.name)
    lines.append(("/" + nm + "/") if b.abstract else ("*" + nm + "*"))
    if not b.compact:
        lines.append("--")
        for a in b.attrs:
            lines.append(("_" + esc(a[1:]) + "_") if a.startswith("_") else esc(a))
        if b.stereo != "enumeration":
            lines.append("--")
            for o in b.ops:
                lines.append(("_" + esc(o[1:]) + "_") if o.startswith("_") else esc(o))
    if b.tags:
        lines.append("bg=white")
    attrs = "\n".join(lines)
    return (f"<element><id>UMLClass</id><coordinates><x>{int(b.x)}</x><y>{int(b.y)}</y>"
            f"<w>{int(b.w)}</w><h>{int(b.h)}</h></coordinates>"
            f"<panel_attributes>{attrs}</panel_attributes><additional_attributes/></element>")


LT = {"gen": "&lt;&lt;-", "real": "&lt;&lt;.", "assoc": "&lt;-", "dep": "&lt;.",
      "comp": "&lt;-&lt;&lt;&lt;&lt;&lt;-", "aggr": "&lt;-&lt;&lt;&lt;&lt;-"}


def uxf_rel(r):
    pts = _pts(r)
    k = r["kind"]
    # UMLet: lt 의 왼쪽 기호가 첫 점. 화살표 끝(부모/대상)을 첫 점으로 둔다.
    if k in ("gen", "real", "assoc", "dep"):
        seq = list(reversed(pts))
        m1, m2 = r["mb"], r["ma"]
        lt = LT[k]
    else:
        # 합성/집합: 첫 점 = 부분(화살표), 마지막 = 전체(마름모)
        seq = list(reversed(pts))
        m1, m2 = r["mb"], r["ma"]
        lt = "&lt;-&lt;&lt;&lt;&lt;&lt;" if k == "comp" else "&lt;-&lt;&lt;&lt;&lt;"
        lt = "&lt;&lt;&lt;&lt;&lt;-" if False else lt
    xs = [p[0] for p in seq]
    ys = [p[1] for p in seq]
    x0, y0 = min(xs) - 10, min(ys) - 10
    w, h = max(xs) - x0 + 10, max(ys) - y0 + 10
    rel_pts = ";".join(f"{p[0] - x0:.0f};{p[1] - y0:.0f}" for p in seq)
    pa = ["lt=" + lt]
    if m1:
        pa.append("m1=" + esc(m1))
    if m2:
        pa.append("m2=" + esc(m2))
    if r["label"]:
        pa.append(esc(r["label"]))
    return (f"<element><id>Relation</id><coordinates><x>{x0:.0f}</x><y>{y0:.0f}</y>"
            f"<w>{w:.0f}</w><h>{h:.0f}</h></coordinates>"
            f"<panel_attributes>{chr(10).join(pa)}</panel_attributes>"
            f"<additional_attributes>{rel_pts}</additional_attributes></element>")


def uxf_pkg(name, keys):
    x, y, w, h = pkg_rect(keys)
    return (f"<element><id>UMLPackage</id><coordinates><x>{x:.0f}</x><y>{y - 22:.0f}</y>"
            f"<w>{w:.0f}</w><h>{h + 22:.0f}</h></coordinates>"
            f"<panel_attributes>{esc(name)}\nbg=#F7F9FC</panel_attributes><additional_attributes/></element>")


def build_uxf():
    parts = ['<?xml version="1.0" encoding="UTF-8" standalone="no"?>', '<diagram program="umlet" version="15.1">',
             "<zoom_level>10</zoom_level>"]
    for name, keys in PKGS:
        parts.append(uxf_pkg(name, keys))
    for b in B.values():
        parts.append(uxf_box(b))
    for r in R:
        parts.append(uxf_rel(r))
    parts.append("</diagram>")
    return "\n".join(parts)


if __name__ == "__main__":
    svg, W, H = build_svg()
    stem = "class_diagram_overview" if MODE == "overview" else "class_diagram"
    (OUT / (stem + ".svg")).write_text(svg, encoding="utf-8")
    (OUT / (stem + ".uxf")).write_text(build_uxf(), encoding="utf-8")
    import json
    (OUT / (stem + "_packages.json")).write_text(json.dumps({n: pkg_rect(k) for n, k in PKGS}), encoding="utf-8")
    print(W, H)
