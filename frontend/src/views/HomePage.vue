<template>
  <section class="home-page">
    <header class="home-introduction" :style="{ backgroundImage: `url(${banner})` }">
      <div class="home-introduction-copy"><p class="section-kicker"><span class="edition-mark" aria-hidden="true" /> Notes / Code / Life</p><h1>留白手记<span>.</span></h1><p class="home-lead">在代码与生活之间，<br>留下一些值得记住的事。</p><div class="home-introduction-actions"><RouterLink to="/articles" class="button-link button-link--primary">开始阅读 <ArrowUpRight :size="17" /></RouterLink><RouterLink to="/about" class="button-link">关于这里 <ArrowRight :size="16" /></RouterLink></div></div>
    </header>
    <nav class="home-sections" aria-label="探索内容分区"><RouterLink v-for="section in sections" :key="section.id" :to="{ path: '/articles', query: { sectionSlug: section.slug } }"><span class="home-section-symbol"><Coffee v-if="section.slug === 'life'" :size="20" /><Code2 v-else :size="20" /></span><span>{{ section.name }}</span><ArrowUpRight :size="16" /></RouterLink><RouterLink to="/knowledge"><span class="home-section-symbol"><Layers :size="20" /></span><span>知识卡片</span><ArrowUpRight :size="16" /></RouterLink><RouterLink to="/collections"><span class="home-section-symbol"><Library :size="20" /></span><span>系列阅读</span><ArrowUpRight :size="16" /></RouterLink></nav>
    <div v-if="error" class="state-panel"><el-alert :title="error" type="error" :closable="false" /><el-button @click="load">重试</el-button></div>
    <el-skeleton v-else-if="loading" :rows="8" animated />
    <template v-else>
      <div class="home-section-heading"><div><p class="section-kicker">The latest</p><h2>最近更新</h2></div><RouterLink class="text-link" to="/articles">所有文章 <ArrowRight :size="16" /></RouterLink></div>
      <el-empty v-if="!articles.length" description="还没有发布的手记" />
      <div class="home-articles"><article v-for="(article, i) in articles" :key="article.id" :style="{ '--item-index': i }"><span class="home-number">{{ String(i + 1).padStart(2, '0') }}</span><div><p class="home-article-meta"><span class="home-article-category">{{ article.sectionName || article.categoryName || '手记' }}</span><span>{{ date(article.updatedAt || article.createdAt) }}</span></p><RouterLink :to="'/articles/' + article.id"><h3>{{ article.title }}</h3></RouterLink><p class="home-excerpt">{{ article.summary || markdownToPlainText(article.content, { title: article.title }).slice(0, 110) }}</p><small><span class="inline-author-avatar">{{ (article.authorName || '?').slice(0, 1) }}</span>{{ article.authorName || '未知作者' }}</small></div><RouterLink :to="'/articles/' + article.id" class="icon-button home-read-button" :aria-label="'阅读 ' + article.title" :title="'阅读 ' + article.title"><ArrowUpRight :size="20" /></RouterLink></article></div>
      <template v-if="series.length"><div class="home-section-heading"><div><p class="section-kicker">In sequence</p><h2>循序而读</h2></div><RouterLink class="text-link" to="/collections">系列目录 <ArrowRight :size="16" /></RouterLink></div><div class="home-series"><RouterLink v-for="item in series" :key="item.id" :to="{ path: '/collections', query: { seriesId: item.id } }"><BookOpen :size="24" /><small>{{ item.sectionName }}</small><h3>{{ item.title }}</h3><p>{{ item.description }}</p><ArrowRight :size="18" /></RouterLink></div></template>
    </template>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight, ArrowUpRight, BookOpen, Code2, Coffee, Layers, Library } from '@lucide/vue'
import { listArticles, listSections, listSeries } from '../api/blog'
import { markdownToPlainText } from '../utils/markdown'
const banner = `${import.meta.env.BASE_URL}studio-editorial-banner.webp`
const articles = ref([]), sections = ref([]), series = ref([]), loading = ref(true), error = ref('')
const date = value => value ? new Date(value).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' }) : ''
async function load() {
  loading.value = true; error.value = ''
  try { const [a, s, r] = await Promise.all([listArticles({ page: 1, size: 6 }), listSections(), listSeries({ page: 1, size: 3 })]); articles.value = a.content || []; sections.value = s; series.value = r.content || [] }
  catch { error.value = '内容加载失败，请稍后重试' } finally { loading.value = false }
}
onMounted(load)
</script>
