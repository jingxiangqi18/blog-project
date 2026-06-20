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
            <el-button type="primary" :icon="EditPen" @click="$router.push(`/articles/${article.id}/edit`)">
              编辑
            </el-button>
            <el-button type="danger" :icon="Delete" :loading="deletingArticle" @click="removeArticle">
              删除
            </el-button>
          </div>
        </div>

        <div class="article-body">
          <p v-for="(paragraph, index) in paragraphs" :key="index">
            {{ paragraph }}
          </p>
          <p v-if="paragraphs.length === 0" class="empty-copy">暂无正文内容</p>
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
            <template v-if="editingComment.id === comment.id">
              <el-input
                v-model="editingComment.content"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
              />
            </template>
            <p v-else>{{ comment.content }}</p>
            <div>
              <span class="time-chip comment-time">
                <el-icon><Calendar /></el-icon>
                {{ comment.updatedAt && comment.updatedAt !== comment.createdAt ? '编辑于' : '发布于' }}
                {{ formatDate(comment.updatedAt || comment.createdAt) }}
              </span>
              <div class="comment-actions">
                <template v-if="editingComment.id === comment.id">
                  <el-button text :disabled="editingComment.saving" @click="cancelCommentEdit">取消</el-button>
                  <el-button
                    text
                    type="primary"
                    :loading="editingComment.saving"
                    @click="saveCommentEdit(comment)"
                  >
                    保存
                  </el-button>
                </template>
                <template v-else>
                  <el-button text type="primary" :icon="EditPen" @click="startCommentEdit(comment)">
                    编辑
                  </el-button>
                  <el-button
                    text
                    type="danger"
                    :icon="Delete"
                    :loading="deletingCommentId === comment.id"
                    @click="removeComment(comment)"
                  >
                    删除
                  </el-button>
                </template>
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
import { Back, Calendar, ChatLineRound, Delete, EditPen, Reading, Refresh, Stopwatch } from '@element-plus/icons-vue'
import { createComment, deleteArticle, deleteComment, getArticle, listComments, updateComment } from '../api/blog'

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

const paragraphs = computed(() => {
  return (article.value?.content || '').split(/\n+/).filter(Boolean)
})
const wordCount = computed(() => (article.value?.content || '').trim().length)
const readingMinutes = computed(() => {
  return wordCount.value ? Math.max(1, Math.ceil(wordCount.value / 500)) : 0
})

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
  editingComment.id = comment.id
  editingComment.content = comment.content
}

function cancelCommentEdit() {
  editingComment.id = null
  editingComment.content = ''
}

async function saveCommentEdit(comment) {
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
