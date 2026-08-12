<template>
  <section class="page-stack knowledge-page">
    <div class="hero-panel knowledge-hero">
      <div>
        <p class="section-kicker">Knowledge Notes</p>
        <h2>知识库</h2>
        <p>把可复用的概念、结论和代码片段整理成卡片。</p>
      </div>
      <div class="metric-card">
        <span>{{ cards.length }}</span>
        <small>知识卡片</small>
      </div>
    </div>

    <section class="panel knowledge-toolbar">
      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索卡片标题或摘要"
        @keyup.enter="loadCards"
        @clear="loadCards"
      />
      <el-button :icon="Refresh" circle aria-label="刷新知识卡片" title="刷新" @click="loadCards" />
      <el-button v-if="isSignedIn" type="primary" :icon="Plus" @click="openCreateDialog">新建卡片</el-button>
      <el-button v-else :icon="Lock" @click="requestLogin">登录后创建</el-button>
    </section>

    <div v-if="loading" class="panel knowledge-loading">
      <el-skeleton :rows="8" animated />
    </div>
    <div v-else-if="error" class="panel state-panel error-panel">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button :icon="Refresh" @click="loadCards">重试</el-button>
    </div>
    <el-empty v-else-if="cards.length === 0" class="panel empty-panel" description="还没有匹配的知识卡片">
      <el-button v-if="isSignedIn" type="primary" :icon="Plus" @click="openCreateDialog">创建第一张卡片</el-button>
    </el-empty>

    <section v-else class="panel knowledge-library">
      <aside class="knowledge-index" aria-label="知识卡片列表">
        <div class="knowledge-index-heading">
          <span>{{ keyword.trim() ? '搜索结果' : '最近整理' }}</span>
          <strong>{{ cards.length }}</strong>
        </div>
        <div class="knowledge-index-list">
          <button
            v-for="(card, index) in cards"
            :key="card.id"
            type="button"
            class="knowledge-index-item"
            :class="{ active: selectedCardId === card.id }"
            @click="selectCard(card)"
          >
            <span class="knowledge-index-number">{{ formatIndex(index) }}</span>
            <span>
              <strong>{{ card.title }}</strong>
              <small>{{ card.summary }}</small>
              <em>{{ card.createdByName || '未知用户' }} · {{ cardTimeLabel(card) }} {{ formatDate(cardTime(card)) }}</em>
            </span>
          </button>
        </div>
      </aside>

      <article class="knowledge-reader">
        <el-skeleton v-if="cardLoading" :rows="10" animated />
        <div v-else-if="cardError" class="state-panel compact-state">
          <el-alert type="error" :title="cardError" show-icon :closable="false" />
          <el-button :icon="Refresh" @click="loadSelectedCard">重试</el-button>
        </div>
        <template v-else-if="selectedCard">
          <header class="knowledge-reader-header">
            <div class="knowledge-reader-owner">
              <span>{{ creatorInitial(selectedCard) }}</span>
              <div>
                <strong>{{ selectedCard.createdByName || '未知用户' }}</strong>
                <small>{{ cardTimeLabel(selectedCard) }} {{ formatDateTime(cardTime(selectedCard)) }}</small>
              </div>
            </div>
            <el-dropdown
              v-if="canManageResource(selectedCard)"
              trigger="click"
              placement="bottom-end"
              @command="handleCardCommand"
            >
              <button class="article-more-button" type="button" aria-label="知识卡片操作" title="知识卡片操作">
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :icon="EditPen">编辑卡片</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" class="danger-dropdown-item">删除卡片</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </header>

          <div class="knowledge-reader-title">
            <span>Knowledge Card</span>
            <h1>{{ selectedCard.title }}</h1>
            <p>{{ selectedCard.summary }}</p>
          </div>
          <div class="knowledge-reader-body markdown-body" v-html="renderMarkdown(selectedCard.content || '')"></div>
        </template>
      </article>
    </section>

    <el-dialog
      v-model="editorOpen"
      :title="editingId ? '编辑知识卡片' : '新建知识卡片'"
      width="min(980px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
      class="knowledge-editor-dialog"
      @closed="resetEditor"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <div class="knowledge-editor-basics">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="一句话说明这张卡片讨论什么" />
          </el-form-item>
          <el-form-item label="摘要" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="便于搜索和快速判断内容"
            />
          </el-form-item>
        </div>

        <el-form-item label="Markdown 内容" prop="content">
          <div class="knowledge-editor-content">
            <div class="knowledge-editor-switch">
              <span>正文</span>
              <el-segmented v-model="editorMode" :options="editorModes" />
            </div>
            <div class="knowledge-editor-panes" :class="`mode-${editorMode}`">
              <textarea
                v-show="editorMode !== 'preview'"
                v-model="form.content"
                class="knowledge-editor-textarea"
                placeholder="支持标题、列表、引用、代码块和图片语法"
                spellcheck="false"
              ></textarea>
              <div
                v-show="editorMode !== 'edit'"
                class="knowledge-editor-preview markdown-body"
                v-html="renderMarkdown(form.content || '')"
              ></div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorOpen = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="saveCard">
          {{ editingId ? '保存修改' : '创建卡片' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, EditPen, Lock, MoreFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createKnowledgeCard,
  deleteKnowledgeCard,
  getKnowledgeCard,
  listKnowledgeCards,
  updateKnowledgeCard,
} from '../api/blog'
import { signedIn } from '../state/session'
import { canManageResource } from '../utils/permissions'
import { renderMarkdown } from '../utils/markdown'

const isSignedIn = signedIn
const cards = ref([])
const selectedCard = ref(null)
const selectedCardId = ref(null)
const keyword = ref('')
const loading = ref(true)
const error = ref('')
const cardLoading = ref(false)
const cardError = ref('')
const editorOpen = ref(false)
const editingId = ref(null)
const editorMode = ref('split')
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ title: '', summary: '', content: '' })
const editorModes = [
  { label: '编辑', value: 'edit' },
  { label: '分屏', value: 'split' },
  { label: '预览', value: 'preview' },
]
const rules = {
  title: [
    { required: true, message: '请输入卡片标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  summary: [
    { required: true, message: '请输入卡片摘要', trigger: 'blur' },
    { max: 500, message: '摘要不能超过 500 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入卡片内容', trigger: 'blur' }],
}
let searchTimer = null

function requestLogin() {
  window.dispatchEvent(new CustomEvent('blog:open-auth'))
}

function formatIndex(index) {
  return String(index + 1).padStart(2, '0')
}

function creatorInitial(card) {
  return (card?.createdByName || 'U').trim().slice(0, 1).toUpperCase()
}

function wasUpdated(card) {
  const createdAt = new Date(card?.createdAt || 0).getTime()
  const updatedAt = new Date(card?.updatedAt || 0).getTime()
  return Number.isFinite(createdAt) && Number.isFinite(updatedAt) && updatedAt - createdAt > 1000
}

function cardTimeLabel(card) {
  return wasUpdated(card) ? '更新' : '创建'
}

function cardTime(card) {
  return wasUpdated(card) ? card.updatedAt : card.createdAt || card.updatedAt
}

function formatDate(value) {
  if (!value) return '时间未知'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value))
}

