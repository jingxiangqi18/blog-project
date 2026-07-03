<template>
  <el-config-provider namespace="el">
    <div class="app-shell">
      <aside class="app-sidebar">
        <RouterLink class="brand" to="/articles">
          <span class="brand-mark">
            <span>B</span>
          </span>
          <span class="brand-copy">
            <strong>Blog Studio</strong>
            <small>内容管理台</small>
          </span>
        </RouterLink>

        <nav class="nav-list" aria-label="主导航">
          <RouterLink to="/articles" class="nav-item">
            <el-icon><Document /></el-icon>
            <span>文章</span>
          </RouterLink>
          <RouterLink to="/categories" class="nav-item">
            <el-icon><CollectionTag /></el-icon>
            <span>分类</span>
          </RouterLink>
          <RouterLink v-if="isAdminUser" to="/users" class="nav-item">
            <el-icon><User /></el-icon>
            <span>用户</span>
          </RouterLink>
        </nav>

        <div v-if="isSignedIn" class="sidebar-user-card">
          <span class="user-avatar large">{{ userInitial }}</span>
          <span>
            <strong>{{ sessionState.user?.username }}</strong>
            <small>{{ roleLabel }}</small>
          </span>
        </div>

        <div class="sidebar-footer">
          <span class="status-dot" :class="{ online: isSignedIn }" aria-hidden="true"></span>
          <span>{{ isSignedIn ? '已登录工作台' : 'Local Studio' }}</span>
        </div>
      </aside>

      <div class="workspace">
        <header class="app-header">
          <div class="topbar-copy">
            <span>Dashboard</span>
            <strong>创作与归档</strong>
          </div>

          <div class="topbar-actions">
            <span v-if="sessionState.checking" class="auth-checking">校验登录中</span>
            <el-dropdown v-else-if="isSignedIn" trigger="click" placement="bottom-end" @command="handleUserCommand">
              <button class="user-pill" type="button">
                <span class="user-avatar">{{ userInitial }}</span>
                <span>
                  <strong>{{ sessionState.user?.username }}</strong>
                  <small>{{ roleLabel }}</small>
                </span>
                <el-icon class="user-pill-arrow"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>
                    <span class="dropdown-identity">{{ sessionState.user?.username }} · {{ roleLabel }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-if="!isSignedIn" :icon="Lock" @click="loginVisible = true">登录</el-button>

            <el-button
              v-if="isSignedIn"
              class="header-action"
              type="primary"
              :icon="EditPen"
              aria-label="写文章"
              title="写文章"
              @click="$router.push('/articles/new')"
            >
              写文章
            </el-button>
          </div>
        </header>

        <main class="app-main">
          <RouterView />
        </main>
      </div>

      <el-dialog v-model="loginVisible" :title="authDialogTitle" width="420px" class="login-dialog">
        <el-segmented v-model="authMode" class="auth-mode-switch" :options="authModeOptions" />
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="loginForm.username" :prefix-icon="User" autocomplete="username" placeholder="输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              :prefix-icon="Lock"
              autocomplete="current-password"
              placeholder="输入密码"
              show-password
              type="password"
              @keyup.enter="submitAuth"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="loginVisible = false">取消</el-button>
          <el-button type="primary" :icon="Lock" :loading="loggingIn" @click="submitAuth">
            {{ authMode === 'login' ? '登录' : '注册并登录' }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, CollectionTag, Document, EditPen, Lock, SwitchButton, User } from '@element-plus/icons-vue'
import { getCurrentUser, login, register } from './api/blog'
import { clearSession, finishSessionCheck, sessionState, setSession, signedIn } from './state/session'

const loginVisible = ref(false)
const loggingIn = ref(false)
const authMode = ref('login')
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: '',
})
const authModeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' },
]

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' },
  ],
}

const isSignedIn = signedIn
const authDialogTitle = computed(() => (authMode.value === 'login' ? '登录 Blog Studio' : '注册 Blog Studio'))
const isAdminUser = computed(() => sessionState.user?.role === 'ADMIN' && !sessionState.checking)
const userInitial = computed(() => {
  return (sessionState.user?.username || 'U').trim().slice(0, 1).toUpperCase()
})
const roleLabel = computed(() => {
  return sessionState.user?.role === 'ADMIN' ? '管理员' : '普通用户'
})

function validateUsername(_rule, value, callback) {
  if (authMode.value === 'register' && (value.length < 3 || value.length > 30)) {
    callback(new Error('用户名长度需要在 3 到 30 个字符之间'))
    return
  }

  callback()
}

function validatePassword(_rule, value, callback) {
  if (authMode.value === 'register' && (value.length < 5 || value.length > 72)) {
    callback(new Error('密码长度需要在 5 到 72 个字符之间'))
    return
  }

  callback()
}

async function submitAuth() {
  await loginFormRef.value.validate()

  loggingIn.value = true
  try {
    const payload = {
      username: loginForm.username.trim(),
      password: loginForm.password,
    }
    const response = authMode.value === 'login' ? await login(payload) : await register(payload)
    setSession(response)
    loginForm.password = ''
    loginVisible.value = false
    ElMessage.success(authMode.value === 'login' ? '登录成功' : '注册成功')
  } finally {
    loggingIn.value = false
  }
}

function logout() {
  clearSession()
  ElMessage.success('已退出登录')
}

function handleUserCommand(command) {
  if (command === 'logout') {
    logout()
  }
}

async function checkCurrentSession() {
  if (!sessionState.token) {
    finishSessionCheck()
    return
  }

  try {
    const user = await getCurrentUser()
    setSession({
      token: sessionState.token,
      id: user.id,
      username: user.username,
      role: user.role,
    })
  } catch {
    clearSession()
  }
}

onMounted(checkCurrentSession)
</script>
