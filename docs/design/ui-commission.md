# UI 에셋 발주서 · 상점 · 강화 · 재화 · 부스트

> **발주 관리**: 대기 목록의 정본은 `docs/design/commissions.json`, 복사용 화면은 [에셋 발주 보드](https://claude.ai/artifact/Pd9VzPTkmJ6fEQsSFh5uqk)(`python3 tools/commissions/build_board.py OUT.html`로 생성). **한 프롬프트 = 시트 한 장**: 묶음의 요소를 격자 칸에 정위치로 그려 받고, `python3 tools/commissions/slice_sheet.py <묶음> <시트.png>`로 칸별로 잘라 `ui-assets/<묶음>/`에 저장한다(배경은 투명, 안 되면 #FF00FF). 반입하면 JSON의 `pending`에서 `delivered`로 옮기고 보드를 다시 발행한다. 이 문서는 설계 배경 기록용.

화면 뼈대(카드·패널·버튼·탭·상태표시줄)는 폰 UI를 흉내 낸 **코드**로 그린다(글래스·디자인 토큰). 아래는 게임 퀄리티를 위해 **이미지로 발주**하는 아이템 아이콘과 상품 아트다. 숫자·글자는 모두 코드가 위에 얹는다.

## 0. 공통 사양 (모든 프롬프트 앞에 붙인다)

**스타일 "잉크 스티커"**: 시연판의 점수 글자·칩/배율 상자와 같은 문법.

- 두꺼운 어두운 잉크 외곽선 `#1A0E06` (아이콘 폭의 약 5%), 모서리 둥글게.
- **딱딱한 그림자**: 같은 잉크색으로 바로 아래 약 4% 내려 찍은 그림자. 흐림 없음.
- 채우기: 위 밝고 아래 진한 **세로 2색 그라데이션** + 왼쪽 위 **광택 하이라이트 한 조각**(흰색 25~35%).
- 빛 방향은 모두 왼쪽 위. 살짝 입체(통통한 베벨) 느낌, 사실적 질감·노이즈·글로우 없음.
- **글자·숫자·이모지·로고 없음.** (×2 같은 숫자는 코드가 얹는다.)
- 실제 브랜드(배터리 제조사, 통신사, 앱스토어) 모양을 따라 그리지 않는다.
- 48px로 줄여도 실루엣만으로 구분될 것. 같은 묶음끼리 크기·두께·각도를 맞춘다.

**납품**: 512×512 투명 PNG(아이템 아이콘) 또는 지정 크기, 전경은 가운데 **80% 안전 영역** 안. 가능하면 SVG도. 경로 `docs/design/ui-assets/<묶음>/<id>.png`.

## 1. 재화 5종 (`currency/`)

| id | 재화 | 색 |
|---|---|---|
| `p` | P (포인트, 회차 금화) | 금 #FFE27A → #F0A000 |
| `data` | 데이터 (유료) | 초록 #6BE38A → #12A150 |
| `backup` | 백업파일 (초기화 환생) | 파랑 #8CC4FF → #2F6FE0 |
| `gb` | GB (동기화 환생) | 하늘 #BDF1FF → #38B8E8 |
| `battery` | 배터리 (에너지) | 초록 #9BEA6B → #34A853 |

> **[공통 사양]** Game currency icon set, five icons in one consistent sticker style.
> 1. `p`: a thick round gold coin seen slightly from the front, raised rim, the face embossed with a simple **bell-shaped notification glyph** (no letters), gold #FFE27A to #F0A000.
> 2. `data`: a **chunky faceted gem shaped like four rising signal bars** fused into one crystal, green #6BE38A to #12A150, small sparkle on the tallest bar.
> 3. `backup`: a **plump document file with a folded corner and a circular arrow** wrapped around it (restore), blue #8CC4FF to #2F6FE0.
> 4. `gb`: a **puffy cloud with a tiny stacked-disk (database) shape** tucked in front, sky blue #BDF1FF to #38B8E8.
> 5. `battery`: a **rounded battery standing upright, filled three-quarters with a lightning bolt** cut into the fill, green #9BEA6B to #34A853, white terminal cap.
> Transparent background, 512×512 each, same outline weight and shadow on all five.

## 2. 부스트 · 광고 보상 8종 (`boost/`)

| id | 항목 | 핵심 모양 |
|---|---|---|
| `income-boost` | 수입 부스트 30분 | 위로 치솟는 동전 + 속도선 |
| `time-warp` | 시간 도약 2시간 | 모래시계 + 앞으로 감기 화살표 |
| `power-bank` | 보조배터리 | 보조배터리 + 케이블 + 번개 |
| `backup-boost` | 백업 부스트 | 백업파일 + 위 화살표 |
| `free-upgrade` | 무료 강화 +5 | 렌치 + 위 화살표 |
| `data-box` | 데이터 상자 | 신호 막대 문양 상자, 뚜껑 살짝 열림 |
| `best-double` | 최고 기록 ×2 | 트로피 두 개 겹침 |
| `offline-double` | 오프라인 ×2 | 초승달 + 동전 두 개 |

> **[공통 사양]** Game boost/reward icon set, eight icons, same sticker style as the currency set.
> 1. `income-boost`: a gold coin rocketing upward with three bold speed lines and a small flame under it. Gold #FFE27A→#F0A000, flame #FF8A1F.
> 2. `time-warp`: an hourglass tilted 15°, sand mid-flow, with a bold **double fast-forward chevron** overlapping its lower right. Glass pale blue #DDF3FF, frame amber #EFA03F, chevron #3867F0.
> 3. `power-bank`: a chunky rectangular power bank with four charge dots, a short coiled cable, and a lightning bolt badge. Body graphite #4A4F5C→#2A2D35, bolt #FFD43B.
> 4. `backup-boost`: the blue backup file from the currency set with a **big upward arrow** bursting from behind it. File #8CC4FF→#2F6FE0, arrow #34C759.
> 5. `free-upgrade`: a stubby wrench crossing a **thick upward arrow**. Wrench silver #E6EAF0→#9AA3B2, arrow #34C759.
> 6. `data-box`: a sturdy loot crate, lid slightly open with light spilling out, the front embossed with **signal bars**. Crate #2F6FE0 trim, body #8CC4FF, glow #6BE38A.
> 7. `best-double`: **two gold trophies**, one slightly behind and offset, sparkles at the rims. Gold #FFE27A→#F0A000.
> 8. `offline-double`: a sleepy crescent moon with closed eyes, **two gold coins** resting in its curve. Moon #FFF3C4→#F5C842, coins gold.
> Transparent background, 512×512 each.

## 3. 상점 상품 아트 (`shop/`, 768×512 투명 PNG, 가로형)

| id | 상품 | 그림 |
|---|---|---|
| `data-1` ~ `data-6` | 데이터 팩 6단계 | 데이터 보석이 1개 → 한 줌 → 주머니 → 상자 → 금고 → 서버 랙까지 커진다 |
| `month-pass` | 월간 패스 (광고 제거 포함) | 금테 카드 + 달력 + 막힌 광고 기호 |
| `starter` | 스타터 팩 | 선물 상자 + 데이터 보석 + 별 하나 |
| `growth` | 성장 지원 팩 | 로켓 + 백업파일 + 데이터 |

> **[공통 사양]** Shop product illustrations, landscape 768×512, transparent background, same sticker style but richer (more objects, still bold).
> `data-1`: one data gem (the green signal-bar crystal) with a small sparkle.
> `data-2`: a small handful of three data gems.
> `data-3`: a cloth pouch spilling about eight data gems.
> `data-4`: an open treasure chest full of data gems, green glow.
> `data-5`: a heavy round vault door ajar, piles of data gems inside.
> `data-6`: a glowing server rack whose drive bays are filled with data gems, green light streaks; the most impressive of the six.
> `month-pass`: a premium gold-rimmed membership card, a small calendar page with 30 squares, and a **crossed-out play-button TV badge** (ad-free), confetti.
> `starter`: a ribbon-tied gift box with a data gem and a single gold star popping out.
> `growth`: a small rocket lifting off with a blue backup file and green data gems trailing behind.
> Escalation must read clearly from `data-1` to `data-6` at thumbnail size. No text, no price, no logos.

## 4. 프리미엄 · 등급 배지 (`badge/`)

| id | 용도 |
|---|---|
| `star-on`, `star-off` | 프리미엄 ★ 칸 (켜짐 금, 꺼짐 회색) 128×128 |
| `frame-1` ~ `frame-5` | 앱 아이콘 위에 얹는 등급 테두리(★1 동, ★3 은, ★5 금빛 무지개) 512×512, 가운데 투명 |
| `pro` | 프로 버전 리본 배지 (글자 없이 리본 + 번개) 256×256 |
| `upgrade-arrow` | 강화 가능 표시 초록 위 화살표 96×96 |

> **[공통 사양]** Rank badge set for app icons.
> `star-on`: a chubby five-point star, gold #FFE27A→#F0A000, glossy. `star-off`: the same star in flat grey #5A5550 with a darker outline, no gloss.
> `frame-1`…`frame-5`: a **rounded-square ring frame** (squircle, transparent centre 78% of the width) that wraps an app icon. ★1 bronze #E0A070, ★2 bronze with small rivets, ★3 silver #E6EAF0 with a gem at the top, ★4 gold #FFD66B with two gems, ★5 **gold with an iridescent rainbow sheen and tiny wings at the top corners**. Same outline/shadow rules.
> `pro`: a short ribbon banner folded at both ends with a lightning bolt in the middle, purple #7A3CFF→#4B1FB8, bolt #FFD43B.
> `upgrade-arrow`: a fat rounded up-arrow, green #6BE38A→#12A150.
> Transparent backgrounds.

## 5. 메타 아이콘 8종 (`meta/`, 256×256)

| id | 용도 |
|---|---|
| `attendance` | 출석 (달력 + 도장) |
| `attendance-day7` | 출석 7일째 큰 보상 (리본 선물 상자, 빛) |
| `missions` | 일일 미션 (체크리스트 클립보드) |
| `achievements` | 업적 (메달) |
| `daily-challenge` | 일일 도전 (스톱워치 + 깃발) |
| `pass` | 시즌 패스 (왕관 티켓) |
| `shop` | 상점 탭 (쇼핑백 + 데이터 보석) |
| `ad` | 광고 보기 버튼 기호 (작은 TV + 재생 삼각형) |

> **[공통 사양]** Meta-system icon set, eight icons, 256×256, same sticker style.
> `attendance`: a desk calendar page with a round red stamp mark on today. `attendance-day7`: a big ribbon gift box with light rays, the weekly jackpot. `missions`: a clipboard with three check boxes, two ticked green. `achievements`: a round medal on a ribbon, gold. `daily-challenge`: a stopwatch with a small checkered flag. `pass`: a ticket stub topped by a small crown, purple and gold. `shop`: a paper shopping bag with a green data gem peeking out. `ad`: a small retro TV with a play triangle on the screen.
> Transparent background, no text.

## 6. 우선순위

1. 재화 5종, 부스트 8종 (그레이박스 상점에 바로 필요)
2. 등급 배지 (프리미엄 화면)
3. 메타 아이콘
4. 상점 상품 아트 (소프트런치 전)

납품 전에는 코드 자리표시(점선 원 + 이름)가 쓰인다.

## 패스 3종 대표 아트 · 혜택 아이콘 (`pass-hero/`, `pass-perks/`, 11-27)

사용자 피드백: 패스 화면이 구매욕을 못 일으킨다 → **비쌀수록 화려한 차등 카드**. 카드 틀·빛·움직임은 코드(월간 금박 / 터보 전기 줄무늬 / 울트라 흑금 홀로 테두리), 그림은 발주.

| 묶음 | 칸 | 쓰는 곳 |
|---|---|---|
| `pass-hero` 3종 768² | 월간(금빛 VIP 티켓) · 터보(로켓 부스터 폰) · 울트라(흑요석·금 폰, 왕관, 프리즘) | 각 패스 카드 오른쪽 대표 그림 → `img/bm/pass-<id>.webp` |
| `pass-perks` 8종 | 광고 없이 · 매일 데이터 · 즉시 데이터 · 오버클럭 · 속도 · 환생 배율 · 왕관 · 빈 홍보 띠 | 카드 혜택 줄 아이콘, 울트라 왕관, "매달 최고 가치" 띠 |

프롬프트 원문은 `commissions.json`의 `pass-hero`, `pass-perks`. **11-27 반입**: 혜택 아이콘은 그대로 사용. 대표 아트는 후광·반짝이·잔광 같은 효과가 그려져 와서 가장자리가 지저분했다 → 불투명 본체만 남기고 떨어진 효과는 잘라냈다(`ui-assets/pass-hero/`). **앞으로 대표 아트 발주 규칙: 물건 본체만, 후광·글로우·반짝이·배경 효과 없이** — 빛·반짝이·홀로 테두리는 코드가 카드에 맞춰 그린다.

## 타이틀 · 로딩 · 온보딩 (`docs/design/title/`, 11-27)

시작 흐름: **로딩**(실제 리소스 진행률 · 팁) → **타이틀**(눌러서 시작 · Google/카카오 로그인 선택) → 첫 실행이면 **온보딩 6단계**(소개 카드 → 밀기 → 연쇄 설명 → 셰이드 → 상점 첫 구매 → 자동 정리 켜기). 기능은 코드, 그림만 발주.

| 묶음 | 파일 | 쓰는 곳 |
|---|---|---|
| `title-bg` | `bg.webp` 1080×1920 (불투명) | 로딩·타이틀 배경 |
| `title-logo` | `logo-en.webp`, `logo-ko.webp` (글자 있음) | 로딩·타이틀 로고 (언어별) |
| `title-stickers` | `mascot`, `mascot-cheer`, `bubble-*` 4종 | 로딩 마스코트, 가이드 첫 카드, 꾸밈 |

반입: 잘라서 `.webp`로 `docs/design/title/`에 넣으면 빌드가 있는 파일만 화면에 싣는다(없으면 코드 그래픽: 그라데이션·회전 광선·떠다니는 알림 알약·글자 로고).

## 상호작용 UI 틀 15종 · 회색조 벡터 (`ui-frames`, 11-28 재발주)

버튼·리본·배지·말풍선·가격표·팝업 틀처럼 **글자가 올라가고 누르는 틀**은 색을 입힌 이미지로 받지 않는다. 회색조 SVG로 받아 게임이 색을 입히고, 글자 길이에 맞춰 늘린다. 그림·스티커(왕관, 시계, 선물, 메달, 광고 TV 등)는 대상이 아니다.

- **회색 5단계만 사용**: `#1A1A1A` 외곽선·딱딱한 그림자 / `#595959` 아래 베벨 / `#A6A6A6` 바탕 아래 / `#D9D9D9` 바탕 위 / `#FFFFFF` 35% 광택. 다른 색·회색은 쓰지 않는다.
- **색 입히기**: 빌드 단계에서 다섯 값을 색 하나의 명도 단계로 바꾼다(예: 금색 버튼 = 외곽 `#4A2600`, 베벨 `#B86E00`, 아래 `#F5A300`, 위 `#FFE27A`). 버튼 색을 늘려도 새 그림이 필요 없다.
- **늘리기**: CSS `border-image`의 9-slice(가로만 늘어나는 것은 3-slice). 항목마다 viewBox와 늘림 경계(왼, 위, 오른, 아래)를 정해 두었다. 곡선·꼬리·구멍은 경계 안 모서리 영역에만 있고, 경계 사이의 변은 어디서나 똑같아야 한다.
- **고정 크기**: 원형 아이콘 버튼, 모서리 띠, 별모양 배지는 늘리지 않고 같은 비율로만 키운다.
- **글자·아이콘 없음**: 글자, ×2 같은 숫자, 닫기 모양은 코드가 얹는다.
- 받을 곳: `docs/design/ui-assets/frames/<id>.svg`. 지금 쓰는 `img/ui/ribbon.webp`·`burst.webp`·`sash.webp`·`bubble.webp`·`bolt.webp`와 CSS로 그린 금색 버튼을 이것으로 바꾼다.

| id | 용도 | viewBox | 늘림 경계 |
|---|---|---|---|
| btn-l / -pressed / -off | 큰 버튼 기본·눌림·비활성 | 240×96 | 40,36,40,44 |
| btn-s | 작은 알약 버튼 | 120×56 | 28,24,28,30 |
| icon-btn | 원형 아이콘 버튼 | 96×96 | 고정 |
| ribbon | 제목 리본(최고 기록 등) | 320×96 | 76,0,76,0 |
| sash | 카드 모서리 띠 | 120×120 | 고정 |
| burst | 별모양 배지(×2, 할인) | 160×160 | 고정 |
| bubble | 말풍선 | 160×112 | 40,32,32,48 |
| tag | 가격표 | 200×80 | 56,0,28,0 |
| pill-socket | 왼쪽 아이콘 홈이 있는 라벨 | 220×64 | 64,0,32,0 |
| count | 숫자·NEW 배지 | 64×40 | 20,0,20,0 |
| panel | 팝업 패널 틀 | 320×320 | 64,64,64,72 |
| bar-track / bar-fill | 진행 막대 바탕·채움 | 240×40 | 20,0,20,0 |

> 이미지 생성 도구가 SVG를 못 내면, 벡터 출력이 되는 도구(벡터 모드가 있는 생성 도구나 일러스트레이터)로 받는다. PNG를 따서 벡터로 바꾸면 경계가 무너지므로 받지 않는다.

### 보완 2종 (`ui-frames-2`, 11-28)
- **sash 다시 받기**: 첫 납품은 대각선 쐐기 하나뿐이라 띠로 읽히지 않고 글자 자리도 없었다. 45° 띠(폭 28, 중심선 (0,80)→(80,0)), 양 끝이 카드 가장자리 뒤로 접히는 삼각 날개, 글자 중심 (40,40) 자리를 비워 두도록 좌표로 지정했다.
- **ribbon v2**: 크기·늘림 경계·앞 띠 위치는 그대로, 띠가 꼬리로 꺾이는 곳에 접힘 음영 삼각형을 넣는다.
- 공통: 루트 `<svg>`에 viewBox와 같은 `width`·`height`를 넣는다(첫 납품은 빠져서 들여올 때 보정했다).
- **결과(11-28)**: 두 번째 납품도 모서리 띠가 쐐기 모양이고 리본 접힘이 띠 안쪽을 찔러서 받지 않았다. 두 개는 화면 세션이 좌표대로 직접 그렸다(`frames/sash.svg`, `frames/ribbon.svg`). **앞으로 회색조 UI 틀은 발주하지 않고 화면 세션이 코드로 만든다.** 그림·스티커류만 발주한다.

## 결정 (11-28 밤): UI 컴포넌트는 안드로이드 모바일 UI로 통일
- 스티커식 회색조 틀(리본, 별 배지, 두꺼운 외곽선 버튼)은 게임 화면에서 뺐다. 버튼·칩·태그·배지는 안드로이드 모양(알약 버튼, 칩, 둥근 배지)으로 그린다.
- 강조는 모양이 아니라 **색과 움직임**으로 한다: 금색·초록 버튼, 빛 지나감(`.mbtn.shine`), 맥박(`.pulse`), 가치 배지의 튀어나옴·박동·빛 지나감(`.mbadge`).
- 틀 키트(`ui-assets/frames`, `tools/game-page/frames.js`, `framestest.js`)는 기록으로만 남긴다. 게임 빌드에는 넣지 않는다.
