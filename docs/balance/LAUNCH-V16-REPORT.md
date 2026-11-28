# 방해 금지! 출시 밸런스와 라이브 운영 v1.6

브랜치 `ccr-balance-port` · 화면 병합 `cf382bf`(`d4bae0b` 포함) · 코어·계측 `c410b5f`. Play 상품 ID 11개, 원화 가격, 광고 제거·즉시/일일 데이터·일일 오버클럭·속도 배수는 v1.5 그대로다. [v1.6 지시서](../prompts/balance-session-v1.6.md) · [Play 상품 원본](../../tools/play/products.json)

## 1. 출시 판정과 새 목표

**v1.5 출시 통과(화면 기준), 16/16.** 운영 기준의 무과금 첫 6티어 중앙 28.00422일을 사용자 승인대로 통과 처리했다. 일 단위 기준은 반올림 전 값에 ±0.1일을 적용한다. 무과금 최악 시드는 **34.50일**로 실측 위험에 남긴다. [결정 기록](../bible/11-decision-log.md) · [v1.5 판정 원자료](launch-v15-data/operating/seeds.csv)

**v1.6 기본 경제도 출시 통과(화면 기준), 16/16.** 실제 화면에서 가능한 행동만 사용해 5프로필 × 8시드 × 84일을 실행했다. 진행·연쇄·해금 공백 CSV는 v1.5 운영 결과와 바이트 단위로 일치했다. 첫 60분의 새 목표와 아시아권 이벤트는 별도 보류다. [v1.6 실제 화면 전체 판정](launch-v16-data/actual-screen/verdict.md) · [40시드 원자료](launch-v16-data/actual-screen/seeds.csv)

| 기존 지표 | 목표 | v1.6 화면 결과 |
|---|---|---:|
| 무과금 마지막 앱 | 70~84일 | 71.25 / 최악 80.17일 |
| 광고 하루 5회 마지막 앱 | 52.5~59.5일 | 54.00 / 60.00일 |
| 월간 패스 마지막 앱 | 38.5~45.5일 | 44.67 / 48.00일 |
| 터보 마지막 앱 | 24.5~31.5일 | 28.00 / 29.50일 |
| 울트라 마지막 앱 | 10.5~17.5일 | 16.00 / 19.00일 |
| 무과금 첫 초기화 · 첫 5티어 | 1~2일 · 6~8일 | 1.34일 · 6.00일 |
| 무과금 첫 6티어 · 첫 동기화 | 21~28일 · 28~42일 | 28.00422일 · 34.43일 |
| 무과금 해금 공백 | 중앙 ≤7일 · 최악 ≤9일 | 6.00 / 8.99일 |
| 무과금 오프라인 P 몫 | 30~50% | 46.00% |
| 무과금 과열 손실 | 중앙 ≤5% · 최악 ≤15% | 0.04 / 4.54% |
| 광고 가속 · 인접 단계 역전·동률 | ≥1.25배 · 0/32 | 1.319배 · 0/32 |
| 무과금 물리 3폭발 이상 | 초반 ≥25% · 후반 ≥45% | 26.62% · 45.59% |

마지막 앱의 목표 구간은 프로필 중앙값에 적용한다. 다섯 프로필 모두 8/8이 마지막 앱을 구매했다. [화면 기준 매개변수](launch-v16-data/actual-screen/parameters.txt) · [화면 기준 최장 공백 원자료](launch-v16-data/actual-screen/unlock-gaps.csv)

도감 버튼 연결 가정에서는 40개 시나리오가 각각 여섯 단계 청구를 완료했다. 합계 240회다. v1.5와 `seeds.csv`, `chain-paths.csv`, `unlock-gaps.csv`가 바이트 단위로 일치했다. 주간 조각은 수입·환생·광고·상점 공식에 사용되지 않아 같은 BM 순서를 보존한다. [도감 청구 대조 판정](launch-v16-data/operating/verdict.md) · [실행 동작 원자료](launch-v16-data/operating/screen-actions.csv)

