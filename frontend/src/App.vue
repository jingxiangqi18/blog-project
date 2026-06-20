<template>
  <el-config-provider namespace="el">
    <div class="app-shell">
      <aside class="app-sidebar">
        <RouterLink class="brand" to="/articles">
          <span class="brand-mark">
            <span>B</span>
          </span>
          <span class="brand-copy">
            <strong>Blog Studio</strong>
            <small>内容管理台</small>
          </span>
        </RouterLink>

        <nav class="nav-list" aria-label="主导航">
          <RouterLink to="/articles" class="nav-item">
            <el-icon><Document /></el-icon>
            <span>文章</span>
          </RouterLink>
          <RouterLink to="/categories" class="nav-item">
            <el-icon><CollectionTag /></el-icon>
            <span>分类</span>
          </RouterLink>
        </nav>

        <div class="sidebar-footer">
          <span class="status-dot" aria-hidden="true"></span>
          <span>Local Studio</span>
        </div>
      </aside>

      <div class="workspace">
        <header class="app-header">
          <div class="topbar-copy">
            <span>Dashboard</span>
            <strong>创作与归档</strong>
          </div>

          <el-button
            class="header-action"
            type="primary"
            :icon="EditPen"
            aria-label="写文章"
            title="写文章"
            @click="$router.push('/articles/new')"
          >
            写文章
          </el-button>
        </header>

        <main class="app-main">
          <RouterView />
        </main>
      </div>
    </div>
  </el-config-provider>
</template>

<script setup>
import { CollectionTag, Document, EditPen } from '@element-plus/icons-vue'
</script>
