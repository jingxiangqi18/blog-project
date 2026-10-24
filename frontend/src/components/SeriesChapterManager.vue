<template>
  <section class="chapter-groups">
    <div class="panel-title-row"><h3>章节分组</h3><el-button :icon="Plus" size="small" :disabled="busy" @click="edit()">添加分组</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false"><el-button text @click="load">重试</el-button></el-alert>
    <el-skeleton v-else-if="loading" :rows="4" animated />
    <template v-else>
      <p v-if="!groups.length" class="muted">暂无章节分组</p>
      <div v-for="group in orderedGroups" :key="group.id" class="chapter-group-row" :style="{ paddingLeft: `${group.depth * 14}px` }">
        <span>{{ group.title }} <small v-if="group.parentId">/ {{ groupName(group.parentId) }}</small></span>
        <el-button :icon="ArrowUp" circle size="small" title="分组上移" aria-label="分组上移" :disabled="busy || siblingIndex(group) === 0" @click="move(group, -1)" />
        <el-button :icon="ArrowDown" circle size="small" title="分组下移" aria-label="分组下移" :disabled="busy || siblingIndex(group) === siblings(group).length - 1" @click="move(group, 1)" />
        <el-button :icon="EditPen" circle size="small" title="编辑分组" aria-label="编辑分组" :disabled="busy" @click="edit(group)" />
        <el-button :icon="Delete" circle size="small" title="删除分组" aria-label="删除分组" :disabled="busy || groups.some(g => g.parentId === group.id)" @click="remove(group)" />
      </div>
      <h3 v-if="articles.length">文章归属</h3>
      <div v-for="article in articles" :key="article.id" class="chapter-candidate"><span>{{ article.title }}</span><el-select class="chapter-select" :model-value="article.chapterId || ''" :disabled="busy" aria-label="文章章节分组" @change="assign(article.id, $event)"><el-option label="未分组" value="" /><el-option v-for="group in orderedGroups" :key="group.id" :label="'　'.repeat(group.depth) + group.title" :value="group.id" /></el-select></div>
    </template>
    <el-dialog v-model="dialog" title="章节分组" width="440px" append-to-body>
      <el-form label-position="top" @submit.prevent="save"><el-form-item label="标题" required><el-input v-model="form.title" maxlength="200" /></el-form-item><el-form-item label="上级分组"><el-select v-model="form.parentId" placeholder="顶层"><el-option label="顶层" value="" /><el-option v-for="group in availableParents" :key="group.id" :label="'　'.repeat(group.depth) + group.title" :value="group.id" /></el-select></el-form-item></el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="busy" :disabled="!form.title.trim()" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowUp, ArrowDown, EditPen, Delete } from '@element-plus/icons-vue'
import { getSeriesDirectory, createSeriesChapter, updateSeriesChapter, deleteSeriesChapter, reorderSeriesChapters, assignSeriesArticleChapter } from '../api/blog'
const props = defineProps({ seriesId: [Number, String] })
const emit = defineEmits(['changed'])
const groups = ref([]), articles = ref([]), loading = ref(false), busy = ref(false), error = ref(''), dialog = ref(false)
const form = reactive({ id: null, title: '', parentId: '' })
let requestId = 0
const groupName = id => groups.value.find(g => g.id === id)?.title || ''
const siblings = group => groups.value.filter(g => g.parentId === group.parentId).sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
const siblingIndex = group => siblings(group).findIndex(g => g.id === group.id)
const orderedGroups = computed(() => {
  function visit(parentId, depth, visited) {
    return groups.value.filter(g => g.parentId === parentId && !visited.has(g.id)).sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id).flatMap(g => [{ ...g, depth }, ...visit(g.id, depth + 1, new Set([...visited, g.id]))])
  }
  return visit(null, 0, new Set())
})
const availableParents = computed(() => orderedGroups.value.filter(g => {
  let cursor = g; const seen = new Set()
  while(cursor && !seen.has(cursor.id)) { if (cursor.id === form.id) return false; seen.add(cursor.id); cursor = groups.value.find(p => p.id === cursor.parentId) }
  return true
}))
async function load() {
  const request = ++requestId
  loading.value = true; error.value = ''
  try { const data = await getSeriesDirectory(props.seriesId); if(request === requestId) { groups.value = data.chapters; articles.value = data.articles } }
  catch { if(request === requestId) error.value = '章节目录加载失败' }
  finally { if(request === requestId) loading.value = false }
}
function edit(group = {}) { Object.assign(form, { id: group.id || null, title: group.title || '', parentId: group.parentId || '' }); dialog.value = true }
async function mutate(action) {
  if (busy.value) return false
  busy.value = true
  try { await action(); await load(); emit('changed'); return true }
  catch(e) { ElMessage.error(e.response?.data?.message || '操作失败，请检查章节关系及权限'); return false }
  finally { busy.value = false }
}
async function save() {
  if(!form.title.trim()) return
  const payload = { title: form.title.trim(), parentId: form.parentId || null }
  if(await mutate(() => form.id ? updateSeriesChapter(props.seriesId, form.id, payload) : createSeriesChapter(props.seriesId, payload))) dialog.value = false
}
async function remove(group) {
  try { await ElMessageBox.confirm(`删除分组「${group.title}」？其中的文章会保留并移回未分组。`, '删除分组', { type: 'warning' }) } catch { return }
  mutate(() => deleteSeriesChapter(props.seriesId, group.id))
}
function move(group, delta) {
  const ids = siblings(group).map(g => g.id), index = ids.indexOf(group.id)
  if(index + delta < 0 || index + delta >= ids.length) return
  ;[ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]
  mutate(() => reorderSeriesChapters(props.seriesId, { parentId: group.parentId || null, chapterIds: ids }))
}
function assign(articleId, chapterId) { mutate(() => assignSeriesArticleChapter(props.seriesId, articleId, chapterId || null)) }
watch(() => props.seriesId, load, { immediate: true })
onBeforeUnmount(() => { requestId++ })
</script>
