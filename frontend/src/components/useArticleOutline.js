import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { renderMarkdownDocument } from '../utils/markdown'

export function useArticleOutline(content, body, title) {
  const route = useRoute(), router = useRouter()
  const document = computed(() => renderMarkdownDocument(content.value, { title: title?.value }))
  const headings = computed(() => document.value.headings.filter(h => h.level >= 2 && h.level <= 4))
  const activeHeading = ref('')
  let observer, frame, elements = []
  const reducedMotion = () => matchMedia('(prefers-reduced-motion: reduce)').matches
  function updateActive() {
    cancelAnimationFrame(frame)
    frame = requestAnimationFrame(() => {
      let active = elements[0]
      for (const element of elements) { if (element.getBoundingClientRect().top <= 120) active = element; else break }
      activeHeading.value = active?.id || ''
    })
  }
  function scrollTo(id, smooth = true) {
    const element = elements.find(item => item.id === id)
    if (!element) return
    element.scrollIntoView({ block: 'start', behavior: smooth && !reducedMotion() ? 'smooth' : 'auto' })
    activeHeading.value = id
  }
  function navigate(id) {
    router.replace({ query: { ...route.query, heading: id } })
    scrollTo(id)
  }
  watch([body, document], async () => {
    observer?.disconnect()
    await nextTick()
    elements = [...(body.value?.querySelectorAll('h2[id], h3[id], h4[id]') || [])]
    observer = new IntersectionObserver(updateActive, { rootMargin: '-88px 0px -60% 0px' })
    elements.forEach(element => observer.observe(element))
    updateActive()
    if (route.query.heading) scrollTo(String(route.query.heading), false)
  }, { flush: 'post' })
  watch(() => route.query.heading, value => { if (value) scrollTo(String(value)) })
  onBeforeUnmount(() => { observer?.disconnect(); cancelAnimationFrame(frame) })
  return { rendered: computed(() => document.value.html), headings, activeHeading, navigate }
}
