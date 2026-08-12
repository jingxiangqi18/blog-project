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
            <div class="reader-author">
              <span class="reader-author-avatar">{{ articleAuthorInitial }}</span>
              <span>
                <strong>{{ articleAuthorName }}</strong>
                <small>作者</small>
              </span>
            </div>
            <div class="card-meta">
              <span class="category-chip">{{ article.categoryName || '未分类' }}</span>
              <span class="time-chip">
                <el-icon><Calendar /></el-icon>
                {{ articleTimeLabel }} {{ formatDate(articleTime) }}
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
            <el-dropdown v-if="canManageArticle" trigger="click" placement="bottom-end" @command="handleArticleCommand">
              <button class="reader-more-button" type="button" aria-label="文章操作" title="文章操作">
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
        </div>

        <div
          v-if="wordCount > 0"
          ref="articleBodyRef"
          class="article-body markdown-body"
          v-html="renderedArticle"
          @click="handleArticleBodyClick"
          @mouseover="handleArticleLinkMouseOver"
          @mouseout="handleArticleLinkMouseOut"
          @focusin="handleArticleLinkFocus"
          @focusout="hideArticlePreview"
        ></div>
        <div v-else class="article-body">
          <p class="empty-copy">暂无正文内容</p>
        </div>

        <footer class="article-interactions" aria-label="文章互动">
          <p>读到这里，留下你的回应</p>
          <div>
            <button
              class="interaction-button"
              :class="{ active: articleLike.active }"
              type="button"
              :disabled="articleLike.loading"
              @click="toggleArticleLike"
            >
              <ThumbsUp class="thumbs-up-icon" :size="16" :stroke-width="1.9" />
              <span>{{ articleLike.active ? '已喜欢' : '喜欢' }}</span>
              <strong>{{ articleLike.count }}</strong>
            </button>
            <button
              class="interaction-button"
              :class="{ active: articleFavorite.active }"
              type="button"
              :disabled="articleFavorite.loading"
              @click="toggleArticleFavorite"
            >
              <el-icon><Star /></el-icon>
              <span>{{ articleFavorite.active ? '已收藏' : '收藏' }}</span>
              <strong>{{ articleFavorite.count }}</strong>
            </button>
          </div>
        </footer>
      </article>

      <section class="panel comments-section">
        <div class="panel-title-row">
          <div>
            <p class="section-kicker">Discussion</p>
            <h3>评论与交流</h3>
          </div>
          <span class="comment-count">{{ comments.length }}</span>
        </div>

        <el-form v-if="isSignedIn" class="comment-form" @submit.prevent>
          <span class="comment-avatar composer-avatar">{{ currentUserInitial }}</span>
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="分享你的想法"
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :icon="ChatLineRound" :loading="savingComment" @click="submitComment">
            发布
          </el-button>
        </el-form>
        <button v-else class="comment-login-prompt" type="button" @click="requestLogin('登录后即可参与评论')">
          <el-icon><ChatLineRound /></el-icon>
          <span><strong>加入讨论</strong><small>登录后发布评论、回复与点赞</small></span>
          <em>去登录</em>
        </button>

        <el-skeleton v-if="commentsLoading" :rows="3" animated />
        <div v-else-if="commentsError" class="state-panel compact-state">
          <el-alert type="error" :title="commentsError" show-icon :closable="false" />
          <el-button :icon="Refresh" @click="loadComments">重试</el-button>
        </div>
        <el-empty v-else-if="comments.length === 0" description="还没有评论，来写下第一条回应" />
        <div v-else class="comment-list">
          <article v-for="thread in commentThreads" :key="thread.root.id" class="comment-thread">
            <div class="comment-item comment-root">
              <span class="comment-avatar">{{ commentAuthorInitial(thread.root) }}</span>
              <div class="comment-main">
                <div class="comment-head">
                  <div class="comment-meta">
                    <strong class="comment-author">{{ commentAuthorName(thread.root) }}</strong>
                    <span class="comment-time">{{ formatCommentTime(thread.root) }}</span>
                  </div>
                  <el-dropdown
                    v-if="canManageResource(thread.root) && editingComment.id !== thread.root.id"
                    trigger="click"
                    placement="bottom-end"
                    @command="handleCommentCommand(thread.root, $event)"
                  >
                    <button class="comment-more-button" type="button" aria-label="评论操作" title="评论操作">
                      <el-icon><MoreFilled /></el-icon>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit" :icon="EditPen">编辑评论</el-dropdown-item>
                        <el-dropdown-item command="delete" :icon="Delete" class="danger-dropdown-item">删除评论</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

                <el-input
                  v-if="editingComment.id === thread.root.id"
                  v-model="editingComment.content"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                />
                <p v-else class="comment-content">{{ thread.root.content }}</p>

                <div v-if="editingComment.id === thread.root.id" class="comment-footer">
                  <div class="comment-actions">
                    <el-button text :disabled="editingComment.saving" @click="cancelCommentEdit">取消</el-button>
                    <el-button text type="primary" :loading="editingComment.saving" @click="saveCommentEdit(thread.root)">
                      保存
                    </el-button>
                  </div>
                </div>
                <div v-else class="comment-inline-actions">
                  <button
                    class="comment-action"
                    :class="{ active: commentLikeState(thread.root).active }"
                    type="button"
                    :disabled="commentLikeState(thread.root).loading"
                    @click="toggleCommentLike(thread.root)"
                  >
                    <ThumbsUp class="thumbs-up-icon" :size="15" :stroke-width="1.9" />
                    {{ commentLikeState(thread.root).count || '喜欢' }}
                  </button>
                  <button class="comment-action" type="button" @click="startReply(thread.root, thread.root.id)">
                    <el-icon><ChatDotRound /></el-icon>回复
                  </button>
                </div>
              </div>
            </div>

            <div v-if="thread.replies.length" class="comment-replies">
              <div v-for="reply in thread.replies" :key="reply.id" class="comment-item comment-reply">
                <span class="comment-avatar small">{{ commentAuthorInitial(reply) }}</span>
                <div class="comment-main">
                  <div class="comment-head">
                    <div class="comment-meta">
                      <strong class="comment-author">{{ commentAuthorName(reply) }}</strong>
                      <span v-if="reply.replyToAuthorName" class="reply-target">回复 @{{ reply.replyToAuthorName }}</span>
                      <span class="comment-time">{{ formatCommentTime(reply) }}</span>
                    </div>
                    <el-dropdown
                      v-if="canManageResource(reply) && editingComment.id !== reply.id"
                      trigger="click"
                      placement="bottom-end"
                      @command="handleCommentCommand(reply, $event)"
                    >
                      <button class="comment-more-button" type="button" aria-label="回复操作" title="回复操作">
                        <el-icon><MoreFilled /></el-icon>
                      </button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="edit" :icon="EditPen">编辑回复</el-dropdown-item>
                          <el-dropdown-item command="delete" :icon="Delete" class="danger-dropdown-item">删除回复</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>

                  <el-input
                    v-if="editingComment.id === reply.id"
                    v-model="editingComment.content"
                    type="textarea"
                    :rows="3"
                    maxlength="500"
                    show-word-limit
                  />
                  <p v-else class="comment-content">{{ reply.content }}</p>

                  <div v-if="editingComment.id === reply.id" class="comment-footer">
                    <div class="comment-actions">
                      <el-button text :disabled="editingComment.saving" @click="cancelCommentEdit">取消</el-button>
                      <el-button text type="primary" :loading="editingComment.saving" @click="saveCommentEdit(reply)">保存</el-button>
                    </div>
                  </div>
                  <div v-else class="comment-inline-actions">
                    <button
                      class="comment-action"
                      :class="{ active: commentLikeState(reply).active }"
                      type="button"
                      :disabled="commentLikeState(reply).loading"
                      @click="toggleCommentLike(reply)"
                    >
                      <ThumbsUp class="thumbs-up-icon" :size="15" :stroke-width="1.9" />
                      {{ commentLikeState(reply).count || '喜欢' }}
                    </button>
                    <button class="comment-action" type="button" @click="startReply(reply, thread.root.id)">
                      <el-icon><ChatDotRound /></el-icon>回复
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="replyingTo.rootId === thread.root.id" class="reply-composer">
              <div class="reply-composer-head">
                <span>回复 <strong>@{{ replyingTo.authorName }}</strong></span>
                <button type="button" aria-label="取消回复" @click="cancelReply"><el-icon><Close /></el-icon></button>
              </div>
              <el-input
                v-model="replyingTo.content"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                :placeholder="`回复 @${replyingTo.authorName}`"
                @keyup.ctrl.enter="submitReply"
              />
              <div class="reply-composer-actions">
                <el-button type="primary" size="small" :loading="replyingTo.saving" @click="submitReply">发布回复</el-button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </template>

    <teleport to="body">
      <aside
        v-if="articlePreview.visible"
        class="internal-article-preview"
        :style="articlePreviewStyle"
        aria-live="polite"
      >
        <el-skeleton v-if="articlePreview.loading" :rows="3" animated />
        <template v-else-if="articlePreview.article">
          <span>站内文章</span>
          <strong>{{ articlePreview.article.title }}</strong>
          <p>{{ articlePreviewSummary }}</p>
          <small>{{ articlePreview.article.categoryName || '未分类' }} · 点击继续阅读</small>
        </template>
        <p v-else class="internal-preview-error">文章预览暂时无法加载</p>
      </aside>
    </teleport>

    <teleport to="body">
      <aside
        v-if="knowledgeCardPreview.visible"
        class="knowledge-card-hover-preview"
        :style="knowledgeCardPreviewStyle"
        aria-live="polite"
      >
        <el-skeleton v-if="knowledgeCardPreview.loading" :rows="3" animated />
        <template v-else-if="knowledgeCardPreview.card">
          <span>Knowledge card</span>
          <strong>{{ knowledgeCardPreview.card.title }}</strong>
          <p>{{ knowledgeCardPreview.card.summary || knowledgeCardPreviewSummary }}</p>
          <small>
            {{ knowledgeCardPreview.card.createdByName || '知识卡片' }} · 点击打开
          </small>
        </template>
        <p v-else class="internal-preview-error">知识卡片预览暂时无法加载</p>
      </aside>

      <article
        v-for="cardWindow in knowledgeWindows"
        :key="cardWindow.key"
        class="knowledge-floating-window"
        :class="{ maximized: cardWindow.maximized, loading: cardWindow.loading }"
        :style="knowledgeWindowStyle(cardWindow)"
        tabindex="-1"
        role="dialog"
        :aria-label="cardWindow.card?.title || '知识卡片'"
        @pointerdown="bringKnowledgeWindowToFront(cardWindow)"
        @keydown.esc.stop="closeKnowledgeWindow(cardWindow.key)"
      >
        <header
          class="knowledge-window-bar"
          @pointerdown="startKnowledgeWindowDrag(cardWindow, $event)"
          @dblclick="toggleKnowledgeWindowMaximize(cardWindow)"
        >
          <span class="knowledge-window-mark">K</span>
          <span class="knowledge-window-title">
            <small>Knowledge card</small>
            <strong>{{ cardWindow.card?.title || '正在加载知识卡片' }}</strong>
          </span>
          <span class="knowledge-window-actions" @pointerdown.stop>
            <button
              type="button"
              :aria-label="cardWindow.maximized ? '还原窗口' : '最大化窗口'"
              :title="cardWindow.maximized ? '还原窗口' : '最大化窗口'"
              @click="toggleKnowledgeWindowMaximize(cardWindow)"
            >
              <el-icon><FullScreen /></el-icon>
            </button>
            <button
              type="button"
              aria-label="关闭知识卡片"
              title="关闭"
              @click="closeKnowledgeWindow(cardWindow.key)"
            >
              <el-icon><Close /></el-icon>
            </button>
          </span>
        </header>

        <div
          class="knowledge-window-body"
          @click="handleArticleBodyClick"
          @mouseover="handleArticleLinkMouseOver"
          @mouseout="handleArticleLinkMouseOut"
          @focusin="handleArticleLinkFocus"
          @focusout="hideLinkPreviews"
        >
          <el-skeleton v-if="cardWindow.loading" :rows="8" animated />
          <div v-else-if="cardWindow.error" class="knowledge-window-error">
            <strong>知识卡片加载失败</strong>
            <el-button :icon="Refresh" @click="loadKnowledgeWindow(cardWindow)">重新加载</el-button>
          </div>
          <template v-else-if="cardWindow.card">
            <p class="knowledge-card-summary">{{ cardWindow.card.summary }}</p>
            <div
              class="markdown-body knowledge-window-markdown"
              v-html="renderMarkdown(cardWindow.card.content || '')"
            ></div>
            <footer class="knowledge-window-meta">
              <span>{{ cardWindow.card.createdByName || '知识卡片' }}</span>
              <span>更新于 {{ formatDate(cardWindow.card.updatedAt) }}</span>
            </footer>
          </template>
        </div>

        <button
          v-if="!cardWindow.maximized"
          type="button"
          class="knowledge-window-resizer"
          aria-label="拖拽调整知识卡片大小"
          title="拖拽调整大小"
          @pointerdown.stop="startKnowledgeWindowResize(cardWindow, $event)"
        ></button>
      </article>
    </teleport>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { ThumbsUp } from '@lucide/vue'
