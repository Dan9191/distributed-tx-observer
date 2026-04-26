import type { VisualizationStep, GroupInstance, TemplateEdge } from '../api'

// ── Точные значения из исходных компонентов ──────────────────────────────────

const STEP_W  = 168   // min-w-[160px] + небольшой запас для текста
const STEP_H  = 60    // py-3 (12*2) + step-name ~18px + service ~16px + gap 2px
const MARKER_W = 88   // min-w-[80px] + px-5 (20*2)
const MARKER_H = 36   // py-2 (8*2) + text-xs (~12px * 1.5 ≈ 18px)
const CANVAS_PAD = 60

// Цвета из Visualizer.tsx LOG_LEVEL_COLOR
const NODE_COLOR: Record<string, string> = {
  info:  '#639922',
  warn:  '#BA7517',
  error: '#A32D2D',
  none:  '#9ca3af',   // Tailwind gray-400
}

// Tailwind-цвета из LEVEL_BADGE
// info:  bg-green-50  text-green-700  border-green-200
// warn:  bg-yellow-50 text-yellow-700 border-yellow-200
// error: bg-red-50    text-red-700    border-red-200
// none:  bg-gray-50   text-gray-500   border-gray-200
const BADGE: Record<string, { bg: string; text: string; border: string }> = {
  info:  { bg: '#f0fdf4', text: '#15803d', border: '#bbf7d0' },
  warn:  { bg: '#fefce8', text: '#a16207', border: '#fef08a' },
  error: { bg: '#fef2f2', text: '#b91c1c', border: '#fecaca' },
  none:  { bg: '#f9fafb', text: '#6b7280', border: '#e5e7eb' },
}

const FONT = `ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif`

