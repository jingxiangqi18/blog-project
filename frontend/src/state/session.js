import { computed, reactive } from 'vue'

const STORAGE_KEY = 'blog-studio-session'

const state = reactive({
  token: '',
  user: null,
  checking: false,
})

function readStoredSession() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeStoredSession(session) {
  if (!session?.token) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function restoreSession() {
  const stored = readStoredSession()

  if (!stored?.token) {
    return
  }

  state.token = stored.token
  state.user = stored.user || null
  state.checking = true
}

export function setSession(payload) {
  state.token = payload.token
  state.user = {
    id: payload.id,
    username: payload.username,
    role: payload.role,
  }
  state.checking = false
  writeStoredSession({ token: state.token, user: state.user })
}

export function clearSession() {
  state.token = ''
  state.user = null
  state.checking = false
  writeStoredSession(null)
}

export function finishSessionCheck() {
  state.checking = false
}

export function getAuthToken() {
  return state.token || readStoredSession()?.token || ''
}

export const sessionState = state
export const signedIn = computed(() => Boolean(state.token) && !state.checking)

restoreSession()
