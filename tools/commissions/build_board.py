"""Builds the asset commission board page from docs/design/commissions.json.

Usage: python3 tools/commissions/build_board.py OUT.html
Delivered groups are moved from "pending" to "delivered" in the JSON, then the page is rebuilt and republished.
"""
import html, json, pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
d = json.loads((ROOT / "docs/design/commissions.json").read_text())
PRI = {1: "1순위 · 그레이박스 상점", 2: "2순위 · 프리미엄 화면", 3: "3순위 · 메타", 4: "4순위 · 소프트런치 전"}

def prompt(g):
    if g.get("format") == "svg":  # vector kit: one SVG per item, each with its viewBox and stretch insets
        lines = [g.get("spec", d["spec"]), "", g["intro"], "", f'Deliver {len(g["items"])} separate SVG files:']
        for it in g["items"]:
            ins = "fixed size, scale uniformly only" if it["insets"] == "fixed" else f'stretch insets (left,top,right,bottom) = {it["insets"]}'
            lines.append(f'- {it["id"]}.svg, viewBox 0 0 {it["viewBox"].replace("x", " ")}, {ins}: {it["desc"]}.')
        return "\n".join(lines + ["", g["rules"]])
    cw, ch = g["cell"]
    lines = [g.get("spec", d["spec"]), "", g["intro"], "",
             f'Sprite sheet: {g["cols"]} columns x {g["rows"]} rows of equal cells, each cell {cw}x{ch} px, whole sheet {cw * g["cols"]}x{ch * g["rows"]} px.']
    for i, it in enumerate(g["items"]):
        r, c = divmod(i, g["cols"])
        lines.append(f"Row {r + 1}, column {c + 1}: " + ("leave this cell EMPTY." if it is None else it["desc"] + "."))
    lines += ["", g.get("rules", d["sheetRules"])]
    return "\n".join(lines)

def grid(g):
    if g.get("format") == "svg":
        return '<div class="grid" style="grid-template-columns:repeat(auto-fill,minmax(130px,1fr));max-width:none">' + "".join(
            f'<div class="cell"><b>{html.escape(it["id"])}.svg</b><span>{html.escape(it["note"])}</span><span>{it["viewBox"]} · {"고정" if it["insets"] == "fixed" else "늘림 " + it["insets"]}</span></div>' for it in g["items"]) + '</div>'
    cells = "".join(
        '<div class="cell empty">비움</div>' if it is None else f'<div class="cell"><b>{html.escape(it["id"])}</b><span>{html.escape(it["note"])}</span></div>'
        for it in g["items"])
    return f'<div class="grid" style="grid-template-columns:repeat({g["cols"]},minmax(0,1fr));aspect-ratio:{g["cell"][0] * g["cols"]}/{g["cell"][1] * g["rows"]}">{cells}</div>'

def meta(g):
    n = sum(1 for it in g["items"] if it); dest = html.escape(g.get("dest", "docs/design/ui-assets/" + g["id"] + "/"))
    if g.get("format") == "svg":
        return f'SVG {n}개 · 회색 5단계만 사용(색은 게임이 헥스로 입힘) · 늘림 수치는 border-image 기준(왼,위,오른,아래) · 받으면 <code>{dest}</code>에 저장'
    return f'시트 1장 · {g["cols"]}×{g["rows"]}칸 · 요소 {n}개 · 받으면 칸별로 잘라 <code>{dest}</code>에 저장'

def card(g):
    full = prompt(g)
    n = sum(1 for it in g["items"] if it)
    return f'''<article class="card">
  <header><div><p class="pri">{PRI[g["priority"]]}</p><h2>{html.escape(g["title"])}</h2></div>
  <button type="button" class="copy" data-copy="{html.escape(full, quote=True)}">프롬프트 복사</button></header>
  <p class="meta">{meta(g)}</p>
  {grid(g)}
  <details><summary>프롬프트 보기</summary><pre>{html.escape(full)}</pre></details>
</article>'''

