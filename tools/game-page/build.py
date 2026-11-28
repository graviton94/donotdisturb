"""Builds the in-app game page: app/src/main/assets/game/ (index.html + icons + fonts + images).
The page is the approved FX demo (docs/design/fx-demo) driven by the core engine through window.Game or the dev server.
  python3 tools/game-page/build.py
  gradle -p core run --args="serve 8765 ../app/src/main/assets/game"   # play it in a browser
"""
import json, pathlib, re, shutil
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "app/src/main/assets/game"
FX = ROOT / "docs/design/fx-demo"

colors = {m[0]: "#" + m[1][2:] for m in re.findall(r'"([a-z0-9]+)" to AppStyle\("[^"]*", Color\(0xFF([0-9A-Fa-f]{6})\)\)', (ROOT / "app/src/main/java/com/graviton94/notificationcleaner/ui/AppStyle.kt").read_text())}
colors = {k: "#" + v for k, v in re.findall(r'"([a-z0-9]+)" to AppStyle\("[^"]*", Color\(0xFF([0-9A-Fa-f]{6})\)\)', (ROOT / "app/src/main/java/com/graviton94/notificationcleaner/ui/AppStyle.kt").read_text())}
page = (ROOT / "tools/game-page/game.tpl.html").read_text()
effects = {k: v for k, v in json.loads((ROOT / "docs/content/app-effects.ko.json").read_text()).items() if not k.startswith("_")}
for k, v in effects.items():  # 3-line grammar: when / then / note, each one line of at most 20 characters
    assert v.get("when") and v.get("then") and set(v) <= {"when", "then", "note"}, k
    assert all(len(x) <= 20 and "\n" not in x for x in v.values()), f"effect line too long: {k}"
texts = {k: [[effects[k]["when"], effects[k]["then"], effects[k].get("note", "")], v[4]] for k, v in json.loads((ROOT / "docs/content/app-texts.json").read_text()).items()}
en = json.loads((ROOT / "docs/content/l10n/en.json").read_text())
eff_en = {k: v for k, v in json.loads((ROOT / "docs/content/app-effects.en.json").read_text()).items() if not k.startswith("_")}
texts_en = {k: [[eff_en[k]["when"], eff_en[k]["then"], eff_en[k].get("note", "")], en["lines"][k]] for k in texts}

# ---- i18n: every on-screen string is a key; each language is one file. The build refuses to ship a hole.
I18N_DIR = ROOT / "tools/game-page/i18n"
locs = {f.stem: json.loads(f.read_text()) for f in sorted(I18N_DIR.glob("*.json")) if re.fullmatch(r"[a-z]{2}(-[A-Za-z]{2,4})?", f.stem)}
core = {f.stem[5:]: json.loads(f.read_text()) for f in sorted(I18N_DIR.glob("core-*.json"))}
HAN = re.compile(r"[\uAC00-\uD7A3\u3131-\u318E]")
problems = []
base = locs["ko"]
for lang, tbl in locs.items():
    for k in base.keys() - tbl.keys(): problems.append(f"{lang}: missing key {k!r} (ko: {base[k]!r})")
    for k in tbl.keys() - base.keys(): problems.append(f"{lang}: extra key {k!r}")
    for k in base.keys() & tbl.keys():
        if set(re.findall(r"\{\d\}", base[k])) != set(re.findall(r"\{\d\}", tbl[k])): problems.append(f"{lang}: placeholders differ in {k!r}")
for k in set(re.findall(r"\$t\('([^']+)'", page)) - base.keys(): problems.append(f"code uses unknown key {k!r}")
for k in set(re.findall(r'data-i18n(?:-aria)?="([^"]+)"', page)) - base.keys(): problems.append(f"html uses unknown key {k!r}")
for n, line in enumerate(page.splitlines(), 1):  # display text must come from the language files, not the code (comments are fine)
    code_part = re.split(r"(?<![:'\"])//\s|/\*", line)[0] if "<script" not in line else line
    if HAN.search(code_part) and not re.match(r"\s*(\*|<!--)", line): problems.append(f"game.tpl.html:{n}: Korean text in code — move it to i18n/ko.json")
if problems: raise SystemExit("i18n check failed:\n  " + "\n  ".join(problems[:60]) + (f"\n  … {len(problems) - 60} more" if len(problems) > 60 else ""))
TITLE_DIR = ROOT / "docs/design/title"  # title/loading art (commission pass-title); only files that exist are requested
title_art = sorted(f.stem for f in TITLE_DIR.glob("*.webp")) if TITLE_DIR.exists() else []
page = page.replace("__TITLE_ART__", json.dumps(title_art))
FRAMES_DIR = ROOT / "docs/design/ui-assets/frames"  # greyscale UI frames, recoloured and stretched at runtime (tools/game-page/frames.js)
page = page.replace("/*__FRAMES_JS__*/", (pathlib.Path(__file__).parent / "frames.js").read_text()).replace("__FRAMES__", (FRAMES_DIR / "frames.json").read_text())
page = page.replace("__MUSIC__", json.dumps(json.load(open(pathlib.Path(__file__).parent / "music.json", encoding="utf-8")), ensure_ascii=False))  # background music score
page = page.replace("__I18N__", json.dumps(locs, ensure_ascii=False)).replace("__I18N_CORE__", json.dumps(core, ensure_ascii=False)).replace("__TEXTS_EN__", json.dumps(texts_en, ensure_ascii=False))
page = page.replace("__COLORS__", json.dumps(colors)).replace("__TEXTS__", json.dumps(texts, ensure_ascii=False)).replace("__HANDSET__", repr((FX / "handset.b64").read_text().strip())).replace("__PHONE__", (FX / "phone-sfx.json").read_text())
assert "__" not in re.sub(r"__(proto|dirname)__", "", page.split("<script>")[1])[:0] + "", "placeholder left"
if OUT.exists(): shutil.rmtree(OUT)
(OUT / "icons").mkdir(parents=True)
(OUT / "index.html").write_text(page)
for f in (ROOT / "docs/design/app-icons").glob("*.svg"): shutil.copy(f, OUT / "icons" / f.name)
shutil.copytree(ROOT / "tools/brand/fonts/black-han-sans", OUT / "fonts/black-han-sans")
shutil.copytree(ROOT / "tools/brand/fonts/pretendard", OUT / "fonts/pretendard")
shutil.copy(ROOT / "tools/game-page/pfx.bundle.js", OUT / "pfx.bundle.js")  # pixelfx (MIT) — rebuild with build-pfx.mjs
(OUT / "img").mkdir()
shutil.copy(ROOT / "docs/design/ui-assets/tutorial/hand-swipe.png", OUT / "img/hand-swipe.png")
shutil.copytree(ROOT / "docs/design/bm-screens/img", OUT / "img/bm")
shutil.copytree(ROOT / "tools/game-page/ui", OUT / "img/ui")
if title_art: shutil.copytree(TITLE_DIR, OUT / "img/title", ignore=shutil.ignore_patterns("*.png", "*.md"))  # cta + promo commissions, tight-cropped webp
print("built", OUT, len(page), "bytes,", len(colors), "colours,", len(list((OUT / "icons").glob("*.svg"))), "icons")
