"""Build a launch verdict from completed per-seed engine outputs."""
import argparse
import csv
from collections import Counter
from pathlib import Path
from statistics import median


PROFILES = (
    ("Free", "무과금", 70, 84),
    ("Ads", "광고", 52.5, 59.5),
    ("Pass", "월간 패스", 38.5, 45.5),
    ("Spend30", "터보", 24.5, 31.5),
    ("Spend100", "울트라", 10.5, 17.5),
)
FREE = (
    ("reset_day", "첫 공장 초기화", "1~2일", 1, 2, "일", 1),
    ("tier5_day", "첫 5티어 구매", "약 7일", 6, 8, "일", 1),
    ("tier6_day", "첫 6티어 구매", "21~28일", 21, 28, "일", 1),
    ("sync_day", "첫 동기화", "28~42일", 28, 42, "일", 1),
    ("stalled_weeks", "정체 주", "0주", 0, 0, "주", 1),
    ("offline_share", "오프라인 P 몫", "30~50%", .30, .50, "%", 100),
    ("unlock_gap_days", "엔진 콘텐츠 최대 공백", "≤7일", 0, 7, "일", 1),
    ("opening_burst", "Lv1~10 폭발", "≥70%", .70, 1, "%", 100),
    ("late_burst", "Lv11 이후 폭발", "≥55%", .55, 1, "%", 100),
    ("chain2", "2연쇄 이상", "30~40%", .30, .40, "%", 100),
    ("chain4", "4연쇄 이상", "8~15%", .08, .15, "%", 100),
    ("chain9", "9연쇄 이상", "0.5~2%", .005, .02, "%", 100),
    ("overloads_per_session", "과부하/15분", "1~3회", 1, 3, "회", 1),
    ("first_week_records_per_day", "1주 차 기록/일", "≥5회", 5, float("inf"), "회", 1),
    ("late_records_per_session", "2주 차 이후 기록/15분", "약 1회", .5, 1.5, "회", 1),
    ("opening_first_unit_minutes", "초반 첫 지갑 단위 상승", "1~5분", 1, 5, "분", 1),
    ("late_unit_sessions_share", "중반 이후 한 단위 이상 상승한 세션", "모든 세션", 1, 1, "%", 100),
    ("events_per_session", "주요 돌발/15분", "1~2개", 1, 2, "개", 1),
    ("repeat_events", "주요 돌발 연속 중복", "0개", 0, 0, "개", 1),
    ("clipped_swipe_share", "스와이프 과열 발생", "관측", None, None, "%", 100),
    ("clipped_p_share", "스와이프로 정산할 P 중 과열로 소실", "관측", None, None, "%", 100),
)


def stats(rows, key, multiply=1, unit=""):
    values = [float(row[key]) * multiply for row in rows if row[key] != ""]
    complete = len(values)
    if not values:
        return "—", "—", None, complete
    return (f"{median(values):.2f}{unit}", f"{min(values):.2f}~{max(values):.2f}{unit}", median(values), complete)


def verdict(value, lower, upper, total, reached):
    if lower is None:
        return "관측"
    if reached != total or value is None:
        return f"미도달 포함 ({reached}/{total})"
    return "충족" if lower <= value <= upper else "미달"


