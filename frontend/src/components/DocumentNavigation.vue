<template>
  <aside class="document-navigation" :class="{ 'mobile-open': mobileOpen }">
    <button class="document-mobile-toggle" :aria-expanded="mobileOpen" @click="mobileOpen = !mobileOpen"><ListTree :size="16" /><span>阅读目录</span><ChevronDown :size="15" :class="{ turned: mobileOpen }" /></button>
    <div ref="navigationRef" class="document-navigation-content">
      <div class="document-navigation-heading"><BookOpen :size="16" /><span>{{ article.sectionName || '阅读手记' }}</span><button v-if="directory?.chapters.length" class="icon-button" :aria-label="allCollapsed ? '展开全部目录' : '收起全部目录'" :title="allCollapsed ? '展开全部' : '收起全部'" @click="toggleAll"><ChevronsUpDown :size="14" /></button></div>
      <el-select v-if="series.length" :model-value="selectedSeries" clearable placeholder="仅看本文目录" aria-label="选择阅读系列" @change="selectSeries"><el-option v-for="item in series" :key="item.id" :label="item.title" :value="String(item.id)" /></el-select>
      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-else-if="error" class="document-nav-error"><span>{{ error }}</span><button @click="load">重试</button></div>
      <DocumentTree v-else-if="directory" :nodes="tree" :expanded="expanded" :article-id="article.id" :series-id="selectedSeries" :headings="headings" :active-heading="activeHeading" @anchor="navigate" />
      <nav v-if="!directory" class="standalone-headings" aria-label="文章内部目录"><strong>{{ article.title }}</strong><button v-for="heading in headings" :key="heading.id" :style="{ paddingLeft: `${Math.max(0, heading.level - 2) * 12}px` }" :class="{ current: activeHeading === heading.id }" :aria-current="activeHeading === heading.id ? 'location' : undefined" @click="navigate(heading.id)">{{ heading.title }}</button><p v-if="!headings.length" class="muted">本文暂无章节目录</p></nav>
      <div v-if="previous || next" class="document-sequence"><RouterLink v-if="previous" :to="articleRoute(previous)"><ArrowLeft :size="14" />{{ previous.title }}</RouterLink><RouterLink v-if="next" :to="articleRoute(next)">{{ next.title }}<ArrowRight :size="14" /></RouterLink></div>
    </div>
  </aside>
</template>
<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, BookOpen, ChevronDown, ChevronsUpDown, ListTree } from '@lucide/vue'
import { getSeriesDirectory, listArticleSeries } from '../api/blog'
import { buildSeriesTree, flattenSeriesArticles } from '../utils/series'
import DocumentTree from './DocumentTree.vue'
const props = defineProps({ article: Object, headings: Array, activeHeading: String })
const emit = defineEmits(['anchor', 'sequence'])
const route = useRoute(), router = useRouter()
const series = ref([]), directory = ref(null), loading = ref(false), error = ref(''), mobileOpen = ref(false), navigationRef = ref(null)
const expanded = reactive(new Set())
const selectedSeries = computed(() => String(route.query.seriesId || ''))
const tree = computed(() => buildSeriesTree(directory.value))
const ordered = computed(() => flattenSeriesArticles(tree.value))
const index = computed(() => ordered.value.findIndex(a => String(a.id) === String(props.article.id)))
const previous = computed(() => index.value > 0 ? ordered.value[index.value - 1] : null)
const next = computed(() => index.value >= 0 ? ordered.value[index.value + 1] || null : null)
const allCollapsed = computed(() => expanded.size === 0)
let requestId = 0, revealTimer
const articleRoute = a => ({ path: '/articles/' + a.id, query: { seriesId: selectedSeries.value } })
function toggleAll() { if (expanded.size) expanded.clear(); else directory.value?.chapters.forEach(c => expanded.add(c.id)) }
function selectSeries(id) { router.replace({ query: { ...route.query, seriesId: id || undefined } }) }
function navigate(id) { emit('anchor', id); mobileOpen.value = false }
async function load() {
  const request = ++requestId
  loading.value = true; error.value = ''; directory.value = null
  try {
    const result = await listArticleSeries(props.article.id)
    if (request !== requestId) return
    series.value = result
    if (selectedSeries.value) {
      if (!result.some(s => String(s.id) === selectedSeries.value)) { error.value = '本文不属于所选系列'; return }
      const data = await getSeriesDirectory(selectedSeries.value)
      if (request !== requestId) return
      directory.value = data
      const article = data.articles.find(a => String(a.id) === String(props.article.id))
      let parentId = article?.chapterId
      const visited = new Set()
      while (parentId && !visited.has(parentId)) { visited.add(parentId); expanded.add(parentId); parentId = data.chapters.find(c => c.id === parentId)?.parentId }
    }
    await revealCurrent()
  } catch { if (request === requestId) error.value = '系列目录暂时无法加载' }
  finally {
    if (request === requestId) {
      loading.value = false
      clearTimeout(revealTimer)
      revealTimer = setTimeout(() => { if(request === requestId) revealCurrent() }, 300)
    }
  }
}
async function revealCurrent() {
  await nextTick()
  const container = navigationRef.value
  const current = container?.querySelector('[aria-current="location"]') || container?.querySelector('[aria-current="page"]')
  if (current && container) {
    const rect = current.getBoundingClientRect(), bounds = container.getBoundingClientRect()
    if (rect.top < bounds.top + 20 || rect.bottom > bounds.bottom - 20) container.scrollTo({ top: container.scrollTop + rect.top - bounds.top - bounds.height / 3, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })
  }
}
watch(() => [props.article.id, selectedSeries.value], load, { immediate: true })
watch(() => props.activeHeading, revealCurrent)
watch([previous, next], () => emit('sequence', { previous: previous.value, next: next.value, seriesId: selectedSeries.value }))
onBeforeUnmount(() => { requestId++; clearTimeout(revealTimer) })
</script>
