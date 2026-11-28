# 방해 금지! 라이브 경제와 플레이 KPI

분석은 Play 프로덕션 빌드의 이벤트 수집 동의 설치를 기준으로 한다. QA와 디버그, 재전송 중복, 알림 동의만 하고 끝난 실행은 별도로 집계한다. 설치 단위는 Firebase의 가명 설치 식별자다. Firebase Auth 계정과 결합하는 경우에는 정책상 허용된 내부 가명 키를 사용한다. 분석 이벤트에 이메일, UID 원문, 영수증 또는 광고 지급 ID를 넣지 않는다.

시뮬레이션은 참여가 정해진 플레이어가 84일 동안 수행할 수 있는 게임을 재생한다. 유저의 재접속·구매·광고 선택 확률에는 예측값이 없다. 아래 경보값은 초기 검토 기준이며, 추적 모집단이 최근 14개 완결 설치 코호트에서 300명 이상일 때 적용한다. 두 완결 코호트 연속 경계 이탈이면 원인 분석을 시작한다. 구매와 광고 오류는 분모 100 미만일 때 값과 표본 수만 보여 준다.

| KPI | 분자 / 분모 또는 구간 | 화면 기준 시뮬에서 확인할 값 | 초기 경보 |
|---|---|---|---|
| D1·D7·D30 잔존 | 설치일 UTC 코호트의 정확히 N일째 `game_session_start` 발생 설치 / 해당 날짜까지 관측이 끝난 설치. 가이드 진행을 포함한다. | 자발 잔존 추정 불가 | 각각 25%·10%·4% 미만 |
| 첫 가이드 | 첫 24시간 첫 도달 단계별 설치 / 게임 첫 시작 설치. 1 안내, 2 첫 폭발, 3 다음 스와이프, 4 첫 앱 구매 가능, 5 상점, 6 첫 앱 구매, 9 종료. 완료와 건너뛰기를 각각 본다. | 4.5초 기준 첫 앱 구매 가능, 이후 60분 신규 진전 간격. 종료율 추정 불가. | 4단계 도달 75% 미만 또는 순수 완료 60% 미만 |
| 첫 유료 구매 전환 | 설치 후 7일 안에 첫 검증 유료 지급이 완료된 설치 / D7 성숙 설치. 갱신·복원 제외. 프로필과 SKU는 따로 본다. | 구매를 지정한 프로필만 있음. 전환 추정 불가. | 0.8% 미만 |
| 보상형 참여 | 광고가 가능한 활성 설치 중 적어도 한 번 `ad_impression`이 있는 설치 / 일일 비패스 활성 설치 | 광고형에 최대 5회/일, 무광고형 0회. 채택률 추정 불가. | 참여율 15% 미만 또는 실 노출→코어 지급 성공 85% 미만 |
| 세션 길이·빈도 | 게임 화면 사용 시간 p50/p90, 설치×활성일당 세션 수 p50/p90. 30분 이상 전경 이탈하면 새 세션. | 15분 × 4 세션과 첫 60분 연속 시험. 현장 평균을 예측하지 않는다. | p50 화면 사용 3분 미만 또는 일일 세션 수 p50 1회 이하 |
| 진행 및 BM | 설치→첫 초기화·첫 5/6티어 구매·첫 동기화·마지막 앱까지 실제 경과일, 활성시간을 함께 분석. 두 시간축과 시드 시험을 비교한다. | v1.5 무과금 1.34·6.00·28.00422·34.43·71.25일 중앙. 광고 마지막 앱 54.00일(×1.319). | 동일 행동량(일 45~75분) 코호트에서 출시 목표 구간을 벗어나거나 광고 가속 1.25배 미만 |
| 손맛·과열·오프라인 | 물리 3폭발 이상 / 유효 수동 입력. 오프라인 P / (활성+오프라인 P). 잘린 P / 과열 상한 전 P. 개인 비율을 집계한다. | v1.5 초반 26.62%·후반 45.59%, 무과금 오프라인 46.00%, 과열 중앙/최악 0.04%/4.54%. | 후반 45% 미만, 오프라인 30~50% 이탈, 과열 중앙 5% 초과. QA 장기 세이브는 별도. |

