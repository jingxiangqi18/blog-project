import { createRouter, createWebHashHistory } from 'vue-router'
import ArticleList from '../views/ArticleList.vue'
import ArticleDetail from '../views/ArticleDetail.vue'
import ArticleEditor from '../views/ArticleEditor.vue'
import CategoryManager from '../views/CategoryManager.vue'
import UserManager from '../views/UserManager.vue'

const routes = [
  { path: '/', redirect: '/articles' },
  { path: '/articles', name: 'articles', component: ArticleList },
  { path: '/articles/new', name: 'article-create', component: ArticleEditor },
  { path: '/articles/:id', name: 'article-detail', component: ArticleDetail, props: true },
  { path: '/articles/:id/edit', name: 'article-edit', component: ArticleEditor, props: true },
  { path: '/categories', name: 'categories', component: CategoryManager },
  { path: '/users', name: 'users', component: UserManager },
  { path: '/:pathMatch(.*)*', redirect: '/articles' },
]

export default createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})
