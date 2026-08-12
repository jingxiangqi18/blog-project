<template>
  <section class="page-stack editor-page">
    <div class="hero-panel">
      <div>
        <p class="section-kicker">Compose</p>
        <h2>{{ isEdit ? '编辑文章' : '发布文章' }}</h2>
        <div class="draft-save-status" :class="[draftStatus, { 'has-draft': hasSavedDraft }]">
          <el-icon><Clock /></el-icon>
          <span>{{ draftStatusLabel }}</span>
          <span v-if="hasSavedDraft" class="draft-server-mark">草稿</span>
        </div>
      </div>
      <el-button :icon="Back" @click="$router.back()">返回</el-button>
    </div>

    <el-alert
      v-if="categoriesLoaded && categories.length === 0"
      type="warning"
      title="请先创建分类，再发布文章"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="loading" class="panel editor-loading" :rows="8" animated />

    <div v-else-if="loadError" class="panel state-panel error-panel">
      <el-alert type="error" :title="loadError" show-icon :closable="false" />
      <el-button :icon="Refresh" @click="loadData">重试</el-button>
    </div>

    <el-form
      v-else
      ref="formRef"
      class="editor-form"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <div ref="editorLayoutRef" class="editor-layout" :style="editorLayoutStyle">
        <div class="panel editor-writing-surface">
          <el-form-item label="标题" prop="title" class="title-field">
            <el-input
              v-model="form.title"
              class="title-input"
              maxlength="200"
              show-word-limit
              placeholder="输入文章标题"
            />
          </el-form-item>

          <el-form-item label="Markdown 正文" prop="content" class="content-field">
            <div class="markdown-editor">
              <div class="markdown-toolbar">
                <div class="markdown-tools">
                  <el-button :icon="Memo" @click="insertMarkdown('heading')">标题</el-button>
                  <el-button :icon="List" @click="insertMarkdown('list')">列表</el-button>
                  <el-button :icon="ChatLineRound" @click="insertMarkdown('quote')">引用</el-button>
                  <el-button :icon="Reading" @click="insertMarkdown('code')">代码块</el-button>
                  <el-button :icon="Picture" @click="insertMarkdown('image')">图片</el-button>
                  <el-button :icon="Link" @mousedown.prevent @click="openReferenceDialog('article')">
                    链接文章
                  </el-button>
                  <el-button :icon="Notebook" @mousedown.prevent @click="openReferenceDialog('card')">
                    知识卡片
                  </el-button>
                </div>
                <el-radio-group v-model="editorMode" size="small">
                  <el-radio-button value="edit">编辑</el-radio-button>
                  <el-radio-button value="split">分屏</el-radio-button>
                  <el-radio-button value="preview">预览</el-radio-button>
                </el-radio-group>
              </div>

              <div
                ref="markdownWorkspaceRef"
                class="markdown-workspace"
                :class="`mode-${editorMode}`"
                :style="workspaceStyle"
              >
                <div v-show="editorMode !== 'preview'" class="markdown-pane markdown-input-pane">
                  <MarkdownMentionEditor
                    ref="contentEditorRef"
                    v-model="form.content"
                    :current-article-id="props.id"
                    @cursor-change="syncPreviewToCursor"
                    @scroll="syncPreviewByScroll"
                  />
                </div>
                <button
                  v-if="editorMode === 'split'"
                  class="markdown-resize-handle"
                  type="button"
                  aria-label="拖拽调整编辑区和预览区宽度"
                  title="拖拽调整左右宽度"
                  @pointerdown="startPaneResize"
                >
                  <span></span>
                </button>
                <div
                  v-show="editorMode !== 'edit'"
                  ref="previewPaneRef"
                  class="markdown-pane markdown-preview-pane"
                >
                  <div v-if="contentLength > 0" class="markdown-body" v-html="renderedContent"></div>
                  <el-empty v-else description="预览会显示在这里" />
                </div>
              </div>
            </div>
          </el-form-item>
        </div>

        <button
          class="editor-layout-resize-handle"
          type="button"
          aria-label="拖拽调整编辑区和发布信息栏宽度"
          title="拖拽调整左右宽度"
          @pointerdown="startLayoutResize"
        >
          <span></span>
        </button>

        <aside class="panel editor-side-panel">
          <div class="side-section side-section-heading">
            <div>
              <p class="section-kicker">Publish</p>
              <h3>发布信息</h3>
            </div>
            <el-button v-if="isEdit" text :icon="DocumentCopy" @click="openRevisionHistory">
              版本历史
            </el-button>
          </div>

          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择分类" class="full-width">
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>

          <section class="draft-control-card" :class="draftStatus">
            <div class="draft-control-copy">
              <span class="draft-control-icon">
                <el-icon><Clock /></el-icon>
              </span>
              <div>
                <strong>{{ draftControlTitle }}</strong>
                <small>{{ draftControlMeta }}</small>
              </div>
            </div>
            <div class="draft-control-actions">
              <el-button
                :icon="Check"
                :loading="isDraftSaving"
                :disabled="!canSaveDraft"
                @click="saveDraftNow"
              >
                {{ hasSavedDraft ? '立即同步' : '保存草稿' }}
              </el-button>
              <el-button
                v-if="hasSavedDraft"
                text
                type="danger"
                :icon="Delete"
                :loading="discardingDraft"
                :disabled="isDraftSaving || saving"
                @click="discardDraft"
              >
                放弃草稿
              </el-button>
            </div>
          </section>

          <div class="writer-stats">
            <div>
              <el-icon><Reading /></el-icon>
              <span>{{ contentLength }} 字符</span>
            </div>
            <div>
              <el-icon><Stopwatch /></el-icon>
              <span>约 {{ estimatedMinutes }} 分钟读完</span>
            </div>
            <div>
              <el-icon><CollectionTag /></el-icon>
              <span>{{ categories.length }} 个分类</span>
            </div>
          </div>

          <div class="editor-actions">
            <el-button :disabled="saving" @click="$router.back()">取消</el-button>
            <el-button
              type="primary"
              :icon="Check"
              :loading="saving"
              :disabled="categories.length === 0 || !canSubmitArticle || isDraftSaving"
              @click="submit"
            >
              {{ isEdit ? '保存修改' : '发布文章' }}
            </el-button>
          </div>
        </aside>
      </div>
    </el-form>

    <el-dialog
      v-model="referenceDialogOpen"
      :title="referenceDialogTitle"
      width="620px"
      append-to-body
      destroy-on-close
    >
      <div class="reference-dialog">
        <p class="reference-selection-copy">
          {{ referenceSelection.text ? `将“${referenceSelection.text}”关联到目标` : '未选中文字，将使用目标标题作为链接文字' }}
        </p>

        <el-tabs v-if="referenceType === 'card'" v-model="cardDialogMode" class="reference-tabs">
          <el-tab-pane label="选择已有卡片" name="search" />
          <el-tab-pane label="新建并关联" name="create" />
        </el-tabs>

        <template v-if="referenceType === 'article' || cardDialogMode === 'search'">
          <div class="reference-search-row">
            <el-input
              v-model="referenceKeyword"
              clearable
              :placeholder="referenceType === 'article' ? '搜索文章标题或正文' : '搜索卡片标题或摘要'"
              @keyup.enter="searchReferenceTargets"
            />
            <el-button type="primary" :icon="Search" :loading="referenceLoading" @click="searchReferenceTargets">
              搜索
            </el-button>
          </div>

          <el-skeleton v-if="referenceLoading" :rows="4" animated />
          <el-alert
            v-else-if="referenceError"
            type="error"
            :title="referenceError"
            show-icon
            :closable="false"
          />
          <el-empty v-else-if="referenceItems.length === 0" description="没有找到可关联的内容" />
          <div v-else class="reference-result-list">
            <button
              v-for="item in referenceItems"
              :key="item.id"
              type="button"
              class="reference-result-item"
              @click="insertReference(item)"
            >
              <span :class="referenceType === 'article' ? 'article-reference-mark' : 'card-reference-mark'">
                <el-icon><component :is="referenceType === 'article' ? Link : Notebook" /></el-icon>
              </span>
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ referenceTargetSummary(item) }}</small>
              </span>
            </button>
          </div>
        </template>

        <el-form v-else label-position="top" class="knowledge-card-create-form" @submit.prevent>
          <el-form-item label="标题" required>
            <el-input v-model="newKnowledgeCard.title" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="摘要" required>
            <el-input
              v-model="newKnowledgeCard.summary"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="完整内容" required>
            <el-input v-model="newKnowledgeCard.content" type="textarea" :rows="7" />
          </el-form-item>
          <div class="reference-create-actions">
            <el-button
              type="primary"
              :icon="Check"
              :loading="creatingKnowledgeCard"
              @click="createAndInsertKnowledgeCard"
            >
              创建并关联
            </el-button>
          </div>
        </el-form>
      </div>
    </el-dialog>

    <el-drawer v-model="historyOpen" title="版本历史" size="520px" destroy-on-close>
      <el-skeleton v-if="historyLoading" :rows="8" animated />
      <div v-else-if="historyError" class="state-panel compact-state">
        <el-alert type="error" :title="historyError" show-icon :closable="false" />
        <el-button :icon="Refresh" @click="loadRevisionHistory">重试</el-button>
      </div>
      <el-empty v-else-if="revisions.length === 0" description="还没有历史版本" />
      <div v-else class="revision-workspace">
        <div class="revision-list">
          <button
            v-for="revision in revisions"
            :key="revision.id"
            class="revision-item"
            :class="{ active: selectedRevision?.id === revision.id }"
            type="button"
            @click="selectRevision(revision.id)"
          >
            <span>版本 {{ revision.revisionNumber }}</span>
            <strong>{{ revision.title }}</strong>
            <small>{{ revision.createdByName || '未知用户' }} · {{ formatRevisionDate(revision.createdAt) }}</small>
          </button>
        </div>

        <el-skeleton v-if="revisionDetailLoading" :rows="6" animated />
        <section v-else-if="selectedRevision" class="revision-preview">
          <div class="revision-preview-heading">
            <div>
              <span>版本 {{ selectedRevision.revisionNumber }}</span>
              <h3>{{ selectedRevision.title }}</h3>
            </div>
            <el-button
              type="primary"
              :icon="RefreshLeft"
              :loading="restoringRevision"
              :disabled="isDraftSaving"
              @click="restoreRevision"
            >
              恢复此版本
            </el-button>
          </div>
          <div class="markdown-body revision-markdown" v-html="renderMarkdown(selectedRevision.content || '')"></div>
        </section>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  ChatLineRound,
  Check,
  CollectionTag,
  Clock,
  Delete,
  DocumentCopy,
  Link,
  List,
  Memo,
  Notebook,
  Picture,
  Reading,
  Refresh,
  RefreshLeft,
  Search,
  Stopwatch,
} from '@element-plus/icons-vue'
import {
  createArticle,
  createArticleDraft,
  createKnowledgeCard,
  deleteArticleDraft,
  deleteArticleDraftById,
  getArticle,
  getArticleDraft,
  getArticleDraftById,
  getArticleRevision,
  listArticleRevisions,
  listArticles,
  listCategories,
  listKnowledgeCards,
  restoreArticleRevision,
  saveArticleDraft,
  updateArticle,
  updateArticleDraft,
} from '../api/blog'
import { sessionState } from '../state/session'
import { canManageResource, currentUserId, isSignedIn } from '../utils/permissions'
import { markdownToPlainText, renderMarkdown } from '../utils/markdown'
import MarkdownMentionEditor from '../components/MarkdownMentionEditor.vue'