function esc(s: string | null | undefined): string {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function hexAlpha(hex: string, a: number): string {
  const n = parseInt(hex.replace('#', ''), 16)
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`
}

function nodeSize(nt: string) {
  return (nt === 'start' || nt === 'end')
    ? { w: MARKER_W, h: MARKER_H }
    : { w: STEP_W,   h: STEP_H   }
}

interface Pt { x: number; y: number }

function stepCenter(s: VisualizationStep, off: Pt) {
  const { w, h } = nodeSize(s.nodeType ?? 'step')
  return { cx: s.x! + off.x + w / 2, cy: s.y! + off.y + h / 2, w, h, lx: s.x! + off.x, ty: s.y! + off.y }
}

function buildEdgePath(
  steps: VisualizationStep[],
  edge: TemplateEdge,
  off: Pt,
): string {
  const src = steps.find(s => s.instanceId === edge.fromInstanceId)
  const tgt = steps.find(s => s.instanceId === edge.toInstanceId)
  if (!src || src.x == null || !tgt || tgt.x == null) return ''

  const sc = stepCenter(src, off)
  const tc = stepCenter(tgt, off)

  // Выбираем порт в зависимости от взаимного расположения узлов (как React Flow)
  let sx: number, sy: number, tx: number, ty: number, cpx1: number, cpy1: number, cpx2: number, cpy2: number

  const dx = tc.cx - sc.cx
  const dy = tc.cy - sc.cy

  if (Math.abs(dx) >= Math.abs(dy)) {
    // Горизонтальный поток
    if (dx >= 0) {
      sx = sc.lx + sc.w; sy = sc.cy; tx = tc.lx;       ty = tc.cy
    } else {
      sx = sc.lx;         sy = sc.cy; tx = tc.lx + tc.w; ty = tc.cy
    }
    const cdx = Math.abs(tx - sx) * 0.45
    cpx1 = sx + (dx >= 0 ? cdx : -cdx); cpy1 = sy
    cpx2 = tx - (dx >= 0 ? cdx : -cdx); cpy2 = ty
  } else {
    // Вертикальный поток
    if (dy >= 0) {
      sx = sc.cx; sy = sc.ty + sc.h; tx = tc.cx; ty = tc.ty
    } else {
      sx = sc.cx; sy = sc.ty;         tx = tc.cx; ty = tc.ty + tc.h
    }
    const cdy = Math.abs(ty - sy) * 0.45
    cpx1 = sx; cpy1 = sy + (dy >= 0 ? cdy : -cdy)
    cpx2 = tx; cpy2 = ty - (dy >= 0 ? cdy : -cdy)
  }

  return `M ${sx} ${sy} C ${cpx1} ${cpy1} ${cpx2} ${cpy2} ${tx} ${ty}`
}

// ── Главная функция ──────────────────────────────────────────────────────────

export function generateHtmlReport(
  txName: string,
  operationId: string,
  steps: VisualizationStep[],
  groups: GroupInstance[],
  edges: TemplateEdge[],
): string {
  const vis = steps.filter(s => s.x != null && s.y != null)

  // bounding box
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  for (const s of vis) {
    const { w, h } = nodeSize(s.nodeType ?? 'step')
    minX = Math.min(minX, s.x!);    minY = Math.min(minY, s.y!)
    maxX = Math.max(maxX, s.x! + w); maxY = Math.max(maxY, s.y! + h)
  }
  for (const g of groups) {
    minX = Math.min(minX, g.x);         minY = Math.min(minY, g.y)
    maxX = Math.max(maxX, g.x + g.width); maxY = Math.max(maxY, g.y + g.height)
  }
  if (!isFinite(minX)) { minX = 0; minY = 0; maxX = 500; maxY = 300 }

  const off: Pt   = { x: -minX + CANVAS_PAD, y: -minY + CANVAS_PAD }
  const canvasW   = maxX - minX + CANVAS_PAD * 2
  const canvasH   = maxY - minY + CANVAS_PAD * 2

  const activeIds = new Set(vis.filter(s => s.logLevel !== 'none').map(s => s.instanceId))

  // Цвет узла — точная копия логики из Visualizer.tsx
  function nodeColor(s: VisualizationStep): string {
    const nt = s.nodeType ?? 'step'
    if (nt === 'start') {
      return edges.some(e => e.fromInstanceId === s.instanceId && activeIds.has(e.toInstanceId))
        ? NODE_COLOR.info : NODE_COLOR.none
    }
    if (nt === 'end') {
      return edges.some(e => e.toInstanceId === s.instanceId && activeIds.has(e.fromInstanceId))
        ? NODE_COLOR.info : NODE_COLOR.none
    }
    return NODE_COLOR[s.logLevel] ?? NODE_COLOR.none
  }

  // ── Groups ────────────────────────────────────────────────────────────────
  const groupsHtml = groups.map(g => {
    const left = g.x + off.x
    const top  = g.y + off.y
    return `<div style="position:absolute;left:${left}px;top:${top}px;width:${g.width}px;height:${g.height}px;
border:2px solid ${g.color};border-radius:8px;background:${hexAlpha(g.color, 0.07)};
box-sizing:border-box;z-index:0">
  <div style="padding:8px 12px 4px;font-size:14px;font-weight:600;color:${g.color};font-family:${FONT}">${esc(g.label)}</div>
</div>`
  }).join('\n')

  // ── Nodes ─────────────────────────────────────────────────────────────────
  const nodesHtml = vis.map(s => {
    const left  = s.x! + off.x
    const top   = s.y! + off.y
    const nt    = s.nodeType ?? 'step'
    const color = nodeColor(s)
    const { w } = nodeSize(nt)
    const clickable = nt === 'step' && s.logs.length >= 0

    if (nt === 'start' || nt === 'end') {
      const label = nt === 'start' ? 'СТАРТ' : 'СТОП'
      return `<div id="node-${s.instanceId}"
style="position:absolute;left:${left}px;top:${top}px;width:${w}px;height:${MARKER_H}px;
border:2px solid ${color};border-radius:9999px;background:${hexAlpha(color, 0.07)};
display:flex;align-items:center;justify-content:center;
box-sizing:border-box;z-index:2;
box-shadow:0 1px 2px rgba(0,0,0,0.05);transition:box-shadow .15s">
<span style="font-size:12px;font-weight:700;letter-spacing:0.1em;text-transform:uppercase;color:${color};font-family:${FONT}">${label}</span>
</div>`
    }

    // step node
    const badge = s.logs.length > 1
      ? `<div title="${s.logs.length} лог-записей"
style="position:absolute;top:-10px;right:-10px;min-width:20px;height:20px;padding:0 4px;
border-radius:9999px;background:${color};color:#fff;font-size:11px;font-weight:700;
display:flex;align-items:center;justify-content:center;user-select:none;z-index:1;font-family:${FONT}">
×${s.logs.length}</div>`
      : ''

    return `<div id="node-${s.instanceId}"
${clickable ? `onclick="selectStep(${s.instanceId})" class="node-step"` : ''}
style="position:absolute;left:${left}px;top:${top}px;width:${w}px;min-height:${STEP_H}px;
border:2px solid ${color};border-radius:8px;background:#fff;
padding:12px 16px;box-sizing:border-box;z-index:2;
${clickable ? 'cursor:pointer;' : ''}box-shadow:0 1px 2px rgba(0,0,0,0.05);
transition:box-shadow .15s;user-select:none">
${badge}
<p style="font-weight:500;color:#111827;font-size:14px;line-height:1.25;margin:0;font-family:${FONT}">${esc(s.stepName)}</p>
<p style="font-size:12px;color:#9ca3af;margin-top:2px;margin-bottom:0;font-family:${FONT}">${esc(s.serviceName)}</p>
</div>`
  }).join('\n')

  // ── SVG edges ─────────────────────────────────────────────────────────────
  const svgPaths = edges.map(e => {
    const d = buildEdgePath(vis, e, off)
    if (!d) return ''
    const bothActive = activeIds.has(e.fromInstanceId) && activeIds.has(e.toInstanceId)
    const stroke = bothActive ? '#3b82f6' : '#94a3b8'
    const sw     = bothActive ? 2.5 : 1.5
    const marker = bothActive ? 'url(#arrow-active)' : 'url(#arrow-default)'
    return `<path d="${d}" fill="none" stroke="${stroke}" stroke-width="${sw}" marker-end="${marker}"/>`
  }).join('\n')

  // ── Steps data for JS ─────────────────────────────────────────────────────
  const stepsJson = JSON.stringify(
    Object.fromEntries(
      vis
        .filter(s => (s.nodeType ?? 'step') === 'step')
        .map(s => [s.instanceId, {
          stepName:    s.stepName,
          serviceName: s.serviceName,
          logLevel:    s.logLevel,
          logs:        s.logs.map(l => ({ ts: l.timestamp, lvl: l.level, msg: l.message, flds: l.fields ?? {} })),
        }]),
    ),
  )

  const badgeJson = JSON.stringify(BADGE)
  const colorJson = JSON.stringify(NODE_COLOR)

  const genTime = new Date().toLocaleString('ru-RU')

  // ── HTML ──────────────────────────────────────────────────────────────────
  return `<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Отчёт: ${esc(txName)}</title>
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
body{font-family:${FONT};background:#f8fafc;color:#1e293b;height:100vh;overflow:hidden;display:flex;flex-direction:column}

/* ── Header ── */
#hdr{display:flex;align-items:center;justify-content:space-between;padding:16px 24px;border-bottom:1px solid #e5e7eb;background:#fff;flex-shrink:0}
#hdr-left{display:flex;align-items:center;gap:16px}
#hdr-title{font-size:18px;font-weight:600;color:#111827;line-height:1.25}
#hdr-sub{font-size:12px;color:#9ca3af;margin-top:1px}
#hdr-right{display:flex;align-items:center;gap:12px}
#hdr-opid{padding:8px 12px;font-size:13px;border:1px solid #e5e7eb;border-radius:6px;
  font-family:ui-monospace,SFMono-Regular,monospace;color:#374151;background:#f9fafb;max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
#hdr-time{font-size:12px;color:#9ca3af}

/* ── Main layout ── */
#main{display:flex;flex:1;overflow:hidden}

/* ── Canvas area ── */
#canvas-wrap{flex:1;overflow:auto;position:relative;
  background-color:#fafafa;
  background-image:radial-gradient(circle,#d1d5db 1px,transparent 1px);
  background-size:16px 16px}
#canvas{position:relative}
#canvas svg{position:absolute;top:0;left:0;width:100%;height:100%;overflow:visible;pointer-events:none}

/* ── RF Controls ── */
#controls{position:absolute;bottom:16px;left:16px;z-index:20;
  display:flex;flex-direction:column;border-radius:6px;
  border:1px solid #e5e7eb;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08)}
.ctrl-btn{width:26px;height:26px;background:#fff;border:none;cursor:pointer;
  display:flex;align-items:center;justify-content:center;
  font-size:14px;color:#374151;border-bottom:1px solid #e5e7eb;transition:background .1s}
.ctrl-btn:last-child{border-bottom:none}
.ctrl-btn:hover{background:#f9fafb}

/* ── Node hover/select ── */
.node-step:hover{box-shadow:0 0 0 3px rgba(59,130,246,0.25) !important}
.node-step.selected{box-shadow:0 0 0 3px #3b82f6 !important}

/* ── Log panel — точная копия w-80 в Visualizer ── */
#log-panel{width:320px;flex-shrink:0;border-left:1px solid #e5e7eb;background:#fff;display:none;flex-direction:column;overflow:hidden}
#log-panel.open{display:flex}

#lp-hdr{display:flex;align-items:center;gap:8px;padding:12px 16px;border-bottom:1px solid #e5e7eb;flex-shrink:0}
#lp-hdr-info{flex:1;min-width:0}
#lp-name{font-weight:500;font-size:14px;color:#111827;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
#lp-svc{font-size:12px;color:#9ca3af;margin-top:1px}
#lp-badge{padding:2px 8px;border-radius:9999px;font-size:12px;font-weight:500;text-transform:uppercase}
#lp-close{background:none;border:none;font-size:18px;color:#9ca3af;cursor:pointer;line-height:1;margin-left:4px;flex-shrink:0;padding:2px}
#lp-close:hover{color:#374151}

#lp-entries{flex:1;overflow-y:auto}
.log-entry{padding:12px 16px;border-bottom:1px solid #f3f4f6}
.log-row{display:flex;align-items:center;gap:8px;margin-bottom:4px}
.log-ts{font-size:12px;color:#9ca3af;font-family:ui-monospace,SFMono-Regular,monospace;font-variant-numeric:tabular-nums}
.log-lvl{padding:2px 6px;border-radius:4px;font-size:11px;font-weight:500;text-transform:uppercase}
.log-msg{font-size:13px;color:#374151;line-height:1.625;overflow-wrap:break-word;word-break:break-word}
.no-logs{font-size:14px;color:#9ca3af;text-align:center;padding:32px 16px}
</style>
</head>
<body>

<!-- Header — точная копия заголовка Visualizer -->
<div id="hdr">
  <div id="hdr-left">
    <div>
      <div id="hdr-title">${esc(txName)}</div>
      <div id="hdr-sub">Визуализация — отчёт</div>
    </div>
  </div>
  <div id="hdr-right">
    <div id="hdr-opid" title="${esc(operationId)}">${esc(operationId)}</div>
    <span id="hdr-time">Сформирован: ${esc(genTime)}</span>
  </div>
</div>

<!-- Main -->
<div id="main">

  <!-- Canvas -->
  <div id="canvas-wrap">
    <div id="controls">
      <button class="ctrl-btn" onclick="zoom(0.15)" title="Увеличить">+</button>
      <button class="ctrl-btn" onclick="zoom(-0.15)" title="Уменьшить">−</button>
      <button class="ctrl-btn" onclick="resetZoom()" title="Сбросить масштаб" style="font-size:12px">⊡</button>
    </div>
    <div id="canvas" style="width:${canvasW}px;height:${canvasH}px">
      <svg>
        <defs>
          <marker id="arrow-default" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
            <path d="M0,0 L10,5 L0,10 Z" fill="#94a3b8"/>
          </marker>
          <marker id="arrow-active" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
            <path d="M0,0 L10,5 L0,10 Z" fill="#3b82f6"/>
          </marker>
        </defs>
        ${svgPaths}
      </svg>
      ${groupsHtml}
      ${nodesHtml}
    </div>
  </div>

  <!-- Log panel — точная копия LogPanel из Visualizer -->
  <div id="log-panel">
    <div id="lp-hdr">
      <div id="lp-hdr-info">
        <div id="lp-name"></div>
        <div id="lp-svc"></div>
      </div>
      <span id="lp-badge"></span>
      <button id="lp-close" onclick="closePanel()">×</button>
    </div>
    <div id="lp-entries"></div>
  </div>

</div><!-- /main -->

<script>
const STEPS  = ${stepsJson};
const BADGE  = ${badgeJson};
const COLOR  = ${colorJson};
let curId    = null;
let curScale = 1;
const canvas = document.getElementById('canvas');

/* zoom */
function zoom(d){
  curScale = Math.max(0.2, Math.min(3, curScale + d));
  canvas.style.transform = 'scale(' + curScale + ')';
  canvas.style.transformOrigin = '0 0';
}
function resetZoom(){
  curScale = 1;
  canvas.style.transform = '';
  document.getElementById('canvas-wrap').scrollTo(0,0);
}

/* time format */
function fmt(iso){
  try{return new Date(iso).toLocaleTimeString('ru-RU',{hour:'2-digit',minute:'2-digit',second:'2-digit'});}
  catch{return iso;}
}
function e(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}

/* badge HTML */
function badgeHtml(lvl, pill){
  const b = BADGE[lvl] || BADGE.none;
  const r = pill ? '9999px' : '4px';
  const p = pill ? '2px 8px' : '2px 6px';
  const fs = pill ? '12px' : '11px';
  return '<span style="padding:'+p+';border-radius:'+r+';font-size:'+fs+
    ';font-weight:'+(pill?'500':'500')+';text-transform:uppercase;background:'+b.bg+
    ';color:'+b.text+';border:1px solid '+b.border+'">'+e(lvl)+'</span>';
}

function selectStep(id){
  /* снять предыдущее выделение */
  if(curId !== null){
    var prev = document.getElementById('node-'+curId);
    if(prev) prev.classList.remove('selected');
  }
  curId = id;
  var node = document.getElementById('node-'+id);
  if(node) node.classList.add('selected');

  var step = STEPS[id];
  if(!step) return;

  document.getElementById('lp-name').textContent = step.stepName;
  document.getElementById('lp-svc').textContent  = step.serviceName;

  var badge = document.getElementById('lp-badge');
  var b = BADGE[step.logLevel] || BADGE.none;
  badge.textContent = step.logLevel;
  badge.style.cssText = 'padding:2px 8px;border-radius:9999px;font-size:12px;font-weight:500;text-transform:uppercase;'+
    'background:'+b.bg+';color:'+b.text+';border:1px solid '+b.border;

  var entries = document.getElementById('lp-entries');
  if(!step.logs || step.logs.length === 0){
    entries.innerHTML = '<div class="no-logs">Логов не найдено</div>';
  } else {
    entries.innerHTML = step.logs.map(function(l){
      var flds = l.flds ? Object.entries(l.flds) : [];
      var fldsHtml = flds.length === 0 ? '' :
        '<div style="margin-top:6px;background:#f9fafb;border-radius:4px;padding:6px 8px;'+
        'font-family:ui-monospace,SFMono-Regular,monospace;font-size:11px;color:#6b7280">'+
        flds.map(function(kv){
          return '<div style="display:flex;gap:4px;min-width:0">'+
            '<span style="color:#9ca3af;flex-shrink:0">'+e(String(kv[0]))+':</span>'+
            '<span style="color:#374151;word-break:break-all">'+e(String(kv[1]))+'</span>'+
            '</div>';
        }).join('')+'</div>';
      return '<div class="log-entry">'+
        '<div class="log-row"><span class="log-ts">'+e(fmt(l.ts))+'</span>'+badgeHtml(l.lvl,false)+'</div>'+
        '<div class="log-msg">'+e(l.msg)+'</div>'+
        fldsHtml+
      '</div>';
    }).join('');
  }

  document.getElementById('log-panel').classList.add('open');
}

function closePanel(){
  document.getElementById('log-panel').classList.remove('open');
  if(curId !== null){
    var n = document.getElementById('node-'+curId);
    if(n) n.classList.remove('selected');
    curId = null;
  }
}
</script>
</body>
</html>`
}
