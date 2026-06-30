<template>
  <section class="page-stack editor-page">
    <div class="hero-panel">
      <div>
        <p class="section-kicker">Compose</p>
        <h2>{{ isEdit ? '编辑文章' : '发布文章' }}</h2>
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
                </div>
                <el-radio-group v-model="editorMode" size="small">
                  <el-radio-button label="edit">编辑</el-radio-button>
                  <el-radio-button label="split">分屏</el-radio-button>
                  <el-radio-button label="preview">预览</el-radio-button>
                </el-radio-group>
              </div>

              <div
                ref="markdownWorkspaceRef"
                class="markdown-workspace"
                :class="`mode-${editorMode}`"
                :style="workspaceStyle"
              >
                <div v-show="editorMode !== 'preview'" class="markdown-pane markdown-input-pane">
                  <textarea
                    ref="contentTextareaRef"
                    v-model="form.content"
                    class="markdown-native-textarea"
                    spellcheck="false"
                    placeholder="使用 Markdown 写作，例如 # 标题、- 列表、> 引用、```js 代码块、![描述](图片地址)"
                    @click="syncPreviewToCursor"
                    @focus="syncPreviewToCursor"
                    @keyup="syncPreviewToCursor"
                    @mouseup="syncPreviewToCursor"
                    @scroll="syncPreviewByScroll"
                  ></textarea>
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
          <div class="side-section">
            <p class="section-kicker">Publish</p>
            <h3>发布信息</h3>
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
              :disabled="categories.length === 0 || !canSubmitArticle"
              @click="submit"
            >
              {{ isEdit ? '保存修改' : '发布文章' }}
            </el-button>
          </div>
        </aside>
      </div>
    </el-form>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  ChatLineRound,
  Check,
  CollectionTag,
  List,
  Memo,
  Picture,
  Reading,
  Refresh,
  Stopwatch,
} from '@element-plus/icons-vue'
import { createArticle, getArticle, listCategories, updateArticle } from '../api/blog'
import { canManageResource, isSignedIn } from '../utils/permissions'
import { renderMarkdown } from '../utils/markdown'

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
const contentTextareaRef = ref(null)
const previewPaneRef = ref(null)
const markdownWorkspaceRef = ref(null)
const editorLayoutRef = ref(null)
const editorPanePercent = ref(50)
const sidePanelWidth = ref(260)
const sourceArticle = ref(null)
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

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
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

function getTextareaElement() {
  return contentTextareaRef.value || null
}

function clampRatio(value) {
  if (!Number.isFinite(value)) {
    return 0
  }

  return Math.min(1, Math.max(0, value))
}

function getCursorLine(textarea) {
  const beforeCursor = form.content.slice(0, textarea.selectionStart)
  return beforeCursor.split('\n').length - 1
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

function syncPreviewByScroll() {
  const textarea = getTextareaElement()
  if (!textarea) {
    return
  }

  const maxSourceScroll = textarea.scrollHeight - textarea.clientHeight
  const ratio = maxSourceScroll > 0 ? textarea.scrollTop / maxSourceScroll : 0
  scrollPreviewToRatio(ratio)
}

function syncPreviewToCursor() {
  const textarea = getTextareaElement()
  if (!textarea) {
    return
  }

  const cursorLine = getCursorLine(textarea)
  const totalLines = Math.max(1, form.content.split('\n').length - 1)
  scrollPreviewToSourceLine(cursorLine, cursorLine / totalLines)
}

async function insertMarkdown(type) {
  const snippet = snippets[type]

  if (!snippet) {
    return
  }

  const textarea = getTextareaElement()
  const selectedText = textarea
    ? form.content.slice(textarea.selectionStart, textarea.selectionEnd)
    : ''
  const content = selectedText || snippet.placeholder
  const inserted = `${snippet.prefix}${content}${snippet.suffix || ''}`

  if (!textarea) {
    form.content = form.content ? `${form.content}\n\n${inserted}` : inserted
    return
  }

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = form.content.slice(0, start)
  const after = form.content.slice(end)
  const needsLeadingBreak = before && !before.endsWith('\n')
  const needsTrailingBreak = after && !after.startsWith('\n')
  const replacement = `${needsLeadingBreak ? '\n' : ''}${inserted}${needsTrailingBreak ? '\n' : ''}`

  form.content = `${before}${replacement}${after}`

  await nextTick()

  const selectionStart = start + (needsLeadingBreak ? 1 : 0) + snippet.prefix.length
  const selectionEnd = selectionStart + content.length
  textarea.focus()
  textarea.setSelectionRange(selectionStart, selectionEnd)
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

  try {
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
  } catch {
    loadError.value = isEdit.value ? '文章或分类加载失败' : '分类加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

async function submit() {
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

    ElMessage.success(isEdit.value ? '文章已保存' : '文章已发布')
    router.push(`/articles/${saved.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
