<template>
  <div class="site-shell">
    <a class="skip-link" href="#main-content" @click.prevent="focusMain">跳到正文</a>
    <header class="site-header">
      <div class="header-inner">
        <RouterLink class="site-brand" to="/" aria-label="留白手记首页"><span class="brand-mark"><BookOpen :size="20" :stroke-width="1.7" /></span><span>留白手记<span class="brand-period">.</span><small>Notes & beyond</small></span></RouterLink>
        <nav class="desktop-navigation primary-navigation" aria-label="主导航">
          <RouterLink to="/" class="primary-nav-link" :class="{ active: route.name === 'home' }">首页</RouterLink>
          <div class="primary-article-navigation" :class="{ active: articleRoute }">
            <RouterLink to="/articles" class="primary-nav-link" :class="{ active: articleRoute }">文章</RouterLink>
            <el-dropdown v-if="sections.length" trigger="click" placement="bottom-start" popper-class="navigation-dropdown" @command="router.push($event)">
              <button class="section-menu-trigger" aria-label="文章分区" title="文章分区"><ChevronDown :size="13" /></button>
              <template #dropdown><el-dropdown-menu><el-dropdown-item command="/articles"><FileText :size="15" />全部文章</el-dropdown-item><el-dropdown-item v-for="(section, index) in sections" :key="section.id" :command="{ path: '/articles', query: { sectionSlug: section.slug } }" :divided="index === 0"><Coffee v-if="section.slug === 'life'" :size="15" /><Code2 v-else :size="15" /><span>{{ section.name }}</span><Check v-if="route.query.sectionSlug === section.slug" :size="13" class="navigation-check" /></el-dropdown-item></el-dropdown-menu></template>
            </el-dropdown>
          </div>
          <RouterLink to="/knowledge" class="primary-nav-link" :class="{ active: route.name === 'knowledge' }">知识库</RouterLink>
          <span class="navigation-divider" aria-hidden="true" />
          <el-dropdown trigger="click" placement="bottom-start" popper-class="navigation-dropdown" @command="router.push($event)">
            <button class="directory-menu-trigger" :class="{ active: ['collections', 'categories'].includes(route.name) }" aria-label="内容目录"><Library :size="15" /><span>目录</span><ChevronDown :size="12" /></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="/collections"><Library :size="15" />系列专题</el-dropdown-item><el-dropdown-item command="/categories"><FolderOpen :size="15" />分类索引</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </nav>
        <div class="header-account"><button class="icon-button header-search" aria-label="搜索手记" title="搜索手记" @click="searchOpen = true"><Search :size="18" /></button><span class="header-divider" aria-hidden="true" /><slot name="actions" /><button class="icon-button mobile-menu-toggle" :aria-expanded="menuOpen" aria-label="打开导航菜单" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="20" /><Menu v-else :size="20" /></button></div>
      </div>
      <Transition name="navigation-panel">
        <nav v-if="menuOpen" ref="mobileNavigationRef" class="mobile-navigation" aria-label="移动导航" @keydown.esc.stop.prevent="closeMenu">
          <div class="mobile-navigation-group"><p>浏览</p><RouterLink to="/" :class="{ selected: route.name === 'home' }">首页</RouterLink><RouterLink to="/articles" :class="{ selected: articleRoute }">文章</RouterLink><RouterLink to="/knowledge" :class="{ selected: route.name === 'knowledge' }">知识库</RouterLink></div>
          <div v-if="sections.length" class="mobile-navigation-group mobile-section-navigation"><p>文章分区</p><RouterLink v-for="section in sections" :key="section.id" :to="{ path: '/articles', query: { sectionSlug: section.slug } }" :class="{ selected: articleRoute && route.query.sectionSlug === section.slug }">{{ section.name }}</RouterLink></div>
          <div class="mobile-navigation-group"><p>内容目录</p><RouterLink to="/collections" :class="{ selected: route.name === 'collections' }">系列专题</RouterLink><RouterLink to="/categories" :class="{ selected: route.name === 'categories' }">分类索引</RouterLink></div>
          <div v-if="signedIn" class="mobile-navigation-group"><p>账户</p><RouterLink to="/me" :class="{ selected: route.name === 'profile' }">个人空间</RouterLink><RouterLink v-if="isAdmin()" to="/users" :class="{ selected: route.name === 'users' }">用户管理</RouterLink></div>
        </nav>
      </Transition>
    </header>
    <main id="main-content" tabindex="-1" class="app-main" :class="{ 'wide-workspace': route.name === 'article-edit' || route.name === 'article-create', 'document-workspace': route.name === 'article-detail' }"><slot /></main>
    <footer class="site-footer"><RouterLink to="/">留白手记</RouterLink><span>Notes, thoughtfully kept.</span><nav aria-label="页脚导航"><RouterLink to="/categories">分类</RouterLink><RouterLink to="/collections">目录</RouterLink><RouterLink v-if="signedIn" to="/me">我的</RouterLink><RouterLink v-if="isAdmin()" to="/users">用户</RouterLink><RouterLink to="/about">关于</RouterLink></nav></footer>
    <p v-if="sectionError" class="navigation-notice" role="status">分区导航加载失败 <button @click="loadSections">重试</button></p>
    <ArticleSearch v-model="searchOpen" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BookOpen, Check, ChevronDown, Code2, Coffee, FileText, FolderOpen, Library, Menu, Search, X } from '@lucide/vue'
import ArticleSearch from './ArticleSearch.vue'
import { listSections } from '../api/blog'
import { signedIn } from '../state/session'
import { isAdmin } from '../utils/permissions'
const route = useRoute()
const router = useRouter()
const sections = ref([]), menuOpen = ref(false), sectionError = ref(false)
const mobileNavigationRef = ref(null)
const searchOpen = ref(false)
const articleRoute = computed(() => ['articles', 'article-detail', 'article-edit', 'article-create'].includes(route.name))
async function loadSections() { sectionError.value = false; try { sections.value = await listSections() } catch { sectionError.value = true } }
function focusMain() { document.getElementById('main-content')?.focus() }
function closeMenu() { menuOpen.value = false; document.querySelector('.mobile-menu-toggle')?.focus() }
function handleKey(event) {
  if (event.key === 'Escape' && menuOpen.value) closeMenu()
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); searchOpen.value = !searchOpen.value }
}
watch(() => route.fullPath, () => { menuOpen.value = false })
watch(menuOpen, async open => { if (open) { await nextTick(); mobileNavigationRef.value?.querySelector('a')?.focus() } })
onMounted(() => { loadSections(); window.addEventListener('keydown', handleKey) })
onBeforeUnmount(() => window.removeEventListener('keydown', handleKey))
</script>
