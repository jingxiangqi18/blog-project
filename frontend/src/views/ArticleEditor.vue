<template>
  <section class="page-stack editor-page">
    <div class="hero-panel">
      <div>
        <p class="section-kicker">Compose</p>
        <h2>{{ isEdit ? '编辑文章' : '发布文章' }}</h2>
      </div>
      <el-button :icon="Back" @click="$router.back()">返回</el-button>
    </div>

    <el-alert
      v-if="categoriesLoaded && categories.length === 0"
      type="warning"
      title="请先创建分类，再发布文章"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="loading" class="panel editor-loading" :rows="8" animated />

    <div v-else-if="loadError" class="panel state-panel error-panel">
      <el-alert type="error" :title="loadError" show-icon :closable="false" />
      <el-button :icon="Refresh" @click="loadData">重试</el-button>
    </div>

    <el-form
      v-else
      ref="formRef"
      class="editor-form"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <div class="editor-layout">
        <div class="panel editor-writing-surface">
          <el-form-item label="标题" prop="title" class="title-field">
            <el-input
              v-model="form.title"
              class="title-input"
              maxlength="200"
              show-word-limit
              placeholder="输入文章标题"
            />
          </el-form-item>

          <el-form-item label="正文" prop="content" class="content-field">
            <el-input
              v-model="form.content"
              type="textarea"
              :autosize="{ minRows: 18, maxRows: 34 }"
              placeholder="开始写作..."
            />
          </el-form-item>
        </div>

        <aside class="panel editor-side-panel">
          <div class="side-section">
            <p class="section-kicker">Publish</p>
            <h3>发布信息</h3>
          </div>

          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择分类" class="full-width">
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>

          <div class="writer-stats">
            <div>
              <el-icon><Reading /></el-icon>
              <span>{{ contentLength }} 字符</span>
            </div>
            <div>
              <el-icon><Stopwatch /></el-icon>
              <span>约 {{ estimatedMinutes }} 分钟读完</span>
            </div>
            <div>
              <el-icon><CollectionTag /></el-icon>
              <span>{{ categories.length }} 个分类</span>
            </div>
          </div>

          <div class="editor-actions">
            <el-button :disabled="saving" @click="$router.back()">取消</el-button>
            <el-button
              type="primary"
              :icon="Check"
              :loading="saving"
              :disabled="categories.length === 0"
              @click="submit"
            >
              {{ isEdit ? '保存修改' : '发布文章' }}
            </el-button>
          </div>
        </aside>
      </div>
    </el-form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Check, CollectionTag, Reading, Refresh, Stopwatch } from '@element-plus/icons-vue'
import { createArticle, getArticle, listCategories, updateArticle } from '../api/blog'

const props = defineProps({
  id: {
    type: String,
    default: '',
  },
})

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const categoriesLoaded = ref(false)
const categories = ref([])
const form = reactive({
  title: '',
  categoryId: '',
  content: '',
})

const isEdit = computed(() => Boolean(props.id))
const contentLength = computed(() => form.content.trim().length)
const estimatedMinutes = computed(() => {
  return contentLength.value ? Math.max(1, Math.ceil(contentLength.value / 500)) : 0
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  loadError.value = ''

  try {
    categories.value = await listCategories()
    categoriesLoaded.value = true

    if (isEdit.value) {
      const article = await getArticle(props.id)
      form.title = article.title
      form.categoryId = article.categoryId
      form.content = article.content
    } else if (categories.value[0]) {
      form.categoryId = categories.value[0].id
    }
  } catch {
    loadError.value = isEdit.value ? '文章或分类加载失败' : '分类加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

async function submit() {
  await formRef.value.validate()

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      categoryId: Number(form.categoryId),
      content: form.content.trim(),
    }

    const saved = isEdit.value
      ? await updateArticle(props.id, payload)
      : await createArticle(payload)

    ElMessage.success(isEdit.value ? '文章已保存' : '文章已发布')
    router.push(`/articles/${saved.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>