import {
  Back,
  Calendar,
  ChatDotRound,
  ChatLineRound,
  Close,
  Delete,
  EditPen,
  FullScreen,
  MoreFilled,
  Reading,
  Refresh,
  Star,
  Stopwatch,
} from '@element-plus/icons-vue'
import {
  createComment,
  createCommentReply,
  deleteArticle,
  deleteComment,
  favoriteArticle,
  getArticle,
  getArticleFavoriteStatus,
  getArticleLikeStatus,
  getKnowledgeCard,
  getCommentLikeStatus,
  likeArticle,
  likeComment,
  listComments,
  unfavoriteArticle,
  unlikeArticle,
  unlikeComment,
  updateComment,
} from '../api/blog'
import { sessionState, signedIn } from '../state/session'
import { canManageResource } from '../utils/permissions'
import { markdownToPlainText, renderMarkdown } from '../utils/markdown'

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
const articleLike = reactive({ count: 0, active: false, loading: false })
const articleFavorite = reactive({ count: 0, active: false, loading: false })
const commentLikes = reactive({})
const editingComment = reactive({ id: null, content: '', saving: false })
const replyingTo = reactive({ id: null, rootId: null, authorName: '', content: '', saving: false })
const articleBodyRef = ref(null)
const articlePreviewCache = new Map()
const knowledgeCardCache = new Map()
const knowledgeWindows = reactive([])
const articlePreview = reactive({
  visible: false,
  loading: false,
  article: null,
  left: 0,
  top: 0,
  requestId: 0,
})
const knowledgeCardPreview = reactive({
  visible: false,
  loading: false,
  card: null,
  left: 0,
  top: 0,
  requestId: 0,
})
let articlePreviewTimer = null
let knowledgeCardPreviewTimer = null
let knowledgeWindowSequence = 0
let knowledgeWindowZIndex = 3600
let activeKnowledgeWindowInteraction = null

