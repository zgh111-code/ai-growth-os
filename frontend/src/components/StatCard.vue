<template>
  <div class="stat-card" :class="`card-${color}`">
    <div class="stat-top-bar"></div>
    <div class="stat-icon-box">
      <slot name="icon">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
      </slot>
    </div>
    <div class="stat-data">
      <span class="stat-num">{{ value }}<small v-if="sub" class="stat-unit">{{ sub }}</small></span>
      <span class="stat-name">{{ label }}</span>
    </div>
    <div class="stat-shine"></div>
  </div>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  sub: String,
  color: { type: String, default: 'blue' }
})
</script>

<style scoped>
.stat-card {
  position: relative;
  border-radius: 18px;
  padding: 18px 20px 16px;
  border: 1px solid transparent;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1.2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: default;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.08);
}
.stat-card:hover .stat-shine { opacity: 0.5; }
.stat-card:hover .stat-top-bar { opacity: 1; transform: scaleX(1.04); }

.stat-top-bar {
  position: absolute;
  top: 0; left: 14px; right: 14px;
  height: 3px;
  border-radius: 0 0 8px 8px;
  transition: all 0.3s;
  opacity: 0.65;
}
.stat-shine {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 70% 0%, rgba(255,255,255,0.5) 0%, transparent 60%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
  border-radius: 18px;
}
.stat-icon-box {
  width: 38px; height: 38px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s;
  position: relative; z-index: 1;
}
.stat-card:hover .stat-icon-box { transform: scale(1.05); }

.stat-data {
  display: flex; flex-direction: column; gap: 2px;
  position: relative; z-index: 1;
}
.stat-num {
  font-size: 30px; font-weight: 750;
  letter-spacing: -0.02em; line-height: 1.1;
}
.stat-unit {
  font-size: 14px; font-weight: 520;
  margin-left: 2px; opacity: 0.7;
}
.stat-name {
  font-size: 12.5px; font-weight: 550;
  opacity: 0.7; letter-spacing: 0.02em;
}

/* ==========================================
   颜色方案
   ========================================== */

/* Blue — 校园蓝（默认） */
.card-blue {
  background: linear-gradient(145deg, #F0F5FF 0%, #E0ECFF 100%);
  border-color: #BDD3FF;
  box-shadow: 0 2px 12px rgba(79,140,255,0.06);
}
.card-blue .stat-top-bar { background: linear-gradient(90deg, #4F8CFF, #7CCBFF); }
.card-blue .stat-icon-box { background: rgba(79,140,255,0.12); color: #3B6FDF; }
.card-blue .stat-num { color: #1E3A8A; }
.card-blue .stat-name { color: #3B6FDF; }

/* Purple */
.card-purple {
  background: linear-gradient(145deg, #F5F3FF 0%, #EDE9FE 100%);
  border-color: #DDD6FE;
  box-shadow: 0 2px 12px rgba(139,92,246,0.06);
}
.card-purple .stat-top-bar { background: linear-gradient(90deg, #8B5CF6, #A78BFA); }
.card-purple .stat-icon-box { background: rgba(139,92,246,0.12); color: #7C3AED; }
.card-purple .stat-num { color: #5B21B6; }
.card-purple .stat-name { color: #7C3AED; }

/* Cyan */
.card-cyan {
  background: linear-gradient(145deg, #ECFEFF 0%, #CFFAFE 100%);
  border-color: #A5F3FC;
  box-shadow: 0 2px 12px rgba(6,182,212,0.06);
}
.card-cyan .stat-top-bar { background: linear-gradient(90deg, #06B6D4, #22D3EE); }
.card-cyan .stat-icon-box { background: rgba(6,182,212,0.12); color: #0891B2; }
.card-cyan .stat-num { color: #155E75; }
.card-cyan .stat-name { color: #0891B2; }

/* Amber */
.card-amber {
  background: linear-gradient(145deg, #FFFBEB 0%, #FEF3C7 100%);
  border-color: #FDE68A;
  box-shadow: 0 2px 12px rgba(245,158,11,0.06);
}
.card-amber .stat-top-bar { background: linear-gradient(90deg, #F59E0B, #FBBF24); }
.card-amber .stat-icon-box { background: rgba(245,158,11,0.12); color: #D97706; }
.card-amber .stat-num { color: #92400E; }
.card-amber .stat-name { color: #B45309; }

/* Coral */
.card-coral {
  background: linear-gradient(145deg, #FFF5F3 0%, #FFEAE6 100%);
  border-color: #FFD5CC;
  box-shadow: 0 2px 12px rgba(255,107,91,0.06);
}
.card-coral .stat-top-bar { background: linear-gradient(90deg, #FF6B5B, #FF8A7A); }
.card-coral .stat-icon-box { background: rgba(255,107,91,0.12); color: #E04A3A; }
.card-coral .stat-num { color: #A62818; }
.card-coral .stat-name { color: #D14536; }

/* Teal */
.card-teal {
  background: linear-gradient(145deg, #F0FDFA 0%, #CCFBF1 100%);
  border-color: #99F6E4;
  box-shadow: 0 2px 12px rgba(20,184,166,0.06);
}
.card-teal .stat-top-bar { background: linear-gradient(90deg, #14B8A6, #2DD4BF); }
.card-teal .stat-icon-box { background: rgba(20,184,166,0.12); color: #0F766E; }
.card-teal .stat-num { color: #134E4A; }
.card-teal .stat-name { color: #115E59; }

/* Pink — 保留兼容 */
.card-pink {
  background: linear-gradient(145deg, #FFF1F7 0%, #FCE7F3 100%);
  border-color: #FBCFE8;
  box-shadow: 0 2px 12px rgba(236,72,153,0.06);
}
.card-pink .stat-top-bar { background: linear-gradient(90deg, #EC4899, #F472B6); }
.card-pink .stat-icon-box { background: rgba(236,72,153,0.12); color: #DB2777; }
.card-pink .stat-num { color: #9D174D; }
.card-pink .stat-name { color: #BE185D; }
</style>