const props = defineProps({
  id: {
    type: String,
    default: '',
  },
})

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const categoriesLoaded = ref(false)
const categories = ref([])
const editorMode = ref('split')
const contentEditorRef = ref(null)
const previewPaneRef = ref(null)
const markdownWorkspaceRef = ref(null)
const editorLayoutRef = ref(null)
const editorPanePercent = ref(50)
const sidePanelWidth = ref(260)
const sourceArticle = ref(null)
const draftId = ref(null)
const draftStatus = ref('saved')
const lastDraftSavedAt = ref(null)
const lastSavedSnapshot = ref('')
const initializingForm = ref(true)
const isDraftSaving = ref(false)
const discardingDraft = ref(false)
const historyOpen = ref(false)
const historyLoading = ref(false)
const historyError = ref('')
const revisions = ref([])
const selectedRevision = ref(null)
const revisionDetailLoading = ref(false)
const restoringRevision = ref(false)
const referenceDialogOpen = ref(false)
const referenceType = ref('article')
const referenceKeyword = ref('')
const referenceItems = ref([])
const referenceLoading = ref(false)
const referenceError = ref('')
const cardDialogMode = ref('search')
const creatingKnowledgeCard = ref(false)
const referenceSelection = reactive({ start: 0, end: 0, text: '' })
const newKnowledgeCard = reactive({ title: '', summary: '', content: '' })
const NEW_DRAFT_STORAGE_KEY = 'blogNewArticleDraftId'
const AUTO_SAVE_DELAY = 1400
let autoSaveTimer = null
let autoSaveDebounceTimer = null
const form = reactive({
  title: '',
  categoryId: '',
  content: '',
})

