# 방해 금지! (Do Not Disturb!) — 프로젝트 현황 · 인수인계 (2026-11-26)

이 문서 하나로 전체 상황을 이해할 수 있게 정리한 기준 문서. 새 세션은 이 문서부터 읽는다.
바뀌면 이 문서를 먼저 고친다. 결정의 이력은 `docs/bible/11-decision-log.md`.

---

## 1. 정체성

| 항목 | 값 |
|---|---|
| 게임 | 폰 알림을 밀어서 합치고 터뜨리는 **무한 증분 성장 + 회귀(초기화·동기화) 방치형 클리커**. 숫자가 자릿수로 커지는 쾌감을 반복하는 도파민 루프 + 과금(P2W 허용) |
| 이름 (기본 언어 영어) | **Do Not Disturb!** · 스토어 제목 `Do Not Disturb!: Merge Idle` |
| 이름 (한국어) | **방해 금지!** · 스토어 제목 `방해 금지!: 방치형 클리커` |
| 옛 가칭 | 알림 청소부 / Notification Sweeper — **폐기**. 새 문서·문구·보고서에 쓰지 않는다 (bible 등 과거 기록은 그대로 둠) |
| 출시 언어 | 영어(기본) · 한국어 먼저. 일본어 · 번체 중국어 · 스페인어는 한국어 검수 뒤 |
| 패키지 (applicationId) | **`io.github.graviton94.donotdisturb`** — 다른 앱과 같은 `io.github.graviton94.<앱>` 규칙. 디버그 접미사 없음 |
| Kotlin 네임스페이스 | `com.graviton94.notificationcleaner` (내부 이름, **바꾸지 않음**) |

## 2. 저장소 · 브랜치

| 항목 | 값 |
|---|---|
| 게임 저장소 | **`graviton94/donotdisturb`** (옛 이름 `andpjt2`에서 변경. 원격 주소를 새 이름으로 바꿀 것) |
| main | PR #1로 화면 세션 작업 전체가 합쳐짐 (11-26) |
| 화면 세션 브랜치 | `ccr-9efafd3c-bh5fdz` |
| 밸런스 세션 브랜치 | `ccr-balance-port` (아직 main에 안 합쳐짐) |
| 웹사이트 저장소 | `graviton94/graviton94.github.io` → 공개 페이지 `https://graviton94.github.io/donotdisturb/` (소개 · `/privacy/` · `/privacy/#delete` · `/terms/`), 옛 `/notificationcleaner/`는 새 주소로 넘어감 |
| 규칙 | PR은 사용자 요청이 있을 때만. 각 세션은 자기 브랜치에만 푸시 |

## 3. 코드 구조 (요약)

```text
core/                  순수 Kotlin 규칙 엔진 (Engine, Meta, Shop, RuleSet, WebSession = 화면용 JSON API, DevServer = 브라우저 테스트 서버)
app/                   Android 셸: MainActivity(WebView) · GameBridge(window.Game) · platform/Ads.kt · platform/Telemetry.kt
tools/game-page/       게임 화면 원본 game.tpl.html → build.py → app/src/main/assets/game/  (+ *test.js 브라우저 회귀 테스트)
tools/brand/           아이콘 · 그래픽 이미지 렌더러 (icon.html, feature.html, render.js) → docs/design/brand/
docs/content/          app-effects.ko.json (앱 고유 효과 3줄 문법), l10n/, play/(Play Console 답안 · 데이터 보안 CSV)
docs/platform/         Firebase · AdMob · 서명 키 설정 (firebase-admob-setup.md)
keystore/              sideload.jks.gpg · upload.jks.gpg (GPG 잠금, README.md)
```

- **소유권**
  - 화면 세션: `tools/game-page`, `app/`(UI·플랫폼), `tools/brand`, `docs/platform`, 스토어 자료
  - 밸런스 세션: 경제 · 밸런스 · BM 수치(`core`의 RuleSet/commerce/Engine 수치), 시뮬레이터, `docs/balance`
  - `core/WebSession.kt`: 양쪽이 **추가만** 한다
