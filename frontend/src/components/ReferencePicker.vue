<template>
  <section class="reference-picker" aria-label="引用内容" @keydown="onKeydown">
    <header class="reference-picker-heading"><span class="reference-picker-mark"><Link2 :size="16" /></span><strong>添加引用</strong><button class="icon-button" type="button" aria-label="关闭引用选择器" title="关闭" @click="$emit('close')"><X :size="16" /></button></header>
    <div class="reference-picker-search"><Search :size="16" /><input ref="searchInput" :value="query" :placeholder="kind === 'article' ? '搜索文章标题或正文' : '搜索知识卡片'" aria-label="搜索引用内容" role="combobox" aria-autocomplete="list" aria-expanded="true" :aria-controls="listId" :aria-activedescendant="activeId" @input="updateQuery($event.target.value)" /><button v-if="query" class="reference-clear" type="button" aria-label="清空引用搜索" title="清空搜索" @click="updateQuery(''); searchInput?.focus()"><X :size="13" /></button></div>
    <div class="reference-picker-types"><FluidTabs :model-value="kind" :items="kinds" appearance="compact" label="引用类型" @update:model-value="changeKind" /><span role="status">{{ loading ? '正在查找' : error ? '暂时不可用' : `${total} ${kind === 'article' ? '篇文章' : '张卡片'}` }}</span></div>
    <div ref="list" class="reference-picker-results" :aria-busy="loading">
      <div v-if="loading" class="reference-picker-skeleton" aria-label="加载引用结果"><span v-for="i in 4" :key="i"><i /><span><b /><em /></span></span></div>
      <div v-else-if="error" class="reference-picker-state"><CircleAlert :size="25" /><p>{{ error }}</p><button type="button" @click="load"><RotateCw :size="13" />重试</button></div>
      <div v-else-if="!entries.length" class="reference-picker-state"><FileSearch :size="28" /><p>{{ query.trim() ? '没有找到可引用的内容' : '本页暂无可引用内容' }}</p></div>
      <div v-else :id="listId" role="listbox" :aria-label="kind === 'article' ? '可引用文章' : '可引用知识卡片'" class="reference-picker-list">
        <button v-for="(item, index) in entries" :id="`${listId}-${index}`" :key="`${kind}-${item.id}`" class="reference-result" :class="{ active: active === index }" type="button" role="option" :aria-selected="active === index" tabindex="-1" @mousemove="active = index" @mousedown.prevent @click="choose(item)">
          <span class="reference-result-symbol" :class="kind"><FileText v-if="kind === 'article'" :size="18" /><Layers v-else :size="18" /></span>
          <span class="reference-result-copy"><strong>{{ item.title }}</strong><small>{{ item.owner }}<span v-if="item.categoryName"> · {{ item.categoryName }}</span></small><span v-if="item.excerpt" class="reference-result-excerpt">{{ item.excerpt }}</span></span>
          <CornerDownLeft class="reference-result-enter" :size="15" aria-hidden="true" />
        </button>
      </div>
    </div>
    <footer class="reference-picker-footer"><span>{{ kind === 'article' ? '站内文章' : '知识卡片' }}</span><div><button type="button" aria-label="上一页引用结果" title="上一页" :disabled="loading || page <= 1" @click="changePage(-1)"><ChevronLeft :size="15" /></button><span>{{ page }} <i>/</i> {{ totalPages }}</span><button type="button" aria-label="下一页引用结果" title="下一页" :disabled="loading || page >= totalPages" @click="changePage(1)"><ChevronRight :size="15" /></button></div></footer>
  </section>
</template>

<script setup>
import { computed, markRaw, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, CircleAlert, CornerDownLeft, FileSearch, FileText, Layers, Link2, RotateCw, Search, X } from '@lucide/vue'
import FluidTabs from './FluidTabs.vue'
import { listArticles, listKnowledgeCards } from '../api/blog'
import { markdownToPlainText } from '../utils/markdown'

const props = defineProps({ keyword: { type: String, default: '' }, currentArticleId: [String, Number], listId: { type: String, required: true } })
const emit = defineEmits(['choose', 'close', 'active'])
const kinds = [{ value: 'article', label: '文章', icon: markRaw(FileText) }, { value: 'card', label: '知识卡片', icon: markRaw(Layers) }]
const kind = ref('article'), query = ref(props.keyword), page = ref(1), total = ref(0), totalPages = ref(1), active = ref(0)
const loading = ref(true), error = ref(''), items = ref([]), cards = ref([]), searchInput = ref(null), list = ref(null)
const pageSize = 5
let request = 0, timer, disposed = false
const entries = computed(() => (kind.value === 'article' ? items.value : cards.value.slice((page.value - 1) * pageSize, page.value * pageSize)).map(item => ({
  ...item,
  owner: item.authorName || item.createdByName || '未知作者',
  excerpt: item.summary || markdownToPlainText(item.content || '', { title: item.title }).slice(0, 90),
})))
const activeId = computed(() => !loading.value && !error.value && entries.value[active.value] ? `${props.listId}-${active.value}` : undefined)

