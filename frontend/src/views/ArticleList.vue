<template>
  <section class="page-stack article-list-page" :class="{ 'life-feed': sectionSlug === 'life' }">
    <header class="listing-heading">
      <p class="section-kicker"><span class="edition-mark" aria-hidden="true" />{{ activeSection?.slug || 'The journal' }}</p>
      <h1>{{ activeSection?.name || '全部手记' }}</h1>
      <p v-if="activeSection?.description">{{ activeSection.description }}</p>
      <div class="listing-quick-links"><RouterLink :to="{ path: '/collections', query: { sectionSlug: sectionSlug || undefined } }"><Library :size="15" />系列专题 <el-icon><ArrowRight /></el-icon></RouterLink><RouterLink to="/knowledge"><Layers :size="15" />知识卡片 <el-icon><ArrowRight /></el-icon></RouterLink><span>{{ pageState.totalElements }} 篇手记</span></div>
    </header>

    <section class="content-discovery">
      <nav class="section-navigation" aria-label="内容分区">
        <FluidTabs :model-value="sectionSlug" :items="sectionOptions" label="内容分区" @update:model-value="selectSection" />
        <RouterLink to="/collections">系列目录 <el-icon><ArrowRight /></el-icon></RouterLink>
      </nav>
      <nav class="topic-index" aria-label="文章分类">
        <span><Hash :size="13" />主题</span>
        <button :class="{ active: !categoryId }" :aria-pressed="!categoryId" @click="selectCategory('')">全部</button>
        <button v-for="category in visibleCategories" :key="category.id" :class="{ active: categoryId === String(category.id) }" :aria-pressed="categoryId === String(category.id)" @click="selectCategory(String(category.id))">{{ category.name }}</button>
      </nav>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          :prefix-icon="Search"
          placeholder="搜索标题、正文或分类"
          clearable
        />
        <el-select v-model="tagSlug" class="toolbar-select" placeholder="全部标签" clearable filterable aria-label="标签筛选">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.slug"><TechBadge :tag="tag" /></el-option>
          <template #label="{ label, value }"><TechBadge :tag="tags.find(tag => tag.slug === value) || { name: label, slug: value }" /></template>
        </el-select>
        <el-input v-model="contentType" class="toolbar-select" clearable placeholder="内容类型" aria-label="内容类型筛选" />
        <el-select v-model="sortMode" class="toolbar-select" placeholder="排序方式">
          <el-option label="最近更新" value="updated-desc" />
          <el-option label="本页：最早更新" value="updated-asc" />
          <el-option label="本页：标题 A-Z" value="title-asc" />
        </el-select>
      </div>

      <div class="content-filter-status">
        <div class="filter-summary">
          <span class="summary-icon">
            <el-icon><DataAnalysis /></el-icon>
          </span>
          <div>
            <strong>{{ activeFilterTitle }}</strong>
            <span>{{ activeFilterMeta }}</span>
          </div>
        </div>
        <el-button
          class="clear-filter-button"
          :icon="Close"
          :disabled="!hasFilters"
          circle
          aria-label="清空筛选"
          title="清空筛选"
          @click="resetFilters"
        />
      </div>
    </section>

    <div v-if="loading" class="article-grid">
      <div v-for="item in 4" :key="item" class="article-card skeleton-card">
        <el-skeleton :rows="5" animated />
      </div>
    </div>

    <div v-else-if="error" class="panel state-panel error-panel">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button :icon="Refresh" @click="loadData">重试</el-button>
    </div>

    <el-empty v-else-if="sortedArticles.length === 0" class="panel empty-panel" description="还没有匹配的文章">
      <el-button v-if="hasFilters" :icon="Close" @click="resetFilters">清空筛选</el-button>
      <el-button v-else-if="signedIn" type="primary" :icon="EditPen" @click="$router.push('/articles/new')">发布第一篇</el-button>
    </el-empty>

    <div v-else class="article-grid">
      <article
        v-for="(article, index) in sortedArticles"
        :key="article.id"
        class="article-card"
        :style="{ '--item-index': index }"
      >
        <div
          class="article-card-main"
        >
          <div class="article-card-topline">
            <div class="card-meta">
              <span class="article-author-chip">
                <span>{{ authorInitial(article) }}</span>
                {{ articleAuthorName(article) }}
              </span>
              <span class="category-chip">{{ article.categoryName || '未分类' }}</span>
              <span class="time-chip">
                <el-icon><Calendar /></el-icon>
                {{ articleTimeLabel(article) }} {{ formatDate(articleTime(article)) }}
              </span>
            </div>
          </div>
          <h3><RouterLink :to="`/articles/${article.id}`">{{ article.title }}</RouterLink></h3>
          <p>{{ article.summary || excerpt(article.content, article.title) }}</p>
          <div v-if="article.tags?.length || article.sectionName" class="article-taxonomy">
            <span v-if="article.sectionName">{{ article.sectionName }}</span>
            <TechBadge v-for="tag in article.tags" :key="tag.id" :tag="tag" interactive :selected="tagSlug === tag.slug" @click="tagSlug = tag.slug" />
          </div>
          <div class="article-card-stats">
            <span class="info-chip">
              <el-icon><Stopwatch /></el-icon>
              约 {{ readingMinutes(article.content) }} 分钟读完
            </span>
            <span class="info-chip">{{ contentLength(article.content) }} 字符</span>
          </div>
        </div>
        <div class="card-actions">
          <el-button text :icon="ArrowRight" @click="$router.push(`/articles/${article.id}`)">阅读全文</el-button>
          <el-dropdown
            v-if="canManageResource(article)"
            trigger="click"
            placement="bottom-end"
            @command="handleArticleCommand(article, $event)"
          >
            <button class="article-more-button" type="button" aria-label="文章操作" title="文章操作">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit" :icon="EditPen">编辑文章</el-dropdown-item>
                <el-dropdown-item command="delete" :icon="Delete" class="danger-dropdown-item">
                  删除文章
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </article>
    </div>

    <div v-if="!loading && !error && pageState.totalPages > 1" class="panel pagination-panel">
      <span>共 {{ pageState.totalElements }} 篇文章</span>
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="pageState.totalElements"
        layout="prev, pager, next"
        background
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Calendar, Close, DataAnalysis, Delete, EditPen, MoreFilled, Refresh, Search, Stopwatch } from '@element-plus/icons-vue'
import { Hash, Layers, Library } from '@lucide/vue'
import FluidTabs from '../components/FluidTabs.vue'
import TechBadge from '../components/TechBadge.vue'
import { signedIn } from '../state/session'
import { deleteArticle, listArticles, listCategories, listSections, listTags } from '../api/blog'
import { canManageResource } from '../utils/permissions'
import { markdownToPlainText } from '../utils/markdown'