- **화면 ↔ 코어 계약**: WebSession JSON. 새 필드는 `docs/balance/api-changes.md`에 적고, 큰 수는 `…Short` 짝을 함께 낸다.
- **브라우저 테스트**
  - 실행: `gradle -p core run --args="serve 8765 ../app/src/main/assets/game"` 후 `node tools/game-page/<paneltest|bmtest|eventtest|prestigetest|settingstest|bignumtest|ftuetest|metatest>.js <out>`
  - 사전 상태: `/api/new?seed=5`, 필요하면 `/api/cheat?level=N&apps=520,320`

## 4. 외부 서비스 연결 현황

### 4-1. Firebase · Google Cloud

| 항목 | 상태 |
|---|---|
| Firebase 프로젝트 | **`donotdisturb`** (Google Cloud 프로젝트와 같음). 요금제 **Blaze** |
| Android 앱 | `io.github.graviton94.donotdisturb` 등록. 디지털 지문 = sideload · upload 키 (6절) |
| `google-services.json` | 시크릿 `GOOGLE_SERVICES_JSON` → CI가 빌드 전에 `app/google-services.json` 생성. Google 로그인 사용 설정 뒤 다시 받은 파일(웹 클라이언트 ID 포함) |
| 앱에 들어간 SDK | Firebase BoM 34.0.0: Analytics · Crashlytics · Auth · Firestore · Remote Config. 파일이 없으면 Firebase 플러그인을 적용하지 않음(`BuildConfig.FIREBASE=false`) |
| 확인 | CI 테스트 빌드 로그에 "Firebase on", `processDebugGoogleServices` · Crashlytics 작업 성공 (11-26) |
| Authentication | **Google 로그인 사용 설정 완료**. 카카오는 Cloud Functions 커스텀 토큰 방식 예정(민트앤프렌즈와 같은 방식) |
| 서비스 계정 | Firebase Admin SDK 서비스 계정, 키 JSON = 시크릿 `FIREBASE_SERVICE_ACCOUNT` |
| IAM | 사용자가 미리 추가함: Firebase Rules 관리자, Cloud Datastore 색인 관리자, Firebase 원격 구성 관리자, Cloud Functions 관리자, 서비스 계정 사용자, Artifact Registry 작성자, Firebase App Distribution 관리자 |
| API | Google Play Android Developer API 사용 설정 |

### 4-2. Google Play Console

| 항목 | 상태 |
|---|---|
| 앱 | 만들어짐: `Do Not Disturb!: Merge Idle`, 게임, 무료 |
| 서비스 계정 권한 | 위 서비스 계정을 **앱 권한**으로 초대. 테스트·프로덕션 출시, 스토어 등록정보, 재무, 주문·구독 관리, 리뷰. 관리자 권한은 없음 |
| 첫 AAB | 아직 업로드 전. Play 정책상 **첫 AAB는 사용자가 콘솔에서 직접 업로드**해야 이후 API 업로드가 열림 |
| 앱 콘텐츠 · 데이터 보안 · 콘텐츠 등급 · 테스터 목록 · 카테고리 · 결제 프로필 | API가 없어 사용자가 직접 입력. 답안: `docs/content/play/play-console.md` |
| API로 할 수 있는 것 | 스토어 등록정보(문구 · 이미지 · 스크린샷, en-US 기본 + ko-KR), 기본 언어 · 연락처, 트랙 출시 · 국가, 인앱 상품 · 구독 등록(결제 권한이 있는 AAB가 올라간 뒤) |
| 비공개 테스트 | 개인 계정 조건: 테스터 12명 × 14일 연속 → 프로덕션 신청 |

### 4-3. AdMob

