<template>
  <section class="page-stack collections-page">
    <header class="collection-heading">
      <div><p class="section-kicker">Collections</p><h2>内容目录</h2></div>
      <el-button v-if="canCreate" :icon="Plus" type="primary" @click="edit()">{{ tab === 'series' ? '新建系列' : tab === 'sections' ? '新建分区' : '新建标签' }}</el-button>
    </header>
    <el-tabs v-model="tab">
      <el-tab-pane label="系列" name="series" />
      <el-tab-pane label="分区" name="sections" />
      <el-tab-pane label="标签" name="tags" />
    </el-tabs>
    <div class="collection-filters">
      <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索目录" aria-label="搜索目录" @input="search" />
      <el-select v-if="tab === 'series'" v-model="sectionId" clearable placeholder="全部分区" @change="resetPage">
        <el-option v-for="s in sections" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon><el-button text @click="load">重试</el-button></el-alert>
    <el-skeleton v-else-if="loading" :rows="6" animated />
    <el-empty v-else-if="!visibleItems.length" description="暂无匹配内容" />
    <div v-else class="collection-rows">
      <article v-for="item in visibleItems" :key="item.id" class="collection-row">
        <div class="collection-row-copy">
          <small>{{ item.sectionName || (tab === 'tags' ? '#' + item.slug : item.slug) }}{{ item.enabled === false ? ' · 已禁用' : '' }}</small>
          <TechBadge v-if="tab === 'tags'" :tag="item" interactive @click="visit(item)" />
          <button v-else class="collection-title" @click="visit(item)">{{ item.title || item.name }}</button>
          <p v-if="item.description">{{ item.description }}</p>
        </div>
        <div class="collection-row-actions">
          <el-switch v-if="tab === 'sections' && admin" :model-value="item.enabled" :disabled="busy" :aria-label="item.name + '启用状态'" @change="toggleSection(item, $event)" />
          <el-button v-if="manageable(item)" :icon="EditPen" circle aria-label="编辑" title="编辑" @click="edit(item)" />
          <el-button v-if="tab !== 'sections' && manageable(item)" :icon="Delete" circle aria-label="删除" title="删除" :disabled="busy" @click="remove(item)" />
          <el-button :icon="ArrowRight" circle aria-label="查看" title="查看" @click="visit(item)" />
        </div>
      </article>
    </div>
    <el-pagination v-if="tab === 'series' && total > 12" v-model:current-page="page" :total="total" :page-size="12" layout="prev, pager, next" @current-change="load" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑目录' : '新建目录'" width="520px" destroy-on-close>
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="名称" required><el-input v-model="form.name" :maxlength="tab === 'series' ? 200 : 100" /></el-form-item>
        <el-form-item label="标识" required><el-input v-model="form.slug" maxlength="100" placeholder="java-backend" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        <template v-if="tab === 'series'">
          <el-form-item label="所属分区" required>
            <el-select v-model="form.sectionId" @change="form.categoryId = null">
              <el-option v-for="s in seriesSectionOptions" :key="s.id" :label="s.name" :value="s.id" :disabled="s.enabled === false && s.id !== originalSectionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类"><el-select v-model="form.categoryId" clearable placeholder="不指定分类"><el-option v-for="c in compatibleCategories" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        </template>
        <template v-if="tab === 'sections'">
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :precision="0" /></el-form-item>
          <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
        </template>
        <el-alert v-if="formError" :title="formError" type="error" :closable="false" />
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="busy" @click="save">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="readerOpen" :title="selected?.title || '系列目录'" size="min(620px, 100vw)">
      <el-skeleton v-if="readerLoading" :rows="6" animated />
      <el-alert v-else-if="readerError" :title="readerError" type="error" :closable="false"><el-button text @click="openSeries(selected)">重试</el-button></el-alert>
      <template v-else-if="selected">
        <p class="collection-description">{{ selected.description }}</p>
        <el-empty v-if="!chapters.length" description="尚未加入文章" />
        <ol class="chapter-list">
          <li v-for="(chapter, index) in chapters" :key="chapter.article.id">
            <span class="chapter-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <RouterLink :to="{ path: '/articles/' + chapter.article.id, query: { seriesId: selected.id } }">{{ chapter.article.title }}</RouterLink>
            <div v-if="canManageResource(selected)" class="chapter-actions">
              <el-button :icon="ArrowUp" circle size="small" title="上移" aria-label="上移" :disabled="busy || index === 0" @click="moveChapter(index, -1)" />
              <el-button :icon="ArrowDown" circle size="small" title="下移" aria-label="下移" :disabled="busy || index === chapters.length - 1" @click="moveChapter(index, 1)" />
              <el-button :icon="Close" circle size="small" title="移出系列" aria-label="移出系列" :disabled="busy" @click="removeChapter(chapter.article.id)" />
            </div>
          </li>
        </ol>
        <SeriesChapterManager v-if="canManageResource(selected)" :key="selected.id + ':' + chapters.length" :series-id="selected.id" />
        <section v-if="canManageResource(selected)" class="chapter-picker">
          <h3>添加章节</h3>
          <el-input v-model="articleKeyword" :prefix-icon="Search" clearable placeholder="搜索文章" @input="searchCandidates" />
          <el-alert v-if="candidateError" :title="candidateError" type="error" :closable="false"><el-button text @click="loadCandidates">重试</el-button></el-alert>
          <el-skeleton v-else-if="candidateLoading" :rows="3" animated />
          <template v-else>
            <div v-for="a in candidates" :key="a.id" class="chapter-candidate"><span>{{ a.title }}</span><el-button :icon="Plus" circle title="加入系列" aria-label="加入系列" :disabled="busy || chapters.some(c => c.article.id === a.id) || (!!a.sectionId && a.sectionId !== selected.sectionId)" @click="addChapter(a.id)" /></div>
            <el-empty v-if="!candidates.length" description="没有匹配文章" />
          </template>
          <el-pagination v-if="candidateTotal > 6" v-model:current-page="candidatePage" :page-size="6" :total="candidateTotal" layout="prev, pager, next" :pager-count="5" @current-change="loadCandidates" />
        </section>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SeriesChapterManager from '../components/SeriesChapterManager.vue'