cards = "\n".join(card(g) for g in d["pending"]) or '<p class="empty">대기 중인 발주가 없습니다.</p>'
done = "".join(f'<li><span>{html.escape(x["date"])}</span>{html.escape(x["title"])}</li>' for x in d["delivered"])
total = sum(1 for g in d["pending"] for it in g["items"] if it)
page = f'''<!DOCTYPE html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>방해 금지! 에셋 발주 보드</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;700;900&display=swap">
<style>
:root{{color-scheme:light dark;--bg:light-dark(#EEF1F7,#100D0A);--ink:light-dark(#16181D,#F6F1EA);--sub:light-dark(#4B5160,#B3A89B);--card:light-dark(#FFFFFF,#201B16);--line:light-dark(#DDE2EC,#3A332C);--acc:light-dark(#3867F0,#EFA03F);--onacc:light-dark(#FFFFFF,#1E1408);--code:light-dark(#F3F5FA,#16120E)}}
:root[data-theme=light]{{color-scheme:light}}:root[data-theme=dark]{{color-scheme:dark}}
*{{box-sizing:border-box;min-width:0}}
body{{margin:0;background:var(--bg);color:var(--ink);font:16px/1.6 'Noto Sans KR',system-ui,sans-serif}}
main{{max-width:880px;margin:0 auto;padding:clamp(20px,4vw,44px) max(4vw,16px)}}
h1{{margin:0 0 6px;font-size:clamp(28px,4vw,38px);font-weight:900;letter-spacing:-.02em;line-height:1.1}}
.lead{{margin:0 0 24px;color:var(--sub);max-width:64ch}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:18px 20px;margin:0 0 16px}}
.card header{{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap}}
h2{{margin:0;font-size:19px}} .pri{{margin:0 0 2px;font-size:12.5px;font-weight:700;color:var(--acc)}}
.meta{{margin:8px 0;color:var(--sub);font-size:14px}}
button{{font:inherit;font-weight:700;font-size:14px;min-height:44px;padding:0 18px;border-radius:999px;border:0;background:var(--acc);color:var(--onacc);cursor:pointer}}
button:focus-visible{{outline:3px solid var(--acc);outline-offset:2px}}

.grid{{display:grid;gap:6px;margin:10px 0;max-width:460px}} .cell{{border:1.5px dashed var(--line);border-radius:10px;padding:6px;display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center;font-size:12.5px;line-height:1.3}} .cell b{{font-size:12px;font-family:ui-monospace,monospace}} .cell span{{color:var(--sub);font-size:11.5px}} .cell.empty{{color:var(--sub);opacity:.6}}
.files{{list-style:none;margin:0;padding:0;display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:4px 16px;font-size:14px}}
.files li{{display:flex;gap:8px;align-items:baseline}} .files span{{color:var(--sub)}}
code{{font-size:12.5px;background:var(--code);padding:1px 5px;border-radius:5px}}
details{{margin-top:10px}} summary{{cursor:pointer;color:var(--sub);font-size:14px}}
pre{{white-space:pre-wrap;background:var(--code);border-radius:12px;padding:12px;font-size:13px;line-height:1.5;overflow-wrap:anywhere}}
.done{{color:var(--sub);font-size:14px;padding-left:18px}} .done span{{font-variant-numeric:tabular-nums;margin-right:8px}}
.toast{{position:fixed;left:50%;bottom:24px;transform:translateX(-50%);background:var(--ink);color:var(--bg);padding:10px 16px;border-radius:999px;font-size:14px;opacity:0;transition:opacity .2s;pointer-events:none}}
.toast.on{{opacity:1}}
</style></head><body><main>
<h1>에셋 발주 보드</h1>
<p class="lead">대기 중인 발주 {len(d["pending"])}장 · 요소 {total}개. 한 프롬프트 = <b>시트 한 장</b>(요소를 격자 칸에 정위치). "프롬프트 복사"는 공통 사양까지 한 번에 복사합니다. 시트를 대화에 올리면 칸별로 잘라 반입하고 이 목록에서 지웁니다.</p>
{cards}
<h2 style="margin-top:28px">반입 완료</h2>
<ul class="done">{done}</ul>
</main><div class="toast" role="status" aria-live="polite"></div>
<script>
{{
const toast = document.querySelector('.toast');
document.querySelectorAll('.copy').forEach(b => b.addEventListener('click', async () => {{
  const text = b.dataset.copy;
  try {{ await navigator.clipboard.writeText(text); }}
  catch (e) {{ const t = document.createElement('textarea'); t.value = text; document.body.append(t); t.select(); document.execCommand('copy'); t.remove(); }}
  toast.textContent = '복사했어요'; toast.classList.add('on'); setTimeout(() => toast.classList.remove('on'), 1400);
}}));
}}
</script></body></html>'''
pathlib.Path(sys.argv[1]).write_text(page)
print("pending", len(d["pending"]), "files", total)
