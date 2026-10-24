<template>
  <div ref="editorHost" class="markdown-mention-editor"></div>
</template>

<script setup>
import { createApp, h, onBeforeUnmount, onMounted, reactive, ref, useId, watch } from 'vue'
import ReferencePicker from './ReferencePicker.vue'
import { autocompletion } from '@codemirror/autocomplete'
import { markdown } from '@codemirror/lang-markdown'
import { EditorState, Prec, RangeSetBuilder, StateEffect, StateField } from '@codemirror/state'
import { Decoration, EditorView, ViewPlugin, WidgetType, keymap, placeholder as editorPlaceholder, showTooltip, tooltips } from '@codemirror/view'
import { basicSetup } from 'codemirror'

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

const referencePattern = /\[([^\]\n]+)\]\((article|card):(\d+)\)/g
const pickerId = `reference-results-${useId()}`
let pickerInstance = null, pickerHost = null
const dismissReference = StateEffect.define()
const referenceDismissed = StateField.define({
  create: () => false,
  update(value, transaction) {
    for (const effect of transaction.effects) if (effect.is(dismissReference)) return effect.value
    return transaction.docChanged ? false : value
  },
})

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

function mentionContext(state) {
  const selection = state.selection.main
  if (!selection.empty) return null
  const line = state.doc.lineAt(selection.head)
  const match = state.sliceDoc(line.from, selection.head).match(/@([^@\n]*)$/u)
  return match ? { from: selection.head - match[0].length, to: selection.head, keyword: match[1].trim() } : null
}

function closeReference(focus = true) {
  if (!editorView) return
  editorView.dispatch({ effects: dismissReference.of(true) })
  if (focus) editorView.focus()
}

function insertReference({ type, item }) {
  const context = editorView && mentionContext(editorView.state)
  if (!context) { closeReference(); return }
  const reference = `[${escapeReferenceLabel(item.title)}](${type}:${item.id})`
  editorView.dispatch({
    changes: { from: context.from, to: context.to, insert: reference },
    selection: { anchor: context.from + reference.length },
    effects: dismissReference.of(true),
  })
  editorView.focus()
}

// CodeMirror owns anchoring and viewport collision; Vue owns the picker surface.
function createReferenceTooltip(view) {
  const dom = document.createElement('div')
  dom.className = 'reference-picker-host'
  dom.style.maxWidth = `${document.documentElement.clientWidth - 32}px`
  pickerHost = dom
  const context = reactive({ keyword: mentionContext(view.state)?.keyword || '' })
  const app = createApp({
    render: () => h(ReferencePicker, {
      keyword: context.keyword,
      currentArticleId: props.currentArticleId,
      listId: pickerId,
      ref: instance => { pickerInstance = instance },
      onChoose: insertReference,
      onClose: () => closeReference(),
      onActive: id => {
        if (id) view.contentDOM.setAttribute('aria-activedescendant', id)
        else view.contentDOM.removeAttribute('aria-activedescendant')
      },
    }),
  })
  app.config.idPrefix = pickerId
  view.contentDOM.setAttribute('aria-autocomplete', 'list')
  view.contentDOM.setAttribute('aria-controls', pickerId)
  view.contentDOM.setAttribute('aria-expanded', 'true')
  app.mount(dom)
  return {
    dom,
    offset: { x: 0, y: 8 },
    positioned(space) { dom.style.maxWidth = `${Math.max(0, space.right - space.left)}px` },
    update(update) {
      const match = mentionContext(update.state)
      if (match) context.keyword = match.keyword
    },
    destroy() {
      app.unmount()
      pickerInstance = null; pickerHost = null
      for (const name of ['aria-autocomplete', 'aria-controls', 'aria-expanded', 'aria-activedescendant']) view.contentDOM.removeAttribute(name)
    },
  }
}

const referenceTooltip = showTooltip.compute([referenceDismissed, 'doc', 'selection'], state => {
  const context = mentionContext(state)
  return !state.field(referenceDismissed) && context ? { pos: context.from, end: context.to, create: createReferenceTooltip } : null
})

function closeReferenceOutside(event) {
  if (pickerHost && !pickerHost.contains(event.target) && !editorView?.dom.contains(event.target)) closeReference(false)
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
        autocompletion({ override: [] }),
        referenceDismissed,
        referenceTooltip,
        tooltips({ parent: document.body, position: 'fixed', tooltipSpace: () => ({ left: 16, top: 90, right: document.documentElement.clientWidth - 16, bottom: window.innerHeight - 16 }) }),
        Prec.highest(keymap.of(['ArrowDown', 'ArrowUp', 'Home', 'End', 'PageDown', 'PageUp', 'Enter', 'Escape', 'Tab'].map(key => ({
          key,
          run: () => pickerInstance?.handleKey(key) || false,
        })))),
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

  document.addEventListener('pointerdown', closeReferenceOutside)
  document.addEventListener('focusin', closeReferenceOutside)
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
  document.removeEventListener('pointerdown', closeReferenceOutside)
  document.removeEventListener('focusin', closeReferenceOutside)
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
