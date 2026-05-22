<template>
  <div class="stat-card">
    <span class="stat-label">{{ label }}</span>
    <span class="stat-value">{{ value }}</span>
    <span v-if="sub" class="stat-sub">{{ sub }}</span>
  </div>
</template>

<script setup>
defineProps({ label: String, value: [String, Number], sub: String })
</script>

<style scoped>
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.stat-label {
  font-size: 13px;
  color: #86868b;
  font-weight: 450;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
  line-height: 1.1;
}
.stat-sub {
  font-size: 12px;
  color: #aeaeb2;
}
</style>