| 항목 | 상태 |
|---|---|
| 앱 | 등록, Firebase 연결 |
| 광고 단위 | 보상형 1개 **`DD_Rewarded_All`**. 지면은 Analytics 이벤트(`ad_show` / `ad_reward` / `ad_fail`의 `slot`)로 구분 |
| ID | 시크릿 `ADMOB_APP_ID`, `ADMOB_REWARDED_ID` → **출시(AAB) 빌드에만** 들어감. 디버그 · 테스트 APK는 항상 Google 테스트 광고 |
| 동의 (UMP) | GDPR 메시지 · 미국 주 메시지 게시 완료 (선택: 기본 + 닫기(동의 안 함)) |
| 앱 코드 | 실행 시 동의 확인 → 필요한 지역에만 동의 창 → 동의 뒤 광고 초기화 → 보상형 1개 선로드. 설정에 "광고 개인정보 설정"(UMP privacy options) |
| 보상 흐름 | 페이지 `window.Game.showAd(id)` → `window.onAdResult(id, earned, reason)`. earned=true일 때만 코어 `watchAd` 호출(지급 판정은 코어) |
| 남은 것 | 웹사이트 루트 `app-ads.txt` 확인, 미디에이션(출시 후) |

### 4-4. 카카오

| 항목 | 상태 |
|---|---|
| 앱 | Kakao Developers 등록, 카카오 로그인 사용, Android 플랫폼(패키지 위와 같음) |
| 키 해시 | sideload `W/NsDID/wtK4skP/m4tMxUV63Rc=` · upload `W9E0eVvoO13/ff5tAT/Hu0bhHpQ=` 등록 완료. Play 앱 서명 키 해시는 첫 업로드 뒤 추가 |
| 시크릿 | `KAKAO_APP_ID`, `KAKAO_ADMIN_KEY`(서버 전용, 앱에 넣지 않음), `KAKAO_NATIVE_APP_KEY`(앱 SDK용) |
| 코드 | 아직 없음 (로그인 구현 예정) |

## 5. GitHub Actions · 시크릿

| 워크플로 | 언제 | 하는 일 |
|---|---|---|
| `android.yml` (Android test build) | main · `ccr-*` 푸시 | Firebase 설정 → sideload 키 잠금 해제 → core 테스트 → 단위 테스트 · 린트 · assembleDebug → 에뮬레이터 스모크(`scripts/android-smoke.py`) → APK 결과물 `notification-cleaner-debug` |
| `android-signing-key.yml` | 이 파일이 처음 푸시될 때 / 수동 | 없는 키만 만들고 GPG로 잠가 커밋, 실행 요약에 SHA-1 · SHA-256만 출력. 기존 키는 replace를 켤 때만 교체 |
| `android-aab.yml` (Android AAB (Play)) | 수동(main), 입력: `version`, `track` | 업로드 키로 서명한 AAB, versionCode = 1000 + 실행 번호, AdMob 실제 ID. `track`이 none이 아니면 서비스 계정으로 그 트랙에 draft 업로드 |

| 시크릿 | 쓰임 |
|---|---|
| `GOOGLE_SERVICES_JSON` | Firebase 설정 파일 |
| `FIREBASE_SERVICE_ACCOUNT` | Play API 업로드, 앞으로 Functions · 규칙 배포 |
| `SIDELOAD_PASSPHRASE` · `ANDROID_UPLOAD_PASSPHRASE` | 서명 키 잠금 해제 |
| `ADMOB_APP_ID` · `ADMOB_REWARDED_ID` | 출시 빌드 광고 |
| `KAKAO_APP_ID` · `KAKAO_ADMIN_KEY` · `KAKAO_NATIVE_APP_KEY` | 카카오 로그인 |

## 6. 서명 키 (keystore/README.md)

| 키 | SHA-1 | SHA-256 | 쓰임 |
|---|---|---|---|
| sideload | `5B:F3:6C:0C:80:FF:C2:D2:B8:B2:43:FF:9B:8B:4C:C5:45:7A:DD:17` | `48:B5:FD:1B:6C:E7:67:FA:BC:94:37:54:36:00:7F:F5:A8:E1:73:92:63:72:7B:1D:91:83:C9:B2:30:73:BE:62` | CI 테스트 APK |
| upload | `5B:D1:34:79:5B:E8:3B:5D:FF:7D:FE:6D:01:3F:C7:BB:46:E1:1E:94` | `18:12:3F:78:E8:1A:31:95:2C:F1:BC:E1:8A:F7:67:56:C9:AF:D2:F3:CF:D0:72:19:E0:72:09:D1:63:71:49:9A` | Play 업로드 AAB |
| Play 앱 서명 키 | 첫 업로드 뒤 Play Console › 앱 무결성에서 확인 | | 스토어 설치본. Firebase · 카카오에 추가 필요 |