const isEdit = computed(() => Boolean(props.id))
const canSubmitArticle = computed(() => {
  return isEdit.value ? canManageResource(sourceArticle.value) : isSignedIn()
})
const contentLength = computed(() => form.content.trim().length)
const estimatedMinutes = computed(() => {
  return contentLength.value ? Math.max(1, Math.ceil(contentLength.value / 500)) : 0
})
const renderedContent = computed(() => renderMarkdown(form.content))
const currentFormSnapshot = computed(() => JSON.stringify({
  title: form.title,
  categoryId: form.categoryId || null,
  content: form.content,
}))
const hasUnsavedChanges = computed(() => {
  return !initializingForm.value && currentFormSnapshot.value !== lastSavedSnapshot.value
})
const hasSavedDraft = computed(() => Boolean(draftId.value))
const newDraftStorageKey = computed(() => {
  const userId = currentUserId()
  return userId === null ? '' : `${NEW_DRAFT_STORAGE_KEY}:${userId}`
})
const canSaveDraft = computed(() => {
  return canSubmitArticle.value && hasUnsavedChanges.value && !saving.value && !isDraftSaving.value
})
const draftStatusLabel = computed(() => {
  if (draftStatus.value === 'saving') {
    return '正在自动保存'
  }
  if (draftStatus.value === 'unsaved') {
    return '有尚未保存的修改'
  }
  if (draftStatus.value === 'error') {
    return '自动保存失败，将在稍后重试'
  }
  if (lastDraftSavedAt.value) {
    return `已保存 · ${formatDraftTime(lastDraftSavedAt.value)}`
  }
  return '草稿自动保存'
})
const draftControlTitle = computed(() => {
  if (draftStatus.value === 'saving') {
    return '正在同步草稿'
  }
  if (draftStatus.value === 'error') {
    return '草稿同步失败'
  }
  if (hasUnsavedChanges.value) {
    return '有未同步的修改'
  }
  return hasSavedDraft.value ? '草稿已同步' : '尚未生成草稿'
})
const draftControlMeta = computed(() => {
  if (draftStatus.value === 'error') {
    return '保留当前页面，点击按钮即可重试'
  }
  if (lastDraftSavedAt.value) {
    return `最近保存于 ${formatDraftTime(lastDraftSavedAt.value)}`
  }
  return '开始写作后自动保存'
})
const workspaceStyle = computed(() => {
  if (editorMode.value !== 'split') {
    return null
  }

  return {
    '--editor-pane-size': `${editorPanePercent.value}fr`,
    '--preview-pane-size': `${100 - editorPanePercent.value}fr`,
  }
})
const editorLayoutStyle = computed(() => ({
  '--editor-side-width': `${sidePanelWidth.value}px`,
}))
const referenceDialogTitle = computed(() => {
  return referenceType.value === 'article' ? '链接站内文章' : '关联知识卡片'
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

watch(currentFormSnapshot, () => {
  if(initializingForm.value){
    return
  }

  draftStatus.value = hasUnsavedChanges.value ? 'unsaved' : 'saved'
  if(hasUnsavedChanges.value){
    scheduleAutoSave()
  }
})

function draftPayload(){
  return {
    title: form.title,
    content: form.content,
    categoryId: form.categoryId ? Number(form.categoryId) : null,
  }
}

function formatDraftTime(value){
  const date = new Date(value)
  if(Number.isNaN(date.getTime())){
    return '刚刚'
  }

  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  return date.toLocaleString('zh-CN', isToday
    ? { hour: '2-digit', minute: '2-digit', hour12: false }
    : { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

function formatRevisionDate(value){
  if(!value){
    return '未知时间'
  }

  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

function applyDraft(draft){
  draftId.value = draft.id
  form.title = draft.title || ''
  form.content = draft.content || ''
  form.categoryId = draft.categoryId || ''
  lastDraftSavedAt.value = draft.updatedAt
}

function getStoredNewDraft(){
  if(newDraftStorageKey.value){
    const scopedDraftId = localStorage.getItem(newDraftStorageKey.value)
    if(scopedDraftId){
      return { id: scopedDraftId, key: newDraftStorageKey.value }
    }
  }

  const legacyDraftId = localStorage.getItem(NEW_DRAFT_STORAGE_KEY)
  return legacyDraftId ? { id: legacyDraftId, key: NEW_DRAFT_STORAGE_KEY } : null
}

function rememberNewDraft(id){
  if(!newDraftStorageKey.value){
    return
  }

  localStorage.setItem(newDraftStorageKey.value, String(id))
  localStorage.removeItem(NEW_DRAFT_STORAGE_KEY)
}

function forgetNewDraft(){
  if(newDraftStorageKey.value){
    localStorage.removeItem(newDraftStorageKey.value)
  }
  localStorage.removeItem(NEW_DRAFT_STORAGE_KEY)
}

function waitForSessionCheck(){
  if(!sessionState.checking){
    return Promise.resolve()
  }

  return new Promise((resolve) => {
    const stop = watch(
      () => sessionState.checking,
      (checking) => {
        if(!checking){
          stop()
          resolve()
        }
      },
    )
  })
}

async function loadSavedDraft(){
  if(isEdit.value){
    const draft = await getArticleDraft(props.id)
    if(draft?.id){
      applyDraft(draft)
      ElMessage.info('已恢复上次自动保存的草稿')
    }
    return
  }

  const storedDraft = getStoredNewDraft()
  if(!storedDraft){
    return
  }

  try{
    const draft = await getArticleDraftById(storedDraft.id)
    applyDraft(draft)
    rememberNewDraft(draft.id)
    ElMessage.info('已恢复上次未发布的草稿')
  }catch(error){
    if(error.response?.status === 404 || storedDraft.key === newDraftStorageKey.value){
      localStorage.removeItem(storedDraft.key)
    }
  }
}

function scheduleAutoSave(delay = AUTO_SAVE_DELAY){
  window.clearTimeout(autoSaveDebounceTimer)
  autoSaveDebounceTimer = window.setTimeout(() => autoSaveDraft(), delay)
}

async function autoSaveDraft({ notify = false } = {}){
  if(
    loading.value ||
    saving.value ||
    !canSubmitArticle.value ||
    !hasUnsavedChanges.value
  ){
    return false
  }

  if(isDraftSaving.value){
    scheduleAutoSave(500)
    return false
  }

  window.clearTimeout(autoSaveDebounceTimer)
  const snapshotBeingSaved = currentFormSnapshot.value
  isDraftSaving.value = true
  draftStatus.value = 'saving'

  try{
    let savedDraft
    if(isEdit.value){
      savedDraft = await saveArticleDraft(props.id, draftPayload())
    }else if(draftId.value){
      savedDraft = await updateArticleDraft(draftId.value, draftPayload())
    }else{
      savedDraft = await createArticleDraft(draftPayload())
      draftId.value = savedDraft.id
      rememberNewDraft(savedDraft.id)
    }

    draftId.value = savedDraft.id
    if(!isEdit.value){
      rememberNewDraft(savedDraft.id)
    }
    lastSavedSnapshot.value = snapshotBeingSaved
    lastDraftSavedAt.value = savedDraft.updatedAt
    draftStatus.value = currentFormSnapshot.value === snapshotBeingSaved ? 'saved' : 'unsaved'
    if(notify){
      ElMessage.success('草稿已保存')
    }
    return true
  }catch{
    draftStatus.value = 'error'
    if(notify){
      ElMessage.error('草稿保存失败，请稍后重试')
    }
    return false
  }finally{
    isDraftSaving.value = false
    if(hasUnsavedChanges.value && draftStatus.value !== 'error'){
      scheduleAutoSave()
    }
  }
}

async function deletePersistedDraft({ ignoreErrors = false } = {}){
  try{
    if(isEdit.value){
      await deleteArticleDraft(props.id)
    }else if(draftId.value){
      await deleteArticleDraftById(draftId.value)
    }
  }catch(error){
    if(!ignoreErrors){
      throw error
    }
  }

  draftId.value = null
  forgetNewDraft()
}

async function clearSavedDraft(){
  await deletePersistedDraft({ ignoreErrors: true })
}

async function saveDraftNow(){
  if(!canSubmitArticle.value){
    ElMessage.warning('当前账号没有保存这篇草稿的权限')
    return
  }

  if(!hasUnsavedChanges.value){
    ElMessage.info(hasSavedDraft.value ? '草稿已经是最新状态' : '还没有需要保存的内容')
    return
  }

  await autoSaveDraft({ notify: true })
}

async function discardDraft(){
  if(!hasSavedDraft.value || discardingDraft.value){
    return
  }

  try{
    await ElMessageBox.confirm(
      isEdit.value ? '放弃草稿后，将恢复为当前已发布版本。' : '放弃后，这份未发布草稿将被永久删除。',
      '放弃草稿',
      {
        confirmButtonText: '确认放弃',
        cancelButtonText: '继续编辑',
        type: 'warning',
      },
    )
  }catch{
    return
  }

  discardingDraft.value = true
  initializingForm.value = true
  window.clearTimeout(autoSaveDebounceTimer)

  try{
    await deletePersistedDraft()

    if(isEdit.value && sourceArticle.value){
      form.title = sourceArticle.value.title
      form.content = sourceArticle.value.content
      form.categoryId = sourceArticle.value.categoryId
    }else{
      form.title = ''
      form.content = ''
      form.categoryId = categories.value[0]?.id || ''
    }

    lastDraftSavedAt.value = null
    await nextTick()
    lastSavedSnapshot.value = currentFormSnapshot.value
    draftStatus.value = 'saved'
    ElMessage.success('草稿已放弃')
  }finally{
    initializingForm.value = false
    discardingDraft.value = false
  }
}

function handleBeforeUnload(event){
  if(!hasUnsavedChanges.value){
    return
  }

  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(() => {
  if(!hasUnsavedChanges.value){
    return true
  }

  return window.confirm('当前修改尚未自动保存，确定离开编辑页面吗？')
})

async function openReferenceDialog(type){
  const selection = getEditorSelection()
  const start = selection.start
  const end = selection.end

  referenceType.value = type
  referenceSelection.start = start
  referenceSelection.end = end
  referenceSelection.text = form.content.slice(start, end).replace(/\s+/g, ' ').trim()
  referenceKeyword.value = referenceSelection.text
  referenceItems.value = []
  referenceError.value = ''
  cardDialogMode.value = 'search'
  newKnowledgeCard.title = referenceSelection.text
  newKnowledgeCard.summary = ''
  newKnowledgeCard.content = ''
  referenceDialogOpen.value = true
  await searchReferenceTargets()
}

async function searchReferenceTargets(){
  referenceLoading.value = true
  referenceError.value = ''

  try{
    if(referenceType.value === 'article'){
      const response = await listArticles({
        page: 1,
        size: 20,
        keyword: referenceKeyword.value.trim(),
      })
      referenceItems.value = (response.content || []).filter((item) => String(item.id) !== String(props.id))
    }else{
      referenceItems.value = await listKnowledgeCards({ keyword: referenceKeyword.value.trim() })
    }
  }catch{
    referenceItems.value = []
    referenceError.value = referenceType.value === 'article' ? '文章搜索失败' : '知识卡片搜索失败'
  }finally{
    referenceLoading.value = false
  }
}

function referenceTargetSummary(item){
  if(referenceType.value === 'card'){
    return item.summary || '暂无摘要'
  }

  const summary = markdownToPlainText(item.content || '')
  return summary ? summary.slice(0, 100) : '暂无正文摘要'
}

async function insertReference(item){
  const scheme = referenceType.value === 'article' ? 'article' : 'card'
  const rawLabel = referenceSelection.text || item.title
  const label = rawLabel.replace(/\]/g, '\\]').replace(/[\r\n]+/g, ' ')
  const referenceMarkdown = `[${label}](${scheme}:${item.id})`
  referenceDialogOpen.value = false

  await nextTick()
  const cursor = referenceSelection.start + referenceMarkdown.length
  contentEditorRef.value?.replaceRange(
    referenceSelection.start,
    referenceSelection.end,
    referenceMarkdown,
    cursor,
  )
  syncPreviewToCursor()
}

async function createAndInsertKnowledgeCard(){
  const payload = {
    title: newKnowledgeCard.title.trim(),
    summary: newKnowledgeCard.summary.trim(),
    content: newKnowledgeCard.content.trim(),
  }

  if(!payload.title || !payload.summary || !payload.content){
    ElMessage.warning('请完整填写知识卡片的标题、摘要和内容')
    return
  }

  creatingKnowledgeCard.value = true
  try{
    const knowledgeCard = await createKnowledgeCard(payload)
    await insertReference(knowledgeCard)
    ElMessage.success('知识卡片已创建并关联')
  }finally{
    creatingKnowledgeCard.value = false
  }
}

const snippets = {
  heading: {
    prefix: '## ',
    placeholder: '小节标题',
  },
  list: {
    prefix: '- ',
    placeholder: '列表项',
  },
  quote: {
    prefix: '> ',
    placeholder: '引用内容',
  },
  code: {
    prefix: '```js\n',
    placeholder: 'console.log("hello markdown")',
    suffix: '\n```',
  },
  image: {
    prefix: '![图片描述](',
    placeholder: 'https://example.com/image.png',
    suffix: ')',
  },
}

function getEditorSelection(){
  return contentEditorRef.value?.getSelection() || {
    start: form.content.length,
    end: form.content.length,
    text: '',
    line: 0,
  }
}

function clampRatio(value) {
  if (!Number.isFinite(value)) {
    return 0
  }

  return Math.min(1, Math.max(0, value))
}

function getSourceLineNodes(preview) {
  return [...preview.querySelectorAll('[data-source-line]')]
    .map((node) => ({
      node,
      line: Number(node.dataset.sourceLine),
    }))
    .filter((item) => Number.isFinite(item.line))
    .sort((left, right) => left.line - right.line)
}

function getPreviewScrollTop(preview, target) {
  const previewRect = preview.getBoundingClientRect()
  const targetRect = target.getBoundingClientRect()
  const nextTop = preview.scrollTop + targetRect.top - previewRect.top - 18
  const maxTop = preview.scrollHeight - preview.clientHeight

  return Math.min(Math.max(nextTop, 0), Math.max(maxTop, 0))
}

async function scrollPreviewToRatio(ratio) {
  if (editorMode.value === 'edit') {
    return
  }

  await nextTick()

  const preview = previewPaneRef.value
  if (!preview) {
    return
  }

  const maxPreviewScroll = preview.scrollHeight - preview.clientHeight
  if (maxPreviewScroll <= 0) {
    preview.scrollTop = 0
    return
  }

  preview.scrollTop = maxPreviewScroll * clampRatio(ratio)
}

async function scrollPreviewToSourceLine(sourceLine, fallbackRatio) {
  if (editorMode.value === 'edit') {
    return
  }

  await nextTick()

  const preview = previewPaneRef.value
  if (!preview) {
    return
  }

  const sourceLineNodes = getSourceLineNodes(preview)
  let target = sourceLineNodes[0]?.node || null

  for (const item of sourceLineNodes) {
    if (item.line > sourceLine) {
      break
    }
    target = item.node
  }

  if (!target) {
    scrollPreviewToRatio(fallbackRatio)
    return
  }

  preview.scrollTop = getPreviewScrollTop(preview, target)
}

function syncPreviewByScroll(ratio = contentEditorRef.value?.getScrollRatio() || 0) {
  scrollPreviewToRatio(ratio)
}

function syncPreviewToCursor(selection) {
  const cursorLine = selection?.line ?? contentEditorRef.value?.getCursorLine() ?? 0
  const totalLines = Math.max(1, form.content.split('\n').length - 1)
  scrollPreviewToSourceLine(cursorLine, cursorLine / totalLines)
}

async function insertMarkdown(type) {
  const snippet = snippets[type]

  if (!snippet) {
    return
  }

  const selection = getEditorSelection()
  const selectedText = selection.text
  const content = selectedText || snippet.placeholder
  const inserted = `${snippet.prefix}${content}${snippet.suffix || ''}`

  const start = selection.start
  const end = selection.end
  const before = form.content.slice(0, start)
  const after = form.content.slice(end)
  const needsLeadingBreak = before && !before.endsWith('\n')
  const needsTrailingBreak = after && !after.startsWith('\n')
  const replacement = `${needsLeadingBreak ? '\n' : ''}${inserted}${needsTrailingBreak ? '\n' : ''}`

  const selectionStart = start + (needsLeadingBreak ? 1 : 0) + snippet.prefix.length
  const selectionEnd = selectionStart + content.length
  contentEditorRef.value?.replaceRange(start, end, replacement, selectionStart, selectionEnd)
  syncPreviewToCursor()
}

function startPaneResize(event) {
  const workspace = markdownWorkspaceRef.value

  if (!workspace) {
    return
  }

  event.preventDefault()
  event.currentTarget.setPointerCapture?.(event.pointerId)

  const rect = workspace.getBoundingClientRect()

  function resize(pointerEvent) {
    const nextPercent = ((pointerEvent.clientX - rect.left) / rect.width) * 100
    editorPanePercent.value = Math.min(78, Math.max(22, Math.round(nextPercent)))
  }

  function stop() {
    window.removeEventListener('pointermove', resize)
    window.removeEventListener('pointerup', stop)
    window.removeEventListener('pointercancel', stop)
  }

  window.addEventListener('pointermove', resize)
  window.addEventListener('pointerup', stop)
  window.addEventListener('pointercancel', stop)
  resize(event)
}

function startLayoutResize(event) {
  const layout = editorLayoutRef.value

  if (!layout) {
    return
  }

  event.preventDefault()
  event.currentTarget.setPointerCapture?.(event.pointerId)

  const rect = layout.getBoundingClientRect()

  function resize(pointerEvent) {
    const nextWidth = rect.right - pointerEvent.clientX
    sidePanelWidth.value = Math.min(360, Math.max(220, Math.round(nextWidth)))
  }

  function stop() {
    window.removeEventListener('pointermove', resize)
    window.removeEventListener('pointerup', stop)
    window.removeEventListener('pointercancel', stop)
  }

  window.addEventListener('pointermove', resize)
  window.addEventListener('pointerup', stop)
  window.addEventListener('pointercancel', stop)
  resize(event)
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  initializingForm.value = true

  try {
    await waitForSessionCheck()
    categories.value = await listCategories()
    categoriesLoaded.value = true

    if (isEdit.value) {
      const article = await getArticle(props.id)
      sourceArticle.value = article

      if (!canManageResource(article)) {
        loadError.value = '当前账号没有编辑这篇文章的权限'
        return
      }

      form.title = article.title
      form.categoryId = article.categoryId
      form.content = article.content
      await nextTick()
      syncPreviewByScroll()
    } else if (categories.value[0]) {
      form.categoryId = categories.value[0].id
    }

    await loadSavedDraft()
  } catch {
    loadError.value = isEdit.value ? '文章或分类加载失败' : '分类加载失败，请确认后端服务已启动'
  } finally {
    lastSavedSnapshot.value = currentFormSnapshot.value
    draftStatus.value = 'saved'
    initializingForm.value = false
    loading.value = false
  }
}

async function openRevisionHistory(){
  historyOpen.value = true
  await loadRevisionHistory()
}

async function loadRevisionHistory(){
  if(!isEdit.value){
    return
  }

  historyLoading.value = true
  historyError.value = ''

  try{
    revisions.value = await listArticleRevisions(props.id)
    if(revisions.value[0]){
      await selectRevision(revisions.value[0].id)
    }else{
      selectedRevision.value = null
    }
  }catch{
    historyError.value = '历史版本加载失败'
  }finally{
    historyLoading.value = false
  }
}

async function selectRevision(revisionId){
  revisionDetailLoading.value = true

  try{
    selectedRevision.value = await getArticleRevision(props.id, revisionId)
  }catch{
    ElMessage.error('版本内容加载失败')
  }finally{
    revisionDetailLoading.value = false
  }
}

async function restoreRevision(){
  if(!selectedRevision.value || isDraftSaving.value){
    return
  }

  try{
    await ElMessageBox.confirm(
      `确定恢复到版本 ${selectedRevision.value.revisionNumber} 吗？当前正式内容会作为新版本保留。`,
      '恢复历史版本',
      {
        confirmButtonText: '确认恢复',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  }catch{
    return
  }

  restoringRevision.value = true
  try{
    const restored = await restoreArticleRevision(props.id, selectedRevision.value.id)
    sourceArticle.value = restored
    form.title = restored.title
    form.content = restored.content
    form.categoryId = restored.categoryId
    await clearSavedDraft()
    lastSavedSnapshot.value = currentFormSnapshot.value
    lastDraftSavedAt.value = null
    draftStatus.value = 'saved'
    ElMessage.success('历史版本已恢复，并生成了新的版本记录')
    await loadRevisionHistory()
  }finally{
    restoringRevision.value = false
  }
}

async function submit() {
  if(isDraftSaving.value){
    ElMessage.info('请等待当前草稿保存完成')
    return
  }

  if (!canSubmitArticle.value) {
    ElMessage.warning('当前账号没有保存这篇文章的权限')
    return
  }

  await formRef.value.validate()

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      categoryId: Number(form.categoryId),
      content: form.content.trim(),
    }

    const saved = isEdit.value
      ? await updateArticle(props.id, payload)
      : await createArticle(payload)

    sourceArticle.value = saved
    form.title = saved.title
    form.content = saved.content
    form.categoryId = saved.categoryId
    await clearSavedDraft()
    lastSavedSnapshot.value = currentFormSnapshot.value
    lastDraftSavedAt.value = null
    draftStatus.value = 'saved'
    ElMessage.success(isEdit.value ? '文章已保存' : '文章已发布')
    router.push(`/articles/${saved.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await loadData()
  autoSaveTimer = window.setInterval(autoSaveDraft, 30000)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.clearInterval(autoSaveTimer)
  window.clearTimeout(autoSaveDebounceTimer)
})
</script>
