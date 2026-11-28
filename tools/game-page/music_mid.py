"""Export the background music score (music.json) as standard MIDI files: docs/design/music/<track>.mid.
Lead/bell, bass, chord stabs and drums on their own tracks, same voicing rules as the page's player.
python3 tools/game-page/music_mid.py"""
import json, pathlib, re, struct

HERE = pathlib.Path(__file__).parent
OUT = HERE.parent.parent / "docs" / "design" / "music"
NOTE = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}
CHORD = {"C": [0, 4, 7], "Dm": [2, 5, 9], "Em": [4, 7, 11], "F": [5, 9, 12], "G": [7, 11, 14], "Am": [9, 12, 16], "E": [4, 8, 11], "D": [2, 6, 9], "A": [9, 13, 16], "Bm": [11, 14, 18]}
TPQ, STEP = 480, 120  # 16th note = 120 ticks


def midi_of(t):
    m = re.fullmatch(r"([A-G]#?)(\d)", t)
    return 12 * (int(m.group(2)) + 1) + NOTE[m.group(1)] if m else None


def vlq(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.insert(0, (n & 0x7F) | 0x80)
        n >>= 7
    return bytes(out)


def track(events, name):
    """events: (tick, kind, ch, note, vel) with kind 'on'/'off'."""
    data = b"\x00\xff\x03" + vlq(len(name)) + name.encode()
    last = 0
    for tick, kind, ch, note, vel in sorted(events, key=lambda e: (e[0], e[1] == "on")):
        data += vlq(tick - last) + bytes([(0x90 if kind == "on" else 0x80) | ch, note, vel])
        last = tick
    data += b"\x00\xff\x2f\x00"
    return b"MTrk" + struct.pack(">I", len(data)) + data


def note(ev, ch, n, start, length, vel):
    ev += [(start, "on", ch, n, vel), (start + max(1, length - 4), "off", ch, n, 0)]


def bars_of(tr):
    bars = []
    for name in tr["order"]:
        sec = tr["sections"][name]
        base = tr["sections"][sec["same"]] if "same" in sec else sec
        for i, ch in enumerate(base["chords"]):
            line = (base.get("lead") or base.get("bell"))[i].split()
            bars.append((ch, line, sec.get("lift", False), base.get("intro", False)))
    return bars


def export(key, tr):
    lead, bass, chords, drums = [], [], [], []
    title = False  # both tracks use the full band now (the title's first pass is kick + ticks + pad)
    for b, (ch, line, lift, intro) in enumerate(bars_of(tr)):
        t0, root = b * 16 * STEP, CHORD[ch]
        for s, tok in enumerate(line):
            m = midi_of(tok)
            if m is None:
                continue
            n = 1
            while s + n < 16 and line[s + n] == "-":
                n += 1
            note(lead, 0, m, t0 + s * STEP, n * STEP, 96)
        if title:
            for i in root:
                note(chords, 2, 60 + i, t0, 16 * STEP, 60)
            for s in (0, 8):
                note(bass, 1, 36 + root[0], t0 + s * STEP, 6 * STEP, 80)
            continue
        for s in (0, 3, 6, 8, 11, 14):
            note(bass, 1, 36 + root[0] + (7 if s in (6, 14) else 0), t0 + s * STEP, int(STEP * (2.5 if s % 8 == 0 else 1.6)), 92)
        for s in (2, 6, 10, 14):
            for i in root:
                note(chords, 2, 60 + i, t0 + s * STEP, int(STEP * 1.4), 70)
        for s in range(16):
            if s in (0, 8) or (lift and s == 10):
                note(drums, 9, 36, t0 + s * STEP, STEP, 110)
            if s in (4, 12) and not intro:
                note(drums, 9, 38, t0 + s * STEP, STEP, 96)
            if s % 2 == 0 or lift:
                note(drums, 9, 42, t0 + s * STEP, STEP // 2, 70 if s % 4 == 2 else 50)
    tempo = b"\x00\xff\x51\x03" + (60_000_000 // tr["bpm"]).to_bytes(3, "big")
    meta = b"MTrk" + struct.pack(">I", len(tempo) + 4) + tempo + b"\x00\xff\x2f\x00"
    parts = [meta, track(lead, "Bell" if title else "Lead"), track(bass, "Bass"), track(chords, "Pad" if title else "Chords")]
    if not title:
        parts.append(track(drums, "Drums"))
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"{key}.mid").write_bytes(b"MThd" + struct.pack(">IHHH", 6, 1, len(parts), TPQ) + b"".join(parts))
    print("wrote", OUT / f"{key}.mid")


for key, tr in json.load(open(HERE / "music.json", encoding="utf-8"))["tracks"].items():
    export(key, tr)
