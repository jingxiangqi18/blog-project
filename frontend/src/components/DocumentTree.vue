<template>
  <ul class="document-tree">
    <li v-for="node in nodes" :key="node.type + node.id">
      <template v-if="node.type === 'group'">
        <button class="document-group" :aria-expanded="expanded.has(node.id)" @click="toggle(node.id)"><ChevronRight :size="13" :class="{ turned: expanded.has(node.id) }" /><span>{{ node.title }}</span></button>
        <div class="document-group-content" :class="{ expanded: expanded.has(node.id) }" :inert="!expanded.has(node.id)" :aria-hidden="!expanded.has(node.id)"><div><DocumentTree :nodes="node.children" :expanded="expanded" :article-id="articleId" :series-id="seriesId" :headings="headings" :active-heading="activeHeading" @anchor="$emit('anchor', $event)" /></div></div>
      </template>
      <template v-else>
        <RouterLink class="document-article" :class="{ current: String(node.id) === String(articleId) }" :aria-current="String(node.id) === String(articleId) ? 'page' : undefined" :to="{ path: '/articles/' + node.id, query: { seriesId } }"><FileText :size="13" /><span>{{ node.title }}</span></RouterLink>
        <ul v-if="String(node.id) === String(articleId)" class="document-headings"><li v-for="heading in headings" :key="heading.id"><button :class="{ current: activeHeading === heading.id }" :style="{ paddingLeft: `${12 + Math.max(0, heading.level - 2) * 12}px` }" :aria-current="activeHeading === heading.id ? 'location' : undefined" @click="$emit('anchor', heading.id)">{{ heading.title }}</button></li></ul>
      </template>
    </li>
  </ul>
</template>
<script setup>
import { ChevronRight, FileText } from '@lucide/vue'
const props = defineProps({ nodes: Array, expanded: Set, articleId: [String, Number], seriesId: [String, Number], headings: Array, activeHeading: String })
defineEmits(['anchor'])
function toggle(id) { props.expanded.has(id) ? props.expanded.delete(id) : props.expanded.add(id) }
</script>