## 7. 진행 상태

### 완료
- 게임 화면
  - 승인된 FX 시연판 + 실제 엔진, 큰 수 표기, 글래스 M3 UI(Pretendard), 토스트
  - 첫 실행 가이드(동작 안내 애니메이션), 복귀 정산 카드
- 시트
  - 상점(캡슐 + 앱 상세 팝업) · 강화(레벨 단계별 강조) · 덱 · 초기화/동기화 · 클라우드 상점
  - 데이터 충전 · 패스 · 프리미엄 · 출석 · 설정 · 업적·칭호
  - 일일 도전(60초 타임어택)
- 플랫폼
  - Firebase(꺼짐/켜짐 자동), AdMob 보상형 + UMP, 분석 이벤트(ad_*, ftue_step)
  - 서명 키, AAB 워크플로
- 웹사이트 이름 · 주소 변경, 스토어 그래픽 이미지(새 제목)
- VFX 3차(빛 계열 통일) — 점검 릴 승인(11-26)
- **언어(키 기반)**: 화면 문구는 `$t('key')`, 언어 파일 `tools/game-page/i18n/ko.json`(원본)·`en.json`, 코어 라벨 `core-en.json`(id 기준). 규칙·언어 추가 방법 `tools/game-page/i18n/README.md`. 빌드가 한글 하드코딩·빠진 키를 거부. 설정 > 언어. 영·한 화면 전체 크롤 누락 0. 테스트는 `&lang=ko`

### 진행 중 · 다음 (화면 세션)
1. 첫 AAB(0.1.0, 가짜 결제) → 사용자가 내부 테스트에 직접 업로드 (API 열기용)
2. **실제 로그인** — 코드 완료(11-26, `docs/platform/login-billing.md`). 서비스 계정 키 교체 완료, Functions(login · deleteAccount) · Firestore 규칙 배포 완료(11-27). 함수 실행 계정 1010040706948-compute@developer.gserviceaccount.com 에 토큰 생성자 있음
   - Google + 카카오 → Firebase Auth. 카카오는 Cloud Functions가 커스텀 토큰 발급
   - 게스트 유지, Firestore `users/{uid}`에 세이브 동기화
3. **실제 결제** — 코드 완료(Billing 8). **상품 11개 Play 등록·활성 완료(11-27, 새 일회성 상품 API, 174개 지역)**. 첫 AAB 0.5.0 비공개 테스트 검토 중
   - Play Billing: 데이터 6종 · 스타터 · 초심자 · 패스 3종
   - 지급은 코어(claim id = 구매 토큰, 멱등)
4. 스토어 자료
   - 영어 · 한국어 스크린샷, 영어 설명
   - API 업로드 워크플로(`play-listing`)
5. 앱 콘텐츠 · 데이터 보안 답안을 실제 로그인 · 결제 기준으로 갱신 → 사용자 입력
6. 인앱 상품 API 등록 → 비공개 테스트 트랙 출시
7. 앱 색·티어·권역·콤보 단계별 VFX(밸런스 세션 burst 필드 추가 후 완성) → 스토어 스크린샷(영·한, 라이트·다크 번갈아)
8. 기타 언어(ja · zh-TW · es)

### 밸런스 세션 · 규칙 B 이식 및 출시 검증

- 화면 세션 변경을 `ccr-balance-port`에 병합 후 작업했다. 기준 지시서는 [balance-session-v1.0.md](prompts/balance-session-v1.0.md), 결과와 출시에 남은 조건은 [LAUNCH-V1-REPORT.md](balance/LAUNCH-V1-REPORT.md)에 있다. 84일 시드 8에서 무과금 마지막 앱 중앙 62.50일, 첫 동기화 15.84일로 출시 구간은 미충족이다.
- 코어에서 같은 앱의 두 줄이 닿으면 즉시 폭발한다. 저장 JSON 외피는 버전 1이고, 규칙 버전 4에서 열린 이전 보드의 폭발 기준을 2로 이관한다.
- VFX 계약은 [api-changes.md](balance/api-changes.md), 결제 등록 ID와 보상은 [17-monetization.md](bible/17-monetization.md)를 따른다.

