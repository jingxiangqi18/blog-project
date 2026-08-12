<template>
  <div ref="editorHost" class="markdown-mention-editor"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { autocompletion, closeCompletion, completionKeymap, startCompletion } from '@codemirror/autocomplete'
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

const cardSection = { name: '知识卡片', rank: 3 }
const referencePattern = /\[([^\]\n]+)\]\((article|card):(\d+)\)/g
const ARTICLE_PAGE_SIZE = 5
const articleMentionState = {
  keyword: '',
  requestedPage: 1,
  loadedPage: 1,
  totalPages: 1,
  totalElements: 0,
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
  articleMentionState.loadedPage = 1
  articleMentionState.totalPages = 1
  articleMentionState.totalElements = 0
}

function reopenCompletion(view){
  closeCompletion(view)
  window.setTimeout(() => {
    view.focus()
    startCompletion(view)
  }, 0)
}

function changeArticleMentionPage(view, page){
  articleMentionState.requestedPage = Math.min(
    articleMentionState.totalPages,
    Math.max(1, page),
  )
  reopenCompletion(view)
}

function articleSection(keyword, page, totalPages, totalElements){
  return {
    name: '站内文章',
    rank: 1,
    header: () => {
      const header = document.createElement('li')
      header.className = 'cm-completionSection cm-mention-section-heading'

      const title = document.createElement('strong')
      title.textContent = keyword ? `搜索“${keyword}”` : '最近发布的文章'

      const meta = document.createElement('span')
      meta.textContent = `${totalElements} 篇 · ${page} / ${totalPages} 页`

      header.append(title, meta)
      return header
    },
  }
}

function navigationSection(page, totalPages){
  return {
    name: '翻页',
    rank: 2,
    header: () => {
      const header = document.createElement('li')
      header.className = 'cm-completionSection cm-mention-pagination-heading'
      header.textContent = `第 ${page} 页，共 ${totalPages} 页`
      return header
    },
  }
}

function completionContext(completion){
  if(!completion.referenceKind || !completion.summary){
    return null
  }

  const summary = document.createElement('span')
  summary.className = 'cm-mention-option-summary'
  summary.textContent = completion.summary
  return summary
}

function completionMark(completion){
  const mark = document.createElement('span')
  mark.className = `cm-mention-option-mark ${completion.referenceKind || 'action'}`
  mark.textContent = completion.referenceKind === 'card'
    ? 'K'
    : completion.referenceKind === 'article'
      ? 'A'
      : completion.referenceKind === 'error'
        ? '!'
        : completion.referenceKind === 'empty'
          ? '0'
          : completion.navigationDirection === 'previous' ? '‹' : '›'
  return mark
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

  let articles = []
  let articleRequestFailed = false

  if(articlesResult.status === 'fulfilled'){
    const response = articlesResult.value
    articles = (response.content || [])
      .filter((article) => String(article.id) !== String(props.currentArticleId))
    articleMentionState.loadedPage = requestedPage
    articleMentionState.totalPages = Math.max(1, response.totalPages || 1)
    articleMentionState.totalElements = response.totalElements ?? articles.length
  }else{
    articleRequestFailed = true
  }

  const cards = cardsResult.status === 'fulfilled' ? cardsResult.value : []
  const currentPage = articleMentionState.loadedPage
  const totalPages = articleMentionState.totalPages
  const articlesSection = articleSection(
    keyword,
    currentPage,
    totalPages,
    articleMentionState.totalElements,
  )
  const pageSection = navigationSection(currentPage, totalPages)
  const articleOptions = articles.map((article) => ({
    label: article.title,
    detail: [article.authorName, article.categoryName || '未分类'].filter(Boolean).join(' · '),
    summary: articleSummary(article),
    referenceKind: 'article',
    section: articlesSection,
    boost: 2,
    apply: referenceApply('article', article),
  }))

  if(articleRequestFailed){
    articleOptions.push({
      label: '文章加载失败，选择此项重试',
      detail: '重试',
      referenceKind: 'error',
      section: articlesSection,
      apply: reopenCompletion,
    })
  }else if(articles.length === 0){
    articleOptions.push({
      label: keyword ? '没有找到匹配的文章' : '当前没有可引用的文章',
      detail: keyword ? '继续输入可调整关键词' : '',
      referenceKind: 'empty',
      section: articlesSection,
      apply: (view) => closeCompletion(view),
    })
  }

  const navigationOptions = []
  if(currentPage > 1){
    navigationOptions.push({
      label: '上一页',
      detail: `${currentPage - 1} / ${totalPages}`,
      navigationDirection: 'previous',
      section: pageSection,
      apply: (view) => changeArticleMentionPage(view, currentPage - 1),
    })
  }
  if(currentPage < totalPages){
    navigationOptions.push({
      label: '下一页',
      detail: `${currentPage + 1} / ${totalPages}`,
      navigationDirection: 'next',
      section: pageSection,
      apply: (view) => changeArticleMentionPage(view, currentPage + 1),
    })
  }

  return {
    from,
    filter: false,
    options: [
      ...articleOptions,
      ...navigationOptions,
      ...cards.map((card) => ({
        label: card.title,
        detail: card.summary,
        summary: card.summary || '暂无摘要',
        referenceKind: 'card',
        section: cardSection,
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
          activateOnTypingDelay: 220,
          maxRenderedOptions: 20,
          icons: false,
          tooltipClass: () => 'mention-completion-tooltip',
          optionClass: (completion) => [
            completion.referenceKind ? `mention-${completion.referenceKind}-option` : '',
            completion.navigationDirection ? 'mention-navigation-option' : '',
            completion.navigationDirection ? `mention-navigation-${completion.navigationDirection}` : '',
          ].filter(Boolean).join(' '),
          addToOptions: [
            { render: completionMark, position: 10 },
            { render: completionContext, position: 70 },
          ],
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
