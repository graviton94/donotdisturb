// UI frames (docs/design/ui-assets/frames, greyscale SVG kit): recoloured by hex and stretched with CSS border-image.
// FRAMES = { id: { svg, w, h, ins: [L,T,R,B] | null (fixed), face: y-centre of the visible face } }.
// Usage: <b class="uf uf-btn-l ut-gold">Text</b>; size with --s (scale of the viewBox, e.g. .5 = half size).
// Labels are centred on the visible face, not on the box: the drop shadow and bottom bevel sit below the face.
function frameCSS(FRAMES, TINTS) {
  const GREYS = ['#1a1a1a', '#595959', '#a6a6a6', '#d9d9d9'];
  const tint = (svg, ramp) => svg.replace(/#[0-9a-fA-F]{6}/g, h => { const i = GREYS.indexOf(h.toLowerCase()); return i < 0 ? h : ramp[i]; });
  const url = s => `url("data:image/svg+xml;charset=utf-8,${encodeURIComponent(s)}")`;
  let css = 'ui-fr{display:inline-flex}button.uf{cursor:pointer;font-family:inherit;-webkit-tap-highlight-color:transparent}.uf{--s:.5;display:inline-flex;align-items:center;justify-content:center;box-sizing:border-box;white-space:nowrap;line-height:1;vertical-align:middle;border-style:solid;border-color:transparent;background:none}\n';
  for (const [id, f] of Object.entries(FRAMES)) {
    const k = `.uf-${id}`;
    if (f.ins) {
      const [L, T, R, B] = f.ins, img = g => `${url(g)} ${T} ${R} ${B} ${L} fill stretch`;
      // padding moves the label's centre from the box's content centre onto the visible face
      const pad = face => { const d = (T + f.h - B) / 2 - face; return d > 0 ? `0 0 calc(${2 * d}px*var(--s))` : `calc(${-2 * d}px*var(--s)) 0 0`; };
      css += `${k}{height:calc(${f.h}px*var(--s));min-width:calc(${L + R}px*var(--s));border-width:calc(${T}px*var(--s)) calc(${R}px*var(--s)) calc(${B}px*var(--s)) calc(${L}px*var(--s));padding:${pad(f.face)}}\n`;
      for (const [t, ramp] of Object.entries(TINTS)) css += `${k}.ut-${t}{border-image:${img(tint(f.svg, ramp))}}\n`;
      // a frame with -pressed / -off siblings becomes a real button: pressed while held, greyed when disabled
      const pr = FRAMES[id + '-pressed'], off = FRAMES[id + '-off'];
      if (pr) for (const [t, ramp] of Object.entries(TINTS)) css += `button${k}.ut-${t}:active:not(:disabled){border-image:${img(tint(pr.svg, ramp))};padding:${pad(pr.face)}}\n`;
      if (off) css += `button${k}:disabled{border-image:${img(tint(off.svg, TINTS.grey))};padding:${pad(off.face)};color:#5A6070!important;text-shadow:none!important;cursor:default}\n`;
    } else {
      css += f.label
        ? `${k}{width:calc(${f.w}px*var(--s));height:calc(${f.h}px*var(--s));border:0;background-size:100% 100%;position:relative;color:transparent}${k}>*{position:absolute;left:calc(${f.label.x}px*var(--s));top:calc(${f.label.y}px*var(--s));transform:translate(-50%,-50%) rotate(${f.label.rotate}deg)}\n`
        : `${k}{width:calc(${f.w}px*var(--s));height:calc(${f.h}px*var(--s));border:0;background-size:100% 100%;padding:0 0 calc(${Math.max(0, f.h / 2 - f.face) * 2}px*var(--s))}\n`;
      for (const [t, ramp] of Object.entries(TINTS)) css += `${k}.ut-${t}{background-image:${url(tint(f.svg, ramp))}}\n`;
    }
  }
  return css;
}
// outline / bevel / base bottom / base top, per colour
const FRAME_TINTS = {
  gold: ['#4A2600', '#B86E00', '#F5A300', '#FFE27A'], blue: ['#0E1E5C', '#1D3FB8', '#2F6BE6', '#8CB8FF'],
  green: ['#0B3D1E', '#128A43', '#22B45A', '#8EEBA8'], red: ['#4A0808', '#A3121A', '#E8251A', '#FF8A7A'],
  purple: ['#24104F', '#5B2FC9', '#7B4DFF', '#C9B4FF'], grey: ['#4B5160', '#8A92A3', '#B8BFCC', '#E3E7EE'],
};