def render(rows, expected):
    free = [row for row in rows if (row["profile"], row["input"]) == ("Free", "Best")]
    if len(free) != expected:
        raise ValueError(f"Expected {expected} Free results, got {len(free)}")
    print("### 진행과 손맛 (무과금, 최선조작)")
    print()
    print("| 지표 | 목표 | 중앙 | 시드 최소~최대 | 판정 |")
    print("|---|---|---:|---:|---|")
    for key, name, goal, lower, upper, unit, scale in FREE:
        mid, span, value, seen = stats(free, key, scale, unit)
        decision = verdict(value, None if lower is None else lower * scale, None if upper is None else upper * scale,
                           len(free), seen)
        print(f"| {name} | {goal} | {mid} | {span} | {decision} |")
    no_reset = [row for row in rows if row["input"] == "NoReset"]
    mid, span, value, seen = stats(no_reset, "peak")
    print(f"| 무환생 30일 최고 레벨 | Lv30~35 | {mid} | {span} | {verdict(value, 30, 35, expected, seen)} |")

    print()
    print("### 결제·광고별 마지막 앱 실제 구매")
    print()
    print("| 프로필 | 목표 | 중앙 | 시드 최소~최대 | 84일 도달 | 판정 |")
    print("|---|---:|---:|---:|---:|---|")
    targets = {"Free": "70~84일", "Ads": "56일", "Pass": "42일", "Spend30": "28일", "Spend100": "14일"}
    cohorts = {}
    for profile, label, lower, upper in PROFILES:
        cohort = [row for row in rows if (row["profile"], row["input"]) == (profile, "Best")]
        if len(cohort) != expected:
            raise ValueError(f"Expected {expected} {profile} results, got {len(cohort)}")
        cohorts[profile] = cohort
        mid, span, value, seen = stats(cohort, "last_app_day", unit="일")
        print(f"| {label} | {targets[profile]} | {mid} | {span} | {seen}/{expected} | {verdict(value, lower, upper, expected, seen)} |")
    print()
    print("단일 목표일의 판정은 가장 가까운 주를 적용한다(±3.5일). 약 7일인 5티어는 6~8일로 표시했다. 미도달 수는 별도로 기록한다.")
    print("Lv124 도달일과 마지막 앱 구매일은 원자료에서 각각 `lv124_day`와 `last_app_day`다.")
    best = {key: {row["seed"]: row["last_app_day"] for row in cohort} for key, cohort in cohorts.items()}
    inversions = 0
    for index in range(len(PROFILES) - 1):
        slow, fast = (best[PROFILES[i][0]] for i in (index, index + 1))
        inversions += sum(bool(slow[seed] and (not fast[seed] or float(slow[seed]) <= float(fast[seed]))) for seed in slow)
    ad_days = [float(row["last_app_day"]) for row in cohorts["Ads"] if row["last_app_day"]]
    free_days = [float(row["last_app_day"]) for row in free if row["last_app_day"]]
    if len(ad_days) == len(free_days) == expected:
        print(f"광고 가속 {median(free_days) / median(ad_days):.3f}배 (목표 ≥1.25배).")
    print(f"인접 과금 프로필 같은 시드 순서의 역전·동률 {inversions}/{(len(PROFILES) - 1) * expected}쌍.")
    print()
    print("| 프로필 | 이전 허용 구간 | 기존 구간에 든 시드 |")
    print("|---|---:|---:|")
    previous_bands = ((66, 84), (49, 63), (35, 49), (24, 32), (11, 17))
    for (profile, label, _, _), (start, end) in zip(PROFILES, previous_bands):
        count = sum(bool(row["lv124_day"] and start <= float(row["lv124_day"]) <= end) for row in cohorts[profile])
        print(f"| {label} | {start}~{end}일 | {count}/{expected} |")
    print("이전 허용 구간은 당시 판정의 Lv124 첫 도달일로 계산했다.")


    print()
    print("### 행동 대조군")
    print()
    print("| 행동 | 관측 일수 | 최고 레벨 중앙 | 시드 최소~최대 | 마지막 앱 도달 |")
    print("|---|---:|---:|---:|---:|")
    for policy in ("Best", "Random", "Auto", "NoReset"):
        cohort = [row for row in rows if row["profile"] == "Free" and row["input"] == policy]
        if cohort:
            mid, span, _, _ = stats(cohort, "peak")
            print(f"| {policy} | {cohort[0]['days']} | {mid} | {span} | {sum(bool(row['last_app_day']) for row in cohort)}/{len(cohort)} |")
    manual = {row["seed"]: row["weekly_peaks"].split("|") for row in free}
    for policy in ("Random", "Auto"):
        ahead = samples = 0
        for row in rows:
            if row["profile"] == "Free" and row["input"] == policy:
                for manual_level, other_level in zip(manual[row["seed"]], row["weekly_peaks"].split("|")):
                    samples += 1
                    ahead += int(other_level) > int(manual_level)
        if samples:
            print(f"{policy}의 같은 주·같은 시드 최고 레벨이 최선 스와이프보다 높은 경우 {ahead}/{samples}쌍.")


def checked_screen_cohorts(rows, expected):
    cohorts = {key: [r for r in rows if (r["profile"], r["input"]) == (key, "Best")]
               for key, _, _, _ in PROFILES}
    for key, cohort in cohorts.items():
        if len(cohort) != expected or {int(r["seed"]) for r in cohort} != set(range(1, expected + 1)):
            raise ValueError(f"Expected seeds 1–{expected} for {key}")
        if any(r["days"] != "84" for r in cohort):
            raise ValueError(f"Expected an 84-day horizon for {key}")
    return cohorts


def screen_physical_rates(path, expected):
    total, direct = Counter(), Counter()
    with path.open(encoding="utf-8") as file:
        for row in csv.DictReader(file):
            if (row["profile"], row["input"]) != ("Free", "Best"):
                continue
            key = (row["period"], int(row["seed"]))
            swipes = int(row["swipes"])
            total[key] += swipes
            if int(row["physical_bursts"]) >= 3:
                direct[key] += swipes
    return [median(direct[period, seed] / total[period, seed] for seed in range(1, expected + 1))
            for period in ("days1_14", "day15_on")]


