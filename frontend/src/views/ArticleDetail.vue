<template>
  <section class="page-stack reader-page">
    <el-skeleton v-if="loading" class="panel reader-loading" :rows="10" animated />

    <div v-else-if="error" class="panel state-panel error-panel">
      <el-alert type="error" :title="error" show-icon :closable="false" />
      <el-button :icon="Refresh" @click="loadArticle">重试</el-button>
    </div>

    <template v-else-if="article">
      <article class="reader-panel panel">
        <div class="reader-cover">
          <div>
            <div class="card-meta">
              <span class="category-chip">{{ article.categoryName || '未分类' }}</span>
              <span class="time-chip">
                <el-icon><Calendar /></el-icon>
                更新 {{ formatDate(article.updatedAt) }}
              </span>
            </div>
            <h2>{{ article.title }}</h2>
            <div class="reader-stats">
              <span class="info-chip">
                <el-icon><Reading /></el-icon>
                {{ wordCount }} 字符
              </span>
              <span class="info-chip">
                <el-icon><Stopwatch /></el-icon>
                约 {{ readingMinutes }} 分钟读完
              </span>
            </div>
          </div>
          <div class="reader-actions">
            <el-button :icon="Back" @click="$router.push('/articles')">返回</el-button>
            <el-button
              v-if="canManageArticle"
              type="primary"
              :icon="EditPen"
              @click="$router.push(`/articles/${article.id}/edit`)"
            >
              编辑
            </el-button>
            <el-button
              v-if="canManageArticle"
              type="danger"
              :icon="Delete"
              :loading="deletingArticle"
              @click="removeArticle"
            >
              删除
            </el-button>
          </div>
        </div>

        <div v-if="wordCount > 0" class="article-body markdown-body" v-html="renderedArticle"></div>
        <div v-else class="article-body">
          <p class="empty-copy">暂无正文内容</p>
        </div>
      </article>

      <section class="panel comments-section">
        <div class="panel-title-row">
          <div>
            <p class="section-kicker">Comments</p>
            <h3>评论</h3>
          </div>
          <span class="comment-count">{{ comments.length }}</span>
        </div>

        <el-form class="comment-form" @submit.prevent>
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写一条评论"
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :icon="ChatLineRound" :loading="savingComment" @click="submitComment">
            发布评论
          </el-button>
        </el-form>

        <el-skeleton v-if="commentsLoading" :rows="3" animated />
        <div v-else-if="commentsError" class="state-panel compact-state">
          <el-alert type="error" :title="commentsError" show-icon :closable="false" />
          <el-button :icon="Refresh" @click="loadComments">重试</el-button>
        </div>
        <el-empty v-else-if="comments.length === 0" description="暂无评论" />
        <div v-else class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <span class="comment-avatar">{{ commentAuthorInitial(comment) }}</span>
            <div class="comment-main">
              <div class="comment-head">
                <div class="comment-meta">
                  <strong class="comment-author">{{ commentAuthorName(comment) }}</strong>
                  <span class="comment-time">
                    {{ comment.updatedAt && comment.updatedAt !== comment.createdAt ? '编辑于' : '发布于' }}
                    {{ formatDate(comment.updatedAt || comment.createdAt) }}
                  </span>
                </div>
                <el-dropdown
                  v-if="canManageResource(comment) && editingComment.id !== comment.id"
                  trigger="click"
                  placement="bottom-end"
                  @command="handleCommentCommand(comment, $event)"
                >
                  <button class="comment-more-button" type="button" aria-label="评论操作" title="评论操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit" :icon="EditPen">编辑评论内容</el-dropdown-item>
                      <el-dropdown-item command="delete" :icon="Delete" class="danger-dropdown-item">
                        删除评论
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <template v-if="editingComment.id === comment.id">
                <el-input
                  v-model="editingComment.content"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                />
              </template>
              <p v-else class="comment-content">{{ comment.content }}</p>

              <div v-if="editingComment.id === comment.id" class="comment-footer">
                <div class="comment-actions">
                  <el-button text :disabled="editingComment.saving" @click="cancelCommentEdit">取消</el-button>
                  <el-button
                    text
                    type="primary"
                    :loading="editingComment.saving"
                    @click="saveCommentEdit(comment)"
                  >
                    保存
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Back, Calendar, ChatLineRound, Delete, EditPen, MoreFilled, Reading, Refresh, Stopwatch } from '@element-plus/icons-vue'
import { createComment, deleteArticle, deleteComment, getArticle, listComments, updateComment } from '../api/blog'
import { canManageResource } from '../utils/permissions'
import { renderMarkdown } from '../utils/markdown'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const article = ref(null)
const comments = ref([])
const loading = ref(true)
const commentsLoading = ref(false)
const savingComment = ref(false)
const deletingArticle = ref(false)
const deletingCommentId = ref(null)
const error = ref('')
const commentsError = ref('')
const commentContent = ref('')
const editingComment = reactive({
  id: null,
  content: '',
  saving: false,
})