import TechBadge from '../components/TechBadge.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, EditPen, Delete, ArrowRight, ArrowUp, ArrowDown, Close } from '@element-plus/icons-vue'
import * as api from '../api/blog'
import { isAdmin, isSignedIn, canManageResource } from '../utils/permissions'

const router = useRouter()
const route = useRoute()
const tab = ref('series'), keyword = ref(''), sectionId = ref(null), page = ref(1), total = ref(0)
const items = ref([]), sections = ref([]), categories = ref([])
const loading = ref(false), busy = ref(false), error = ref(''), dialog = ref(false), formError = ref('')
const form = reactive({}), originalSectionId = ref(null)
const admin = computed(isAdmin)
const canCreate = computed(() => tab.value === 'series' ? isSignedIn() : admin.value)
const compatibleCategories = computed(() => categories.value.filter(c => !c.sectionId || c.sectionId === form.sectionId))
const seriesSectionOptions = computed(() => sections.value.some(s => s.id === originalSectionId.value) || !originalSectionId.value ? sections.value : [...sections.value, { id: originalSectionId.value, name: '原分区（已禁用）', enabled: false }])
const visibleItems = computed(() => tab.value === 'series' ? items.value : items.value.filter(i => `${i.name} ${i.slug}`.toLowerCase().includes(keyword.value.trim().toLowerCase())))
const readerOpen = ref(false), readerLoading = ref(false), readerError = ref(''), selected = ref(null), chapters = ref([])
const articleKeyword = ref(''), candidatePage = ref(1), candidateTotal = ref(0), candidates = ref([]), candidateError = ref(''), candidateLoading = ref(false)
let timer, candidateTimer, requestId = 0, candidateRequest = 0, readerRequest = 0
const message = e => e.response?.data?.message || e.response?.data?.error || '请求失败，请重试'
function manageable(item) { return tab.value === 'series' ? canManageResource(item) : admin.value }
async function load() {
  const request = ++requestId
  loading.value = true; error.value = ''
  try {
    const result = tab.value === 'series' ? await api.listSeries({ page: page.value, size: 12, keyword: keyword.value.trim(), sectionId: sectionId.value || undefined, sectionSlug: route.query.sectionSlug || undefined }) : tab.value === 'sections' ? await (admin.value ? api.listAllSections() : api.listSections()) : await api.listTags()
    if (request !== requestId) return
    items.value = result.content || result
    total.value = result.totalElements || 0
  } catch (e) { if (request === requestId) error.value = message(e) }
  finally { if (request === requestId) loading.value = false }
}
function resetPage() { page.value = 1; load() }
function search() { clearTimeout(timer); requestId++; timer = setTimeout(resetPage, 250) }
function edit(item = {}) {
  if (!canCreate.value || (item.id && !manageable(item))) return
  originalSectionId.value = item.sectionId || null
  Object.assign(form, { id: item.id, name: item.title || item.name || '', slug: item.slug || '', description: item.description || '', sectionId: item.sectionId || null, categoryId: item.categoryId || null, sortOrder: item.sortOrder ?? 0, enabled: item.enabled ?? true })
  formError.value = ''; dialog.value = true
}
async function save() {
  if (busy.value || !canCreate.value) return
  if (!form.name.trim() || !/^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/.test(form.slug)) { formError.value = '请输入名称和有效标识，例如 java-backend'; return }
  if (tab.value === 'series' && !form.sectionId) { formError.value = '请选择分区'; return }
  busy.value = true; formError.value = ''
  const data = { slug: form.slug, description: form.description, ...(tab.value === 'series' ? { title: form.name.trim(), sectionId: form.sectionId, categoryId: form.categoryId || null } : { name: form.name.trim() }), ...(tab.value === 'sections' ? { sortOrder: form.sortOrder ?? 0, enabled: form.enabled } : {}) }
  const methods = tab.value === 'series' ? [api.createSeries, api.updateSeries] : tab.value === 'sections' ? [api.createSection, api.updateSection] : [api.createTag, api.updateTag]
  try {
    await (form.id ? methods[1](form.id, data) : methods[0](data)); dialog.value = false
    if (tab.value === 'sections') sections.value = await api.listAllSections()
    await load(); ElMessage.success('已保存')
  } catch (e) { formError.value = message(e) } finally { busy.value = false }
}
async function remove(item) {
  if (!manageable(item) || busy.value) return
  try { await ElMessageBox.confirm(`删除「${item.title || item.name}」及其关联？文章正文会保留。`, '删除目录', { type: 'warning' }) } catch { return }
  busy.value = true
  try { await (tab.value === 'series' ? api.deleteSeries(item.id) : api.deleteTag(item.id)); if (items.value.length === 1 && page.value > 1) page.value--; await load() }
  catch (e) { error.value = message(e) } finally { busy.value = false }
}
async function toggleSection(item, enabled) {
  if (!admin.value || busy.value) return
  busy.value = true
  try { await api.updateSectionEnabled(item.id, enabled); item.enabled = enabled; sections.value = await api.listAllSections() }
  catch (e) { error.value = message(e) } finally { busy.value = false }
}
function visit(item) {
  if (tab.value === 'series') openSeries(item)
  else if (tab.value === 'sections') router.push({ path: '/articles', query: { sectionSlug: item.slug } })
  else router.push({ path: '/articles', query: { tagSlug: item.slug } })
}
async function openSeries(item) {
  const request = ++readerRequest
  selected.value = item; readerOpen.value = true; readerLoading.value = true; readerError.value = ''; chapters.value = []
  try {
    const result = await api.getSeries(item.id)
    if (request !== readerRequest) return
    selected.value = result; chapters.value = result.articles || []; articleKeyword.value = ''; candidatePage.value = 1
    if (canManageResource(result)) loadCandidates()
  } catch (e) { if (request === readerRequest) readerError.value = message(e) }
  finally { if (request === readerRequest) readerLoading.value = false }
}
function searchCandidates() { clearTimeout(candidateTimer); candidateRequest++; candidateTimer = setTimeout(() => { candidatePage.value = 1; loadCandidates() }, 250) }
async function loadCandidates() {
  const request = ++candidateRequest
  candidateLoading.value = true; candidateError.value = ''
  try {
    const result = await api.listArticles({ keyword: articleKeyword.value.trim(), page: candidatePage.value, size: 6 })
    if (request !== candidateRequest) return
    candidates.value = result.content; candidateTotal.value = result.totalElements
  } catch (e) { if (request === candidateRequest) candidateError.value = message(e) }
  finally { if (request === candidateRequest) candidateLoading.value = false }
}
async function changeChapters(action) {
  if (busy.value || !canManageResource(selected.value)) return
  busy.value = true
  try { await action(); chapters.value = (await api.getSeries(selected.value.id)).articles || [] }
  catch (e) { ElMessage.error(message(e)) } finally { busy.value = false }
}
function addChapter(id) { changeChapters(() => api.addSeriesArticle(selected.value.id, id)) }
function removeChapter(id) { changeChapters(() => api.removeSeriesArticle(selected.value.id, id)) }
function moveChapter(index, delta) {
  const ids = chapters.value.map(c => c.article.id)
  ;[ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]
  changeChapters(() => api.reorderSeriesArticles(selected.value.id, ids))
}
watch(tab, () => { clearTimeout(timer); keyword.value = ''; resetPage() })
watch(admin, async () => { dialog.value = false; await initialize() })
async function initialize() {
  try { [sections.value, categories.value] = await Promise.all([admin.value ? api.listAllSections() : api.listSections(), api.listCategories()]); await load() }
  catch (e) { error.value = message(e) }
}
onMounted(async () => { await initialize(); if(route.query.seriesId) openSeries({ id: Number(route.query.seriesId) }) })
onBeforeUnmount(() => { clearTimeout(timer); clearTimeout(candidateTimer); requestId++; candidateRequest++; readerRequest++ })
</script>
