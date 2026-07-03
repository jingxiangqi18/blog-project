<template>
  <section class="page-stack user-page">
    <div class="hero-panel user-hero">
      <div>
        <p class="section-kicker">Users</p>
        <h2>用户管理</h2>
      </div>
      <div class="hero-side">
        <div class="metric-card">
          <span>{{ users.length }}</span>
          <small>用户数量</small>
        </div>
        <div class="metric-card">
          <span>{{ adminCount }}</span>
          <small>管理员</small>
        </div>
        <div class="metric-card">
          <span>{{ disabledCount }}</span>
          <small>已禁用</small>
        </div>
      </div>
    </div>

    <div v-if="!canViewUsers" class="panel state-panel error-panel">
      <el-alert type="warning" title="只有管理员可以查看用户列表" show-icon :closable="false" />
      <el-button :icon="Back" @click="$router.push('/articles')">返回文章</el-button>
    </div>

    <section v-else class="panel user-panel">
      <div class="user-panel-head">
        <div>
          <p class="section-kicker">Accounts</p>
          <h3>账号状态</h3>
        </div>
        <div class="user-toolbar">
          <el-input
            v-model="keyword"
            class="user-search"
            :prefix-icon="Search"
            placeholder="搜索用户名或 ID"
            clearable
          />
          <el-segmented v-model="statusFilter" :options="statusOptions" />
          <el-button :icon="Refresh" :loading="loading" @click="loadUsers">刷新</el-button>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-else-if="error" class="state-panel compact-state">
        <el-alert type="error" :title="error" show-icon :closable="false" />
        <el-button :icon="Refresh" @click="loadUsers">重试</el-button>
      </div>
      <el-empty v-else-if="filteredUsers.length === 0" description="暂无匹配用户" />

      <div v-else class="user-table">
        <div class="user-table-row user-table-head">
          <span>用户</span>
          <span>角色</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <article
          v-for="user in filteredUsers"
          :key="user.id"
          class="user-table-row"
          :class="{ disabled: !user.enabled }"
        >
          <div class="user-cell">
            <span class="user-avatar large">{{ userInitial(user.username) }}</span>
            <div class="user-row-main">
              <strong>{{ user.username }}</strong>
              <span>ID {{ user.id }}</span>
            </div>
          </div>
          <span class="role-chip" :class="{ admin: user.role === 'ADMIN' }">
            {{ roleText(user.role) }}
          </span>
          <span class="status-chip" :class="{ disabled: !user.enabled }">
            {{ user.enabled ? '已启用' : '已禁用' }}
          </span>
          <div class="user-actions">
            <el-switch
              :model-value="user.enabled"
              :disabled="isSelf(user) || updatingUserId === user.id"
              :loading="updatingUserId === user.id"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="toggleUserEnabled(user, $event)"
            />
            <span v-if="isSelf(user)" class="self-lock">当前账号</span>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Refresh, Search } from '@element-plus/icons-vue'
import { listUsers, updateUserEnabled } from '../api/blog'
import { currentUserId, isAdmin } from '../utils/permissions'

const users = ref([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const statusFilter = ref('all')
const updatingUserId = ref(null)
const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '启用', value: 'enabled' },
  { label: '禁用', value: 'disabled' },
]

const canViewUsers = computed(() => isAdmin())
const adminCount = computed(() => users.value.filter((user) => user.role === 'ADMIN').length)
const disabledCount = computed(() => users.value.filter((user) => !user.enabled).length)
const filteredUsers = computed(() => {
  const key = keyword.value.trim().toLowerCase()

  return users.value.filter((user) => {
    const matchesKeyword =
      !key ||
      String(user.id).includes(key) ||
      String(user.username || '').toLowerCase().includes(key)
    const matchesStatus =
      statusFilter.value === 'all' ||
      (statusFilter.value === 'enabled' && user.enabled) ||
      (statusFilter.value === 'disabled' && !user.enabled)

    return matchesKeyword && matchesStatus
  })
})

function userInitial(username) {
  return (username || 'U').trim().slice(0, 1).toUpperCase()
}

function roleText(role) {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

function isSelf(user) {
  return Number(user.id) === currentUserId()
}

async function loadUsers() {
  if (!canViewUsers.value) {
    users.value = []
    return
  }

  loading.value = true
  error.value = ''

  try {
    users.value = await listUsers()
  } catch {
    error.value = '用户列表加载失败，请确认当前账号是管理员'
  } finally {
    loading.value = false
  }
}

async function toggleUserEnabled(user, enabled) {
  if (isSelf(user)) {
    ElMessage.warning('不能禁用当前登录账号')
    return
  }

  const actionText = enabled ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定${actionText}用户「${user.username}」？`, `${actionText}用户`, {
      type: enabled ? 'info' : 'warning',
      confirmButtonText: actionText,
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  updatingUserId.value = user.id
  try {
    const updated = await updateUserEnabled(user.id, { enabled })
    users.value = users.value.map((item) => (item.id === updated.id ? updated : item))
    ElMessage.success(`用户已${actionText}`)
  } finally {
    updatingUserId.value = null
  }
}

onMounted(loadUsers)

watch(canViewUsers, (value) => {
  if (value && users.value.length === 0) {
    loadUsers()
  }
})
</script>