def render_release(directory, rows, expected, variant=False):
    manifest = (directory / "parameters.txt").read_text(encoding="utf-8")
    if "screenOnly=true;screenActions=v1.5" not in manifest or not (directory / "screen-actions.csv").is_file():
        raise ValueError("Release requires SIM_SCREEN_ONLY=1 and the v1.5 screen action audit")
    if not manifest.startswith("launch-v1;") or (not variant and ";rules=;" not in manifest):
        raise ValueError("Release requires the operating engine without probe overrides")
    if not variant and ("bookClaims=false;" in manifest or "stopPaidAfterDay=" in manifest
                        or ("liveTuning=" in manifest and ";liveTuning=;" not in manifest)):
        raise ValueError("Release requires default live settings and the visible collection claim policy")
    if "adsPlan=scheduled" in manifest:
        raise ValueError("Use the baseline ad policy for the release verdict")
    cohorts = checked_screen_cohorts(rows, expected)
    free = cohorts["Free"]
    print("| 지표 | 목표 | 결과 | 판정 |")
    print("|---|---:|---:|---|")
    decisions = []
    day_tolerance = 0.1

    def emit(label, goal, result, ok):
        decisions.append(ok)
        print(f"| {label} | {goal} | {result} | {'충족' if ok else '미달'} |")

    for profile, label, lo, hi in PROFILES:
        mid, _, value, reached = stats(cohorts[profile], "last_app_day", unit="일")
        emit(f"{label} 마지막 앱", f"{lo}~{hi}일", f"{mid} · {reached}/{expected} 도달",
             reached == expected and lo - day_tolerance <= value <= hi + day_tolerance)

    for key, label, lo, hi, scale, unit in (
        ("reset_day", "무과금 첫 초기화", 1, 2, 1, "일"),
        ("tier5_day", "무과금 첫 5티어", 6, 8, 1, "일"),
        ("tier6_day", "무과금 첫 6티어", 21, 28, 1, "일"),
        ("sync_day", "무과금 첫 동기화", 28, 42, 1, "일"),
        ("offline_share", "무과금 오프라인 P 몫", 30, 50, 100, "%"),
    ):
        mid, _, value, reached = stats(free, key, scale, unit)
        tolerance = day_tolerance if unit == "일" else 0
        within_target = reached == expected and lo - tolerance <= value <= hi + tolerance
        if value is not None and not lo <= value <= hi and unit == "일":
            mid = f"{value:.5f}{unit}"
        emit(label, f"{lo}~{hi}{unit}", f"{mid} · {reached}/{expected}", within_target)

    gaps = [float(row["unlock_gap_days"]) for row in free]
    emit("무과금 최대 해금 공백", "중앙 ≤7일 · 최악 ≤9일",
         f"{median(gaps):.2f}일 / {max(gaps):.2f}일", median(gaps) <= 7 + day_tolerance and max(gaps) <= 9 + day_tolerance)
    clipped = [float(row["clipped_p_share"]) for row in free]
    emit("무과금 과열로 잘린 P", "중앙 ≤5% · 최악 ≤15%",
         f"{median(clipped):.2%} / {max(clipped):.2%}", median(clipped) <= .05 and max(clipped) <= .15)

    purchase = {profile: {row["seed"]: float(row["last_app_day"]) if row["last_app_day"] else None
                          for row in cohort} for profile, cohort in cohorts.items()}
    free_days, ad_days = (list(purchase[profile].values()) for profile in ("Free", "Ads"))
    speed = median(free_days) / median(ad_days) if all(x is not None for x in free_days + ad_days) else None
    emit("광고 가속", "≥1.25배", f"{speed:.3f}배" if speed is not None else "미판정", speed is not None and speed >= 1.25)
    pairs = [(slow[seed], fast[seed]) for slow, fast in zip(purchase.values(), list(purchase.values())[1:]) for seed in slow]
    unknown = sum(slow is None and fast is None for slow, fast in pairs)
    wrong = sum(slow is not None and (fast is None or slow <= fast) for slow, fast in pairs)
    emit("인접 과금 단계 역전·동률", f"0/{len(pairs)}", f"{wrong}/{len(pairs) - unknown} · 미판정 {unknown}",
         wrong == unknown == 0)

    early, late = screen_physical_rates(directory / "chain-paths.csv", expected)
    emit("무과금 초반 물리 3+", "≥25%", f"{early:.2%}", early >= .25)
    emit("무과금 후반 물리 3+", "≥45%", f"{late:.2%}", late >= .45)
    print()
    print("일 단위 기준 허용 오차 ±0.1일")
    if variant:
        print(f"대조판 {sum(decisions)}/{len(decisions)} 충족")
    else:
        print("출시 통과(화면 기준)" if all(decisions) else "출시 보류")
    return all(decisions)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("csv", type=Path)
    parser.add_argument("--seeds", type=int, default=8)
    verdict_group = parser.add_mutually_exclusive_group()
    verdict_group.add_argument("--release", action="store_true", help="Evaluate the screen-only launch criteria")
    verdict_group.add_argument("--variant", action="store_true", help="Apply the same criteria to a screen-only probe")
    args = parser.parse_args()
    with args.csv.open(encoding="utf-8") as file:
        rows = list(csv.DictReader(file))
    if args.release or args.variant:
        if args.seeds != 8:
            parser.error("--release requires eight seeds")
        raise SystemExit(0 if render_release(args.csv.parent, rows, args.seeds, variant=args.variant) else 1)
    render(rows, args.seeds)
