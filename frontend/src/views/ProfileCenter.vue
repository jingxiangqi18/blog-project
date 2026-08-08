<template>
  <section class="page-stack profile-page">
    <div class="profile-hero">
      <div class="profile-identity">
        <span class="profile-avatar">{{ userInitial }}</span>
        <div>
          <p class="section-kicker">Personal Space</p>
          <h2>{{ sessionState.user?.username || '我的留白' }}</h2>
          <span>{{ profileSubtitle }}</span>
        </div>
      </div>
      <div class="profile-metrics" aria-label="个人数据">
        <span><strong>{{ articles.length }}</strong><small>篇创作</small></span>
        <span><strong>{{ favorites.length }}</strong><small>个收藏</small></span>
        <span><strong>{{ comments.length }}</strong><small>条评论</small></span>
      </div>
    </div>

    <div v-if="sessionState.checking" class="panel profile-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="!isSignedIn" class="panel profile-auth-prompt">
      <span><el-icon><Lock /></el-icon></span>
      <h3>登录后查看个人空间</h3>
      <p>你的文章、收藏、评论与账户设置会集中显示在这里。</p>
      <el-button type="primary" :icon="Lock" @click="requestLogin">登录</el-button>
    </div>

    <template v-else>
      <nav class="profile-tabs" aria-label="个人中心导航">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
          <strong v-if="tab.count !== null">{{ tab.count }}</strong>
        </button>
      </nav>

      <div v-if="loading" class="panel profile-loading">
        <el-skeleton :rows="7" animated />
      </div>
      <div v-else-if="error" class="panel state-panel error-panel">
        <el-alert type="error" :title="error" show-icon :closable="false" />
        <el-button :icon="Refresh" @click="loadProfile">重试</el-button>
      </div>

      <section v-else-if="activeTab === 'articles'" class="panel profile-content-panel">
        <div class="profile-section-heading">
          <div><p class="section-kicker">Writing</p><h3>我的文章</h3></div>
          <el-button type="primary" :icon="EditPen" @click="$router.push('/articles/new')">写新文章</el-button>
        </div>
        <el-empty v-if="articles.length === 0" description="还没有发布文章" />
        <div v-else class="profile-record-list">
          <article v-for="article in articles" :key="article.id" class="profile-record">
            <button class="profile-record-main" type="button" @click="$router.push(`/articles/${article.id}`)">
              <span class="profile-record-eyebrow">{{ article.categoryName || '未分类' }} · {{ formatDate(article.updatedAt) }}</span>
              <strong>{{ article.title }}</strong>
              <p>{{ excerpt(article.content) }}</p>
            </button>
            <div class="profile-record-actions">
              <el-button circle :icon="EditPen" aria-label="编辑文章" title="编辑文章" @click="$router.push(`/articles/${article.id}/edit`)" />
              <el-button circle :icon="ArrowRight" aria-label="阅读文章" title="阅读文章" @click="$router.push(`/articles/${article.id}`)" />
            </div>
          </article>
        </div>
      </section>

      <section v-else-if="activeTab === 'favorites'" class="panel profile-content-panel">
        <div class="profile-section-heading">
          <div><p class="section-kicker">Saved</p><h3>我的收藏</h3></div>
          <span class="profile-section-note">稍后继续阅读</span>
        </div>
        <el-empty v-if="favorites.length === 0" description="还没有收藏文章" />
        <div v-else class="profile-record-list">
          <article v-for="article in favorites" :key="article.id" class="profile-record">
            <button class="profile-record-main" type="button" @click="$router.push(`/articles/${article.id}`)">
              <span class="profile-record-eyebrow">{{ article.authorName || '留白手记' }} · {{ article.categoryName || '未分类' }}</span>
              <strong>{{ article.title }}</strong>
              <p>{{ excerpt(article.content) }}</p>
            </button>
            <div class="profile-record-actions">
              <el-button
                circle
                :icon="Delete"
                aria-label="取消收藏"
                title="取消收藏"
                :loading="removingFavoriteId === article.id"
                @click="removeFavorite(article)"
              />
              <el-button circle :icon="ArrowRight" aria-label="阅读文章" title="阅读文章" @click="$router.push(`/articles/${article.id}`)" />
            </div>
          </article>
        </div>
      </section>

      <section v-else-if="activeTab === 'comments'" class="panel profile-content-panel">
        <div class="profile-section-heading">
          <div><p class="section-kicker">Footprints</p><h3>我的评论</h3></div>
          <span class="profile-section-note">讨论足迹</span>
        </div>
        <el-empty v-if="comments.length === 0" description="还没有参与评论" />
        <div v-else class="profile-comment-list">
          <button
            v-for="comment in comments"
            :key="comment.id"
            class="profile-comment-record"
            type="button"
            @click="$router.push(`/articles/${comment.articleId}`)"
          >
            <span class="profile-comment-mark"><el-icon><ChatLineRound /></el-icon></span>
            <span>
              <small>文章 #{{ comment.articleId }} · {{ formatDate(comment.updatedAt || comment.createdAt) }}</small>
              <strong>{{ comment.content }}</strong>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </section>

      <section v-else class="panel profile-content-panel security-panel">
        <div class="profile-section-heading">
          <div><p class="section-kicker">Security</p><h3>账户安全</h3></div>
          <span class="profile-section-note">定期更新密码</span>
        </div>
        <div class="security-layout">
          <div class="security-copy">
            <span><el-icon><Lock /></el-icon></span>
            <h4>修改登录密码</h4>
            <p>新密码需要包含 6 到 72 个字符。修改成功后，请在下次登录时使用新密码。</p>
          </div>
          <el-form
            ref="passwordFormRef"
            class="password-form"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            @submit.prevent
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-button type="primary" :icon="Lock" :loading="changingPassword" @click="submitPasswordChange">
              更新密码
            </el-button>
          </el-form>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, markRaw, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, ChatLineRound, Delete, Document, EditPen, Lock, Refresh, Star } from '@element-plus/icons-vue'
