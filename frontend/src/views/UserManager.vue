<template>
  <section class="page-stack">
    <div class="hero-panel">
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
      </div>
    </div>

    <div v-if="!canViewUsers" class="panel state-panel error-panel">
      <el-alert type="warning" title="只有管理员可以查看用户列表" show-icon :closable="false" />
      <el-button :icon="Back" @click="$router.push('/articles')">返回文章</el-button>
    </div>

    <section v-else class="panel user-panel">
      <div class="panel-title-row">
        <div>
          <p class="section-kicker">Accounts</p>
          <h3>账号状态</h3>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadUsers">刷新</el-button>
      </div>

      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-else-if="error" class="state-panel compact-state">
        <el-alert type="error" :title="error" show-icon :closable="false" />
        <el-button :icon="Refresh" @click="loadUsers">重试</el-button>
      </div>
      <el-empty v-else-if="users.length === 0" description="暂无用户" />

      <div v-else class="user-list">
        <article v-for="user in users" :key="user.id" class="user-row">
          <span class="user-avatar large">{{ userInitial(user.username) }}</span>
          <div class="user-row-main">
            <strong>{{ user.username }}</strong>
            <span>ID {{ user.id }}</span>
          </div>
          <span class="role-chip" :class="{ admin: user.role === 'ADMIN' }">
            {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </span>
          <span class="status-chip" :class="{ disabled: !user.enabled }">
            {{ user.enabled ? '已启用' : '已禁用' }}
          </span>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Back, Refresh } from '@element-plus/icons-vue'
import { listUsers } from '../api/blog'
import { isAdmin } from '../utils/permissions'

const users = ref([])
const loading = ref(false)
const error = ref('')

const canViewUsers = computed(() => isAdmin())
const adminCount = computed(() => users.value.filter((user) => user.role === 'ADMIN').length)

function userInitial(username) {
  return (username || 'U').trim().slice(0, 1).toUpperCase()
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

onMounted(loadUsers)

watch(canViewUsers, (value) => {
  if (value && users.value.length === 0) {
    loadUsers()
  }
})
</script>
