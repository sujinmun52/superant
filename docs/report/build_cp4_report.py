"""CP3 제출본(2단 표수정본)을 복사해 CP4 보고서를 만든다.

- 기능 F-01~10 과 유스케이스 UC-01~10 을 일대일로 맞춘다 (UC-10A/10B -> UC-10).
- 게임 이름 '슈퍼개미' 반영.
- IX Class Diagram, X Screenshot of API Manual, XI Screenshot of Beta Release, XII GitHub URL 추가.
- assets/ 폴더에 스크린샷이 있으면 넣고, 없으면 자리 표시 문구를 넣는다.
"""
from copy import deepcopy
from pathlib import Path

import docx
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Inches, Pt
from docx.table import Table
from docx.text.paragraph import Paragraph

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]                       # 소시설 폴더
SRC = ROOT / "ssd_ip_cp3_2026f_244489_2단_표수정본.docx"
OUT = ROOT / "ssd_ip_cp4_2026f_244489_report.docx"
DIAG = HERE.parent / "diagrams"
ASSETS = HERE / "assets"
GITHUB_URL = (HERE / "github_url.txt").read_text(encoding="utf-8").strip() if (HERE / "github_url.txt").exists() else "https://github.com/[작성 예정]"

d = docx.Document(str(SRC))
body = d.element.body
els = list(body.iterchildren())
P = lambda i: Paragraph(els[i], d)
T = lambda i: Table(els[i], d)


def set_text(p, text):
    runs = p.runs
    if not runs:
        p.add_run(text)
        return
    runs[0].text = text
    for r in runs[1:]:
        r._r.getparent().remove(r._r)


def set_cell(cell, lines):
    """셀 문단을 lines 로 교체 (첫 문단 서식 유지)."""
    paras = cell.paragraphs
    proto = deepcopy(paras[0]._p)
    for p in paras[1:]:
        p._p.getparent().remove(p._p)
    set_text(paras[0], lines[0])
    anchor = paras[0]._p
    for line in lines[1:]:
        np_ = deepcopy(proto)
        anchor.addnext(np_)
        set_text(Paragraph(np_, cell), line)
        anchor = np_


def replace_everywhere(old, new):
    for p in d.paragraphs:
        if old in p.text:
            for r in p.runs:
                if old in r.text:
                    r.text = r.text.replace(old, new)
    for t in d.tables:
        for row in t.rows:
            for c in row.cells:
                for p in c.paragraphs:
                    for r in p.runs:
                        if old in r.text:
                            r.text = r.text.replace(old, new)


# ------------------------------------------------------------------ 기존 내용 수정
set_text(P(0), "슈퍼개미: 가상 주식 투자 시뮬레이션 게임")
set_text(P(9), "5. Simple Game Program. 슈퍼개미는 가상 기업의 주식을 매매하며 30턴 안에 목표 수익을 "
               "달성하는 1인용 투자 시뮬레이션 게임이다. 개인 투자자 '개미'가 큰손 '슈퍼개미'로 성장한다는 "
               "의미로 이름을 정했다. Java Swing의 표·버튼·차트로 구현한다.")
set_text(P(12), "실시간 모의투자와 달리 가상 기업, 제한된 턴, 난이도, 엔딩·업적을 제공한다. 종목 분산과 현금 "
                "보유에 따라 위험이 달라진다. 결과는 수익률에 따라 여왕개미 등극(50% 이상은 슈퍼개미 등극), "
                "일개미 생존, 개미굴로 귀환 칭호로 보여 주고, 게임 중에도 수익률에 따라 칭호가 오르내린다. "
                "최고 기록을 저장하여 반복 플레이를 지원한다. 외부 시세 연동, 온라인 기능, "
                "진행 중 저장은 범위에서 제외한다.")
set_text(P(14), "CP2의 기능 F-01~10과 세부 요구사항 FR 번호를 유지한다. 기능 10개는 IV의 유스케이스 "
                "UC-01~10과 일대일로 대응하며, 객체 간 처리는 VI의 시퀀스에, 구현 구조는 IX의 클래스 "
                "다이어그램에 기술한다.")
set_text(P(55), "CP2의 UC-01~10을 유지하며 기능 F-01~10과 일대일로 대응한다. UC-10은 기록 저장과 기존 "
                "기록 조회를 하나의 목표(기록 관리)로 다루고, 조회는 대안 흐름으로 기술한다. 조회는 게임 "
                "상태를 변경하지 않으며 실패·취소는 기존 상태를 보존한다.")