import {
  changeMyPassword,
  listMyArticles,
  listMyComments,
  listMyFavoriteArticles,
  unfavoriteArticle,
} from '../api/blog'
import { sessionState, signedIn } from '../state/session'
import { markdownToPlainText } from '../utils/markdown'

const isSignedIn = signedIn
const activeTab = ref('articles')
const articles = ref([])
const favorites = ref([])
const comments = ref([])
const loading = ref(false)
const error = ref('')
const loadedForUserId = ref(null)
const removingFavoriteId = ref(null)
const changingPassword = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const userInitial = computed(() => (sessionState.user?.username || 'U').trim().slice(0, 1).toUpperCase())
const roleLabel = computed(() => (sessionState.user?.role === 'ADMIN' ? '管理员' : '普通用户'))
const profileSubtitle = computed(() => {
  return isSignedIn.value ? `${roleLabel.value} · 管理你的创作与收藏` : '访客 · 登录后开启个人空间'
})
const tabs = computed(() => [
  { label: '我的文章', value: 'articles', icon: markRaw(Document), count: articles.value.length },
  { label: '我的收藏', value: 'favorites', icon: markRaw(Star), count: favorites.value.length },
  { label: '我的评论', value: 'comments', icon: markRaw(ChatLineRound), count: comments.value.length },
  { label: '账户安全', value: 'security', icon: markRaw(Lock), count: null },
])

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度需要在 6 到 72 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validatePasswordConfirmation, trigger: 'blur' },
  ],
}

function validatePasswordConfirmation(_rule, value, callback) {
  if (value !== passwordForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

function requestLogin() {
  window.dispatchEvent(new CustomEvent('blog:open-auth'))
}

function formatDate(value) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(date)
}

function excerpt(content) {
  const plain = markdownToPlainText(content)
  return plain.length > 100 ? `${plain.slice(0, 100)}...` : plain || '暂无正文摘要'
}

async function loadProfile() {
  if (!isSignedIn.value) return
  loading.value = true
  error.value = ''
  try {
    const [myArticles, myFavorites, myComments] = await Promise.all([
      listMyArticles(),
      listMyFavoriteArticles(),
      listMyComments(),
    ])
    articles.value = myArticles
    favorites.value = myFavorites
    comments.value = myComments
    loadedForUserId.value = sessionState.user?.id
  } catch {
    error.value = '个人数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function removeFavorite(article) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${article.title}」？`, '取消收藏', {
      confirmButtonText: '取消收藏',
      cancelButtonText: '保留',
    })
  } catch {
    return
  }

  removingFavoriteId.value = article.id
  try {
    await unfavoriteArticle(article.id)
    favorites.value = favorites.value.filter((item) => item.id !== article.id)
    ElMessage.success('已取消收藏')
  } finally {
    removingFavoriteId.value = null
  }
}

async function submitPasswordChange() {
  await passwordFormRef.value.validate()
  changingPassword.value = true
  try {
    await changeMyPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
    ElMessage.success('密码已更新')
  } finally {
    changingPassword.value = false
  }
}

watch(
  () => [sessionState.checking, sessionState.user?.id],
  ([checking, userId]) => {
    if (!checking && userId && loadedForUserId.value !== userId) loadProfile()
    if (!userId) loadedForUserId.value = null
  },
  { immediate: true },
)
</script>