function formatDateTime(value) {
  if (!value) return '时间未知'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

async function loadCards() {
  loading.value = true
  error.value = ''
  try {
    cards.value = await listKnowledgeCards({ keyword: keyword.value.trim() })
    const retained = cards.value.find((card) => card.id === selectedCardId.value)
    if (retained) await selectCard(retained)
    else if (cards.value[0]) await selectCard(cards.value[0])
    else {
      selectedCardId.value = null
      selectedCard.value = null
    }
  } catch {
    error.value = '知识卡片加载失败，请确认后端服务和数据库已启动'
  } finally {
    loading.value = false
  }
}

async function selectCard(card) {
  selectedCardId.value = card.id
  cardLoading.value = true
  cardError.value = ''
  try {
    selectedCard.value = await getKnowledgeCard(card.id)
  } catch {
    selectedCard.value = null
    cardError.value = '知识卡片不存在或加载失败'
  } finally {
    cardLoading.value = false
  }
}

function loadSelectedCard() {
  const card = cards.value.find((item) => item.id === selectedCardId.value)
  if (card) selectCard(card)
}

function openCreateDialog() {
  if (!isSignedIn.value) return requestLogin()
  editingId.value = null
  form.title = ''
  form.summary = ''
  form.content = ''
  editorMode.value = 'split'
  editorOpen.value = true
}

function openEditDialog() {
  if (!selectedCard.value || !canManageResource(selectedCard.value)) return
  editingId.value = selectedCard.value.id
  form.title = selectedCard.value.title
  form.summary = selectedCard.value.summary
  form.content = selectedCard.value.content
  editorMode.value = 'split'
  editorOpen.value = true
}

function resetEditor() {
  editingId.value = null
  form.title = ''
  form.summary = ''
  form.content = ''
  formRef.value?.clearValidate()
}

async function saveCard() {
  await formRef.value.validate()
  const payload = {
    title: form.title.trim(),
    summary: form.summary.trim(),
    content: form.content.trim(),
  }
  saving.value = true
  try {
    const saved = editingId.value
      ? await updateKnowledgeCard(editingId.value, payload)
      : await createKnowledgeCard(payload)
    editorOpen.value = false
    selectedCardId.value = saved.id
    await loadCards()
    ElMessage.success(editingId.value ? '知识卡片已更新' : '知识卡片已创建')
  } finally {
    saving.value = false
  }
}

async function removeCard() {
  if (!selectedCard.value || !canManageResource(selectedCard.value)) return
  try {
    await ElMessageBox.confirm(`确定删除知识卡片「${selectedCard.value.title}」？`, '删除知识卡片', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }

  await deleteKnowledgeCard(selectedCard.value.id)
  selectedCardId.value = null
  selectedCard.value = null
  await loadCards()
  ElMessage.success('知识卡片已删除')
}

function handleCardCommand(command) {
  if (command === 'edit') openEditDialog()
  if (command === 'delete') removeCard()
}

watch(keyword, () => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(loadCards, 320)
})

onMounted(loadCards)
onBeforeUnmount(() => window.clearTimeout(searchTimer))
</script>
