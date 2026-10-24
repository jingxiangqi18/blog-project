<template><div class="markdown-body" v-html="html"></div></template>
<script setup>
import { computed } from 'vue'
import { renderMarkdownDocument } from '../utils/markdown'
const props = defineProps({ content: { type: String, default: '' }, title: { type: String, default: '' } })
const html = computed(() => renderMarkdownDocument(props.content, { title: props.title }).html)
</script>
