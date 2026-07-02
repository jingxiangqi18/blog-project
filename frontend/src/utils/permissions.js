import { sessionState } from '../state/session'

export function isSignedIn() {
  return Boolean(sessionState.token) && !sessionState.checking
}

export function isAdmin() {
  if (sessionState.checking) {
    return false
  }

  return sessionState.user?.role === 'ADMIN'
}

export function currentUserId() {
  if (sessionState.checking) {
    return null
  }

  const id = Number(sessionState.user?.id)
  return Number.isFinite(id) ? id : null
}

export function isResourceOwner(resource) {
  const ownerId = Number(resource?.authorId)
  const userId = currentUserId()

  return userId !== null && Number.isFinite(ownerId) && ownerId === userId
}

export function canManageResource(resource) {
  return isAdmin() || isResourceOwner(resource)
}
