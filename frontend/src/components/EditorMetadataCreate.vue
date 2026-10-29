<template>
  <el-dialog
    v-model="open"
    :title="`新建${config.label}`"
    class="metadata-create-dialog"
    modal-class="metadata-create-overlay"
    width="460px"
    align-center
    append-to-body
    destroy-on-close
    transition="metadata-sheet"
    :close-icon="X"
    :close-on-click-modal="!busy"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    :before-close="beforeClose"
    @open="reset"
    @opened="nameInput?.focus()"
    @closed="emit('closed')"
  >
    <template #header>
      <div class="metadata-create-heading"><span class="metadata-create-symbol" :class="`is-${kind}`"><component :is="config.icon" :size="22" :stroke-width="1.7" /></span><div><span>NEW {{ kind.toUpperCase() }}</span><h3>新建{{ config.label }}</h3></div></div>
    </template>
    <el-form class="metadata-create-form" label-position="top" :disabled="busy" @submit.prevent="submit">
      <el-form-item :label="`${config.label}名称`" required>
        <el-input ref="nameInput" v-model="form.name" :maxlength="kind === 'series' ? 200 : 100" :placeholder="config.placeholder" aria-label="新建名称" autocomplete="off" @input="error = ''" />
      </el-form-item>
      <div v-if="kind === 'tag' && form.name.trim()" class="metadata-create-preview"><TechBadge :tag="{ name: form.name.trim(), slug: form.slug }" /></div>
      <template v-if="kind === 'category' || kind === 'series'">
        <el-form-item label="所属分区" :required="kind === 'series'">
          <el-select v-model="form.sectionId" :clearable="kind === 'category'" :placeholder="kind === 'category' ? '全站通用' : '选择分区'" aria-label="新建内容所属分区" @change="form.categoryId = null">
            <el-option v-for="section in availableSections" :key="section.id" :value="section.id" :label="section.name" />
            <template v-if="kind === 'category'" #empty><span class="metadata-menu-empty">全站通用分类</span></template>
          </el-select>
        </el-form-item>
        <el-form-item v-if="kind === 'series'" label="系列分类">
          <el-select v-model="form.categoryId" clearable placeholder="不指定分类" aria-label="新建系列分类"><el-option v-for="category in availableCategories" :key="category.id" :label="category.name" :value="category.id" /></el-select>
        </el-form-item>
      </template>
      <el-form-item v-if="kind !== 'category'" label="链接标识" class="metadata-slug-field">
        <el-input ref="slugInput" v-model="form.slug" maxlength="100" aria-label="链接标识" spellcheck="false" autocomplete="off" @input="slugEdited = true; error = ''"><template #prefix><Link2 :size="14" /></template></el-input>
      </el-form-item>
      <el-form-item v-if="kind === 'series' || kind === 'section'" label="简介">
        <el-input v-model="form.description" type="textarea" :rows="2" maxlength="1000" placeholder="添加简介（可选）" aria-label="新建内容简介" />
      </el-form-item>
      <Transition name="publish-notice"><p v-if="existing" class="metadata-existing" role="status"><Check :size="15" />{{ canUseExisting ? `已有同名${config.label}，可以直接选用` : '同名内容已存在于其他分区，或你没有管理权限，请修改名称' }}</p></Transition>
      <Transition name="publish-notice"><p v-if="error" class="metadata-create-error" role="alert"><CircleAlert :size="15" /><span>{{ error }}</span></p></Transition>
      <button type="submit" class="sr-only" tabindex="-1">提交</button>
    </el-form>
    <template #footer>
      <el-button :disabled="busy" @click="open = false">取消</el-button>
      <el-button type="primary" :loading="busy" :disabled="!allowed || (!!existing && !canUseExisting)" @click="submit"><Check v-if="existing" :size="15" /><Plus v-else :size="15" /><span>{{ existing ? '使用已有' + config.label : '创建并选中' }}</span></el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, markRaw, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, CircleAlert, FolderOpen, Layers, Library, Link2, Plus, Tags, X } from '@lucide/vue'
import { createCategory, createSection, createSeries, createTag } from '../api/blog'
import { canManageResource, isAdmin, isSignedIn } from '../utils/permissions'
import { metadataCreationError, metadataCreationPayload, suggestMetadataSlug } from '../utils/publishing'
import TechBadge from './TechBadge.vue'