const isSignedIn = signedIn
const currentUserInitial = computed(() => (sessionState.user?.username || 'U').trim().slice(0, 1).toUpperCase())
const wordCount = computed(() => (article.value?.content || '').trim().length)
const readingMinutes = computed(() => (wordCount.value ? Math.max(1, Math.ceil(wordCount.value / 500)) : 0))
const renderedArticle = computed(() => renderMarkdown(article.value?.content || ''))
const canManageArticle = computed(() => canManageResource(article.value))
const articleAuthorName = computed(() => {
  return article.value?.authorName || article.value?.username || article.value?.authorUsername || '留白手记'
})
const articleAuthorInitial = computed(() => articleAuthorName.value.trim().slice(0, 1).toUpperCase())
const articleWasUpdated = computed(() => {
  const createdAt = new Date(article.value?.createdAt || 0).getTime()
  const updatedAt = new Date(article.value?.updatedAt || 0).getTime()
  return Number.isFinite(createdAt) && Number.isFinite(updatedAt) && updatedAt - createdAt > 1000
})
const articleTimeLabel = computed(() => (articleWasUpdated.value ? '更新' : '发布'))
const articleTime = computed(() => {
  return articleWasUpdated.value ? article.value?.updatedAt : article.value?.createdAt || article.value?.updatedAt
})
const articlePreviewSummary = computed(() => {
  const content = articlePreview.article?.content || ''
  const summary = markdownToPlainText(content)
  return summary ? `${summary.slice(0, 150)}${summary.length > 150 ? '…' : ''}` : '暂无正文摘要'
})
const articlePreviewStyle = computed(() => ({
  left: `${articlePreview.left}px`,
  top: `${articlePreview.top}px`,
}))
const knowledgeCardPreviewSummary = computed(() => {
  const content = knowledgeCardPreview.card?.content || ''
  const summary = markdownToPlainText(content)
  return summary ? `${summary.slice(0, 130)}${summary.length > 130 ? '…' : ''}` : '暂无内容摘要'
})
const knowledgeCardPreviewStyle = computed(() => ({
  left: `${knowledgeCardPreview.left}px`,
  top: `${knowledgeCardPreview.top}px`,
}))
const commentThreads = computed(() => {
  const byId = new Map(comments.value.map((comment) => [Number(comment.id), comment]))
  const roots = []
  const repliesByRoot = new Map()

  for (const comment of comments.value) {
    const parentId = Number(comment.parentId)
    if (!Number.isFinite(parentId) || !byId.has(parentId)) {
      roots.push(comment)
      repliesByRoot.set(Number(comment.id), [])
    }
  }

  for (const comment of comments.value) {
    if (roots.includes(comment)) {
      continue
    }

    let cursor = comment
    const visited = new Set([Number(comment.id)])
    while (cursor?.parentId && byId.has(Number(cursor.parentId)) && !visited.has(Number(cursor.parentId))) {
      cursor = byId.get(Number(cursor.parentId))
      visited.add(Number(cursor.id))
    }

    const rootId = Number(cursor?.id)
    if (repliesByRoot.has(rootId)) {
      repliesByRoot.get(rootId).push(comment)
    } else {
      roots.push(comment)
      repliesByRoot.set(Number(comment.id), [])
    }
  }

  return roots.map((root) => ({ root, replies: repliesByRoot.get(Number(root.id)) || [] }))
})