# UC-10A -> UC-10, UC-10B 삭제
set_text(P(74), "UC-10 기록 저장 및 조회")
uc10 = T(75)
rows = {r.cells[0].text.strip(): r.cells[1] for r in uc10.rows}
set_cell(rows["관련 요구사항"], ["FR-10-2~3"])
set_cell(rows["목적"], ["완료한 게임 기록 보존과 이전 기록 확인"])
set_cell(rows["사전 조건"], ["저장: 최종 결과가 있고 미저장 또는 저장 실패 상태다.",
                            "조회: 프로그램 실행 중이면 새 게임 없이 가능하다."])
set_cell(rows["시작 이벤트"], ["기록 저장 또는 기록 보기 선택"])
set_cell(rows["기본 흐름"], ["1. 시스템이 기존 기록을 읽는다.",
                            "2. 시스템이 최고 기록·엔딩·업적을 병합한다.",
                            "3. 시스템이 파일을 저장하고 완료를 표시한다."])
set_cell(rows["대안·예외"], ["1a. 파일 없음: 신규 생성.",
                            "1b. 손상 또는 3a. 저장 실패: 이전 기록과 결과를 보존하고 재시도 안내.",
                            "2a. 같은 게임 재저장: 중복 추가 없음.",
                            "1c. 기록 보기 선택: 이름·난이도로 기록을 읽어 표시한다. 기록 없음은 빈 기록, "
                            "읽기 실패는 재시도를 안내한다."])
set_cell(rows["사후 조건"], ["재실행 후 기록을 조회할 수 있다. 조회는 파일을 변경하지 않는다."])
for i in (76, 77):
    body.remove(els[i])

# 유스케이스 다이어그램 교체
img_p = els[80]
blip = img_p.xpath(".//a:blip")[0]
rid = blip.get(qn("r:embed"))
part = d.part.related_parts[rid]
part._blob = (DIAG / "usecase_diagram.png").read_bytes()
ext = img_p.xpath(".//wp:extent")[0]
cx = int(ext.get("cx"))
cy = int(cx * 2000 / 1440)
ext.set("cy", str(cy))
for e in img_p.xpath(".//a:ext"):
    e.set("cy", str(cy))
set_text(P(81), "단일 플레이어와 10개 사용자 목표")
set_text(P(82), "선은 플레이어의 참여 관계다. 주가 계산·뉴스 추첨은 내부 책임이므로 외부 액터로 두지 않는다. "
                "UC-10은 기록 저장과 조회를 함께 다루며, 조회는 새 게임 없이도 실행할 수 있다.")
set_text(P(84), "GameView는 입력·표시를, GameController는 검증·처리 순서를 담당한다. GameSession은 확정 상태, "
                "Portfolio는 거래·원가, Market은 가격, NewsService는 뉴스, ResultEvaluator는 평가, "
                "RecordRepository는 파일 저장을 맡는다. 상태는 private으로 보호하고 메서드로 변경한다. "
                "구현에서 GameView는 MainFrame과 대화상자 클래스로 나뉜다(IX 참고).")
set_text(P(99), "SD-04 UC-09·10 (조회는 별도 요청)")
replace_everywhere("기록 보기는 UC-10B다", "기록 보기는 UC-10의 조회 흐름이다")
replace_everywhere("UC-10B의 기존 기록", "UC-10 조회 흐름의 기존 기록")
replace_everywhere("UC-10A / SD-04", "UC-10 / SD-04")
replace_everywhere("UC-10A", "UC-10")
replace_everywhere("UC-10B", "UC-10")

# ------------------------------------------------------------------ 섹션 나누기 (이후 가로, 1단)
final_sect = body.find(qn("w:sectPr"))
brk = d.add_paragraph()
brk._p.get_or_add_pPr().append(deepcopy(final_sect))
for t in final_sect.findall(qn("w:type")):
    final_sect.remove(t)
pg = final_sect.find(qn("w:pgSz"))
pg.set(qn("w:w"), "16834")
pg.set(qn("w:h"), "11909")
pg.set(qn("w:orient"), "landscape")
mar = final_sect.find(qn("w:pgMar"))
for k, v in (("top", "900"), ("bottom", "900"), ("left", "900"), ("right", "900")):
    mar.set(qn("w:" + k), v)
cols = final_sect.find(qn("w:cols"))
for a in list(cols.attrib):
    del cols.attrib[a]
cols.set(qn("w:space"), "720")

TEXT_W = Inches(10.4)
MAX_H = Inches(6.1)


def heading(text):
    return d.add_paragraph(text, style="Heading 1")