const articles = ref([])
const router = useRouter()
const route = useRoute()
const categories = ref([])
const sections = ref([])
const sectionOptions = computed(() => [{ label: '全部手记', value: '' }, ...sections.value.map(s => ({ label: s.name, value: s.slug }))])
const tags = ref([])
const activeSection = computed(() => sections.value.find(s => s.slug === sectionSlug.value))
const sectionSlug = ref(String(route.query.sectionSlug || ''))
const tagSlug = ref(String(route.query.tagSlug || ''))
const contentType = ref('')
const visibleCategories = computed(() => categories.value.filter(c => !sectionSlug.value || !c.sectionId || c.sectionSlug === sectionSlug.value))
function selectSection(slug) {
  sectionSlug.value = slug; categoryId.value = ''
  router.replace({ query: { ...route.query, sectionSlug: slug || undefined } })
}
const loading = ref(true)
const error = ref('')
const keyword = ref(String(route.query.keyword || ''))
const categoryId = ref('')
const sortMode = ref('updated-desc')
const page = ref(1)
const pageSize = 9
const deletingArticleId = ref(null)
const pageState = reactive({
  totalElements: 0,
  totalPages: 1,
})

const sortedArticles = computed(() => {
  return [...articles.value].sort((a, b) => {
    if (sortMode.value === 'title-asc') {
      return String(a.title || '').localeCompare(String(b.title || ''), 'zh-CN')
    }

    const left = new Date(a.updatedAt || a.createdAt || 0).getTime()
    const right = new Date(b.updatedAt || b.createdAt || 0).getTime()
    return sortMode.value === 'updated-asc' ? left - right : right - left
  })
})

const activeCategory = computed(() => {
  return categories.value.find((category) => String(category.id) === categoryId.value) || null
})
const hasFilters = computed(() => Boolean(keyword.value.trim() || categoryId.value || sectionSlug.value || tagSlug.value || contentType.value))
const activeFilterTitle = computed(() => {
  if (keyword.value.trim() && activeCategory.value) {
    return `${activeCategory.value.name} 中搜索“${keyword.value.trim()}”`
  }

  if (keyword.value.trim()) {
    return `搜索“${keyword.value.trim()}”`
  }

  if (activeCategory.value) {
    return activeCategory.value.name
  }

  return [sections.value.find(s => s.slug === sectionSlug.value)?.name, tags.value.find(t => t.slug === tagSlug.value)?.name, contentType.value.trim()].filter(Boolean).join(' · ') || '全部内容'
})
const activeFilterMeta = computed(() => {
  const sortMap = {
    'updated-desc': '最近更新',
    'updated-asc': '最早更新',
    'title-asc': '标题 A-Z',
  }
  return `${pageState.totalElements} 篇文章 · ${sortMap[sortMode.value] || '最近更新'}`
})

