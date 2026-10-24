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

export function listCategories(params = {}) {
  return http.get('/categories', { params })
}

export const listSections = () => http.get('/sections')
export const listAllSections = () => http.get('/sections/admin/all')
export const createSection = (data) => http.post('/sections', data)
export const updateSection = (id, data) => http.put(`/sections/${id}`, data)
export const updateSectionEnabled = (id, enabled) => http.patch(`/sections/${id}/enabled`, { enabled })
export const listTags = (params = {}) => http.get('/tags', { params })
export const createTag = (data) => http.post('/tags', data)
export const updateTag = (id, data) => http.put(`/tags/${id}`, data)
export const deleteTag = (id) => http.delete(`/tags/${id}`)
export const listSeries = (params = {}) => http.get('/series', { params })
export const getSeries = (id) => http.get(`/series/${id}`)
export const createSeries = (data) => http.post('/series', data)
export const updateSeries = (id, data) => http.put(`/series/${id}`, data)
export const deleteSeries = (id) => http.delete(`/series/${id}`)
export const addSeriesArticle = (id, articleId) => http.post(`/series/${id}/articles/${articleId}`)
export const removeSeriesArticle = (id, articleId) => http.delete(`/series/${id}/articles/${articleId}`)
export const reorderSeriesArticles = (id, articleIds) => http.put(`/series/${id}/articles/order`, { articleIds })
export const getSeriesDirectory = (id) => http.get(`/series/${id}/directory`)
export const listArticleSeries = (id) => http.get(`/articles/${id}/series`)
export const createSeriesChapter = (id, data) => http.post(`/series/${id}/chapters`, data)
export const updateSeriesChapter = (id, chapterId, data) => http.put(`/series/${id}/chapters/${chapterId}`, data)
export const deleteSeriesChapter = (id, chapterId) => http.delete(`/series/${id}/chapters/${chapterId}`)
export const reorderSeriesChapters = (id, data) => http.put(`/series/${id}/chapters/order`, data)
export const assignSeriesArticleChapter = (id, articleId, chapterId) => http.put(`/series/${id}/articles/${articleId}/chapter`, { chapterId })

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
