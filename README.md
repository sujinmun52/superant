# 슈퍼개미 - 가상 주식 투자 시뮬레이션 게임

소프트웨어시스템설계 개인 프로젝트 (CP4 Beta Release) · 244489 문수진

100만 원으로 30턴 동안 6개 가상 종목에 투자해 120만 원(수익률 20%)을 만드는 Java Swing 게임입니다.

## 실행

```bash
java -jar ssd_ip_cp4_2026f_244489_app.jar
```

JDK 17 이상이 필요합니다. 기록은 `%APPDATA%\stock-game\records.json`(Windows) 또는 `~/stock-game/records.json`에 저장됩니다.

## 빌드

```bash
bash build.sh
```

순서: 컴파일 -> 규칙 테스트(`test/superant/RuleTest.java`) -> 실행 jar(`dist/`) -> Javadoc(`build/api/`).

## 구조

| 패키지 | 역할 |
|---|---|
| `superant.view` | Swing 화면 (시작, 게임, 주문, 결과, 기록) |
| `superant.controller` | 요청 검증과 위임 (FN-01 ~ FN-05), 결과 객체 |
| `superant.domain` | 세션, 시장, 종목, 포트폴리오, 뉴스 |
| `superant.service` | 결과 판정 (엔딩, 투자 성향, 업적) |
| `superant.persistence` | JSON 기록 저장소 |
| `superant.model` | 공통 열거형 |

설계 문서와 다이어그램 생성 스크립트는 `docs/`에 있습니다.