function formatDate(value) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hour}:${minute}`
}

function formatCommentTime(comment) {
  const edited = comment.updatedAt && comment.updatedAt !== comment.createdAt
  return `${edited ? '编辑于' : '发布于'} ${formatDate(comment.updatedAt || comment.createdAt)}`
}

function commentAuthorName(comment) {
  return comment.authorName || comment.username || (comment.authorId ? `用户 ${comment.authorId}` : '匿名用户')
}

function commentAuthorInitial(comment) {
  return commentAuthorName(comment).trim().slice(0, 1).toUpperCase()
}

function requestLogin(message) {
  ElMessage.info(message)
  window.dispatchEvent(new CustomEvent('blog:open-auth'))
}

function findInternalLink(event, selector){
  const link = event.target.closest?.(selector)
  return link && event.currentTarget?.contains(link) ? link : null
}

function positionArticlePreview(link){
  const rect = link.getBoundingClientRect()
  const previewWidth = 340
  const previewHeight = 180
  articlePreview.left = Math.max(16, Math.min(rect.left, window.innerWidth - previewWidth - 16))
  articlePreview.top = rect.bottom + 10

  if(articlePreview.top + previewHeight > window.innerHeight){
    articlePreview.top = Math.max(16, rect.top - previewHeight - 10)
  }
}

async function showArticlePreview(link){
  window.clearTimeout(articlePreviewTimer)
  const articleId = Number(link.dataset.articleId)
  if(!Number.isFinite(articleId)){
    return
  }

  positionArticlePreview(link)
  articlePreview.visible = true
  articlePreview.article = articlePreviewCache.get(articleId) || null
  articlePreview.loading = !articlePreview.article

  if(articlePreview.article){
    return
  }

  const requestId = ++articlePreview.requestId
  try{
    const previewArticle = await getArticle(articleId)
    articlePreviewCache.set(articleId, previewArticle)
    if(requestId === articlePreview.requestId && articlePreview.visible){
      articlePreview.article = previewArticle
    }
  }catch{
    if(requestId === articlePreview.requestId){
      articlePreview.article = null
    }
  }finally{
    if(requestId === articlePreview.requestId){
      articlePreview.loading = false
    }
  }
}

function hideArticlePreview(){
  window.clearTimeout(articlePreviewTimer)
  articlePreviewTimer = window.setTimeout(() => {
    articlePreview.visible = false
    articlePreview.requestId += 1
  }, 120)
}

function dismissArticlePreview(){
  window.clearTimeout(articlePreviewTimer)
  articlePreview.visible = false
  articlePreview.requestId += 1
}

function positionKnowledgeCardPreview(link){
  const rect = link.getBoundingClientRect()
  const previewWidth = 320
  const previewHeight = 170
  knowledgeCardPreview.left = Math.max(16, Math.min(rect.left, window.innerWidth - previewWidth - 16))
  knowledgeCardPreview.top = rect.bottom + 10

  if(knowledgeCardPreview.top + previewHeight > window.innerHeight){
    knowledgeCardPreview.top = Math.max(16, rect.top - previewHeight - 10)
  }
}

async function showKnowledgeCardPreview(link){
  window.clearTimeout(knowledgeCardPreviewTimer)
  const cardId = Number(link.dataset.knowledgeCardId)
  if(!Number.isFinite(cardId)){
    return
  }

  positionKnowledgeCardPreview(link)
  knowledgeCardPreview.visible = true
  knowledgeCardPreview.card = knowledgeCardCache.get(cardId) || null
  knowledgeCardPreview.loading = !knowledgeCardPreview.card

  if(knowledgeCardPreview.card){
    return
  }

  const requestId = ++knowledgeCardPreview.requestId
  try{
    const card = await getKnowledgeCard(cardId)
    knowledgeCardCache.set(cardId, card)
    if(requestId === knowledgeCardPreview.requestId && knowledgeCardPreview.visible){
      knowledgeCardPreview.card = card
    }
  }catch{
    if(requestId === knowledgeCardPreview.requestId){
      knowledgeCardPreview.card = null
    }
  }finally{
    if(requestId === knowledgeCardPreview.requestId){
      knowledgeCardPreview.loading = false
    }
  }
}

function hideKnowledgeCardPreview(){
  window.clearTimeout(knowledgeCardPreviewTimer)
  knowledgeCardPreviewTimer = window.setTimeout(() => {
    knowledgeCardPreview.visible = false
    knowledgeCardPreview.requestId += 1
  }, 120)
}

function dismissKnowledgeCardPreview(){
  window.clearTimeout(knowledgeCardPreviewTimer)
  knowledgeCardPreview.visible = false
  knowledgeCardPreview.requestId += 1
}

function hideLinkPreviews(){
  hideArticlePreview()
  hideKnowledgeCardPreview()
}

function dismissLinkPreviews(){
  dismissArticlePreview()
  dismissKnowledgeCardPreview()
}

function handleArticleLinkMouseOver(event){
  const link = findInternalLink(event, '.internal-article-link')
  if(link && !link.contains(event.relatedTarget)){
    showArticlePreview(link)
    return
  }

  const cardLink = findInternalLink(event, '.knowledge-card-link')
  if(cardLink && !cardLink.contains(event.relatedTarget)){
    showKnowledgeCardPreview(cardLink)
  }
}

function handleArticleLinkMouseOut(event){
  const link = findInternalLink(event, '.internal-article-link')
  if(link && !link.contains(event.relatedTarget)){
    hideArticlePreview()
    return
  }

  const cardLink = findInternalLink(event, '.knowledge-card-link')
  if(cardLink && !cardLink.contains(event.relatedTarget)){
    hideKnowledgeCardPreview()
  }
}

function handleArticleLinkFocus(event){
  const link = findInternalLink(event, '.internal-article-link')
  if(link){
    showArticlePreview(link)
    return
  }

  const cardLink = findInternalLink(event, '.knowledge-card-link')
  if(cardLink){
    showKnowledgeCardPreview(cardLink)
  }
}

function constrain(value, min, max){
  return Math.min(Math.max(value, min), Math.max(min, max))
}

function clampKnowledgeWindow(cardWindow){
  const margin = 8
  const minWidth = Math.max(240, Math.min(320, window.innerWidth - margin * 2))
  const minHeight = Math.max(200, Math.min(260, window.innerHeight - margin * 2))

  if(cardWindow.maximized){
    cardWindow.x = 12
    cardWindow.y = 12
    cardWindow.width = Math.max(minWidth, window.innerWidth - 24)
    cardWindow.height = Math.max(minHeight, window.innerHeight - 24)
    return
  }

  cardWindow.width = constrain(cardWindow.width, minWidth, window.innerWidth - margin * 2)
  cardWindow.height = constrain(cardWindow.height, minHeight, window.innerHeight - margin * 2)
  cardWindow.x = constrain(cardWindow.x, margin, window.innerWidth - cardWindow.width - margin)
  cardWindow.y = constrain(cardWindow.y, margin, window.innerHeight - cardWindow.height - margin)
}

function createKnowledgeWindow(cardId, anchorRect){
  const width = Math.min(480, window.innerWidth - 24)
  const height = Math.min(590, window.innerHeight - 32)
  const offset = (knowledgeWindows.length % 5) * 24
  let x = window.innerWidth - width - 28 - offset
  let y = 48 + offset

  if(anchorRect){
    if(window.innerWidth - anchorRect.right >= width + 20){
      x = anchorRect.right + 12
    }else if(anchorRect.left >= width + 20){
      x = anchorRect.left - width - 12
    }
    y = anchorRect.top - 24 + offset
  }

  const cardWindow = reactive({
    key: ++knowledgeWindowSequence,
    cardId,
    card: knowledgeCardCache.get(cardId) || null,
    loading: !knowledgeCardCache.has(cardId),
    error: '',
    x,
    y,
    width,
    height,
    zIndex: ++knowledgeWindowZIndex,
    maximized: false,
    restoreBounds: null,
  })
  clampKnowledgeWindow(cardWindow)
  return cardWindow
}

function bringKnowledgeWindowToFront(cardWindow){
  cardWindow.zIndex = ++knowledgeWindowZIndex
}

function knowledgeWindowStyle(cardWindow){
  return {
    left: `${cardWindow.x}px`,
    top: `${cardWindow.y}px`,
    width: `${cardWindow.width}px`,
    height: `${cardWindow.height}px`,
    zIndex: cardWindow.zIndex,
  }
}

async function loadKnowledgeWindow(cardWindow){
  cardWindow.loading = true
  cardWindow.error = ''
  try{
    const card = await getKnowledgeCard(cardWindow.cardId)
    knowledgeCardCache.set(cardWindow.cardId, card)
    if(knowledgeWindows.includes(cardWindow)){
      cardWindow.card = card
    }
  }catch{
    if(knowledgeWindows.includes(cardWindow)){
      cardWindow.error = '知识卡片不存在或加载失败'
    }
  }finally{
    if(knowledgeWindows.includes(cardWindow)){
      cardWindow.loading = false
    }
  }
}

function openKnowledgeCard(cardId, anchorRect){
  dismissLinkPreviews()
  const normalizedCardId = Number(cardId)
  if(!Number.isFinite(normalizedCardId)){
    return
  }

  const existingWindow = knowledgeWindows.find((item) => item.cardId === normalizedCardId)
  if(existingWindow){
    bringKnowledgeWindowToFront(existingWindow)
    return
  }

  const cardWindow = createKnowledgeWindow(normalizedCardId, anchorRect)
  knowledgeWindows.push(cardWindow)
  if(!cardWindow.card){
    loadKnowledgeWindow(cardWindow)
  }
}

function closeKnowledgeWindow(key){
  const index = knowledgeWindows.findIndex((item) => item.key === key)
  if(index >= 0){
    knowledgeWindows.splice(index, 1)
  }
}

function toggleKnowledgeWindowMaximize(cardWindow){
  bringKnowledgeWindowToFront(cardWindow)
  if(cardWindow.maximized){
    cardWindow.maximized = false
    Object.assign(cardWindow, cardWindow.restoreBounds)
    cardWindow.restoreBounds = null
    clampKnowledgeWindow(cardWindow)
    return
  }

  cardWindow.restoreBounds = {
    x: cardWindow.x,
    y: cardWindow.y,
    width: cardWindow.width,
    height: cardWindow.height,
  }
  cardWindow.maximized = true
  clampKnowledgeWindow(cardWindow)
}

function beginKnowledgeWindowInteraction(cardWindow, type, event){
  if(event.button !== 0 || (type === 'move' && cardWindow.maximized)){
    return
  }

  event.preventDefault()
  bringKnowledgeWindowToFront(cardWindow)
  activeKnowledgeWindowInteraction = {
    cardWindow,
    type,
    startX: event.clientX,
    startY: event.clientY,
    x: cardWindow.x,
    y: cardWindow.y,
    width: cardWindow.width,
    height: cardWindow.height,
  }
  document.body.classList.add('knowledge-window-interacting')
  window.addEventListener('pointermove', handleKnowledgeWindowPointerMove)
  window.addEventListener('pointerup', stopKnowledgeWindowInteraction, { once: true })
  window.addEventListener('pointercancel', stopKnowledgeWindowInteraction, { once: true })
}

function startKnowledgeWindowDrag(cardWindow, event){
  if(event.target.closest('button')){
    return
  }
  beginKnowledgeWindowInteraction(cardWindow, 'move', event)
}

function startKnowledgeWindowResize(cardWindow, event){
  beginKnowledgeWindowInteraction(cardWindow, 'resize', event)
}

function handleKnowledgeWindowPointerMove(event){
  const interaction = activeKnowledgeWindowInteraction
  if(!interaction){
    return
  }

  const deltaX = event.clientX - interaction.startX
  const deltaY = event.clientY - interaction.startY
  if(interaction.type === 'move'){
    interaction.cardWindow.x = interaction.x + deltaX
    interaction.cardWindow.y = interaction.y + deltaY
  }else{
    interaction.cardWindow.width = interaction.width + deltaX
    interaction.cardWindow.height = interaction.height + deltaY
  }
  clampKnowledgeWindow(interaction.cardWindow)
}

function stopKnowledgeWindowInteraction(){
  activeKnowledgeWindowInteraction = null
  document.body.classList.remove('knowledge-window-interacting')
  window.removeEventListener('pointermove', handleKnowledgeWindowPointerMove)
  window.removeEventListener('pointerup', stopKnowledgeWindowInteraction)
  window.removeEventListener('pointercancel', stopKnowledgeWindowInteraction)
}

function handleKnowledgeViewportResize(){
  knowledgeWindows.forEach(clampKnowledgeWindow)
}

function handleArticleBodyClick(event){
  const articleLink = findInternalLink(event, '.internal-article-link')
  if(articleLink){
    event.preventDefault()
    dismissLinkPreviews()
    router.push(`/articles/${articleLink.dataset.articleId}`)
    return
  }

  const cardLink = findInternalLink(event, '.knowledge-card-link')
  if(cardLink){
    event.preventDefault()
    openKnowledgeCard(cardLink.dataset.knowledgeCardId, cardLink.getBoundingClientRect())
  }
}

function applyInteraction(target, response) {
  target.count = Number(response?.count) || 0
  target.active = Boolean(response?.active)
}

async function loadArticleInteractions() {
  const [likeResult, favoriteResult] = await Promise.allSettled([
    getArticleLikeStatus(props.id),
    getArticleFavoriteStatus(props.id),
  ])
  if (likeResult.status === 'fulfilled') applyInteraction(articleLike, likeResult.value)
  if (favoriteResult.status === 'fulfilled') applyInteraction(articleFavorite, favoriteResult.value)
}

async function loadArticle() {
  loading.value = true
  error.value = ''
  try {
    article.value = await getArticle(props.id)
    await Promise.all([loadComments(), loadArticleInteractions()])
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
    for (const comment of comments.value) {
      commentLikes[comment.id] = {
        count: Number(comment.likeCount) || 0,
        active: false,
        loading: false,
      }
    }
    if (isSignedIn.value) await loadCommentLikeStatuses()
  } catch {
    commentsError.value = '评论加载失败'
  } finally {
    commentsLoading.value = false
  }
}

async function loadCommentLikeStatuses() {
  const results = await Promise.allSettled(
    comments.value.map((comment) => getCommentLikeStatus(props.id, comment.id)),
  )
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') {
      const comment = comments.value[index]
      commentLikes[comment.id] = {
        count: Number(result.value?.count) || 0,
        active: Boolean(result.value?.active),
        loading: false,
      }
    }
  })
}

function commentLikeState(comment) {
  return commentLikes[comment.id] || { count: Number(comment.likeCount) || 0, active: false, loading: false }
}

async function toggleArticleLike() {
  if (!isSignedIn.value) return requestLogin('登录后即可喜欢文章')
  articleLike.loading = true
  try {
    applyInteraction(articleLike, await (articleLike.active ? unlikeArticle(props.id) : likeArticle(props.id)))
  } finally {
    articleLike.loading = false
  }
}

async function toggleArticleFavorite() {
  if (!isSignedIn.value) return requestLogin('登录后即可收藏文章')
  articleFavorite.loading = true
  try {
    applyInteraction(
      articleFavorite,
      await (articleFavorite.active ? unfavoriteArticle(props.id) : favoriteArticle(props.id)),
    )
  } finally {
    articleFavorite.loading = false
  }
}

async function toggleCommentLike(comment) {
  if (!isSignedIn.value) return requestLogin('登录后即可喜欢评论')
  const state = commentLikeState(comment)
  state.loading = true
  try {
    commentLikes[comment.id] = {
      ...state,
      ...(await (state.active ? unlikeComment(props.id, comment.id) : likeComment(props.id, comment.id))),
      loading: false,
    }
  } finally {
    if (commentLikes[comment.id]) commentLikes[comment.id].loading = false
  }
}

async function submitComment() {
  if (!isSignedIn.value) return requestLogin('登录后即可参与评论')
  const content = commentContent.value.trim()
  if (!content) return ElMessage.warning('请输入评论内容')
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

function startReply(comment, rootId) {
  if (!isSignedIn.value) return requestLogin('登录后即可回复评论')
  replyingTo.id = comment.id
  replyingTo.rootId = rootId
  replyingTo.authorName = commentAuthorName(comment)
  replyingTo.content = ''
}

function cancelReply() {
  replyingTo.id = null
  replyingTo.rootId = null
  replyingTo.authorName = ''
  replyingTo.content = ''
}

async function submitReply() {
  const content = replyingTo.content.trim()
  if (!content) return ElMessage.warning('请输入回复内容')
  replyingTo.saving = true
  try {
    await createCommentReply(props.id, replyingTo.id, { content })
    cancelReply()
    await loadComments()
    ElMessage.success('回复已发布')
  } finally {
    replyingTo.saving = false
  }
}

async function removeArticle() {
  if (!canManageArticle.value) return
  try {
    await ElMessageBox.confirm(`确定删除文章「${article.value.title}」？`, '删除文章', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger',
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

function handleArticleCommand(command) {
  if (command === 'edit') router.push(`/articles/${article.value.id}/edit`)
  if (command === 'delete') removeArticle()
}

function startCommentEdit(comment) {
  if (!canManageResource(comment)) return
  editingComment.id = comment.id
  editingComment.content = comment.content
}

function cancelCommentEdit() {
  editingComment.id = null
  editingComment.content = ''
}

function handleCommentCommand(comment, command) {
  if (command === 'edit') startCommentEdit(comment)
  if (command === 'delete') removeComment(comment)
}

async function saveCommentEdit(comment) {
  if (!canManageResource(comment)) return cancelCommentEdit()
  const content = editingComment.content.trim()
  if (!content) return ElMessage.warning('请输入评论内容')
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
  if (!canManageResource(comment)) return
  try {
    await ElMessageBox.confirm(`确定删除这条评论？\n${comment.content.slice(0, 32)}`, '删除评论', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger',
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

watch(
  () => [sessionState.token, sessionState.checking],
  ([token, checking]) => {
    if (!checking && article.value) {
      loadArticleInteractions()
      if (token) loadCommentLikeStatuses()
      else Object.values(commentLikes).forEach((state) => { state.active = false })
    }
  },
)

watch(
  () => props.id,
  () => {
    knowledgeWindows.splice(0)
    dismissLinkPreviews()
    loadArticle()
  },
  { immediate: true },
)

onMounted(() => {
  window.addEventListener('resize', handleKnowledgeViewportResize)
})

onBeforeUnmount(() => {
  window.clearTimeout(articlePreviewTimer)
  window.clearTimeout(knowledgeCardPreviewTimer)
  window.removeEventListener('resize', handleKnowledgeViewportResize)
  stopKnowledgeWindowInteraction()
})
</script>
