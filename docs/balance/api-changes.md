# 화면 계약

## 이름과 배포 호환 기준

앱 이름은 기본 영어 **Do Not Disturb!**, 한국어 **방해 금지!**다. Android 설치 ID는 디버그 빌드까지 `io.github.graviton94.donotdisturb`를 사용한다. Kotlin 네임스페이스는 `com.graviton94.notificationcleaner`다. adb의 컴포넌트 이름은 `io.github.graviton94.donotdisturb/com.graviton94.notificationcleaner.MainActivity`이며, `scripts/android-smoke.py`가 같은 설치 ID를 `balance_smoke`, 프로세스 조회, `run-as` 저장 검증에 전달한다. [프로젝트 인수인계 `8aca804`](https://github.com/graviton94/donotdisturb/blob/8aca804/docs/HANDOFF.md) · [공개 사이트](https://graviton94.github.io/donotdisturb/)

Firestore 동기화에서 쓰는 저장 데이터는 `SaveCodec` JSON 버전 1이다. 배포 상품 ID는 인수인계 문서를 따른다. Play Billing의 지급 ID는 구매 토큰이며, AdMob 광고의 `ad-` 지급 ID는 SDK가 보상을 확정한 뒤 발급한다. 기존 `fakead-` 기록의 광고 원장 이관을 유지한다. 복귀, 업적, 일일 도전, BM 응답의 기존 필드와 단위를 유지하고 새 필드만 추가한다.

`main` 병합 시점에 Android 광고 SDK의 `earned` 콜백과 디버그 스모크의 테스트 영수증 경로를 각각 유지했다. 출시 빌드에서 SDK의 보상 확인을 실제 `ad-` 영수증으로 코어에 연결하는 작업은 화면 호스트에 남아 있다.

## 오프라인 보너스

`bm()`과 `snapshot`에 같은 `offlineBonus` 객체를 추가했다. `bm().state`와 다른 응답의 상태 스냅샷에서도 읽을 수 있다.

| 필드 | 단위와 의미 |
|---|---|
| `offlineBonus.rewardBonusPermille` | 가산분 천분율. `700`은 광고 시청 또는 패스 공통 +70% |
| `offlineBonus.ultraBonusPermille` | 가산분 천분율. `500`은 울트라 추가 +50% |
| `offlineBonus.ultraWithPassMultiplierPermille` | 울트라 가입 시 총배율. 활성 구독이 없어도 상품 카드에 표시할 수 있다. |
| `offlineBonus.adActive`, `adLeftMs` | 광고 시청 유효 여부와 남은 밀리초. UTC 기준 시청일과 다음날에 유효 |
| `offlineBonus.passActive`, `passLeftMs` | 패스 공통 혜택 유효 여부와 남은 밀리초. 터보와 울트라 포함 |
| `offlineBonus.ultraActive`, `ultraLeftMs` | 울트라 추가 혜택 유효 여부와 남은 밀리초 |
| `offlineBonus.rewardActive` | 광고 시청 또는 패스 중 하나 이상 유효 |
| `offlineBonus.multiplierPermille` | 현재 정산 총배율. 기본 `1000`, 공통 혜택 적용 시 `1700`, 울트라와 중첩 시 `2550` |

광고 기록과 패스가 둘 다 유효하면 공통 +70%를 한 번 적용한다. 울트라는 여기에 1.5를 곱한다. 총배율은 2.55배이고 기본 정산 대비 +155%다. 광고 버튼은 패스 공통 혜택과 남은 시간, 울트라 카드는 울트라 추가 혜택과 중첩값에 이 필드를 사용한다.

`offline(awayMs, now)` 응답에는 정산 시점의 `offlineBonus`와 `basePaid`, `bonusPaid`, `bonusPaidShort`를 추가했다. `basePaid` 및 `bonusPaid`는 `paid`와 같은 10진 문자열이다. `basePaid`는 동일 속도와 레벨 상한에서 광고·패스·울트라 가산분 없이 받을 값이다. `bonusPaid`는 정산에 실제 더해진 P이며, 상한에 걸리면 보너스가 활성 상태여도 0일 수 있다. 복귀 정산 화면에서는 `paidShort`와 `bonusPaidShort`를 사용한다.

## 기존 화면 및 저장 필드

`WebSession`의 `panel()`, `bm()`, 스냅샷, 스와이프 비트의 필드 이름과 타입, 단위는 기존과 같다. 화면에서 쓰는 상품 가격·지급량·효율의 데이터 소스를 `RuleSet.commerce`로 옮겼다. 월간·터보·울트라의 일일 데이터와 오버클럭은 앱 진입 및 구매에서도 정산한다. 스타터 카운트다운은 첫 세션 시작 시점을 보존한다. 저장은 기존 버전 1을 사용하고 기존 계정 필드를 그대로 읽는다.

디버그 APK에서 WebView CDP와 가짜 결제·광고 영수증을 활성화한다. 검증 스크립트는 서비스 화면의 버튼과 게임 브리지에 연결한다.

저장 내부의 광고 지급 번호는 버전 1에 선택 필드로 추가했다. 기존 `fakead-` 지급 번호는 첫 로드 때 최근 기록만 광고 원장으로 옮긴다. 결제 및 출처를 분류할 수 없는 기존 번호는 구입 원장에 남는다.

## 무한 성장 후보의 큰 수 계약

화면은 `…Short` 값을 그대로 표시한다. 정확값이 필요한 저장·공유용 데이터는 십진 문자열로 받는다. 사용 가능 여부와 기록 경신은 코어가 정확 정수끼리 비교해 불리언으로 내보낸다. `…Short`가 생기기 전의 화면은 기존 문자열을 소수로 바꾸지 않고 앞 세 자리와 문자열 길이로 표시할 수 있다.

| 응답 | 표시할 문자열 | 코어 판정 |
|---|---|---|
| `snapshot` | `walletShort`, `runEarnedShort`, `bestSwipeShort`, `filesShort`, `backupFilesNowShort`, `gbShort`, `cloudGbNowShort` | `canReset`, `canSync` |
| `snapshot.deckApps[]`, `snapshot.shopApps[]` | `upgradePriceShort`, `priceShort` | `canUpgrade`, `canBuy` |
| `panel.apps[]`, `panel.backupShop[]`, `panel.cloudShop[]` | `priceShort`, `price1Short`, `price10Short` | `canBuy`, `canUpgrade`, `canUpgrade10` |
| `panel()` | `nextLevelPShort`, `filesShort`, `gbShort` | 이미 계산된 숫자형 `levelProgress` |
| 스와이프와 비트 | `earnedShort`, `cappedShort`, `chipsShort`, `lostShort` | `newBestSwipe` |
| 복귀 정산 | `paidShort`, `basePaidShort`, `bonusPaidShort` | 실제 인정 시간과 EX 추가 정산은 아래 필드를 사용 |

기존 숫자 필드는 호환 범위를 유지하고, 파일·GB·그 가격처럼 이전에 숫자형이었던 필드의 정확값은 `filesExact`, `gbExact`, `priceExact`처럼 별도로 담는다. 화면은 이 값들을 문자열로 읽는다. 표기는 3유효숫자를 내림하며 기존 K·M·B·T·aa…zz를 사용한다. zz 뒤의 표기는 `1.23e+2043`처럼 십진 지수 문자열이다.

```json
{
  "walletShort": "1.23cn",
  "backupFilesNowShort": "2.4ab",
  "gbShort": "5.1M",
  "canReset": true,
  "canSync": false,
  "deckApps": [{"id": "memo", "upgradePriceShort": "1.24cn", "canUpgrade": false}]
}
```

### EX 상품 적용 후 추가할 계약

스와이프는 `exExtraP`와 `exExtraPShort`, 복귀는 `exExtraP`, `exExtraPShort`, `baseEligibleMs`, `eligibleMs`, `exExtraEligibleMs`를 낸다. 환생은 `paidFilesExact`, `paidFilesShort`, `exExtraFilesExact`, `exExtraFilesShort`를, GB 동기화는 `paidGbExact`, `paidGbShort`, `exExtraGbExact`, `exExtraGbShort`를 낸다. `exExtra…`는 동일 행동·상태에서 EX 없이 지급할 양과의 차이다. 이 수치에 이후의 복리 효과는 포함하지 않는다.

```json
{
  "earned": "1280000",
  "earnedShort": "1.28M",
  "exExtraP": "280000",
  "exExtraPShort": "280K",
  "newBestSwipe": true
}
```

```json
{
  "paidFilesExact": "1840000",
  "paidFilesShort": "1.84M",
  "exExtraFilesExact": "340000",
  "exExtraFilesShort": "340K"
}
```

위 큰 수 계약은 검증 후보용이다. 사용자가 선택한 B안의 EX 계정 연동은 운영 이식 승인 뒤 진행한다. [B안 검증 결과](B-CALIBRATION-REPORT.md)

## B안 복귀 표시 후보

`offline(awayMs, now)`의 `baseOfflineCapMs`, `offlineCapMs`, `exExtraOfflineCapMs`는 복귀 직전 상태에서 계산한 기본 상한, B 포함 상한, 둘의 차이다. `baseEligibleMs`와 `eligibleMs`는 실제 부재와 상한의 최솟값이며, `exExtraEligibleMs`는 이번 복귀에 추가로 인정된 시간이다. `exExtraPShort`는 정수 정산과 회차 지급 상한을 거친 실제 추가 지급의 표시 문자열이다.

```json
{
  "baseOfflineCapMs": 43200000,
  "offlineCapMs": 52020000,
  "exExtraOfflineCapMs": 8820000,
  "baseEligibleMs": 43200000,
  "eligibleMs": 52020000,
  "exExtraEligibleMs": 8820000,
  "exExtraPShort": "1.76M"
}
```

15시간 부재의 시험 응답이다. 화면 후보는 `EX 정산 상한 +147분`과 `이번 추가 인정 147분 · +1.76M P`를 표시했다. 6시간 부재는 같은 상한과 `이번 추가 인정 0분 · +0 P`였다. 기존 화면과 후보의 복귀 핸들러를 동일 응답으로 Chromium에서 비교했다. [화면 패치](../../tools/economy-sim/growth-loop/offline-display.patch) · [실측 결과](b-calibration-data/screen/rendered.json)

## 동기화 가속 검증 후보

`offline().awayMs`는 호스트가 전달한 실제 부재 시간의 밀리초다. 하한은 0이다. `paid`·`basePaid`·`bonusPaid`와 각 `Short` 짝, `offlineCapMs`, `baseOfflineCapMs`, `exExtraEligibleMs`, `exExtraPShort`, `offlineBonus`의 의미와 단위는 그대로 사용한다. 아래 15시간 복귀에서는 상한 14시간 27분을 빼면 초과 33분이다.

`basePaid`는 광고·패스 등의 추가 배율을 적용하기 전 지급액이며 EX는 그 안에 이미 반영돼 있다. `exExtraPShort`는 기존 시간 창 안의 효과와 시간 연장의 효과를 합친 비교값이다. 카드에서 `basePaid`와 `exExtraPShort`를 합치면 EX가 중복된다. 짧은 부재에서는 `exExtraEligibleMs`가 0이어도 `exExtraPShort`가 양수일 수 있다.

```json
{
  "awayMs": 54000000,
  "baseOfflineCapMs": 43200000,
  "offlineCapMs": 52020000,
  "exExtraEligibleMs": 8820000
}
```

수입을 이어받는 동안 `snapshot().carry`와 `panel().state.carry`에 같은 객체를 보낸다. `panel().carry`에서도 이 값을 읽을 수 있다. `ratio`는 채택 후보의 최초 계승 비율(0~1), `untilLevel`은 가산이 끝나는 레벨, `pPerSec`와 `pPerSecShort`는 현재 감쇄까지 반영한 초당 기준 수입의 정확값과 표시값이다. 실제 지급은 유효 스와이프, 활동 시간과 남은 P 상한을 같이 적용한다. 가산이 끝났거나 상품화하지 않은 규칙에서 `carry`는 생략한다.

```json
{
  "carry": {"ratio": 0.65, "untilLevel": 60, "pPerSec": "1240000", "pPerSecShort": "1.24M"}
}
```

## 규칙 B 출시 브랜치 추가 응답

| 응답 | 추가 키 | 값 |
|---|---|---|
| 스와이프 `burst.row` | `region` | `stock`, `asia`, `southeast_asia`, `west`, `global`. 와일드 두 줄은 가치 계산 앱의 권역을 사용 |
| 스와이프 `burst` | `tierMax` | 이 스와이프 직접 폭발의 최고 티어. 같은 스와이프 안 각 폭발에서 같은 정수 |
| 스와이프 `burst` | `synergy` | 적용된 권역·카테고리 중 큰 구성 배율 하나. 동률은 권역. `mult: 1.25`는 +25%. 덱 다양성 및 합성 상한은 이 표시 배율과 별도로 계산 |
| 스와이프 `burst` | `effect` | 발동한 코어 효과 앱 ID. 폭발이 없는 독립 이벤트는 이 목록에 포함하지 않음. 스와이프 전체 배율은 첫 폭발에 표시하고, 파동에 연동된 앱 효과는 관련 폭발에 귀속 |
| 스와이프 `wave` | `waveIndex` | 1부터 시작하는 파동 번호. 기존 `index`와 같음 |
| `snapshot` 및 상태가 포함된 응답 | `overloadReadyInMs` | 현재 시간부터 다시 파동이 가능한 시점까지의 밀리초. 0이면 시간 조건 충족 |
| `snapshot` 및 상태가 포함된 응답 | `autoIncomePermille` | 활성 자동 정리 지급 계수. 600은 60%. 과열 상한 전에 적용하며, 오프라인 효율과는 별개 |
| `offline()` | `awayMs`, `offlineCapMs`, `baseOfflineCapMs`, `basePaidShort`, `exExtraEligibleMs`, `exExtraPShort` | 실제 부재·정산 시점 상한·기본 상한·기본 지급 표기. 규칙 B 출시판에서는 기본과 실제 상한이 같고 EX 추가는 0 |

```json
{
  "beats": [
    {"t":"burst","row":{"id":1,"app":"green","region":"global"},"chain":1,"earned":"3200","tierMax":10,
      "synergy":{"kind":"region","key":"global","mult":1.25},"effect":["green"]},
    {"t":"wave","index":1,"waveIndex":1,"chain":2,"earned":"14400"}
  ]
}
```

`synergy.key`의 카테고리는 기존 `row.cat`의 `Social`·`Work`·`Daily`·`Media`·`Money` 키를 사용한다. `effect`와 `synergy`가 비어 있으면 그 키를 생략한다. 새 비트 타입은 없다.

`watchAd("OfflineDouble", adId, now)`가 당일의 가장 최근 양수 복귀 정산을 한 번 더 지급한다. 실제 추가분은 광고 레벨 상한을 적용한다. 한 세션의 제안은 한 번만 사용하며 공장 초기화·동기화 때 만료된다. 복귀 후 광고를 보는 동작과 당일·다음날 공통 +70%는 별도 효과다. v1.2 화면 병합본은 SDK 보상 확인 후 이 지면을 확인된 `ad-` 클레임으로 호출한다.

`bm()`은 `starterProductId`, `rookieProductId`, `passProductIds`로 Play 상품 ID를 제공한다. `passProductIds`의 키는 기존 청구 요청의 구독 종류다.

~~~json
{
  "starterProductId": "starter_pack",
  "rookieProductId": "rookie_pass",
  "passProductIds": {"month":"pass_monthly","Turbo":"pass_turbo","Ultra":"pass_ultra"}
}
~~~

`BestDouble`은 Lv20 이상에서 회차 최고 기록 P를 읽는다. 광고 확인이 늦어지는 동안 일반 스와이프가 생겨도 최신 스와이프 지급액으로 바뀌지 않는다. 광고 레벨 상한은 동일하다.

## v1.1 패스 가치

`WebSession.bm()`에 `passValues`, `starterValueX`, `rookieValueX`가 추가됐다. `passValues`의 키는 대소문자를 구분하며 `month`, `Turbo`, `Ultra`다. 금액은 원, 시간은 시간 단위, 나머지는 데이터 개수 또는 천분율이다. 월간의 속도·환생 천분율 0은 해당 추가 혜택이 없음을 나타낸다. 화면은 기존과 같이 월간의 추가 배율 행을 숨긴다.

```json
{
  "passValues": {
    "month": {
      "wonPrice": 5500, "dataNow": 300, "dataDaily": 60, "days": 30,
      "overclockHoursDaily": 3.0, "adFree": true, "speedPermille": 0, "prestigePermille": 0,
      "dataTotal": 2100, "dataEquivalent": 5700, "valueX": 22.8, "valueXDataOnly": 8.4
    },
    "Turbo": {
      "wonPrice": 33000, "dataNow": 300, "dataDaily": 60, "days": 30,
      "overclockHoursDaily": 3.0, "adFree": true, "speedPermille": 3500, "prestigePermille": 1750,
      "dataTotal": 2100, "dataEquivalent": 5700, "valueX": 3.8, "valueXDataOnly": 1.4
    },
    "Ultra": {
      "wonPrice": 99000, "dataNow": 300, "dataDaily": 260, "days": 30,
      "overclockHoursDaily": 3.0, "adFree": true, "speedPermille": 8000, "prestigePermille": 4000,
      "dataTotal": 8100, "dataEquivalent": 11700, "valueX": 2.6, "valueXDataOnly": 1.8
    }
  },
  "starterValueX": 10.2,
  "rookieValueX": 7.1
}
```

`valueXDataOnly`는 30일 모두 청구한 데이터의 기준가 환산이다. 월간 카드의 '최대 ×8.4 가치'는 이 값으로 표시한다. `valueX`는 오버클럭 90시간을 시간당 데이터 40으로 전부 환산한 비교 지표다. 광고 제거와 터보·울트라의 상시 배율은 두 환산에서 제외한다. 스타터와 초심자 패스는 기존의 선택권·소모품 포함 기준을 쓴다.

`packs[].id`, `starterProductId`, `rookieProductId`, `passProductIds`는 그대로 안내한다. 출시 상품 ID와 원화는 [Play 상품 등록 원본](../../tools/play/products.json)에 있다. `panel().syncLevel`의 기존 단위는 레벨이고 기본값은 75다.

저장 외피 버전은 1이며 규칙 버전은 5다. `backup.legacyAppUnlockLevel`은 이전 규칙에서 달성한 최고 레벨을 기록하는 선택 필드다. 값이 없는 새 저장은 0으로 취급한다. 기존에 열었던 앱은 업데이트 후에도 구입할 수 있고, 그 권한은 두 환생을 거쳐 유지된다.

## v1.2 낙하 연쇄

`swipe()`와 `auto()`의 `beats`에 `spawn`과 `chainEnd`가 추가됐다. 현재 판에서 붙은 줄은 이전과 같이 `merged` 뒤 `burst` 순서로 사라진다. `spawn`은 다음 폭발에 참여할 수 있는 중간 보충이다. `spawned`는 연쇄가 끝난 뒤 기존과 같이 남은 칸을 채운다.

| 비트 | 필드 | 재생 규칙 |
|---|---|---|
| `spawn` | `row`, `slot`, `chain` | `row.id`와 `row.app`는 기존 행 객체의 값이다. `slot`은 이 시점의 0부터 시작하는 도착 위치이며 현재는 맨 위인 0이다. 즉시 행을 삽입해야 뒤 `merged`나 `burst`가 같은 ID를 찾을 수 있다. `chain`은 떨어지기 직전까지 쌓인 연쇄 번호다. |
| `chainEnd` | `reason`, `chain`, `bursts`, `steps` | 한 번이라도 폭발한 스와이프에 한 번 나간다. `chain`은 튄더 가산 및 파동을 포함한 최종 표시 번호, `bursts`는 물리 폭발 횟수, `steps`는 폭발·중간 보충 작업 합계다. |
| `chainEnd.reason` | `natural`, `spam`, `wave`, `cap` | 더 닿는 쌍 없음, 스팸 차단, 과부하 파동 피날레, 안전 상한이다. `wave`는 마지막 `wave` 비트 뒤에 온다. |

다음은 시드 1의 실제 응답에서 연쇄 비트를 추린 예다. 앞서 떨어진 행을 다시 합치고 터뜨리는 ID가 이어진다. [전체 응답](launch-v12-data/falling-swipe.json)

```json
{
  "chain": 2,
  "beats": [
    {"t":"merged","into":{"id":3,"kind":"App","app":"memo"},"from":5},
    {"t":"burst","row":{"id":3,"kind":"App","app":"memo"},"chain":1},
    {"t":"spawn","row":{"id":8,"kind":"App","app":"weather"},"slot":0,"chain":1},
    {"t":"spawn","row":{"id":9,"kind":"App","app":"weather"},"slot":0,"chain":1},
    {"t":"merged","into":{"id":9,"kind":"App","app":"weather"},"from":8},
    {"t":"burst","row":{"id":9,"kind":"App","app":"weather"},"chain":2},
    {"t":"spawn","row":{"id":10,"kind":"App","app":"memo"},"slot":0,"chain":2},
    {"t":"spawn","row":{"id":11,"kind":"App","app":"alarm"},"slot":0,"chain":2},
    {"t":"chainEnd","reason":"natural","chain":2,"bursts":2,"steps":6}
  ]
}
```

정산이 끝난 전체 판은 기존의 `state.rows`다. 화면은 낙하·합치기·폭발·파동의 순서를 유지하며 전체 재생을 6초 안에 압축할 수 있다. `cap`은 처리 안전 상한을 뜻한다. 지급액이 과열 보호로 잘린 경우에는 기존 `capped` 비트가 별도로 나간다. 숫자 필드 이름과 단위, 저장 외피 버전 1, 상품 ID 11개는 계속 사용한다.

## v1.3 입력 대기 판

`swipe()`·`auto()`는 기존 연출 비트를 재생한 뒤 마지막 `state.rows`에 맞춘다. 파동·스팸·상한에서 끝난 뒤에도 최종 보충과 OTP 만료로 드러난 줄을 한 번 검사하므로 입력을 기다리는 판에 합칠 수 있는 이웃은 없다. 최종 정리는 `merged`·`burst`·`wave`를 내보내지 않는다. 남은 알림 ID는 그대로일 수 있으며 `app`·`kind`는 `state.rows`가 최종값이다.

`offline()`·`snapshot()`·덱 변경 응답도 같은 판 조건을 보장한다. 이전 저장을 처음 열 때 코어가 판을 정리한다. 저장 외피는 버전 1이고 판 복구에 필요한 새 필드는 없다. [v1.3 검증](LAUNCH-V13-REPORT.md)

## v1.4 화면에서 여는 회차 내 성장 (화면 세션, 추가만)

출시 화면에 없던 엔진 경로를 연결했다. 새 메서드는 모두 `act()`를 거쳐 `{ok, state}`를 돌려준다. Android에서는 `Game.call(method, args)`, dev 서버에서는 `/api/<method>`로 호출한다.

| 메서드 | 인자 | 엔진 |
|---|---|---|
| `buyPro` | `app` | `Engine.buyPro` — 보유 앱, `proLevel` 이상, P 충분 |
| `buyEvent` | `card` (EventCard 이름) | `Engine.buyEvent` — 빈 카드 칸이 있으면 바로 장착 |
| `buyAssist` | `item` (Assist 이름) | `Engine.buyAssist` |
| `setCards` | `cards` (쉼표 목록) | `Engine.setDeck(s, s.active, cards)` — 앱 덱은 그대로 |
| `dnd` | 없음 | `Engine.activateDnd` — 방해 금지 카드 장착 시 위쪽 방해 금지 버튼 |

`panel()` 추가 필드:
- 보유 앱마다 `pro`(bool), `proLevel`, `proPrice`(미보유일 때)
- `proMultiplier`, `eventSlots`
- `cards`: `{id, unlockLevel, open, owned, equipped, price}` × EventCard 10종
- `assists`: `{id, unlockLevel, open, lv, max, price}` × Assist 4종
- `dnd`: `{equipped, left, activeMs}`

화면 위치:
- 강화 시트: 앱 줄마다 PRO 구매, 아래에 '알림창 강화'(어시스트)
- 덱 시트: '이벤트 카드 · 장착 n/m'(구매·장착·해제)
- 위쪽 방해 금지 버튼: 카드 장착 시 발동. 미장착이면 기존처럼 효과음 토글.

## v1.5 Lv124 이후 파동 정산

현재 또는 이전 회차에서 Lv124에 도달한 계정은 실제 과부하 파동의 스와이프 정산 여유를 8레벨 더 받는다. `capped` 비트는 확대된 여유를 적용하고도 잘린 P를 뜻한다. 지급 P와 최종 레벨은 기존 필드에서 읽는다. JSON 필드, 저장 외피, 연쇄 종료 비트의 뜻은 유지한다. [v1.5 결과](LAUNCH-V15-REPORT.md)

### v1.5 첫 동기화와 후반 전투 보정

새 회차의 덱 10칸은 Lv74에서 열리고, Lv73 저장에서 이미 장착된 열 번째 칸은 보존한다. 스냅샷 `slots`와 패널 `maxSlots`는 기존 칸을 같이 표시한다. 유효 초기화 19회 이후 백업 이벤트 슬롯을 두 번째로 구매할 수 있다. `swipe()`의 파동·개별 폭발·과열 데이터와 출석·미션 API는 같은 필드와 단위를 사용한다. [v1.5 운영 수치](LAUNCH-V15-REPORT.md)

## v1.6 화면 쪽 준비 (화면 세션, 11-28)
- **원격 조정**: 앱이 Remote Config 문자열 매개변수 `tuning_json`(평평한 JSON 객체, 기본 `"{}"`)을 읽는다.
  - 시작할 때 지난 실행에서 활성화된 값을 `WebSession.applyTuning(json: String)`으로 한 번 넣는다. 새 값은 백그라운드에서 받아 **다음 실행부터** 적용한다. 판 도중에는 숫자가 바뀌지 않는다.
  - 데이터 초기화·클라우드 복원으로 세션을 새로 만들 때도 다시 넣는다.
  - 밸런스 세션이 `applyTuning(String)`을 추가하면 바로 연결된다. 그 전에는 아무 일도 하지 않는다(리플렉션 호출, 추가 후 직접 호출로 바꾼다).
  - 키 이름·범위는 `docs/balance/remote-tuning.md`(밸런스 세션 작성)를 따른다. 범위 검사는 코어가 한다.
- **분석 이벤트 추가**(`track()`): `level_up{level}`, `chain{k,bursts,end}`, `buy_app`·`upgrade`·`buy_pro`·`buy_card`·`buy_boost`·`buyBackup`·`buyCloud`·`use_item`·`premium`·`attend`·`missions_claim`·`ad_reward_claim`, `reset{amount,level}`·`sync{amount,level}`, `offline_return{away_min,paid}`, `challenge_end{score,new_best}`. 더 필요한 이벤트는 KPI 정의서에 적어 주면 붙인다.
- **주간 순위 정산**: Functions `settleWeekly`가 월요일 00:10(KST)에 지난주 판을 정산해 `weekly/{board}/results/{uid} = {rank, tier, of}`를 쓴다. tier는 `first`·`top10`·`top100`·`joined`. 보상 수치와 `grantWeekly(claimId, tier)`는 밸런스 세션 몫이다. claimId는 `{board}`(예: `2026-W48-L0`)를 쓸 예정이다.

## v1.6 도감·주간 지급·기간 한정 콘텐츠

`panel().book`의 `owned`는 지금 활성화된 카탈로그에서 한 번이라도 보유한 앱의 수다. 기본 앱과 백업으로 기억된 앱도 포함한다. 버전 1 옛 저장에 수집 이력이 없으면 현재 회차 보유 앱부터 센다. 화면의 출시 전 도감 목록은 회차별 `apps[].owned`를 쓰므로, 수집률·보상 자격은 새 `book.owned`로 표시한다. 개별 항목은 `book.collectedIds`를 현재 카탈로그의 앱 ID와 대조한다. 회차 보유 여부는 `apps[].owned`를 계속 읽는다.

```json
{"book":{"owned":3,"total":60,"collectedIds":["alarm","memo","weather"],"milestones":[{"at":10,"reward":{"data":0,"pieces":1,"vouchers":0},"claimed":false}]}}
```

`claimBook(at)`를 일반 게임에서 호출한다. `owned >= at && !claimed`일 때 받기 버튼을 켠다. 반환은 기존 동작의 `{"ok":true,"state":{…}}` 형식이며 이후 `panel()`과 `meta()`를 새로 읽는다. 같은 목표를 다시 청구하거나 도전 중에 호출하면 `ok:false`다. 앱 수·마일스톤·이미 청구 여부는 초기화·동기화·저장 로드 뒤에도 유지된다.

`grantWeekly(claimId, tier)`는 **검증된 서버 지급을 받는 네이티브 호스트**가 호출한다. 웹 JavaScript 액션 목록에는 두지 않는다. 구간 ID는 `first`(1위), `top10`(2~10위), `top100`(11~100위), `participant`(101위 이상, 그 주 유효 점수 제출자)다. 각각 조각 8·5·3·1개를 지급한다. 지급 ID는 서버에서 계정과 마감 UTC ISO 주 기준으로 한 번 만든다. 리그별 순위를 계산하더라도 같은 계정에는 그 주에 자격이 있는 최고 구간 1건을 보낸다. 재전송·구간을 바꾼 동일 ID는 추가 지급하지 않는다. 표시 순위와 같이 높은 점수 인원 수에 1을 더하므로 동점은 같은 구간을 받는다. 현재 클라이언트 제출 점수는 서버에서 결과를 검증하는 함수가 붙을 때 지급에 사용한다. 서버 지급 확인은 지급과 원장 기록을 포함한 저장이 끝난 뒤 한다. Firebase 계정 충돌 또는 새 기기에서는 서버의 권위 있는 주간 지급 기록을 유지한다.

`WebSession.applyTuning(json)`은 신뢰된 네이티브 브리지에서만 호출한다. 값, 날짜 형식과 되돌리기는 [원격 조정 계약](remote-tuning.md)을 따른다. 도전 중에도 호출할 수 있으며, 일반 게임에 돌아오면 반영된다. 적용·주간 지급은 기존 세션 잠금 안에서 호출한다. 세션 교체나 기기 재실행 뒤에는 캐시된 최신 JSON을 호스트가 다시 전달한다.

```json
{"liveEvent":{"id":"chain_week","title":"연쇄 주간","startsAt":1795824000000,"endsAt":1796428800000,"leftMs":604800000,"bonusBp":1200}}
```

`snapshot().liveEvent`와 `panel().state.liveEvent`는 활성 기간의 일반 게임 응답에서만 생긴다. 기간은 UTC epoch 밀리초이고 `bonusBp`는 자격 스와이프의 가산 bp다. 활성 중 성공한 스와이프 결과는 `liveEventExtraP`(정수 문자열)와 `liveEventExtraPShort`(표시 문자열)를 같이 준다. 실제 지급이 0일 때 둘 다 0이다. 기존 비트와 지갑 필드는 그대로 읽는다. 이벤트 종료 시 `snapshot()`을 갱신하면 배너를 내릴 수 있다.

[코어 API 원본](../../core/src/main/kotlin/com/graviton94/notificationcleaner/core/WebSession.kt)
