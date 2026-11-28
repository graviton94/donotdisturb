// App book rewards: a milestone you have reached shows a claim button; claiming grants it once.
// Run after: /api/new?seed=7, /api/cheat?level=40&wallet=100000, then /api/buy for every open app (see docs/HANDOFF.md).
// node tools/game-page/booktest.js <outDir>
const { chromium } = require('playwright'); const out = process.argv[2]; require('fs').mkdirSync(out, { recursive: true });
(async () => { const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 390, height: 844 } });
const err = []; p.on('pageerror', e => err.push(e.message)); p.on('console', m => m.type() === 'error' && err.push(m.text()));
await p.goto('http://127.0.0.1:8765/index.html?notitle&auto=0&lang=ko&noftue'); await p.waitForTimeout(3000);
for (let i = 0; i < 6; i++) { await p.waitForTimeout(500); await p.evaluate(() => { if (document.querySelector('#sheet.open,#shade.open')) document.querySelector('#scrim').click(); }); }
await p.evaluate(() => __dbg.open('book')); await p.waitForTimeout(1200); await p.screenshot({ path: out + '/book.png' });
const before = await p.$$eval('[data-act="claimBook"]', x => x.length);
await p.evaluate(() => document.querySelector('[data-act="claimBook"]').click()); await p.waitForTimeout(1500); await p.screenshot({ path: out + '/claimed.png' });
const after = await p.$$eval('[data-act="claimBook"]', x => x.length), done = await p.$$eval('.bm-i.done', x => x.length);
console.log(JSON.stringify({ before, after, done }), after === before - 1 && done >= 1 ? 'ok' : 'FAIL', err.length ? err.join('\n') : 'no errors');
await b.close(); })();