const wordCount = computed(() => (article.value?.content || '').trim().length)
const readingMinutes = computed(() => {
  return wordCount.value ? Math.max(1, Math.ceil(wordCount.value / 500)) : 0
})
const renderedArticle = computed(() => renderMarkdown(article.value?.content || ''))
const canManageArticle = computed(() => canManageResource(article.value))

function formatDate(value) {
  if (!value) {
    return '暂无时间'
  }
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hour}:${minute}`
}

function commentAuthorName(comment) {
  return comment.authorName || comment.username || (comment.authorId ? `用户 ${comment.authorId}` : '匿名用户')
}

function commentAuthorInitial(comment) {
  return commentAuthorName(comment).trim().slice(0, 1).toUpperCase()
}

async function loadArticle() {
  loading.value = true
  error.value = ''

  try {
    article.value = await getArticle(props.id)
    await loadComments()
  } catch {
    error.value = '文章不存在或加载失败'
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  commentsError.value = ''

  try {
    comments.value = await listComments(props.id)
  } catch {
    commentsError.value = '评论加载失败'
  } finally {
    commentsLoading.value = false
  }
}

async function submitComment() {
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }

  savingComment.value = true
  try {
    await createComment(props.id, { content })
    commentContent.value = ''
    await loadComments()
    ElMessage.success('评论已发布')
  } finally {
    savingComment.value = false
  }
}

async function removeArticle() {
  if (!canManageArticle.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除文章「${article.value.title}」？`, '删除文章', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }

  deletingArticle.value = true
  try {
    await deleteArticle(props.id)
    ElMessage.success('文章已删除')
    router.push('/articles')
  } finally {
    deletingArticle.value = false
  }
}

function startCommentEdit(comment) {
  if (!canManageResource(comment)) {
    return
  }

  editingComment.id = comment.id
  editingComment.content = comment.content
}

function cancelCommentEdit() {
  editingComment.id = null
  editingComment.content = ''
}

function handleCommentCommand(comment, command) {
  if (command === 'edit') {
    startCommentEdit(comment)
    return
  }

  if (command === 'delete') {
    removeComment(comment)
  }
}

async function saveCommentEdit(comment) {
  if (!canManageResource(comment)) {
    cancelCommentEdit()
    return
  }

  const content = editingComment.content.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }

  editingComment.saving = true
  try {
    await updateComment(props.id, comment.id, { content })
    cancelCommentEdit()
    await loadComments()
    ElMessage.success('评论已更新')
  } finally {
    editingComment.saving = false
  }
}

async function removeComment(comment) {
  if (!canManageResource(comment)) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除这条评论？\n${comment.content.slice(0, 32)}`, '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }

  deletingCommentId.value = comment.id
  try {
    await deleteComment(props.id, comment.id)
    await loadComments()
    ElMessage.success('评论已删除')
  } finally {
    deletingCommentId.value = null
  }
}

onMounted(loadArticle)
</script>
