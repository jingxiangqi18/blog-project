<template>
  <el-config-provider namespace="el">
    <SiteShell>
      <template #actions>
          <div class="topbar-actions">
            <span v-if="sessionState.checking" class="auth-checking">校验登录中</span>
            <template v-else-if="isSignedIn">
              <el-button
                class="header-action"
                type="primary"
                :icon="EditPen"
                aria-label="写文章"
                title="写文章"
                @click="$router.push('/articles/new')"
              >
                写文章
              </el-button>
              <el-dropdown trigger="click" placement="bottom-end" @command="handleUserCommand">
                <button class="user-pill" type="button" :aria-label="`${sessionState.user?.username}，账户菜单`" :title="sessionState.user?.username">
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
                    <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                    <el-dropdown-item command="collections">管理系列</el-dropdown-item>
                    <el-dropdown-item v-if="sessionState.user?.role === 'ADMIN'" command="categories">管理分类</el-dropdown-item>
                    <el-dropdown-item v-if="sessionState.user?.role === 'ADMIN'" command="users">管理用户</el-dropdown-item>
                    <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <el-button v-else :icon="Lock" @click="loginVisible = true">登录</el-button>
          </div>
      </template>
      <RouterView v-slot="{ Component, route }">
        <Transition name="page-shift" mode="out-in">
          <component :is="Component" :key="route.path" />
        </Transition>
      </RouterView>
    </SiteShell>
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
  </el-config-provider>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, EditPen, Lock, SwitchButton, User } from '@element-plus/icons-vue'
import SiteShell from './components/SiteShell.vue'
import { getCurrentUser, login, register } from './api/blog'
import { clearSession, finishSessionCheck, sessionState, setSession, signedIn } from './state/session'

const loginVisible = ref(false)
const router = useRouter()
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
const authDialogTitle = computed(() => (authMode.value === 'login' ? '登录留白手记' : '注册留白手记'))
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
  if (authMode.value === 'register' && (value.length < 6 || value.length > 72)) {
    callback(new Error('密码长度需要在 6 到 72 个字符之间'))
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
  if (['collections', 'categories', 'users'].includes(command)) {
    router.push(`/${command}`)
    return
  }
  if (command === 'profile') {
    router.push('/me')
    return
  }

  if (command === 'logout') {
    logout()
  }
}

function openAuthDialog() {
  authMode.value = 'login'
  loginVisible.value = true
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

onMounted(() => {
  window.addEventListener('blog:open-auth', openAuthDialog)
  checkCurrentSession()
})

onBeforeUnmount(() => {
  window.removeEventListener('blog:open-auth', openAuthDialog)
})
</script>
