import http from './http'

export function login(payload) {
  return http.post('/auth/login', payload)
}

export function register(payload) {
  return http.post('/auth/register', payload)
}

export function getCurrentUser() {
  return http.get('/auth/me')
}

export function listUsers() {
  return http.get('/users')
}

export function updateUserEnabled(id, payload) {
  return http.patch(`/users/${id}/enabled`, payload)
}

export function changeMyPassword(payload) {
  return http.patch('/users/me/password', payload)
}

export function listMyArticles() {
  return http.get('/users/me/articles')
}

export function listMyComments() {
  return http.get('/users/me/comments')
}

export function listMyFavoriteArticles() {
  return http.get('/users/me/favorites')
}

export function listArticles(params = {}) {
  return http.get('/articles', { params: { size: 9, ...params } })
}

export function getArticle(id) {
  return http.get(`/articles/${id}`)
}

export function createArticle(payload) {
  return http.post('/articles', payload)
}

export function updateArticle(id, payload) {
  return http.put(`/articles/${id}`, payload)
}

export function deleteArticle(id) {
  return http.delete(`/articles/${id}`)
}

export function getArticleLikeStatus(id) {
  return http.get(`/articles/${id}/likes`)
}

export function likeArticle(id) {
  return http.post(`/articles/${id}/likes`)
}

export function unlikeArticle(id) {
  return http.delete(`/articles/${id}/likes`)
}

export function getArticleFavoriteStatus(id) {
  return http.get(`/articles/${id}/favorites`)
}

export function favoriteArticle(id) {
  return http.post(`/articles/${id}/favorites`)
}

export function unfavoriteArticle(id) {
  return http.delete(`/articles/${id}/favorites`)
}

export function createArticleDraft(payload) {
  return http.post('/articles/drafts', payload)
}

export function getArticleDraftById(draftId) {
  return http.get(`/articles/drafts/${draftId}`)
}

export function updateArticleDraft(draftId, payload) {
  return http.put(`/articles/drafts/${draftId}`, payload)
}

export function deleteArticleDraftById(draftId) {
  return http.delete(`/articles/drafts/${draftId}`)
}

export function getArticleDraft(articleId) {
  return http.get(`/articles/${articleId}/draft`)
}

export function saveArticleDraft(articleId, payload) {
  return http.put(`/articles/${articleId}/draft`, payload)
}

export function deleteArticleDraft(articleId) {
  return http.delete(`/articles/${articleId}/draft`)
}

export function listArticleRevisions(articleId) {
  return http.get(`/articles/${articleId}/revisions`)
}

export function getArticleRevision(articleId, revisionId) {
  return http.get(`/articles/${articleId}/revisions/${revisionId}`)
}

export function restoreArticleRevision(articleId, revisionId) {
  return http.post(`/articles/${articleId}/revisions/${revisionId}/restore`)
}

export function listKnowledgeCards(params = {}) {
  return http.get('/knowledge-cards', { params })
}

export function getKnowledgeCard(id) {
  return http.get(`/knowledge-cards/${id}`)
}

export function createKnowledgeCard(payload) {
  return http.post('/knowledge-cards', payload)
}

export function updateKnowledgeCard(id, payload) {
  return http.put(`/knowledge-cards/${id}`, payload)
}

export function deleteKnowledgeCard(id) {
  return http.delete(`/knowledge-cards/${id}`)
}

export function listCategories() {
  return http.get('/categories')
}

export function createCategory(payload) {
  return http.post('/categories', payload)
}

export function updateCategory(id, payload) {
  return http.put(`/categories/${id}`, payload)
}

export function deleteCategory(id) {
  return http.delete(`/categories/${id}`)
}

export function listArticlesByCategory(categoryId) {
  return http.get(`/categories/${categoryId}/articles`)
}

export function listComments(articleId) {
  return http.get(`/articles/${articleId}/comments`)
}

export function createComment(articleId, payload) {
  return http.post(`/articles/${articleId}/comments`, payload)
}

export function createCommentReply(articleId, commentId, payload) {
  return http.post(`/articles/${articleId}/comments/${commentId}/replies`, payload)
}

export function getCommentLikeStatus(articleId, commentId) {
  return http.get(`/articles/${articleId}/comments/${commentId}/likes`)
}

export function likeComment(articleId, commentId) {
  return http.post(`/articles/${articleId}/comments/${commentId}/likes`)
}

export function unlikeComment(articleId, commentId) {
  return http.delete(`/articles/${articleId}/comments/${commentId}/likes`)
}

export function updateComment(articleId, commentId, payload) {
  return http.put(`/articles/${articleId}/comments/${commentId}`, payload)
}

export function deleteComment(articleId, commentId) {
  return http.delete(`/articles/${articleId}/comments/${commentId}`)
}