## 기존 이벤트 해석

- `ftue_step{step}`은 로컬 진행 저장 시 발행된다. 9는 완료와 건너뛰기가 합쳐져 있고, 복귀 로드로 초기 단계가 다시 발행될 수 있다. 설치×단계의 첫 발생만 쓴다.
- `purchase{product}`는 Play 응답 성공을 받았을 때 웹에서 발생한다. 복원·갱신·일회 지급과 정확히 구별하려면 네이티브 거래 처리 결과를 사용한다.
- `ad_show{slot}`은 SDK 호출 시도다. `ad_reward{slot,code}`는 SDK 보상 응답이며 코어 지급 여부와 구분한다. `ad_fail`의 `code`로 로딩·동의·실패를 구분한다.
- `login{method}`는 시도다. 성공 및 본게임 진입과 구분한다. Firebase 자동 `first_open`, `session_start`, `user_engagement`로 설치·전경 사용을 대조할 수 있지만 타이틀과 실제 판 이용 시간은 구분되지 않는다.

## 화면·플랫폼에 요청할 계측

| 이벤트 | 파라미터 | 발행 시점 |
|---|---|---|
| `game_session_start`, `session_summary` | `session_id,trigger` / `session_id,foreground_ms,active_ms,swipes,profile` | 본게임 시작·30분 경과 재진입 및 종료. 단말 장기 중단은 마지막 체크포인트로 종료 처리. |
| `ftue_exit` | `outcome,step,elapsed_ms` | 완료·건너뛰기 분리. |
| `purchase_granted` | `product,outcome,first,renewal` | 검증·코어 저장 후 첫 반영, 복원과 구분. `first`는 실제 구매 첫 1회. |
| `ad_impression`, `ad_grant` | `slot,attempt_id` / `slot,attempt_id,outcome` | AdMob 노출 콜백과 코어 저장 성공. `attempt_id`는 새 로컬 추적용 값이며 SDK 지급 ID와 다르다. |
| `login_result` | `method,outcome` | 네이티브 로그인 결과. |
| `level_up`, `app_first_buy` | `level,highest,mode` / `app,tier,mode` | 게임의 레벨 갱신과 앱 최초 보유. 다단계 레벨업은 최종값 1회. |
| `reset`, `sync` | `level,reward_log10,lifetime_log10,active_ms` | 유효 지급 저장 뒤. |
| `chain` | `physical,waves,k,end,actor,mode,sample_rate` | 권장 1/20 고정 표집. 개인 평균은 세션 합산도 함께 사용. |
| `offline_paid`, `economy_summary` | `away_ms,eligible_ms,paid_log10,bonus_bp` / `active_log10,offline_log10,clipped_log10,physical3,manual_swipes,tuning_revision,event_id` | 복귀 지급과 세션 종료. 큰 수의 로그는 영수증이 아닌 정산 집계로 만든다. |
| `book_claim`, `weekly_grant` | `at,pieces,data,vouchers` / `tier,pieces` | 저장에 실제 반영된 지급 1회. |

광고형 코호트의 광고 가속은 일괄 평균에서 인과 효과로 해석하지 않는다. 무광고·광고형의 활성 시간과 최고 도달 수준을 맞춰 경보를 본다. 롤아웃 시점은 앱 버전·콘텐츠 팩·튜닝 리비전으로 분리한다.

[화면 이벤트 원본](../../tools/game-page/game.tpl.html) · [Firebase Analytics 브리지](../../app/src/main/java/com/graviton94/notificationcleaner/platform/Telemetry.kt) · [출시 기준선](LAUNCH-V15-REPORT.md)