v1.1은 화면 원본 `280ec1b`를 `b299f07`에서 병합했다. v1.2 화면 병합본에서 복귀 광고 보상 콜백을 코어 지급으로 연결했다. [작업 지시서](prompts/balance-session-v1.1.md) · [출시 판정과 재현](balance/LAUNCH-V11-REPORT.md) · [패스 BM과 복귀 계약](balance/api-changes.md)

**11-27 화면 통합(v1.2 이후)**
- 낙하 비트를 재생한다. `spawn`은 위에서 떨어져 들어오고 `chainEnd` `spam`이면 안내 문구를 띄운다. 긴 연쇄는 압축해 연쇄와 정산을 합쳐 6초 이하로 끝난다. 브라우저 25스와이프에서 화면 판과 코어 판이 일치했다.
- 멈춘 쌍 버그(같은 앱이 붙은 채 남음)를 재현했다. 0.6.0에서 12.5%, v1.2에서 0.27%(`wave`·`spam` 종료 직후). [v1.3 지시서](prompts/balance-session-v1.3.md)로 밸런스 세션에 넘겼다.
- 렌더링: HUD를 별도 화면 캔버스로 분리해 프레임마다 하던 큰 GPU 업로드 2회를 1회로 줄였다. 메뉴가 판을 덮으면 판 그리기를 멈춘다(블러 재계산 중단). 4초간 45fps 미만이면 해상도를 3/4로 낮춘다(테스트·캡처에서는 끈다). 헤드리스 기준 9 → 15.7fps.
- 조작: 위쪽 덱·배속 버튼이 열자마자 닫히던 문제 수정(같은 터치의 뒤따르는 클릭을 0.45초 흡수). 안드로이드 뒤로가기는 대화상자 → 시트 → 셰이드 순으로 닫고, 판에서는 2초 안에 두 번 눌러야 종료. 낙하 연쇄가 처음 이어질 때 1회 안내.
- 기기 스모크(CI 에뮬레이터)가 다시 통과한다: 타이틀·가이드 통과, 페이지 안 클릭, 영어 문구 허용, 캡처 버퍼, 꽉 찬 덱 처리.
- 11-28 화면 개선 라운드: PRO·이벤트 카드·알림창 강화 연결, 카드 줄 아이콘·문구, 셰이드 타일 알림, 방해 금지 카드 카운트다운. 셰이더 화면 비율 미설정 버그 수정과 `rendertest.js` 추가. 시트 문구 줄바꿈·칭호 따옴표·영어 문법, 배너 폭 맞춤·"Combo" 단계명, 라이트 테마 정산 막, 도전 중 광고 제안 제거. 360px 폭 타일·스타터 카드, 16:9 화면 비율 유지, ⚠ 표시, 정지 화면 20fps(배터리). 3분 자동 플레이 메모리·오류 이상 없음.
- 사운드 모음 아티팩트: https://claude.ai/artifact/2U5joM5fgoKrXvo1HximEN (slug `0be123ea-4ca6-4d52-ad2f-226b14c4160f`). 원본 `docs/design/sounds.json`, 다시 만들기 `node tools/sounds/render.js` → `python3 tools/sounds/build_board.py` → `tools/sounds/board/` 게시. 배경음악 악보 `tools/game-page/music.json`, 미디 `docs/design/music/`.
- 로그인: 한 번에 하나만 진행하고 "로그인하는 중…"을 표시한다. 서버 단계에 30초 제한을 둔다. 앱 시작 때 `login` 함수를 미리 깨운다(`provider: warm`). 카카오톡에서 돌아오는 것을 복귀 정산으로 보지 않는다. 타이틀에서 로그인한 뒤 시작 대기가 두 번 걸리던 버그를 고쳤다.

