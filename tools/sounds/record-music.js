// Records the background music (title, main with the chain layer for the last loop) from the live page into docs/design/sounds/music-*.wav. Needs the dev server on :8765.

const { chromium } = require('playwright'); const fs = require('fs');
const wav = (pcm, sr, f) => { const n = pcm.length, buf = Buffer.alloc(44 + n * 2); buf.write('RIFF', 0); buf.writeUInt32LE(36 + n * 2, 4); buf.write('WAVEfmt ', 8); buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(1, 22); buf.writeUInt32LE(sr, 24); buf.writeUInt32LE(sr * 2, 28); buf.writeUInt16LE(2, 32); buf.writeUInt16LE(16, 34); buf.write('data', 36); buf.writeUInt32LE(n * 2, 40); for (let i = 0; i < n; i++) buf.writeInt16LE(Math.max(-32767, Math.min(32767, Math.round(pcm[i] * 32767))), 44 + i * 2); fs.writeFileSync(f, buf); };
(async () => { const b = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] });
 for (const [track, secs] of [['title', 34], ['main', 92]]) {
  const p = await b.newPage(); await p.addInitScript(() => localStorage.setItem('prefs', JSON.stringify({ sound: false, music: true, mvol: 1 })));
  await p.goto('http://127.0.0.1:8765/index.html?noftue&noattend&auto=0&lang=ko'); await p.waitForTimeout(1500);
  await p.evaluate(tr => { const { A, BGM } = __dbg.audio(); BGM.stop(.01); BGM.track = null; BGM.start(tr); const c = A.c; const sp = c.createScriptProcessor(4096, 1, 1); window.__pcm = []; sp.onaudioprocess = e => window.__pcm.push(Array.from(e.inputBuffer.getChannelData(0))); BGM.bus.connect(sp); sp.connect(c.destination); window.__sr = c.sampleRate;
    if (tr === 'main') { setInterval(() => { if (BGM.step > 16 * 32) BGM.heat = 2; }, 200); } }, track);
  await p.waitForTimeout(secs * 1000);
  const { pcm, sr } = await p.evaluate(() => ({ pcm: window.__pcm.flat(), sr: window.__sr })); wav(pcm, sr, require('path').join(__dirname, '..', '..', 'docs/design/sounds', 'music-' + track + '.wav')); console.log(track, (pcm.length / sr).toFixed(1), 's'); await p.close(); }
 await b.close(); })();
