<template>
  <div ref="editorHost" class="markdown-mention-editor"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { autocompletion, completionKeymap, startCompletion } from '@codemirror/autocomplete'
import { markdown } from '@codemirror/lang-markdown'
import { EditorState, RangeSetBuilder } from '@codemirror/state'
import { Decoration, EditorView, ViewPlugin, WidgetType, keymap, placeholder as editorPlaceholder } from '@codemirror/view'
import { basicSetup } from 'codemirror'
import { listArticles, listKnowledgeCards } from '../api/blog'
import { markdownToPlainText } from '../utils/markdown'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  currentArticleId: {
    type: [String, Number],
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'cursor-change', 'scroll'])
const editorHost = ref(null)
let editorView = null

const articleSection = { name: '站内文章', rank: 1 }
const cardSection = { name: '知识卡片', rank: 2 }
const referencePattern = /\[([^\]\n]+)\]\((article|card):(\d+)\)/g
const ARTICLE_PAGE_SIZE = 10
const articleMentionState = {
  keyword: '',
  requestedPage: 1,
  loadedPage: 0,
  totalPages: 1,
  totalElements: 0,
  articles: [],
}

class ReferenceWidget extends WidgetType {
  constructor(label, type){
    super()
    this.label = label
    this.type = type
  }

  eq(other){
    return other.label === this.label && other.type === this.type
  }

  toDOM(){
    const element = document.createElement('span')
    element.className = `cm-reference-token ${this.type}`
    element.textContent = `@${this.label}`
    element.title = this.type === 'article' ? '站内文章引用' : '知识卡片引用'
    element.setAttribute('aria-label', element.title)
    return element
  }

  ignoreEvent(){
    return false
  }
}

function buildReferenceDecorations(view){
  const builder = new RangeSetBuilder()
  const source = view.state.doc.toString()

  for(const match of source.matchAll(referencePattern)){
    const from = match.index
    const to = from + match[0].length
    builder.add(
      from,
      to,
      Decoration.replace({
        widget: new ReferenceWidget(match[1], match[2]),
        inclusive: false,
      }),
    )
  }

  return builder.finish()
}

const referenceDecorations = ViewPlugin.fromClass(class {
  constructor(view){
    this.decorations = buildReferenceDecorations(view)
  }

  update(update){
    if(update.docChanged || update.viewportChanged){
      this.decorations = buildReferenceDecorations(update.view)
    }
  }
}, {
  decorations: (value) => value.decorations,
  provide: (plugin) => EditorView.atomicRanges.of((view) => {
    return view.plugin(plugin)?.decorations || Decoration.none
  }),
})

function escapeReferenceLabel(value){
  return value.replace(/\]/g, '\\]').replace(/[\r\n]+/g, ' ')
}

function articleSummary(article){
  const summary = markdownToPlainText(article.content || '')
  return summary ? summary.slice(0, 90) : '暂无正文摘要'
}

function referenceApply(type, item){
  return (view, completion, from, to) => {
    const label = escapeReferenceLabel(item.title)
    const reference = `[${label}](${type}:${item.id})`
    view.dispatch({
      changes: { from, to, insert: reference },
      selection: { anchor: from + reference.length },
    })
    view.focus()
  }
}

function resetArticleMentionState(keyword){
  articleMentionState.keyword = keyword
  articleMentionState.requestedPage = 1
  articleMentionState.loadedPage = 0
  articleMentionState.totalPages = 1
  articleMentionState.totalElements = 0
  articleMentionState.articles = []
}

function loadMoreArticles(view){
  articleMentionState.requestedPage = articleMentionState.loadedPage + 1
  window.setTimeout(() => startCompletion(view), 0)
}

async function mentionCompletionSource(context){
  const line = context.state.doc.lineAt(context.pos)
  const sourceBeforeCursor = context.state.sliceDoc(line.from, context.pos)
  const match = sourceBeforeCursor.match(/@([^@\n]*)$/u)

  if(!match){
    return null
  }

  const from = context.pos - match[0].length
  const keyword = match[1].trim()

  if(articleMentionState.keyword !== keyword){
    resetArticleMentionState(keyword)
  }

  const requestedPage = articleMentionState.requestedPage
  const [articlesResult, cardsResult] = await Promise.allSettled([
    listArticles({ page: requestedPage, size: ARTICLE_PAGE_SIZE, keyword }),
    listKnowledgeCards({ keyword }),
  ])

  if(context.aborted || articleMentionState.keyword !== keyword || articleMentionState.requestedPage !== requestedPage){
    return null
  }

  if(articlesResult.status === 'fulfilled'){
    const response = articlesResult.value
    const pageArticles = (response.content || [])
      .filter((article) => String(article.id) !== String(props.currentArticleId))
    const articleMap = new Map(articleMentionState.articles.map((article) => [String(article.id), article]))

    pageArticles.forEach((article) => articleMap.set(String(article.id), article))
    articleMentionState.articles = [...articleMap.values()]
    articleMentionState.loadedPage = requestedPage
    articleMentionState.totalPages = response.totalPages || 1
    articleMentionState.totalElements = response.totalElements || articleMentionState.articles.length
  }

  const articles = articleMentionState.articles
  const cards = cardsResult.status === 'fulfilled' ? cardsResult.value : []
  const hasMoreArticles = articleMentionState.loadedPage < articleMentionState.totalPages

  return {
    from,
    filter: false,
    options: [
      ...articles.map((article) => ({
        label: article.title,
        detail: article.categoryName || '未分类',
        info: articleSummary(article),
        section: articleSection,
        type: 'text',
        boost: 2,
        apply: referenceApply('article', article),
      })),
      ...(hasMoreArticles ? [{
        label: '加载更多文章',
        detail: `第 ${articleMentionState.loadedPage + 1} / ${articleMentionState.totalPages} 页`,
        info: `共 ${articleMentionState.totalElements} 篇文章，选择后继续加载候选项`,
        section: articleSection,
        type: 'text',
        boost: -10,
        apply: loadMoreArticles,
      }] : []),
      ...cards.map((card) => ({
        label: card.title,
        detail: card.summary,
        info: card.summary || '暂无摘要',
        section: cardSection,
        type: 'keyword',
        boost: 1,
        apply: referenceApply('card', card),
      })),
    ],
  }
}

