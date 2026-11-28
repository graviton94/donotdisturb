"""Summarize paired screen-only opening and live-event audits."""
import argparse
import csv
from collections import defaultdict
from math import ceil, log10
from pathlib import Path
from statistics import median


def rows(path):
    with path.open(encoding="utf-8", newline="") as file:
        yield from csv.DictReader(file)


def opening(paths):
    windows = [(0, 180, 60), (0, 600, 60), (180, 900, 180), (600, 1800, 180), (1800, 3600, None)]
    print("file,t0_seconds,t1_seconds,seeds,gaps,median_seconds,p90_seconds,max_seconds,in_target,all_gaps_in_target")
    for path in paths:
        moments = defaultdict(set)
        for row in rows(path):
            moments[int(row["seed"])].add(int(row["elapsed_ms"]) / 1000)
        for start, end, target in windows:
            samples = []
            for times in moments.values():
                touched = [start] + sorted(t for t in times if start < t < end) + [end]
                samples.append([b - a for a, b in zip(touched, touched[1:])])
            gaps = sorted(gap for sample in samples for gap in sample)
            within = sum(g <= target for g in gaps) if target is not None else ""
            all_within = sum(max(sample) <= target for sample in samples) if target is not None else ""
            print(f"{path.stem},{start},{end},{len(samples)},{len(gaps)},{median(gaps):.3f},"
                  f"{gaps[ceil(len(gaps) * .9) - 1]:.3f},{max(gaps):.3f},{within},{all_within}")


def events(control_path, event_paths):
    def read(path):
        return {(r["profile"], r["seed"], int(r["day"])): r for r in rows(path)}

    def gain(data, key, day):
        ends = [data[(*key, d)] for d in (day - 7, day)]
        return tuple(int(ends[1][k]) - int(ends[0][k]) for k in ("active_p", "offline_p", "event_extra_p", "event_swipes"))

    control = read(control_path)
    print("event,profile,seed,week_start,week_end,log10_total_p_ratio,peak_level_delta,live_extra_p,live_paid_swipes")
    for path in event_paths:
        variant = read(path)
        for profile, seed, end in sorted(variant):
            if end not in (28, 35):
                continue
            key = profile, seed
            manual, offline, extra, swipes = gain(variant, key, end)
            baseline_p = sum(gain(control, key, end)[:2])
            total_p = manual + offline
            assert total_p > 0 and baseline_p > 0
            peak_delta = int(variant[(*key, end)]["peak"]) - int(control[(*key, end)]["peak"])
            print(f"{path.parent.name},{profile},{seed},{end - 6},{end},"
                  f"{log10(total_p) - log10(baseline_p):.6f},{peak_delta},{extra},{swipes}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("opening", "events"))
    parser.add_argument("files", type=Path, nargs="+")
    args = parser.parse_args()
    if args.mode == "opening":
        opening(args.files)
    else:
        events(args.files[0], args.files[1:])
