<template>
  <component :is="to ? RouterLink : interactive ? 'button' : 'span'" :to="to" :type="interactive && !to ? 'button' : undefined" class="tech-badge" :class="{ 'is-interactive': to || interactive, 'is-selected': selected }" :style="colors" :aria-pressed="interactive && !to ? selected : undefined" :title="name">
    <span class="tech-badge-symbol" aria-hidden="true"><component :is="icon" :size="12" :stroke-width="1.9" /></span><span class="tech-badge-label">{{ name }}</span>
  </component>
</template>
<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Braces, CodeXml, Coffee, Database, Hash, Leaf } from '@lucide/vue'
import { tagAppearance } from '../utils/tags'
const props = defineProps({ tag: { type: Object, required: true }, to: [String, Object], interactive: Boolean, selected: { type: Boolean, default: undefined } })
const name = computed(() => props.tag.name || props.tag.slug || '标签')
const appearance = computed(() => tagAppearance(props.tag))
const icons = { braces: Braces, code: CodeXml, coffee: Coffee, database: Database, hash: Hash, leaf: Leaf }
const icon = computed(() => icons[appearance.value.icon])
const colors = computed(() => ({ '--badge-bg': appearance.value.background, '--badge-border': appearance.value.border, '--badge-ink': appearance.value.ink, '--badge-solid': appearance.value.solid }))
</script>
<style scoped>
.tech-badge { display: inline-flex; align-items: stretch; max-width: 100%; min-height: 25px; padding: 0; overflow: hidden; border: 1px solid var(--badge-border); border-radius: 5px; background: var(--badge-bg); color: var(--badge-ink); box-shadow: inset 0 1px 0 #ffffff75; font-family: var(--font-body); font-size: 11px; font-weight: 550; line-height: 1.5; vertical-align: middle; }
.tech-badge-symbol { display: inline-grid; place-items: center; width: 25px; min-height: 23px; flex-shrink: 0; background: var(--badge-solid); color: #fff; box-shadow: inset 0 1px 0 #ffffff20; }
.tech-badge-label { padding: 3px 9px; overflow-wrap: anywhere; min-width: 0; text-align: left; }
.is-interactive { cursor: pointer; transition: transform 240ms var(--ease-spring), box-shadow 180ms; text-decoration: none; }
.is-interactive:hover { transform: translateY(-2px); box-shadow: 0 3px 6px #202c3e0c, inset 0 1px 0 #ffffff75; }
.is-interactive:active { transform: translateY(0) scale(.97); }
.is-selected { box-shadow: 0 0 0 2px #ffffff, 0 0 0 3px var(--badge-border); }
@media (pointer: coarse) { button.tech-badge, a.tech-badge { min-height: 32px; } .tech-badge-label { align-self: center; } }
</style>
