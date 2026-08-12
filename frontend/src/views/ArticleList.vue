<template>
  <section class="page-stack">
    <div class="hero-panel article-hero">
      <div class="hero-copy">
        <p class="section-kicker">Curated Notes</p>
        <h2>文章与灵感</h2>
      </div>
      <div class="hero-side">
        <div class="metric-card">
          <span>{{ pageState.totalElements }}</span>
          <small>全部文章</small>
        </div>
        <div class="metric-card">
          <span>{{ categories.length }}</span>
          <small>分类</small>
        </div>
        <div class="metric-card">
          <span>{{ totalWords }}</span>
          <small>正文字符</small>
        </div>
      </div>
    </div>

    <section class="panel discovery-panel">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          :prefix-icon="Search"
          placeholder="搜索标题、正文或分类"
          clearable
        />
        <el-select v-model="categoryId" class="toolbar-select" placeholder="全部分类">
          <el-option label="全部分类" value="" />
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="String(category.id)"
          />
        </el-select>
        <el-select v-model="sortMode" class="toolbar-select" placeholder="排序方式">
          <el-option label="最近更新" value="updated-desc" />
          <el-option label="最早更新" value="updated-asc" />
          <el-option label="标题 A-Z" value="title-asc" />
        </el-select>
      </div>

      <div class="insight-panel">
        <div class="filter-summary">
          <span class="summary-icon">
            <el-icon><DataAnalysis /></el-icon>
          </span>
          <div>
            <strong>{{ activeFilterTitle }}</strong>
            <span>{{ activeFilterMeta }}</span>
          </div>
        </div>
        <div class="category-rail">
          <button class="category-filter" :class="{ active: !categoryId }" type="button" @click="selectCategory('')">
            <span class="category-filter-dot all-dot">全</span>
            <span>全部</span>
          </button>
          <button
            v-for="category in categories"
            :key="category.id"
            class="category-filter"
            :class="{ active: categoryId === String(category.id) }"
            type="button"
            @click="selectCategory(String(category.id))"
          >
            <span class="category-filter-icon" :style="articleTone(category)" aria-hidden="true">
              <el-icon><CollectionTag /></el-icon>
            </span>
            <span>{{ category.name }}</span>
          </button>
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
      <el-button type="primary" @click="$router.push('/articles/new')">发布第一篇</el-button>
    </el-empty>

    <div v-else class="article-grid">
      <article
        v-for="(article, index) in sortedArticles"
        :key="article.id"
        class="article-card"
        :style="articleTone(article)"
      >
        <div
          class="article-card-main"
          role="link"
          tabindex="0"
          @click="$router.push(`/articles/${article.id}`)"
          @keyup.enter="$router.push(`/articles/${article.id}`)"
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
            <span class="article-sequence">{{ formatSequence(index) }}</span>
          </div>
          <h3>{{ article.title }}</h3>
          <p>{{ excerpt(article.content) }}</p>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Calendar, Close, CollectionTag, DataAnalysis, Delete, EditPen, MoreFilled, Refresh, Search, Stopwatch } from '@element-plus/icons-vue'
import { deleteArticle, listArticles, listCategories } from '../api/blog'
import { canManageResource } from '../utils/permissions'
import { markdownToPlainText } from '../utils/markdown'

const articles = ref([])
const router = useRouter()
const categories = ref([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const categoryId = ref('')
const sortMode = ref('updated-desc')
const page = ref(1)
const pageSize = 9
const deletingArticleId = ref(null)
const pageState = reactive({
  totalElements: 0,
  totalPages: 1,
})
const tones = ['#e77968', '#82977c', '#b58aa5', '#c69a52', '#7c8da6']

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

const totalWords = computed(() => {
  return articles.value.reduce((sum, article) => sum + contentLength(article.content), 0)
})
const activeCategory = computed(() => {
  return categories.value.find((category) => String(category.id) === categoryId.value) || null
})
const hasFilters = computed(() => Boolean(keyword.value.trim() || categoryId.value))
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

  return '全部内容'
})
const activeFilterMeta = computed(() => {
  const sortMap = {
    'updated-desc': '最近更新',
    'updated-asc': '最早更新',
    'title-asc': '标题 A-Z',
  }
  return `${pageState.totalElements} 篇文章 · ${sortMap[sortMode.value] || '最近更新'}`
})

function excerpt(content) {
  const plainText = markdownToPlainText(content)
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

function articleTone(article) {
  const source = Number(article.categoryId || article.id || 0)
  return { '--tone': tones[Math.abs(source) % tones.length] }
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

function formatSequence(index) {
  return String((page.value - 1) * pageSize + index + 1).padStart(2, '0')
}

function selectCategory(id) {
  categoryId.value = id
}

function resetFilters() {
  keyword.value = ''
  categoryId.value = ''
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
    const [articleData, categoryData] = await Promise.all([loadArticles(), listCategories()])
    categories.value = categoryData
  } catch {
    error.value = '文章数据加载失败，请确认后端服务和数据库已启动'
  } finally {
    loading.value = false
  }
}

async function loadArticles() {
  const params = {
    page: page.value,
    size: pageSize,
  }

  const key = keyword.value.trim()
  if (key) {
    params.keyword = key
  }

  if (categoryId.value) {
    params.categoryId = categoryId.value
  }

  const articleData = await listArticles(params)
  articles.value = Array.isArray(articleData) ? articleData : articleData.content || []
  pageState.totalElements = Array.isArray(articleData) ? articles.value.length : articleData.totalElements || 0
  pageState.totalPages = Array.isArray(articleData) ? 1 : articleData.totalPages || 1

  return articleData
}

async function reloadArticles() {
  loading.value = true
  error.value = ''

  try {
    await loadArticles()
  } catch {
    error.value = '文章数据加载失败，请确认后端服务和数据库已启动'
  } finally {
    loading.value = false
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
watch([keyword, categoryId], () => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    reloadArticles()
  }, 260)
})

watch(page, () => {
  reloadArticles()
})

onMounted(loadData)
</script>