function excerpt(content, title) {
  const plainText = markdownToPlainText(content, { title })
  if (!plainText) {
    return '暂无正文内容'
  }
  return plainText.length > 120 ? `${plainText.slice(0, 120)}...` : plainText
}

function contentLength(content) {
  return (content || '').trim().length
}

function readingMinutes(content) {
  const length = contentLength(content)
  return length ? Math.max(1, Math.ceil(length / 500)) : 0
}

function articleAuthorName(article) {
  return article.authorName || article.username || article.authorUsername || '未知作者'
}

function authorInitial(article) {
  return articleAuthorName(article).trim().slice(0, 1).toUpperCase()
}

function wasArticleUpdated(article) {
  const createdAt = new Date(article?.createdAt || 0).getTime()
  const updatedAt = new Date(article?.updatedAt || 0).getTime()
  return Number.isFinite(createdAt) && Number.isFinite(updatedAt) && updatedAt - createdAt > 1000
}

function articleTimeLabel(article) {
  return wasArticleUpdated(article) ? '更新' : '发布'
}

function articleTime(article) {
  return wasArticleUpdated(article) ? article.updatedAt : article.createdAt || article.updatedAt
}

function selectCategory(id) {
  categoryId.value = id
}

function resetFilters() {
  keyword.value = ''
  categoryId.value = ''
  sectionSlug.value = ''
  tagSlug.value = ''
  contentType.value = ''
  router.replace({ query: { ...route.query, sectionSlug: undefined, tagSlug: undefined, keyword: undefined } })
}

function handleArticleCommand(article, command) {
  if (command === 'edit') {
    router.push(`/articles/${article.id}/edit`)
    return
  }

  if (command === 'delete') {
    removeArticle(article)
  }
}

function formatDate(value) {
  if (!value) {
    return '时间未知'
  }
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日`
}

async function loadData() {
  loading.value = true
  error.value = ''

  try {
    const [categoryData, sectionData, tagData] = await Promise.all([listCategories(), listSections(), listTags()])
    categories.value = categoryData
    sections.value = sectionData
    tags.value = tagData
    await loadArticles()
  } catch {
    error.value = '文章数据加载失败，请确认后端服务和数据库已启动'
  } finally {
    loading.value = false
  }
}

async function loadArticles() {
  const requestId = ++articleRequestId
  const params = {
    page: page.value,
    size: pageSize,
    sectionSlug: sectionSlug.value || undefined,
    tagSlug: tagSlug.value || undefined,
    contentType: contentType.value.trim() || undefined,
  }

  const key = keyword.value.trim()
  if (key) {
    params.keyword = key
  }

  if (categoryId.value) {
    params.categoryId = categoryId.value
  }

  const articleData = await listArticles(params)
  if (requestId !== articleRequestId) return articleData
  articles.value = Array.isArray(articleData) ? articleData : articleData.content || []
  pageState.totalElements = Array.isArray(articleData) ? articles.value.length : articleData.totalElements || 0
  pageState.totalPages = Array.isArray(articleData) ? 1 : articleData.totalPages || 1

  return articleData
}

async function reloadArticles() {
  const request = ++reloadRequestId
  loading.value = true
  error.value = ''

  try {
    await loadArticles()
  } catch {
    if (request === reloadRequestId) error.value = '文章数据加载失败，请确认后端服务和数据库已启动'
  } finally {
    if (request === reloadRequestId) loading.value = false
  }
}

async function removeArticle(article) {
  if (!canManageResource(article)) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除文章「${article.title}」？`, '删除文章', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }

  deletingArticleId.value = article.id
  try {
    await deleteArticle(article.id)
    if (articles.value.length === 1 && page.value > 1) {
      page.value -= 1
    } else {
      await reloadArticles()
    }
    ElMessage.success('文章已删除')
  } finally {
    deletingArticleId.value = null
  }
}

let searchTimer = null
let articleRequestId = 0
let reloadRequestId = 0
watch([keyword, categoryId, sectionSlug, tagSlug, contentType], () => {
  articleRequestId++
  reloadRequestId++
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    if (page.value !== 1) page.value = 1
    else reloadArticles()
  }, 260)
})

watch(page, () => {
  window.clearTimeout(searchTimer)
  reloadArticles()
})

onMounted(loadData)
watch(() => route.query.sectionSlug, value => { sectionSlug.value = String(value || ''); categoryId.value = '' })
watch(() => route.query.tagSlug, value => { tagSlug.value = String(value || '') })
watch(() => route.query.keyword, value => { keyword.value = String(value || '') })
onBeforeUnmount(() => { window.clearTimeout(searchTimer); articleRequestId++ })
</script>
