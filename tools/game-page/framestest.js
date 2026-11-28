// Visual check of the UI frame kit: every frame, stretched to short/long labels, in every tint, with the game font.
// Writes <out>/frames.png and prints, per frame, how far the label's ink centre sits from the visible face centre (px).
// node tools/game-page/framestest.js <outDir>
const { chromium } = require('playwright'); const fs = require('fs'), path = require('path');
const out = process.argv[2]; fs.mkdirSync(out, { recursive: true });
const root = path.resolve(__dirname, '../..');
const FRAMES = JSON.parse(fs.readFileSync(root + '/docs/design/ui-assets/frames/frames.json', 'utf8'));
const JS = fs.readFileSync(__dirname + '/frames.js', 'utf8');
const LABELS = { 'btn-l': ['확인', '광고 보고 ×2', '무료로 받기 · 오늘 3회'], 'btn-l-pressed': ['확인', '광고 보고 ×2'], 'btn-l-off': ['잠김', '내일 다시 받을 수 있어요'], 'btn-s': ['OK', '구매', '800 P'],
  ribbon: ['NEW', '최고 기록!', '주간 순위 1위 달성!'], bubble: ['!', '새 앱', '알림이 도착했어요'], tag: ['50%', '1,200원', '₩12,000 · 한정'], 'pill-socket': ['3', '1.2K', '34.1B P'],
  count: ['1', 'NEW', '99+'], 'icon-btn': ['✕'], burst: ['×2'], sash: ['HOT'] };
(async () => {
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 1000, height: 800 } });
  const font = 'file://' + root + '/app/src/main/assets/game/fonts/pretendard/index.css';
  const rows = Object.entries(LABELS).map(([id, ls]) => `<h3>${id}</h3><div class="r">${['gold', 'blue', 'green', 'red', 'purple', 'grey'].map(t => ls.map(l => `<b class="fr fr-${id} t-${t}" data-id="${id}">${l}</b>`).join('')).join('')}</div>`).join('');
  await p.setContent(`<html><head><link rel="stylesheet" href="${font}"><style>body{margin:16px;background:#2b2f3a;color:#ddd;font:900 15px Pretendard Variable,Pretendard,sans-serif}h3{font:600 12px sans-serif;margin:12px 0 6px}.r{display:flex;flex-wrap:wrap;gap:10px;align-items:center}
    .t-gold{color:#4A2600}.fr:not(.t-gold){color:#fff}.fr-count{font-size:12px}.fr-btn-s{font-size:13px}.fr-sash{font-size:11px}</style>
    <script>${JS}; const s = document.createElement('style'); s.textContent = frameCSS(${JSON.stringify(FRAMES)}, FRAME_TINTS); document.head.appendChild(s);</script></head><body>${rows}</body></html>`);
  await p.evaluate(() => document.fonts.ready); await p.waitForTimeout(300);
  await p.screenshot({ path: out + '/frames.png', fullPage: true });
  // label ink centre vs face centre, using a range over the text node
  const off = await p.evaluate(F => [...document.querySelectorAll('.fr.t-gold')].map(el => {
    const f = F[el.dataset.id], r = el.getBoundingClientRect(), rg = document.createRange(); rg.selectNodeContents(el); const t = rg.getBoundingClientRect();
    const scale = r.height / f.h, face = r.top + f.face * scale, ink = t.top + t.height / 2;
    return [el.dataset.id, el.textContent, +(ink - face).toFixed(1)]; }), FRAMES);
  const worst = {}; off.forEach(([id, , d]) => { worst[id] = Math.max(worst[id] || 0, Math.abs(d)); });
  console.log(JSON.stringify(worst)); await b.close();
})();
