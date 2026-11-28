// UI frames (docs/design/ui-assets/frames, greyscale SVG kit): recoloured by hex and stretched with CSS border-image.
// FRAMES = { id: { svg, w, h, ins: [L,T,R,B] | null (fixed), face: y-centre of the visible face } }.
// Usage: <b class="fr fr-btn-l t-gold">Text</b>; size with --s (scale of the viewBox, e.g. .5 = half size).
// Labels are centred on the visible face, not on the box: the drop shadow and bottom bevel sit below the face.
function frameCSS(FRAMES, TINTS) {
  const GREYS = ['#1a1a1a', '#595959', '#a6a6a6', '#d9d9d9'];
  const tint = (svg, ramp) => svg.replace(/#[0-9a-fA-F]{6}/g, h => { const i = GREYS.indexOf(h.toLowerCase()); return i < 0 ? h : ramp[i]; });
  const url = s => `url("data:image/svg+xml;charset=utf-8,${encodeURIComponent(s)}")`;
  let css = '.fr{--s:.5;display:inline-flex;align-items:center;justify-content:center;box-sizing:border-box;white-space:nowrap;line-height:1;vertical-align:middle;border-style:solid;border-color:transparent;background:none}\n';
  for (const [id, f] of Object.entries(FRAMES)) {
    const k = `.fr-${id}`;
    if (f.ins) {
      const [L, T, R, B] = f.ins, content = (T + f.h - B) / 2, lift = Math.max(0, content - f.face) * 2;  // padding below lifts the label's centre onto the face
      css += `${k}{height:calc(${f.h}px*var(--s));min-width:calc(${L + R}px*var(--s));border-width:calc(${T}px*var(--s)) calc(${R}px*var(--s)) calc(${B}px*var(--s)) calc(${L}px*var(--s));padding:0 0 calc(${lift}px*var(--s))}\n`;
      for (const [t, ramp] of Object.entries(TINTS)) css += `${k}.t-${t}{border-image:${url(tint(f.svg, ramp))} ${T} ${R} ${B} ${L} fill stretch}\n`;
    } else {
      css += f.label
        ? `${k}{width:calc(${f.w}px*var(--s));height:calc(${f.h}px*var(--s));border:0;background-size:100% 100%;position:relative;color:transparent}${k}>*{position:absolute;left:calc(${f.label.x}px*var(--s));top:calc(${f.label.y}px*var(--s));transform:translate(-50%,-50%) rotate(${f.label.rotate}deg)}\n`
        : `${k}{width:calc(${f.w}px*var(--s));height:calc(${f.h}px*var(--s));border:0;background-size:100% 100%;padding:0 0 calc(${Math.max(0, f.h / 2 - f.face) * 2}px*var(--s))}\n`;
      for (const [t, ramp] of Object.entries(TINTS)) css += `${k}.t-${t}{background-image:${url(tint(f.svg, ramp))}}\n`;
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