function schedule(delay = 0) { clearTimeout(timer); request++; loading.value = true; error.value = ''; timer = setTimeout(load, delay) }
function updateQuery(value) { query.value = value; page.value = 1; active.value = 0; schedule(220) }
function changeKind(value) { if (value === kind.value) return; kind.value = value; page.value = 1; active.value = 0; schedule() }
function changePage(direction) {
  const target = Math.max(1, Math.min(totalPages.value, page.value + direction))
  if (target === page.value || loading.value) return
  page.value = target; active.value = 0
  if (kind.value === 'article') schedule()
  else list.value?.scrollTo({ top: 0 })
}
async function load() {
  const current = ++request
  loading.value = true; error.value = ''
  try {
    if (kind.value === 'article') {
      const data = await listArticles({ page: page.value, size: pageSize, keyword: query.value.trim() || undefined })
      if (current !== request || disposed) return
      items.value = (data.content || []).filter(item => String(item.id) !== String(props.currentArticleId))
      total.value = data.totalElements || 0; totalPages.value = Math.max(1, data.totalPages || 1)
    } else {
      const data = await listKnowledgeCards({ keyword: query.value.trim() || undefined })
      if (current !== request || disposed) return
      cards.value = data; total.value = data.length; totalPages.value = Math.max(1, Math.ceil(data.length / pageSize))
    }
    active.value = 0
    await nextTick(); list.value?.scrollTo({ top: 0 })
  } catch { if (current === request && !disposed) error.value = kind.value === 'article' ? '文章暂时无法加载' : '知识卡片暂时无法加载' }
  finally { if (current === request && !disposed) loading.value = false }
}
function choose(item) { if (!loading.value && !error.value) emit('choose', { type: kind.value, item }) }
function handleKey(key) {
  if (key === 'Tab') { searchInput.value?.focus(); return true }
  if (key === 'Escape') { emit('close'); return true }
  if (['ArrowDown', 'ArrowUp', 'Home', 'End', 'Enter', 'PageDown', 'PageUp'].includes(key)) {
    if (loading.value) return true
    if (key === 'PageDown' || key === 'PageUp') { changePage(key === 'PageDown' ? 1 : -1); return true }
    if (key === 'Enter') { if (entries.value[active.value]) choose(entries.value[active.value]); return true }
    if (!entries.value.length) return true
    active.value = key === 'Home' ? 0 : key === 'End' ? entries.value.length - 1 : (active.value + (key === 'ArrowDown' ? 1 : -1) + entries.value.length) % entries.value.length
    nextTick(() => document.getElementById(activeId.value)?.scrollIntoView({ block: 'nearest' }))
    return true
  }
  return false
}
function onKeydown(event) {
  if (event.isComposing || event.keyCode === 229) return
  const fromInput = event.target === searchInput.value
  if (fromInput && ['Home', 'End', 'ArrowLeft', 'ArrowRight', 'Tab'].includes(event.key)) return
  if (event.key === 'Escape' || (fromInput && handleKey(event.key))) { if (event.key === 'Escape') emit('close'); event.preventDefault(); event.stopPropagation() }
}
watch(() => props.keyword, value => { if (value !== query.value) updateQuery(value) })
watch(activeId, value => emit('active', value), { immediate: true })
schedule()
onBeforeUnmount(() => { disposed = true; request++; clearTimeout(timer) })
defineExpose({ handleKey })
</script>