function selectionPayload(view){
  const selection = view.state.selection.main
  return {
    start: selection.from,
    end: selection.to,
    text: view.state.sliceDoc(selection.from, selection.to),
    line: view.state.doc.lineAt(selection.head).number - 1,
  }
}

function emitScroll(){
  if(!editorView){
    return
  }

  const scrollElement = editorView.scrollDOM
  const maxScroll = scrollElement.scrollHeight - scrollElement.clientHeight
  emit('scroll', maxScroll > 0 ? scrollElement.scrollTop / maxScroll : 0)
}

onMounted(() => {
  editorView = new EditorView({
    parent: editorHost.value,
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        basicSetup,
        markdown(),
        EditorView.lineWrapping,
        editorPlaceholder('使用 Markdown 写作，输入 @ 可以引用文章或知识卡片'),
        referenceDecorations,
        autocompletion({
          override: [mentionCompletionSource],
          activateOnTyping: true,
          maxRenderedOptions: 20,
        }),
        keymap.of(completionKeymap),
        EditorView.updateListener.of((update) => {
          if(update.docChanged){
            emit('update:modelValue', update.state.doc.toString())
          }
          if(update.docChanged || update.selectionSet){
            emit('cursor-change', selectionPayload(update.view))
          }
        }),
        EditorView.theme({
          '&': {
            height: '100%',
            backgroundColor: 'transparent',
          },
          '&.cm-focused': {
            outline: 'none',
          },
          '.cm-scroller': {
            overflow: 'auto',
            fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
          },
          '.cm-content': {
            minHeight: '100%',
            padding: '22px 24px 44px',
            caretColor: '#1f2937',
          },
          '.cm-line': {
            padding: '0',
            lineHeight: '1.85',
          },
          '.cm-gutters': {
            display: 'none',
          },
          '.cm-activeLine': {
            backgroundColor: 'transparent',
          },
          '.cm-selectionBackground, &.cm-focused .cm-selectionBackground, ::selection': {
            backgroundColor: 'rgba(37, 99, 235, 0.16) !important',
          },
        }),
      ],
    }),
  })

  editorView.scrollDOM.addEventListener('scroll', emitScroll, { passive: true })
  emit('cursor-change', selectionPayload(editorView))
})

watch(
  () => props.modelValue,
  (value) => {
    if(!editorView){
      return
    }

    const currentValue = editorView.state.doc.toString()
    if(value === currentValue){
      return
    }

    editorView.dispatch({
      changes: { from: 0, to: currentValue.length, insert: value || '' },
    })
  },
)

onBeforeUnmount(() => {
  if(editorView){
    editorView.scrollDOM.removeEventListener('scroll', emitScroll)
    editorView.destroy()
    editorView = null
  }
})

function getSelection(){
  return editorView ? selectionPayload(editorView) : { start: 0, end: 0, text: '', line: 0 }
}

function replaceRange(start, end, text, selectionStart, selectionEnd = selectionStart){
  if(!editorView){
    return
  }

  editorView.dispatch({
    changes: { from: start, to: end, insert: text },
    selection: {
      anchor: selectionStart,
      head: selectionEnd,
    },
  })
  editorView.focus()
}

function focus(){
  editorView?.focus()
}

function getCursorLine(){
  return editorView ? editorView.state.doc.lineAt(editorView.state.selection.main.head).number - 1 : 0
}

function getScrollRatio(){
  if(!editorView){
    return 0
  }
  const scrollElement = editorView.scrollDOM
  const maxScroll = scrollElement.scrollHeight - scrollElement.clientHeight
  return maxScroll > 0 ? scrollElement.scrollTop / maxScroll : 0
}

defineExpose({
  focus,
  getCursorLine,
  getScrollRatio,
  getSelection,
  replaceRange,
})
</script>
