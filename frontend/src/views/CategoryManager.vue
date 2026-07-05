<template>
  <section class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="section-kicker">Categories</p>
        <h2>分类管理</h2>
      </div>
      <div class="metric-card">
        <span>{{ categories.length }}</span>
        <small>分类数量</small>
      </div>
    </div>

    <div class="panel category-panel">
      <el-form v-if="canManageCategories" class="category-form" :model="form" @submit.prevent>
        <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="新的分类名称" />
        <el-button type="primary" :icon="Plus" :loading="saving" :disabled="!form.name.trim()" @click="submit">
          添加分类
        </el-button>
      </el-form>

      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-else-if="error" class="state-panel compact-state">
        <el-alert type="error" :title="error" show-icon :closable="false" />
        <el-button :icon="Refresh" @click="loadCategories">重试</el-button>
      </div>
      <el-empty v-else-if="categories.length === 0" description="暂无分类" />

      <div v-else class="category-list">
        <div
          v-for="category in categories"
          :key="category.id"
          class="category-item"
          :class="{ active: selectedCategoryId === category.id }"
          :style="categoryTone(category)"
          role="button"
          tabindex="0"
          @click="selectCategory(category)"
          @keyup.enter="selectCategory(category)"
        >
          <div class="category-copy">
            <span class="category-avatar">{{ categoryInitial(category.name) }}</span>
            <div>
              <strong>{{ category.name }}</strong>
              <span>ID {{ category.id }}</span>
            </div>
          </div>
          <div v-if="canManageCategories" class="category-actions">
            <el-button text type="primary" :icon="Edit" @click.stop="startEdit(category)">重命名</el-button>
            <el-button text type="danger" :icon="Delete" @click.stop="remove(category)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <section v-if="selectedCategory" class="panel category-preview-panel">
      <div class="panel-title-row">
        <div>
          <p class="section-kicker">Category Articles</p>
          <h3>{{ selectedCategory.name }}</h3>
        </div>
        <span class="comment-count">{{ categoryArticles.length }}</span>
      </div>

      <el-skeleton v-if="articlesLoading" :rows="4" animated />
      <div v-else-if="articlesError" class="state-panel compact-state">
        <el-alert type="error" :title="articlesError" show-icon :closable="false" />
        <el-button :icon="Refresh" @click="loadCategoryArticles">重试</el-button>
      </div>
      <el-empty v-else-if="categoryArticles.length === 0" description="该分类下暂无文章" />

      <div v-else class="category-article-list">
        <article v-for="article in categoryArticles" :key="article.id" class="category-article-item">
          <div>
            <strong>{{ article.title }}</strong>
            <span>{{ excerpt(article.content) }}</span>
          </div>
          <el-button text type="primary" @click="$router.push(`/articles/${article.id}`)">查看</el-button>
        </article>
      </div>
    </section>

    <el-dialog v-if="canManageCategories" v-model="editing.visible" title="重命名分类" width="420px">
      <el-input v-model="editing.name" maxlength="100" show-word-limit />
      <template #footer>
        <el-button @click="editing.visible = false">取消</el-button>
        <el-button type="primary" :loading="editing.saving" :disabled="!editing.name.trim()" @click="saveEdit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { createCategory, deleteCategory, listArticlesByCategory, listCategories, updateCategory } from '../api/blog'
import { isAdmin } from '../utils/permissions'

const categories = ref([])
const categoryArticles = ref([])
const loading = ref(true)
const articlesLoading = ref(false)
const saving = ref(false)
const error = ref('')
const articlesError = ref('')
const selectedCategoryId = ref(null)
const form = reactive({ name: '' })
const tones = ['#2563eb', '#475569', '#1e40af', '#64748b', '#334155']
const editing = reactive({
  visible: false,
  id: null,
  name: '',
  saving: false,
})

const selectedCategory = computed(() => {
  return categories.value.find((category) => category.id === selectedCategoryId.value) || null
})
const canManageCategories = computed(() => isAdmin())

function categoryTone(category) {
  const source = Number(category.id || 0)
  return { '--tone': tones[Math.abs(source) % tones.length] }
}

function categoryInitial(name) {
  return (name || '#').trim().slice(0, 1).toUpperCase()
}

function excerpt(content) {
  if (!content) {
    return '暂无正文内容'
  }
  return content.length > 72 ? `${content.slice(0, 72)}...` : content
}

async function loadCategories() {
  loading.value = true
  error.value = ''

  try {
    categories.value = await listCategories()
    if (!categories.value.some((category) => category.id === selectedCategoryId.value)) {
      selectedCategoryId.value = categories.value[0]?.id || null
    }
    await loadCategoryArticles()
  } catch {
    error.value = '分类加载失败，请确认后端服务和数据库已启动'
  } finally {
    loading.value = false
  }
}

async function loadCategoryArticles() {
  if (!selectedCategoryId.value) {
    categoryArticles.value = []
    return
  }

  articlesLoading.value = true
  articlesError.value = ''

  try {
    categoryArticles.value = await listArticlesByCategory(selectedCategoryId.value)
  } catch {
    articlesError.value = '分类文章加载失败'
  } finally {
    articlesLoading.value = false
  }
}

async function selectCategory(category) {
  selectedCategoryId.value = category.id
  await loadCategoryArticles()
}

async function submit() {
  if (!canManageCategories.value) {
    return
  }

  const name = form.name.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }

  saving.value = true
  try {
    const saved = await createCategory({ name })
    form.name = ''
    selectedCategoryId.value = saved.id
    await loadCategories()
    ElMessage.success('分类已添加')
  } finally {
    saving.value = false
  }
}

function startEdit(category) {
  if (!canManageCategories.value) {
    return
  }

  editing.visible = true
  editing.id = category.id
  editing.name = category.name
}

async function saveEdit() {
  if (!canManageCategories.value) {
    editing.visible = false
    return
  }

  const name = editing.name.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }

  editing.saving = true
  try {
    await updateCategory(editing.id, { name })
    editing.visible = false
    await loadCategories()
    ElMessage.success('分类已更新')
  } finally {
    editing.saving = false
  }
}

async function remove(category) {
  if (!canManageCategories.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除分类「${category.name}」？`, '删除分类', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }

  await deleteCategory(category.id)
  if (selectedCategoryId.value === category.id) {
    selectedCategoryId.value = null
  }
  await loadCategories()
  ElMessage.success('分类已删除')
}

onMounted(loadCategories)
</script>