v1.2 작업 원본은 화면 세션 `e9469c1`의 [balance-session-v1.2.md](prompts/balance-session-v1.2.md)다. [v1.2 밸런스 보고서](balance/LAUNCH-V12-REPORT.md) · [낙하 비트 계약](balance/api-changes.md#v12-낙하-연쇄). `spawn`을 도착 위치에 삽입하고 `chainEnd`를 종료 표시로 쓴다. 전체 애니메이션을 6초 이하로 압축하는 작업은 화면 통합에서 검수한다.

v1.3은 화면 원본 `3cd1acb`를 병합하고 멈춘 쌍을 코어에서 방지했다. 화면은 기존 비트를 재생한 뒤 최종 `state.rows`에 맞춘다. 기기 스모크는 가이드 완료 표시 → 새로고침 → 타이틀 시작 버튼 순서다. [지시서](prompts/balance-session-v1.3.md) · [v1.3 결과와 재현](balance/LAUNCH-V13-REPORT.md) · [입력 대기 판 계약](balance/api-changes.md#v13-입력-대기-판)

v1.4는 화면 원본 `51815af`를 병합했다. 월간·연쇄 회귀를 분해하고 출시 화면에서 가능한 구매 경로를 적용한 검증 모드 `SIM_SCREEN_ONLY=1`을 추가했다. 기존 코어의 판 강화, 이벤트 카드 구매·장착, 앱 프로 구매를 이번 출시 화면에 포함할지 결정한 뒤 운영 수치 후보를 판정한다. [v1.4 지시서](prompts/balance-session-v1.4.md) · [v1.4 결과와 재현](balance/LAUNCH-V14-REPORT.md)

v1.5는 화면 원본 `42dbfb7`을 병합했다. 출시 판정은 `SIM_SCREEN_ONLY=1`의 다섯 프로필 × 여덟 시드 × 84일을 쓴다. 코어 `60a5383`은 최고 Lv124에 도달한 계정에 파동 정산 여유 8레벨을 추가했다. `bbce6d9`에서 첫 유효 동기화 전 파일 곡선과 후반 낙하 연쇄를 보정했다. 무과금 첫 6티어 구입 중앙값 28.00422일은 일 단위 ±0.1일 허용 오차 안으로 출시 통과(화면 기준)다. 최악 시드 34.50일은 추적 항목이다. [v1.5 지시서](prompts/balance-session-v1.5.md) · [v1.5 결과와 재현](balance/LAUNCH-V15-REPORT.md)

### 별도 트랙 · 무한 강화와 EX 사전 이식 기준
1. GB 동기화 직후 가속 복구: Lv60 재도달 비율 ≤0.80이 인접 회차 쌍의 70% 이상
2. EX: 단조성 역전 0곳, 짧은 부재(45초 ≥22/24, 3스와이프 ≥18/24)
3. P 대기 목표 구간 70%, 초기화 지급으로 강화 3~8개 ≥60%
4. 이식 조건을 모두 통과하면 "이식 준비 완료" 보고

## 8. 지켜야 할 규칙

- 확률형(가챠) 상품 없음. 모든 결제는 확인 팝업. 첫 구매 2배는 데이터 상품 공통 1회
- 지급 판정은 코어, SDK는 "보여 주고 확인"만. 모든 지급은 claim id로 멱등
- 비밀값은 코드 · 문서 · 로그에 넣지 않는다(지문 · 키 해시는 공개값이라 괜찮음). 사용자 개인정보(이메일 등)를 외부 요청 · 문서에 넣지 않는다
- 앱 고유 효과 문구: `docs/content/app-effects.ko.json`, when / then / (note), 줄당 20자. 빌드가 검사
- 문장은 어중간하게 줄바꿈하지 않는다(한 줄 원칙, 한국어는 단어 단위 줄바꿈)
- 화면 쪽이 읽는 코어 필드 이름은 바꾸지 않는다(복귀 카드 · 업적 · 도전 · BM 필드, `docs/balance/api-changes.md`)
- 커밋 메시지 끝 서명 두 줄(Co-Authored-By, Claude-Session)은 각 세션 규칙대로
