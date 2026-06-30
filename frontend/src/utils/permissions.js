import { sessionState } from '../state/session'

export function isSignedIn() {
  return Boolean(sessionState.token)
}

export function isAdmin() {
  return sessionState.user?.role === 'ADMIN'
}

export function currentUserId() {
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
