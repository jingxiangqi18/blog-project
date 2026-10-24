<template>
  <div ref="root" class="fluid-tabs" :class="[`fluid-tabs--${appearance}`, { 'is-ready': ready }]" role="group" :aria-label="label" @keydown="onKeydown">
    <span class="fluid-tabs-indicator" :style="indicator" aria-hidden="true" />
    <component
      :is="item.to ? RouterLink : 'button'"
      v-for="item in items"
      :key="item.value"
      :to="item.to"
      :type="item.to ? undefined : 'button'"
      class="fluid-tab"
      :class="{ active: modelValue === item.value }"
      :data-value="item.value"
      :aria-current="item.to && modelValue === item.value ? 'page' : undefined"
      :aria-pressed="item.to ? undefined : modelValue === item.value"
      @click="!item.to && $emit('update:modelValue', item.value)"
    >
      <component :is="item.icon" v-if="item.icon" class="fluid-tab-icon" :size="16" aria-hidden="true" />
      <span>{{ item.label }}</span>
      <span v-if="item.count != null" class="fluid-tab-count">{{ item.count }}</span>
    </component>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  modelValue: { type: String, default: '' },
  items: { type: Array, required: true },
  label: { type: String, required: true },
  appearance: { type: String, default: 'surface' },
})
defineEmits(['update:modelValue'])
const root = ref(null), ready = ref(false), indicator = ref({ opacity: 0 })
let observer, frame, disposed = false

function measure() {
  if (!root.value || disposed) return
  const selected = [...root.value.children].find(el => el.dataset.value === props.modelValue)
  indicator.value = selected ? { width: `${selected.offsetWidth}px`, height: `${selected.offsetHeight}px`, transform: `translate(${selected.offsetLeft}px, ${selected.offsetTop}px)`, opacity: 1 } : { opacity: 0 }
}
function scheduleMeasure() { if (disposed) return; cancelAnimationFrame(frame); frame = requestAnimationFrame(() => { measure(); ready.value = true }) }
function onKeydown(event) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  const controls = [...root.value.querySelectorAll('.fluid-tab')]
  const current = controls.indexOf(document.activeElement)
  if (current < 0) return
  event.preventDefault()
  const index = event.key === 'Home' ? 0 : event.key === 'End' ? controls.length - 1 : (current + (event.key === 'ArrowRight' ? 1 : -1) + controls.length) % controls.length
  controls[index].focus()
  if (controls[index].tagName === 'BUTTON') controls[index].click()
}
watch(() => [props.modelValue, props.items], async () => { await nextTick(); scheduleMeasure() }, { deep: true })
onMounted(async () => {
  await nextTick()
  if (disposed) return
  measure()
  frame = requestAnimationFrame(() => { ready.value = true })
  observer = new ResizeObserver(scheduleMeasure)
  observer.observe(root.value)
  document.fonts.ready.then(() => { if (root.value) scheduleMeasure() })
})
onBeforeUnmount(() => { disposed = true; observer?.disconnect(); cancelAnimationFrame(frame) })
</script>

<style scoped>
.fluid-tabs { --tab-height: 38px; position: relative; isolation: isolate; display: inline-flex; align-items: center; gap: 3px; padding: 4px; border: 1px solid #dfe1e6; border-radius: 8px; background: #e9ebee; box-shadow: inset 0 1px 2px #222b3b08; max-width: 100%; overflow-x: auto; scrollbar-width: none; vertical-align: middle; }
.fluid-tabs::-webkit-scrollbar { display: none; }
.fluid-tabs-indicator { position: absolute; z-index: -1; top: 0; left: 0; border: 1px solid #d8dce2; border-radius: 5px; background: #fff; box-shadow: 0 2px 4px #222b3b0a, 0 1px 1px #222b3b06; pointer-events: none; }
.is-ready .fluid-tabs-indicator { transition: transform 420ms var(--ease-spring), width 320ms var(--ease), height 320ms var(--ease), opacity 180ms; }
.fluid-tab { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; gap: 7px; min-height: var(--tab-height); padding: 7px 15px; border: 0; border-radius: 5px; background: transparent; color: var(--muted); font-size: 12px; font-weight: 550; white-space: nowrap; transition: color 180ms, background 180ms; }
.fluid-tab:hover { color: var(--text); background: #ffffff45; }
.fluid-tab.active { color: var(--text); }
.fluid-tab:active { transform: scale(.97); }
.fluid-tab-icon { width: 16px; height: 16px; flex-shrink: 0; }
.fluid-tab-count { display: inline-grid; place-items: center; min-width: 20px; padding: 1px 5px; border-radius: 4px; background: #1e253309; color: var(--muted); font-size: 10px; font-variant-numeric: tabular-nums; }
.fluid-tabs--navigation { --tab-height: 32px; padding: 3px; background: #eceef1b3; border-color: #e2e4e9b3; }
.fluid-tabs--navigation .fluid-tab { padding: 6px 13px; font-size: 12px; }
.fluid-tabs--compact { --tab-height: 28px; padding: 3px; gap: 2px; }
.fluid-tabs--compact .fluid-tab { font-size: 11px; padding: 4px 9px; gap: 5px; }
.fluid-tabs--compact .fluid-tab-icon { width: 14px; height: 14px; }
@media (pointer: coarse) { .fluid-tabs { --tab-height: 40px; } }
</style>