## 2. 첫 60분 간격

무과금 화면 기준 8시드, 연속 60분, 수동 입력 사이 최소 4.5초다. 유효한 판 스와이프 중 연쇄를 잘 만드는 선택을 사용한다. 판 재생·이벤트 모달·정기 시트 탐색과 4초 자동 청소 대기를 모델링한다. 별도의 실사용 표본인 직접 힌트 플레이의 입력 속도를 참고했다. 시트에 표시되는 앱·카드 신규 구매 가능, 앱·PRO·어시스트·덱 슬롯 해금, 레벨업만 센다. 동시 갱신은 한 번이다. 구간 시작·종료 사이 플레이어가 연속으로 기다린 시간도 분포에 넣는다. [직접 플레이 기록](../design/genre-review-11-28.md) · [계측 코드 Opening.kt](../../tools/economy-sim/src/main/kotlin/Opening.kt)

| 시간 구간 | 관찰 목표 | 현재 중앙 / p90 / 최악(초) | PRO 조기 개방 + 초기 강화비 10% 중앙 / p90 / 최악(초) | 후보 적중 |
|---|---|---:|---:|---:|
| 0~3분 | 60초 | 9 / 39.5 / 77.5 | 9 / 36 / 77.5 | 86/88 |
| 0~10분 | 60초 | 20.25 / 100 / 241 | 18 / 91 / 182 | 127/148 |
| 3~15분 | 180초 보조 관측 | 55 / 241 / 273 | 40.5 / 146 / 259.5 | 94/98 |
| 10~30분 | 180초 | 149.5 / 449.5 / 504.5 | 63.5 / 473 / 554 | 50/66 |
| 30~60분 | 관측 | 554 / 1,086.5 / 1,186.5 | 336 / 940.5 / 1,141.5 | — |

통과 기준은 첫 10분 p90 ≤60초, 10~30분 p90 ≤180초다. 시작 직후 잦은 레벨업이 합산 중앙값을 좋게 보이게 하므로 10개 중 9개 간격의 목표 충족을 요구한다. 최악과 전 구간 적중 시드는 함께 기록한다. 3~15분 구간의 후보에서는 180초 이내 구간이 51/63 → 94/98로 늘었지만 10~30분 p90은 473초다. 0~10분·10~30분에 전 구간을 통과한 시드는 25개 후보 각각 0/8이었다. [간격 결과 opening-gaps.csv](launch-v16-data/opening-gaps.csv) · [기준 시점 opening-before.csv](launch-v16-data/opening/opening-before.csv) · [PRO 후보 시점 opening-pro-100.csv](launch-v16-data/opening/opening-pro-100.csv)

