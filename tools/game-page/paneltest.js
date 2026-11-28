// Opens the shade and every sheet, does one action in each. Run after /api/new?seed=5 and /api/cheat?level=25 (reset opens at Lv19). node tools/game-page/paneltest.js <outDir>
const { chromium } = require('playwright');
const out = process.argv[2]; require('fs').mkdirSync(out, { recursive: true });
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 390, height: 844 } });
  const errors = []; p.on('pageerror', e => errors.push(e.message)); p.on('console', m => m.type() === 'error' && errors.push(m.text()));
  await p.goto('http://127.0.0.1:8765/index.html?notitle&auto=0&lang=ko&noftue'); await p.waitForTimeout(3500);
  // the daily check-in sheet opens itself once a day: close it first
  await p.evaluate(() => document.querySelector('#scrim').click()); await p.waitForTimeout(500);
  const pay = async () => { await p.waitForTimeout(250); if (await p.$('#cf.on')) await p.click('#cf-ok'); };
  const shot = async n => { await p.waitForTimeout(450); await p.screenshot({ path: `${out}/${n}.png` }); };
  await p.mouse.move(200, 165); await p.mouse.down(); for (let y = 170; y < 330; y += 20) { await p.mouse.move(200, y); await p.waitForTimeout(10); } await p.mouse.up();
  await shot('1-shade');
  const tile = async label => { await p.click(`#shade .tile:has(b:text-is("${label}"))`); };
  await tile('앱 상점'); await shot('2-shop');
  await p.click('#sheet-b [data-act="buy"]:not([disabled])'); await pay(); await shot('3-shop-bought');
  await p.click('#sheet-x'); await p.waitForTimeout(400); await tile('강화'); await shot('4-upgrade');
  await p.click('#sheet-b [data-mode="10"]'); await p.click('#sheet-b .it [data-act="upgradeN"]:not([disabled])'); await shot('5-upgrade-x10');
  await p.click('#sheet-x'); await p.waitForTimeout(400); await tile('덱'); await shot('6-deck');
  const add = await p.$('#sheet-b [data-in]:not([disabled])'); if (add) { await add.click(); await shot('7-deck-added'); }
  await p.click('#sheet-x'); await p.waitForTimeout(400); await tile('초기화'); await shot('8-reset');
  await p.click('#sheet-b [data-act="reset"]'); await shot('9-reset-armed');
  await p.click('#sheet-b [data-act="reset"]'); await p.waitForTimeout(1200); await shot('10-after-reset');
  console.log(errors.length ? errors.join('\n') : 'no errors');
  await b.close();
})();