def para(text, bold=False):
    p = d.add_paragraph(style="Body Text")
    r = p.add_run(text)
    r.bold = bold
    return p


def figure(path, caption, max_w=TEXT_W, max_h=MAX_H):
    from PIL import Image
    w, h = Image.open(path).size
    width = max_w
    if width * h / w > max_h:
        width = int(max_h * w / h)
    p = d.add_paragraph(style="Body Text")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = 0
    p.add_run().add_picture(str(path), width=width)
    d.add_paragraph(caption, style="figure caption")


def placeholder(text):
    p = para("[" + text + "]", bold=True)
    return p


def table(header, rows, widths):
    t = d.add_table(rows=1 + len(rows), cols=len(header))
    from docx.oxml import parse_xml
    tblPr = t._tbl.tblPr
    tblPr.append(parse_xml(
        '<w:tblBorders xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
        + "".join(f'<w:{s} w:val="single" w:sz="4" w:space="0" w:color="000000"/>'
                  for s in ("top", "left", "bottom", "right", "insideH", "insideV"))
        + "</w:tblBorders>"))
    for i, h in enumerate(header):
        c = t.rows[0].cells[i]
        c.text = h
        c.paragraphs[0].runs[0].bold = True
    for ri, row in enumerate(rows, start=1):
        for ci, v in enumerate(row):
            t.rows[ri].cells[ci].text = v
    for row in t.rows:
        for ci, w in enumerate(widths):
            row.cells[ci].width = Inches(w)
            for p in row.cells[ci].paragraphs:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                p.paragraph_format.first_line_indent = 0
                for r in p.runs:
                    r.font.size = Pt(9)
    d.add_paragraph(style="Body Text")
    return t


# ------------------------------------------------------------------ IX Class Diagram
heading("Class Diagram")
para("구현 구조를 6개 패키지, 30개 클래스로 나눈다. 화면(view)은 입력과 표시만 담당하고 모든 요청은 "
     "GameController로 들어간다. 게임 규칙은 domain, 결과 판정은 service, 기록 파일은 persistence가 맡으며 "
     "열거형은 model에 둔다. 각 클래스 위의 파란 글씨는 연결된 기능(F), 함수 명세(FN), 화면(UI) 번호다. "
     "표기는 수업 자료의 UML 규칙(가시성 + - #, static 밑줄, 제약 { }, 다중성, 패키지)을 따른다.")
figure(DIAG / "class_diagram_overview.png", "Class Diagram 1: 전체 구조 (패키지와 관계)", max_h=Inches(5.2))
figure(DIAG / "class_detail_1_view_controller.png", "Class Diagram 2: view, controller 패키지 상세")
figure(DIAG / "class_detail_2_domain.png", "Class Diagram 3: domain 패키지 상세")
figure(DIAG / "class_detail_3_service_persistence_model.png", "Class Diagram 4: service, persistence, model 패키지 상세")

para("클래스와 기능의 대응", bold=True)
table(["기능", "화면", "진입 메서드 (함수 명세)", "처리 클래스와 메서드"], [
    ["F-01 게임 시작", "StartPanel, MainFrame", "GameController.startGame (FN-01)", "GameSession 생성자가 Portfolio, Market, NewsService를 합성으로 생성"],
    ["F-02 종목 정보", "MainFrame 종목 표", "GameController.getView", "Market.getStocks, Stock.changeRate"],
    ["F-03 턴 진행", "MainFrame 다음 턴", "GameController.advanceTurn (FN-03)", "NewsService.drawFor, effectsFor → Market.applyTurn → GameSession.closeTurn"],
    ["F-04 매수", "OrderDialog", "GameController.placeOrder (FN-02)", "Portfolio.buy → Holding.add, Trade 생성"],
    ["F-05 매도", "OrderDialog", "GameController.placeOrder (FN-02)", "Portfolio.sell → Holding.remove, Trade 생성"],
    ["F-06 자산 조회", "MainFrame 포트폴리오 탭", "GameController.getView", "Portfolio.totalAssets, weights"],
    ["F-07 뉴스 조회", "MainFrame 뉴스 피드", "GameController.getView", "NewsService.currentNews, pastNews, News.hint"],
    ["F-08 이력 조회", "PriceChartPanel, 거래 내역 탭", "GameController.getView", "Stock.getPriceHistory, Portfolio의 Trade 목록"],
    ["F-09 결과 평가", "ResultDialog", "GameController.evaluateResult", "ResultEvaluator.evaluate (FN-04) → GameResult"],
    ["F-10 영구 기록", "ResultDialog, RecordDialog", "GameController.saveResult, readRecords", "RecordStore를 구현한 RecordRepository.saveResult (FN-05), RecordEntry.merge"],
], [1.4, 2.0, 2.8, 4.2])