| 대조군 | 수식 또는 배치 | 0~10분 / 10~30분 p90 | 판정 |
|---|---|---:|---|
| 첫 회차 2티어 구입 가격 | 정가 ×0.30·0.50·0.70·0.85. 첫 리셋 뒤 적용 종료 | 85%에서 86.5초 / 382초 | 초기 구매가 몰리고 Lv12 뒤 공백 지속 |
| 첫 회차 티어1~2 앱 강화비 | Lv8부터 현재 강화 단계 25 미만에 정가 ×0.10·0.25·0.40·0.60·0.80 | 10%에서 95.5초 / 636초 | 싼 앱 강화로 첫 3분이 촘촘해지고 뒷부분 공백 지속 |
| 초반 PRO | 시계·날씨·메모·손전등·계산기·음성 녹음의 첫 회차 게이트 Lv10~15. 가격은 다음 레벨 누적 P ×0.30 | 95.5초 / 300초 | 완급 개선, 목표 미달 |
| 초반 PRO + 강화비 10% | 두 변경을 함께 적용 | 91초 / 473초 | 3~15분 개선, 10~30분 최악 악화 |
| 초반 PRO + 강화 구간 확장 | 낮은 강화비를 단계 65 미만까지. 2.5·10·25% 대조 | 10%에서 86.5초 / 286.5초 | 개선, 목표 미달 |
| 카드 6종 조기 개방 + 초기비 혼합 | 방해 금지·@모두·훈련·인증·세일·문자를 Lv9~16에 배치. @모두 5,000 P, 인증 12,000 P. PRO·강화비 10%·Lv8~29 완화와 결합 | 68.5초 / 340.5초 | 첫 10분 p90이 가장 근접, 뒤 구간 미달 |
| 3티어 앱 조기 배치 + 할인 | 카드 혼합안에 3티어 6종을 Lv11~16에 배치하고 첫 회차 가격에 8·25·50% 적용 | 25%에서 68.5초 / 399.5초 | 구매 가능이 한꺼번에 몰림 |
| 초반 레벨 필요 P 완화 | Lv8~29에서 `기존 P × exp(−k × min((Lv−7)/10,(30−Lv)/13))`, Lv7·30 기준 보존. `k=0.9,1.6,2.3,3.0` | 2.3에서 95.5초 / 249.5초 | 공백이 다음 구간으로 이동 |

출시 성장표에는 시험 가격을 적용하지 않았다. 후보에서 Lv9~13에 앱·PRO를 한꺼번에 살 수 있는 시드가 생긴 뒤 Lv14~16 사이에 다음 고유 행동을 기다렸다. `PRO + 강화비 10%` 장기 선별 대조의 광고 시드 5는 마지막 앱이 47.00838→57.50002일로 움직였다. 초기 60분 목표를 통과하지 못해 장기 대조를 9/40에서 종료했으며 이 결과로 출시 여부를 판정하지 않았다. 카드 조기 개방과 세 후보를 합쳐도 10~30분 p90은 340.5초였다. 후속 조정은 Lv12~16의 실제 구매 가능 시점을 분산시키는 게 우선이다. [선별 대조 CSV](launch-v16-data/opening-pro-100-84d-partial.csv) · [격리 대조 패치](launch-v16-data/opening-research.patch)

## 3. 도감·주간 순위 보상

| 누적 보유 앱 | 10 | 20 | 30 | 40 | 50 | 60 | 66(확장 카탈로그) |
|---|---:|---:|---:|---:|---:|---:|---:|
| 꾸미기 조각 | 1 | 2 | 3 | 4 | 6 | 8 | 8 |
| 데이터 | 0 | 0 | 0 | 0 | 0 | 0 | 30 |
| ★ 선택권 | 0 | 0 | 0 | 0 | 0 | 0 | 1 |

도감은 같은 앱을 다시 사도 한 번만 세고, 기본·기억된 앱도 포함한다. 계정에 최초 획득 기록과 청구 번호를 저장한다. 옛 저장은 현재 회차 보유 앱으로 시작한다. 60종에 데이터 30과 선택권 1을 준 선별 대조에서 무과금 시드 6의 마지막 앱은 72.00001일로 같았고, 84일 오프라인 몫이 49.03→63.92%로 변했다. 지급 후 구매·덱·환생 선택의 경로가 바뀐 사례다. 출시 60종에서 조각 총 24개를 지급하고, 수입에 관여하는 보상은 66종으로 배치했다. 66종 단계는 해당 팩이 열렸을 때 노출된다. [60종 성장 보상 선별 원자료 19/40](launch-v16-data/book-growth-draft-partial.csv) · [보상 원본 CollectionRewards.kt](../../core/src/main/kotlin/com/graviton94/notificationcleaner/core/CollectionRewards.kt)

| 주간 구간 | ID | 조각 | 데이터·P·★ |
|---|---|---:|---:|
| 1위 | `first` | 8 | 0 |
| 2~10위 | `top10` | 5 | 0 |
| 11~100위 | `top100` | 3 | 0 |
| 참가 101위 이상 | `participant` | 1 | 0 |