<style scoped>
.reference-picker { display: flex; flex-direction: column; width: 100%; max-width: 100%; min-height: 0; flex: 1; overflow: hidden; border: 1px solid #dce1e9; border-radius: 8px; background: #fafbfdf7; color: var(--text); font-family: var(--font-body); box-shadow: 0 24px 70px #202c3e26, 0 5px 18px #202c3e0b, inset 0 1px 0 #fff; backdrop-filter: saturate(140%) blur(20px); animation: reference-reveal 300ms var(--ease-spring); }
.reference-picker-heading { display: flex; align-items: center; gap: 9px; padding: 12px 16px 8px; flex-shrink: 0; }
.reference-picker-mark { display: inline-grid; place-items: center; width: 28px; height: 28px; color: #596d86; border: 1px solid #dde3eb; border-radius: 6px; background: #fff; box-shadow: var(--shadow-control); }
.reference-picker-heading > strong { font-size: 12px; font-weight: 600; }
.reference-picker-heading > button { width: 28px; height: 28px; margin-left: auto; }
.reference-picker-search { display: flex; align-items: center; gap: 9px; margin: 3px 16px 12px; padding: 0 11px; min-height: 39px; border: 1px solid #dce2e9; border-radius: 6px; background: #eef1f5; box-shadow: inset 0 1px 2px #202c3e06; color: #8390a1; flex-shrink: 0; transition: border-color 180ms, box-shadow 180ms; }
.reference-picker-search:focus-within { border-color: #a5b6ca; box-shadow: 0 0 0 3px #49667f0d; }
.reference-picker-search input { width: 100%; min-width: 0; border: 0; outline: 0; padding: 9px 0; background: transparent; color: var(--text); font-family: var(--font-body); font-size: 12px; }
.reference-picker-search input::placeholder { color: #8a96a5; }
.reference-clear { display: inline-grid; place-items: center; width: 22px; height: 22px; padding: 0; flex-shrink: 0; border: 0; border-radius: 4px; background: transparent; color: var(--muted); }
.reference-clear:hover { background: #dfe5ed; }
.reference-picker-types { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 0 16px 11px; flex-shrink: 0; }
.reference-picker-types > span { font-size: 10px; color: var(--muted); white-space: nowrap; }
.reference-picker-results { min-height: 0; max-height: 300px; flex: 1 1 auto; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; scrollbar-color: #c4cdd8 transparent; padding: 0 9px 6px; border-top: 1px solid #e7ebf0; }
.reference-picker-list { min-height: 160px; padding-top: 6px; }
.reference-result { display: grid; grid-template-columns: 33px minmax(0, 1fr) 17px; align-items: center; gap: 12px; width: 100%; min-height: 69px; padding: 10px; margin-bottom: 3px; border: 1px solid transparent; border-radius: 6px; background: transparent; color: var(--text); text-align: left; transition: background 160ms, border-color 160ms, box-shadow 160ms; }
.reference-result.active { background: #fff; border-color: #dce3ed; box-shadow: 0 2px 4px #202c3e08; }
.reference-result-symbol { display: inline-grid; place-items: center; width: 32px; height: 36px; border: 1px solid #d9e2ed; border-radius: 6px; background: #edf2f8; color: #637c9b; }
.reference-result-symbol.card { border-color: #e5dccb; background: #f7f2e8; color: #9a7e4c; }
.reference-result-copy { display: grid; gap: 4px; min-width: 0; }
.reference-result-copy > strong { font-family: var(--font-body); font-size: 12px; font-weight: 600; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.reference-result-copy > small { color: #7c8796; font-size: 10px; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.reference-result-excerpt { color: #929aa6; font-size: 10px; line-height: 1.5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.reference-result-enter { color: #7f8c9f; opacity: 0; transform: translateX(-3px); transition: opacity 160ms, transform 250ms var(--ease-spring); }
.reference-result.active .reference-result-enter { opacity: 1; transform: translateX(0); }
.reference-picker-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 16px; min-height: 43px; border-top: 1px solid #e0e5ed; background: #eff2f6; color: #7d8999; font-size: 10px; flex-shrink: 0; }
.reference-picker-footer > div { display: inline-flex; align-items: center; gap: 10px; }
.reference-picker-footer button { display: inline-grid; place-items: center; width: 26px; height: 26px; padding: 0; border: 1px solid #d7dfe9; border-radius: 5px; background: #fff; color: #64768b; box-shadow: var(--shadow-control); transition: transform 220ms var(--ease-spring), background 160ms; }
.reference-picker-footer button:hover:not(:disabled) { transform: translateY(-1px); background: #f9fbfd; }
.reference-picker-footer button:active:not(:disabled) { transform: scale(.91); }
.reference-picker-footer > div > span { min-width: 38px; text-align: center; font-family: var(--font-code); }
.reference-picker-footer i { font-style: normal; color: #b1bac6; margin: 0 3px; }
.reference-picker-state { display: grid; place-items: center; align-content: center; gap: 12px; min-height: 160px; color: #939daa; font-size: 12px; text-align: center; }
.reference-picker-state > p { margin: 0; }
.reference-picker-state > button { display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border: 1px solid var(--line); border-radius: 5px; background: #fff; color: var(--text-soft); font-size: 11px; }
.reference-picker-skeleton { padding: 6px; }
.reference-picker-skeleton > span { display: flex; align-items: center; gap: 14px; padding: 16px 4px; }
.reference-picker-skeleton > span > i { width: 32px; height: 36px; border-radius: 5px; background: #e8edf3; flex-shrink: 0; }
.reference-picker-skeleton > span > span { display: grid; gap: 9px; width: 80%; }
.reference-picker-skeleton b, .reference-picker-skeleton em { width: 80%; height: 9px; background: #e8edf3; border-radius: 3px; animation: reference-pulse 900ms ease-in-out infinite alternate; }
.reference-picker-skeleton em { width: 50%; }
@keyframes reference-reveal { from { opacity: 0; transform: translateY(6px) scale(.975); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes reference-pulse { to { opacity: .4; } }
@media (max-width: 600px) { .reference-result { gap: 10px; padding: 10px 8px; } .reference-picker-types { gap: 8px; } .reference-picker-search input { font-size: 16px; } }
@media (pointer: coarse) { .reference-picker-footer button { width: 36px; height: 36px; } .reference-picker-heading > button { width: 36px; height: 36px; } }
</style>