const open = defineModel({ type: Boolean, default: false })
const props = defineProps({
  kind: { type: String, default: 'tag' },
  initialName: { type: String, default: '' },
  sectionId: { type: Number, default: null },
  categoryId: [Number, String],
  sections: { type: Array, default: () => [] },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  series: { type: Array, default: () => [] },
})
const emit = defineEmits(['created', 'closed'])
const configs = {
  tag: { label: '标签', icon: markRaw(Tags), placeholder: '例如 Spring Boot', create: createTag },
  category: { label: '分类', icon: markRaw(FolderOpen), placeholder: '例如 Java 后端', create: createCategory },
  section: { label: '分区', icon: markRaw(Layers), placeholder: '例如 技术札记', create: createSection },
  series: { label: '系列', icon: markRaw(Library), placeholder: '例如 从零学习 Spring Boot', create: createSeries },
}
const config = computed(() => configs[props.kind] || configs.tag)
const allowed = computed(() => props.kind === 'series' ? isSignedIn() : isAdmin())
const form = reactive({ name: '', slug: '', description: '', sectionId: null, categoryId: null })
const busy = ref(false), error = ref(''), slugEdited = ref(false), nameInput = ref(null), slugInput = ref(null)
let fallbackSlug = ''
const availableSections = computed(() => props.sections.filter(section => section.enabled !== false && (!props.sectionId || section.id === props.sectionId)))
const availableCategories = computed(() => props.categories.filter(category => !category.sectionId || category.sectionId === form.sectionId))
const existing = computed(() => {
  const items = { tag: props.tags, category: props.categories, section: props.sections, series: props.series }[props.kind] || []
  const name = form.name.trim().toLocaleLowerCase()
  return name ? items.find(item => (item.name || item.title || '').trim().toLocaleLowerCase() === name) : null
})
const canUseExisting = computed(() => {
  if (!existing.value || !allowed.value) return false
  const item = existing.value
  if (props.kind === 'section') return item.enabled !== false
  if (props.kind === 'series') return canManageResource(item) && availableSections.value.some(section => section.id === item.sectionId)
  if (props.kind === 'category') return !item.sectionId || !props.sectionId || props.sectionId === item.sectionId
  return true
})
function reset() {
  const suffix = Array.from(crypto.getRandomValues(new Uint8Array(4)), value => value.toString(16).padStart(2, '0')).join('')
  fallbackSlug = `${props.kind}-${suffix}`
  slugEdited.value = false
  const sectionId = availableSections.value.some(section => section.id === props.sectionId) ? props.sectionId : null
  Object.assign(form, { name: props.initialName.slice(0, props.kind === 'series' ? 200 : 100), slug: suggestMetadataSlug(props.initialName, fallbackSlug), description: '', sectionId, categoryId: null })
  if (props.kind === 'series' && availableCategories.value.some(category => category.id === props.categoryId)) form.categoryId = props.categoryId
  error.value = ''
}
watch(() => form.name, value => { if (!slugEdited.value) form.slug = suggestMetadataSlug(value, fallbackSlug) })
watch(allowed, value => { if (!value) open.value = false })
function beforeClose(done) { if (!busy.value) done() }
async function submit() {
  if (busy.value || !allowed.value) return
  if (existing.value) {
    if (canUseExisting.value) { emit('created', { kind: props.kind, item: existing.value }); open.value = false }
    return
  }
  error.value = metadataCreationError(props.kind, form)
  if (error.value) return
  busy.value = true
  try {
    const item = await config.value.create(metadataCreationPayload(props.kind, form))
    if (!allowed.value) return
    emit('created', { kind: props.kind, item })
    open.value = false
    ElMessage.success(`${config.value.label}已创建并选中`)
  } catch (cause) {
    const status = cause.response?.status
    error.value = status === 409 ? (props.kind === 'category' ? '分类名称已存在，请修改名称后重试' : '链接标识已存在，请修改后重试')
      : status === 403 ? '当前账号没有创建权限'
        : status >= 500 ? '后端服务暂时不可用，填写的内容已保留'
          : cause.response?.data?.message || '创建失败，请检查网络后重试'
    if (status === 409 && props.kind !== 'category') nextTick(() => slugInput.value?.focus())
  } finally { busy.value = false }
}
</script>