para("관계 설명", bold=True)
for line in [
    "일반화: MainFrame은 JFrame, StartPanel과 PriceChartPanel은 JPanel, 세 대화상자는 JDialog를 상속한다. "
    "StartResult, TradeResult, TurnResult, SaveResult는 추상 클래스 ActionResult를 상속하여 VIII의 공통 반환값(성공 여부, 오류 코드, 메시지)을 가진다.",
    "실체화: RecordRepository가 인터페이스 RecordStore를 구현한다. GameController는 인터페이스에만 의존하므로 저장소를 바꿔도 다른 클래스는 변하지 않는다.",
    "합성: GameSession이 사라지면 Portfolio, Market, NewsService도 함께 사라진다. Portfolio는 Holding과 Trade를, Market은 종목 6개를, NewsService는 30턴 추첨 자료(TurnDraw 30개)를 소유한다.",
    "집합: NewsService가 공개한 News 목록은 추첨 자료와 별개로 조회용으로 유지된다.",
    "연관: MainFrame은 GameController를, GameController는 GameSession(0..1), ResultEvaluator, RecordStore를 단방향으로 참조한다.",
    "의존: GameController는 ViewData와 ActionResult를 생성해 반환하고, GameSession은 GameSnapshot을, ResultEvaluator는 GameResult를 만든다.",
    "제약과 static: 현금 {cash >= 0}, 가격 {price >= 1}, 완료 턴 {0..30}. GameSession.TOTAL_TURNS = 30, INITIAL_CASH = 1000000, GOAL_ASSETS = 1200000, Portfolio.MAX_QUANTITY = 99999, Market.MAX_RATE = 0.20은 static 상수(밑줄)다. 열거형 AntRank는 수익률별 칭호 사다리로 화면 연출과 결과 칭호에만 쓰이며 엔딩 판정 기준은 바꾸지 않는다.",
    "다이어그램 외 보조 클래스: 화면 색과 글꼴을 모은 Theme, 둥근 버튼 같은 작은 컴포넌트를 모은 Widgets, JSON 읽기/쓰기 도우미 Json은 패키지 내부(package-private) 보조 클래스라 기능 매핑에서 제외했다.",
    "CP3 대비 변경: 시퀀스의 GameView를 MainFrame과 대화상자 4개로 나누고, 공통 반환값을 상속 구조로, 저장소를 인터페이스와 구현으로 분리했다.",
]:
    para(line)

# ------------------------------------------------------------------ X API Manual
heading("Screenshot of API Manual")
para("Javadoc으로 생성한 현재 버전의 API 매뉴얼이다. 모든 public 클래스와 메서드에 설명, 파라미터, 반환값을 작성했다.")
api_shots = sorted(ASSETS.glob("api_*.png")) if ASSETS.exists() else []
if api_shots:
    for i, s in enumerate(api_shots, 1):
        cap = ASSETS / (s.stem + ".txt")
        figure(s, cap.read_text(encoding="utf-8").strip() if cap.exists() else "API Manual " + str(i))
else:
    placeholder("Javadoc 스크린샷 삽입 예정")

# ------------------------------------------------------------------ XI Beta Release
heading("Screenshot of Beta Release")
para("베타 버전 실행 화면이다. 새 게임 시작부터 30턴 진행, 결과 확인, 기록 저장과 조회까지 한 판을 끝까지 플레이할 수 있다.")
beta_shots = sorted(ASSETS.glob("beta_*.png")) if ASSETS.exists() else []
if beta_shots:
    for i, s in enumerate(beta_shots, 1):
        cap = (HERE / "assets" / (s.stem + ".txt"))
        figure(s, cap.read_text(encoding="utf-8").strip() if cap.exists() else "Beta " + str(i), max_h=Inches(5.6))
else:
    placeholder("베타 실행 화면 스크린샷 삽입 예정")
notes = HERE / "beta_notes.txt"
if notes.exists():
    para("베타 범위와 단순화 항목", bold=True)
    for line in notes.read_text(encoding="utf-8").splitlines():
        if line.strip():
            para(line.strip())

# ------------------------------------------------------------------ XII GitHub
heading("Github URL for Source Code")
para(GITHUB_URL)

d.save(str(OUT))
print("saved", OUT)
