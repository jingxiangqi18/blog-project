<template>
  <el-dialog :model-value="modelValue" class="search-dialog" width="640px" :show-close="false" :close-on-click-modal="true" @update:model-value="$emit('update:modelValue', $event)" @opened="focusSearch" @closed="reset">
    <template #header="{ titleId }"><span class="search-dialog-heading"><Search :size="18" /><span :id="titleId">查找手记</span><button class="icon-button" aria-label="关闭搜索" title="关闭搜索" @click="$emit('update:modelValue', false)"><X :size="17" /></button></span></template>
    <el-input ref="input" v-model="keyword" class="global-search-input" placeholder="搜索文章标题或正文" clearable :prefix-icon="Search" aria-label="搜索文章" @keydown.down.prevent="focusResult" @keydown.enter.prevent="openFirst" />
    <div class="search-results" aria-live="polite">
      <el-skeleton v-if="loading" :rows="5" animated />
      <div v-else-if="error" class="search-state"><Search :size="28" /><p>{{ error }}</p><el-button @click="search">重试</el-button></div>
      <div v-else-if="!results.length" class="search-state"><FileSearch :size="32" /><p>{{ keyword.trim() ? '没有找到相关手记' : '还没有发布的手记' }}</p></div>
      <template v-else><p class="search-result-label">{{ keyword.trim() ? '搜索结果' : '最近的手记' }}<span>{{ total }} 篇</span></p><RouterLink v-for="article in results" :key="article.id" class="search-result" :to="'/articles/' + article.id" @click="$emit('update:modelValue', false)"><span class="search-result-icon"><FileText :size="18" /></span><span><strong>{{ article.title }}</strong><small>{{ article.authorName || '未知作者' }}<span v-if="article.categoryName"> · {{ article.categoryName }}</span></small></span><ArrowUpRight :size="16" /></RouterLink><button v-if="total > results.length" class="search-all" @click="viewAll">查看全部 {{ total }} 篇结果 <ArrowRight :size="15" /></button></template>
    </div>
  </el-dialog>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ArrowUpRight, FileSearch, FileText, Search, X } from '@lucide/vue'
import { listArticles } from '../api/blog'
const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])
const router = useRouter(), input = ref(null), keyword = ref(''), results = ref([]), total = ref(0), loading = ref(false), error = ref('')
let timer, request = 0
async function search() {
  const current = ++request
  loading.value = true; error.value = ''
  try {
    const data = await listArticles({ page: 1, size: 8, keyword: keyword.value.trim() || undefined })
    if (current === request) { results.value = data.content || []; total.value = data.totalElements || 0 }
  } catch { if (current === request) error.value = '搜索暂时不可用，请稍后重试' }
  finally { if (current === request) loading.value = false }
}
function reset() { clearTimeout(timer); request++; keyword.value = ''; results.value = []; loading.value = false }
function focusResult() { document.querySelector('.search-result')?.focus() }
function focusSearch() { if (!document.activeElement?.closest('.search-results')) input.value?.focus() }
function openFirst() { if (!loading.value && results.value[0]) { router.push('/articles/' + results.value[0].id); emit('update:modelValue', false) } }
function viewAll() { router.push({ path: '/articles', query: { keyword: keyword.value.trim() || undefined } }); emit('update:modelValue', false) }
watch(keyword, () => { clearTimeout(timer); request++; if (!props.modelValue) return; loading.value = true; timer = setTimeout(search, 250) })
watch(() => props.modelValue, open => { if (open) search(); else { clearTimeout(timer); request++ } })
onBeforeUnmount(() => { clearTimeout(timer); request++ })
</script>
