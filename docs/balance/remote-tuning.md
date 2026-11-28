# 방해 금지! v1.6 원격 밸런스 조정

| 키 | 타입 | 기본값 | 코어 검증 범위 | 효과 |
|---|---|---:|---|---|
| `veteranOfflinePercentDelta` | 정수 | 0 | −1…+1 (%p) | Lv124에 한 번 도달한 계정의 오프라인 기본 효율 9%에 적용한다. 예를 들어 +1은 10%다. 백업 강화 단계당 +2%p는 그대로 더한다. |
| `eventBonusScalePermille` | 정수 | 1000 | 900…1100 | 활성 이벤트의 추가 P에만 적용하는 배율이다. 가산 bp는 `floor(기본 가산 bp × 배율 / 1000)`이다. P 지급과 화면 표시가 이 값을 공유한다. |
| `event` | 객체 또는 미지정 | 미지정 | 등록 ID 1개, 양 끝은 UTC epoch 정수 밀리초, 0 < 기간 ≤7일 | `chain_week`, `asia_week` 중 1개를 시작 이상·종료 미만에서 켠다. |

```json
{"veteranOfflinePercentDelta":0,"eventBonusScalePermille":1000,"event":{"id":"chain_week","startAt":1795824000000,"endAt":1796428800000}}
```

Firebase Remote Config의 JSON 값은 한 문서로 원자적으로 적용한다. 범위를 넘거나 정수 대신 문자열을 보낸 필드, 누락된 필드는 기본값이 된다. 전체 JSON이 깨져도 같은 기본값이 된다. 롤백 JSON은 `{}`이다. 서버 시간 기준으로 만료를 표시하도록 호스트 시계를 맞춘다. 코어가 사용한 배율은 세이브에 기록하지 않는다. 배터리·패스·상품 가격·레벨 곡선·챌린지 규칙은 이 계약에 포함되지 않는다.

한정 이벤트는 [콘텐츠 팩 구조](../bible/14-live-updates.md)의 기간 한정 카탈로그다. `LiveEvent` ID 목록은 빌드에 고정된다. 원격 JSON은 한 팩의 기간만 지정하므로 한 판에서 이벤트가 겹치지 않는다. 이벤트 없이 돌아가는 화면 검증을 출시 판정으로 사용한다. 원격 범위 하·상한과 이벤트 활성 주/종료 주의 실제 영향은 [v1.6 결과](LAUNCH-V16-REPORT.md)에서 확인한다.

[원격 값 검증 원본](../../core/src/main/kotlin/com/graviton94/notificationcleaner/core/LiveTuning.kt)
