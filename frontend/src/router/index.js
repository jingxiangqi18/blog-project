import { createRouter, createWebHashHistory } from 'vue-router'
const ArticleList = () => import('../views/ArticleList.vue')
const ArticleDetail = () => import('../views/ArticleDetail.vue')
const ArticleEditor = () => import('../views/ArticleEditor.vue')
const CategoryManager = () => import('../views/CategoryManager.vue')
const KnowledgeCardLibrary = () => import('../views/KnowledgeCardLibrary.vue')
const ProfileCenter = () => import('../views/ProfileCenter.vue')
const UserManager = () => import('../views/UserManager.vue')
const ContentCollections = () => import('../views/ContentCollections.vue')

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomePage.vue') },
  { path: '/about', name: 'about', component: () => import('../views/AboutPage.vue') },
  { path: '/articles', name: 'articles', component: ArticleList },
  { path: '/articles/new', name: 'article-create', component: ArticleEditor },
  { path: '/articles/:id', name: 'article-detail', component: ArticleDetail, props: true },
  { path: '/articles/:id/edit', name: 'article-edit', component: ArticleEditor, props: true },
  { path: '/categories', name: 'categories', component: CategoryManager },
  { path: '/collections', name: 'collections', component: ContentCollections },
  { path: '/knowledge', name: 'knowledge', component: KnowledgeCardLibrary },
  { path: '/me', name: 'profile', component: ProfileCenter },
  { path: '/users', name: 'users', component: UserManager },
  { path: '/:pathMatch(.*)*', redirect: '/articles' },
]

export default createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, saved) {
    if (to.path === from.path && to.query.heading) return false
    return saved || { top: 0 }
  },
})
