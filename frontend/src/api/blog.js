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

export function updateComment(articleId, commentId, payload) {
  return http.put(`/articles/${articleId}/comments/${commentId}`, payload)
}

export function deleteComment(articleId, commentId) {
  return http.delete(`/articles/${articleId}/comments/${commentId}`)
}