서버가 UTC ISO 주 마감 후 주당 계정 1건을 보낸다. 조각 10개는 무료 외형 하나의 설계 교환 단위다. 현재 업적 화면은 잔액을 표시하며 외형 교환 연결은 예정돼 있다. 1위 8조각은 교환 단위보다 적다. 도전 중 도착한 지급은 일반 게임 저장에 들어간다. SHA-256 128비트 요약을 영수증 원장에 남겨 재전송, 동일 ID 구간 변경, 저장 후 재실행의 추가 지급을 막는다. 출시 도감 전체, 한 해 하루 광고 5회, 주간 53건을 섞은 저장을 20KB 기준으로 검증했다. [화면과 서버 계약](api-changes.md#v16-도감주간-지급기간-한정-콘텐츠) · [저장·지급 테스트 LiveEconomyTest.kt](../../core/src/test/kotlin/com/graviton94/notificationcleaner/core/LiveEconomyTest.kt)

## 4. 원격 손잡이와 한정 이벤트

| 손잡이 | 기본 / 코어 검증 범위 | 실제 계산 |
|---|---|---|
| `veteranOfflinePercentDelta` | 0 / 정수 −1…+1%p | 최고 Lv124 계정의 기본 오프라인 효율 `max(0, 9 + delta)`에 백업 단계당 2%p를 더한다. |
| `eventBonusScalePermille` | 1000 / 정수 900…1100 | 적격 이벤트 추가분만 배율 적용. 비활성 시 기준 16개 지표에 영향이 없다. |
| `event` | 미지정 / 등록 ID 한 개·최대 7일 | 시작 이상, 종료 미만의 UTC 창. 도전과 저장에는 반영하지 않는다. |

| 기간 한정 팩 | 적격 수동 행동 | 기본 / 배율 끝값 |
|---|---|---|
| 연쇄 주간 `chain_week` | 물리 폭발 3회 이상 | +12% / +10.8~13.2% |
| 아시아권 시너지 주간 `asia_week` | 아시아권 앱 2개 이상 장착 + 아시아권 앱 물리 폭발 | +18% / +16.2~19.8% |

일반 수동 스와이프의 기존 지급 예정 P를 `P`, 적격 이벤트 가산률을 `r`, 과열 여유를 `R`이라고 하면 지급은 `min(R, P + floor(P × r))`다. 실제 이벤트 추가는 여기서 `min(R, P)`를 뺀다. 일반 게임의 수동 스와이프에 한해 지급한다. 기존 최근 수입 산정과 오프라인 입력에는 추가 P를 넣지 않는다. 기간이 끝나면 새 스와이프 가산은 0이고, 활성 주에 번 P·그 진행은 보존한다. [원격 조정 계약](remote-tuning.md) · [이벤트 원본 LiveTuning.kt](../../core/src/main/kotlin/com/graviton94/notificationcleaner/core/LiveTuning.kt) · [화면 응답 JSON](api-changes.md#v16-도감주간-지급기간-한정-콘텐츠)

### 원격 설정 한계의 84일 영향

| 설정 또는 이벤트 | 기존 기준 | 무과금 마지막 앱 중앙 | 첫 6티어 중앙 | 오프라인 몫 | 최장 공백 중앙 / 최악 | 광고 가속 | 역전·동률 | v1.5 기준 충족 |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| 이벤트 없음, 기본 설정 | 화면 기준 | 71.25일 | 28.00422일 | 46.00% | 6.00 / 8.99일 | 1.319배 | 0/32 | 16/16 |
| 오프라인 −1%p | Lv124 이후 | 71.50일 | 28.00422일 | 46.49% | 6.00 / 8.99일 | 1.324배 | 0/32 | 16/16 |
| 오프라인 +1%p | Lv124 이후 | 71.25일 | 28.00422일 | 47.80% | 6.00 / 8.99일 | 1.320배 | 0/32 | 16/16 |
| 연쇄 주간, 1000‰ | 22~28일 | 71.08일 | 25.75일 | 41.65% | 5.74 / 9.00일 | 1.322배 | 0/32 | 16/16 |
| 아시아권 주간, 900‰ | 22~28일 | 69.00일 | 29.00001일 | 38.25% | 7.00 / 15.51일 | 1.290배 | 0/32 | 13/16 |
| 아시아권 주간, 1000‰ | 22~28일 | 72.17일 | 28.00일 | 43.60% | 5.50 / 11.34일 | 1.283배 | 0/32 | 14/16 |
| 아시아권 주간, 1100‰ | 22~28일 | 69.34일 | 28.00일 | 43.57% | 5.74 / 10.49일 | 1.282배 | 0/32 | 14/16 |

| 프로필 | 기본 | 연쇄 1000‰ | 아시아권 1000‰ | 아시아권 하한 / 상한 |
|---|---:|---:|---:|---:|
| 무과금 | 71.25 / 80.17 | 71.08 / 76.00 | 72.17 / 83.50 | 69.00 / 78.50 · 69.34 / 82.50 |
| 광고 | 54.00 / 60.00 | 53.75 / 68.00 | 56.25 / 71.17 | 53.50 / 65.00 · 54.08 / 64.33 |
| 월간 패스 | 44.67 / 48.00 | 44.50 / 48.00 | 45.84 / 48.00 | 45.08 / 48.34 · 43.17 / 49.50 |
| 터보 | 28.00 / 29.50 | 27.50 / 30.00 | 27.25 / 29.33 | 28.08 / 32.00 · 28.50 / 30.33 |
| 울트라 | 16.00 / 19.00 | 16.00 / 19.00 | 16.00 / 19.00 | 16.00 / 19.00 · 16.00 / 19.00 |

두 번째 표는 마지막 앱 도달일 중앙 / 최악이다. 오프라인 효율 두 끝값은 16/16이고, 후반 물리 3폭발은 45.08~45.40%로 하한 45%에 가깝다. 연쇄 주간은 기본 배율로 단독 운영할 수 있다. 아시아권 주간은 기본 배율에서 월간 45.84일과 공백 11.34일로 보류한다. 900·1100‰에서도 공백 미달이 남아 단순 배율 조정으로 해결되지 않았다. 두 원격 축을 동시에 올리는 경우는 대조 범위 밖이다. [오프라인 하한](launch-v16-data/veteran--1/verdict.md) · [오프라인 상한](launch-v16-data/veteran-1/verdict.md) · [연쇄](launch-v16-data/chain_week/verdict.md) · [아시아권 하한](launch-v16-data/asia-stress-900/verdict.md) · [아시아권 기본](launch-v16-data/asia_week/verdict.md) · [아시아권 상한](launch-v16-data/asia-stress-1100/verdict.md)

### 활성 주의 가속과 종료 다음 주

| 이벤트 | 프로필 | 활성 주 추가분 / 그 주 활성 P | 활성 주 총 P Δlog₁₀ / 최고 Lv 차이 | 종료 다음 주 총 P Δlog₁₀ / 최고 Lv 차이 |
|---|---|---:|---:|---:|
| 연쇄 | 무과금 | 1.330% | +0.167 / +0.5 | +0.186 / +1.0 |
| 연쇄 | 광고 | 0.102% | −0.012 / 0.0 | −0.025 / −0.5 |
| 연쇄 | 월간 | 1.010% | −0.230 / −1.0 | −0.536 / −2.5 |
| 연쇄 | 터보 | 0.083% | +0.488 / +1.0 | +0.294 / +1.0 |
| 연쇄 | 울트라 | 0.040% | −0.019 / 0.0 | −0.029 / 0.0 |
| 아시아권 | 무과금 | 7.227% | +0.017 / 0.0 | +0.514 / +2.0 |
| 아시아권 | 광고 | 2.881% | −0.567 / −1.5 | −0.962 / −3.5 |
| 아시아권 | 월간 | 3.005% | +0.222 / +0.5 | −0.040 / 0.0 |
| 아시아권 | 터보 | 0.637% | +0.158 / 0.0 | +0.713 / +2.5 |
| 아시아권 | 울트라 | 0.349% | −0.106 / −0.5 | +0.096 / 0.0 |

활성 주는 22~28일, 종료 다음 주는 29~35일이다. `Δlog₁₀ = log₁₀(이벤트 주간 활성+오프라인 P) − log₁₀(동일 시드 기준 주간 P)`이며 두 표본의 중앙값을 따로 빼는 대신 시드별 차이를 내고 중앙값을 계산했다. 최고 Lv는 주 종료까지의 기록이다. 두 이벤트 모두 40/40에서 활성 주에 추가 P가 지급됐고, 29~35일 직접 가산은 0이다. 장기 합계는 초기화·동기화 및 덱 경로가 달라져 양쪽 방향으로 움직였다. 연쇄 월간의 종료 뒤 격차와 아시아권 광고의 −0.962자리를 추적한다. [시드 짝 비교 CSV](launch-v16-data/event-pairs.csv) · [연쇄 누적 원자료](launch-v16-data/chain_week/weeks.csv) · [아시아권 누적 원자료](launch-v16-data/asia_week/weeks.csv)

배율의 35일 단기 대조(각 10시나리오)에서 전체 실현 추가분 중앙은 연쇄 **0.041% → 1.644%**, 아시아권 **4.701% → 5.749%**였다(900‰ → 1100‰). 분모는 그 시나리오의 이벤트 주 활성 P다. 장기 가속 효과와 원격 구간 통과 여부는 위 84일 표를 사용한다. [연쇄 배율 하한](launch-v16-data/chain_week-900/weeks.csv) · [연쇄 배율 상한](launch-v16-data/chain_week-1100/weeks.csv) · [권역 배율 하한](launch-v16-data/asia_week-900/weeks.csv) · [권역 배율 상한](launch-v16-data/asia_week-1100/weeks.csv)

## 5. KPI와 화면 인계

D1/D7/D30 잔존, 첫 가이드, 첫 구매, 광고 참여, 세션 길이와 빈도의 분모·중복 규칙·알람 표본을 정의했다. 4×15분, 광고 최대 5회, 지정 구독이라는 행동 가정으로 진행 시간을 계산했다. 잔존·전환·광고 채택은 라이브 데이터가 필요하다.

화면·플랫폼 추가 계측은 `game_session_start`, `session_summary`, `ftue_exit`, `purchase_granted`, `ad_impression`, `ad_grant`, `login_result`, `level_up`, `app_first_buy`, `reset`, `sync`, `chain`, `offline_paid`, `economy_summary`, `book_claim`, `weekly_grant`다. `ad_show`는 요청, `ad_reward`는 SDK 완료, `ad_grant`는 코어 지급 성공을 각각 집계한다. [지표 정의 원본 live-kpi.md](live-kpi.md)

병합 기존 문구에는 `#goal_buy`, `#now_open`, `#app_book`, `#book_progress`, `#book_collected`, `#mission_done`, `#rank_week`, `#rank_resets`, `#music`가 있다. 이번 화면 연결 때 필요한 문구는 아래와 같다.

| 제안 키 | 한국어 |
|---|---|
| `#book_reward_at` | 앱 {0}종 수집 보상 |
| `#book_claim_reward` | 보상 받기 |
| `#book_claimed` | 받음 |
| `#weekly_rank_reward` | 주간 순위 보상 · 꾸미기 조각 {0}개 |
| `#event_chain_week` | 연쇄 주간 |
| `#event_asia_week` | 아시아권 시너지 주간 |
| `#event_p_bonus` | 이벤트 추가 {0} P |
| `#event_time_left` | 종료까지 {0} |
| `#event_chain_rule` | 한 번 밀어 물리 폭발 3회 이상 |
| `#event_asia_rule` | 아시아권 앱 2개 장착 · 아시아권 앱 폭발 |

새 코어 엔드포인트는 `claimBook(at)`, `grantWeekly(claimId,tier)`, `applyTuning(json)`이다. 화면 호스트에는 도감 받기, 이벤트 배너·시간·지급 표시, 네이티브 원격 값 전달과 서버 지급 확인이 남는다. [병합 문구 변경 목록](launch-v16-data/screen-ko-keys.tsv) · [화면 한국어 원본 ko.json](../../tools/game-page/i18n/ko.json) · [WebSession.kt](../../core/src/main/kotlin/com/graviton94/notificationcleaner/core/WebSession.kt)

## 6. 검증 결과와 운영 판정

- 실제 화면 16/16, 코어 106개 통과, Android 빌드·API 30 에뮬레이터 스모크 통과. UI 소유 브랜치의 병합점은 `cf382bf`, 런타임과 시뮬 분리까지의 검증 커밋은 `ee3f768`이다. 패키지는 `io.github.graviton94.donotdisturb`다. [Android CI](https://github.com/graviton94/donotdisturb/actions/runs/46145822921) · [코어 테스트 원본 LiveEconomyTest.kt](../../core/src/test/kotlin/com/graviton94/notificationcleaner/core/LiveEconomyTest.kt)
- 첫 10분과 10~30분 목표는 미달로 남았다. 25개 후보 중 첫 10분 최선 p90은 68.5초, 10~30분 최선 p90은 249.5초이며 서로 다른 대조다. 운영 초반 가격은 기존 공식을 사용한다. 무과금 6티어 최악 34.50일과 해금 공백 8.99일도 계속 관측한다. [초반 분포 CSV](launch-v16-data/opening-gaps.csv)
- 연쇄 주간 1000‰는 운영 후보로 선정했다. 아시아권 주간은 코드와 화면 계약까지 제공했고, 활성화는 보류한다. 도감 버튼, 조각 교환 UI, 검증된 주간 서버 지급, 플랫폼 원격 값 전달은 화면·플랫폼 연결이 남아 있다. [원격 설정 운영 범위](remote-tuning.md) · [화면·서버 계약](api-changes.md#v16-도감주간-지급기간-한정-콘텐츠)

## 재현

JDK 17, 코어 `c410b5f`의 동일 정산을 사용한다. 현재 시뮬의 도감 청구 기본값은 화면 연결 여부에 맞춰 `2c55309`에서 명시했다. 아래 빌드의 실행 배포본을 별도 폴더에 복사하면, 다른 Gradle 빌드와 동시에 실행해도 계산 도중 JAR가 교체되지 않는다. 시드는 각 프로필에 1~8, RC 이벤트 배율 단기 대조는 1~2다.

```bash
./gradlew -p core test
./gradlew -p tools/economy-sim installDist
trial="$(mktemp -d)"
cp -a tools/economy-sim/build/install/economy-sim "$trial/runner"
sim="$trial/runner/bin/economy-sim"
export SIM_SCREEN_ONLY=1
export JAVA_OPTS="-Xms256m -Xmx2g"

"$sim" opening "$trial/opening-baseline.csv" 8
SIM_RULES="firstRunEarlyPro=true,firstRunRankPricePermille=100" \
  "$sim" opening "$trial/opening-pro-100.csv" 8

SIM_BOOK_CLAIMS=off "$sim" launch-v1 "$trial/actual-screen" 84 8 all Best
python3 tools/economy-sim/launch-v1-summary.py "$trial/actual-screen/seeds.csv" --release

export SIM_BOOK_CLAIMS=on
"$sim" launch-v1 "$trial/operating" 84 8 all Best
python3 tools/economy-sim/launch-v1-summary.py "$trial/operating/seeds.csv" --variant

for delta in -1 1; do
  SIM_STOP_PAID_AFTER_DAY=35 SIM_LIVE_TUNING="{\"veteranOfflinePercentDelta\":$delta}" \
    "$sim" launch-v1 "$trial/veteran-$delta" 84 8 all Best
done
for event in chain_week asia_week; do
  SIM_LIVE_TUNING="{\"event\":{\"id\":\"$event\",\"startAt\":1815400000,\"endAt\":2420200000}}" \
    "$sim" launch-v1 "$trial/$event" 84 8 all Best
  for scale in 900 1100; do
    SIM_LIVE_TUNING="{\"eventBonusScalePermille\":$scale,\"event\":{\"id\":\"$event\",\"startAt\":1815400000,\"endAt\":2420200000}}" \
      "$sim" launch-v1 "$trial/$event-$scale" 35 2 all Best
  done
done
for scale in 900 1100; do
  SIM_STOP_PAID_AFTER_DAY=1 SIM_LIVE_TUNING="{\"eventBonusScalePermille\":$scale,\"event\":{\"id\":\"asia_week\",\"startAt\":1815400000,\"endAt\":2420200000}}" \
    "$sim" launch-v1 "$trial/asia-stress-$scale" 84 8 all Best
done
python3 tools/economy-sim/launch-v16-audit.py events "$trial/operating/weeks.csv" \
  "$trial/chain_week/weeks.csv" "$trial/asia_week/weeks.csv" "$trial"/{chain_week,asia_week}-{900,1100}/weeks.csv
```

`SIM_STOP_PAID_AFTER_DAY=35` 대조는 무과금 84일을 모두 실행하며, 다른 네 프로필은 35일 이상이면서 마지막 앱을 산 뒤 끝낸다. `SIM_STOP_PAID_AFTER_DAY=1`의 아시아권 스트레스 대조도 무과금은 84일이며 다른 네 프로필은 마지막 앱 구매일에 종료한다. 다른 프로필의 마지막 앱 일자는 비교할 수 있고 84일 후반 관측은 공식 출시 실행에서만 쓴다. `--variant`는 대조판에 같은 16개 목표를 적용한다.

가격 2티어 후보는 `SIM_RULES=firstRunTier2PricePermille=300|500|700|850`, 강화비 후보는 `firstRunRankPricePermille=100|250|400|600|800` 중 하나씩 쓴다. `opening-pro-true`는 `firstRunEarlyPro=true`, `opening-pro-700`은 여기에 `firstRunRankPricePermille=250,firstRunTier2PricePermille=700`을 추가한다. 격리 대조에는 [확장 가격·레벨 완화 패치](launch-v16-data/opening-research.patch)를 적용한다. 단계 65 대조는 `firstRunEarlyPro=true,firstRunDiscountedRankCap=65,firstRunRankPricePermille=25|100|250`, 필요 P 완화만 보는 대조는 `openingLevelRelief=900|1600|2300|3000`이다. 카드 후보에는 [카드 배치 패치](launch-v16-data/opening-cards.patch)를 추가한다. `plain`은 기본값, `pro`는 조기 PRO, `blend`는 조기 PRO·단계 65·강화비 100‰·완화 2300이다. 세 후보 전부 같은 `opening` 명령과 8시드를 쓴다.

3티어까지 바꾸는 대조는 [합본 패치](launch-v16-data/opening-tier3.patch)를 `c410b5f`에 단독 적용한다. 위 `blend` 설정에 `openingTier3PricePermille=80|250|500` 중 하나를 추가한다.

60종 성장 보상 선별 대조는 `CollectionRewards.kt`에서 60종 보상만 `CollectionReward(data = 30, pieces = 8, vouchers = 1)`로 바꿔 `SIM_BOOK_CLAIMS=on`으로 실행한다. 옛 배포본 교체로 실행이 중단되기 전에 완료된 19개 시드만 보존했다.
