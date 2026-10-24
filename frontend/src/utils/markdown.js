import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight(code, language) {
    const validLanguage = language && hljs.getLanguage(language)

    if (validLanguage) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(code, { language }).value}</code></pre>`
      } catch {
        return ''
      }
    }

    return `<pre class="hljs"><code>${markdown.utils.escapeHtml(code)}</code></pre>`
  },
})

const colorSpanOpenPattern = /^<span\s+style\s*=\s*["'“”‘’]\s*color\s*:\s*(#(?:[0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8}))\s*;?\s*["'“”‘’]\s*>/i
const colorSpanClosePattern = /^<\/span\s*>/i

function renderSafeColorSpan(state, silent) {
  const source = state.src.slice(state.pos)
  const openingTag = source.match(colorSpanOpenPattern)

  if (openingTag) {
    if (!silent) {
      const token = state.push('safe_color_span_open', 'span', 1)
      token.meta = { color: openingTag[1] }
      state.safeColorSpanDepth = (state.safeColorSpanDepth || 0) + 1
    }

    state.pos += openingTag[0].length
    return true
  }

  const closingTag = source.match(colorSpanClosePattern)

  if (!closingTag || !state.safeColorSpanDepth) {
    return false
  }

  if (!silent) {
    state.push('safe_color_span_close', 'span', -1)
    state.safeColorSpanDepth -= 1
  }

  state.pos += closingTag[0].length
  return true
}

markdown.inline.ruler.before('text', 'safe_color_span', renderSafeColorSpan)
markdown.renderer.rules.safe_color_span_open = (tokens, index) => {
  return `<span class="markdown-color-text" style="color: ${tokens[index].meta.color}">`
}
markdown.renderer.rules.safe_color_span_close = () => '</span>'

const defaultImageRenderer = markdown.renderer.rules.image
const defaultLinkOpenRenderer = markdown.renderer.rules.link_open || ((tokens, index, options, env, self) => {
  return self.renderToken(tokens, index, options)
})

markdown.core.ruler.after('block', 'source_line_marker', (state) => {
  state.tokens.forEach((token) => {
    if (!token.map || token.nesting === -1) {
      return
    }

    token.attrSet('data-source-line', String(token.map[0]))
  })
})

markdown.renderer.rules.image = (tokens, index, options, env, self) => {
  tokens[index].attrSet('loading', 'lazy')
  tokens[index].attrJoin('class', 'markdown-image')

  return defaultImageRenderer(tokens, index, options, env, self)
}

markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  const hrefIndex = tokens[index].attrIndex('href')
  const href = hrefIndex >= 0 ? tokens[index].attrs[hrefIndex][1] : ''

  const articleMatch = href.match(/^article:(\d+)$/)
  if (articleMatch) {
    tokens[index].attrSet('href', `#/articles/${articleMatch[1]}`)
    tokens[index].attrSet('data-article-id', articleMatch[1])
    tokens[index].attrJoin('class', 'internal-article-link')
    return defaultLinkOpenRenderer(tokens, index, options, env, self)
  }

  const cardMatch = href.match(/^card:(\d+)$/)
  if (cardMatch) {
    tokens[index].attrSet('href', `#knowledge-card-${cardMatch[1]}`)
    tokens[index].attrSet('data-knowledge-card-id', cardMatch[1])
    tokens[index].attrSet('role', 'button')
    tokens[index].attrJoin('class', 'knowledge-card-link')
    return defaultLinkOpenRenderer(tokens, index, options, env, self)
  }

  if (/^https?:\/\//i.test(href)) {
    tokens[index].attrSet('target', '_blank')
    tokens[index].attrSet('rel', 'noopener noreferrer')
  }

  return defaultLinkOpenRenderer(tokens, index, options, env, self)
}

export function renderMarkdownDocument(content, { title: pageTitle } = {}) {
  const env = {}
  const tokens = markdown.parse(content || '', env)
  const used = new Set()
  const headings = []
  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return
    const inline = tokens[index + 1]
    const title = (inline.children || []).map(child => child.type === 'image' ? child.content : child.type === 'text' || child.type === 'code_inline' ? child.content : '').join('') || inline.content
    if (index === 0 && token.tag === 'h1' && pageTitle?.trim() === title.trim()) token.attrJoin('class', 'markdown-title-echo')
    const base = title.trim().toLowerCase().replace(/[^\p{L}\p{N}\s_-]/gu, '').replace(/\s+/g, '-') || 'heading'
    let id = base, suffix = 2
    while (used.has(id)) id = `${base}-${suffix++}`
    used.add(id)
    token.attrSet('id', id)
    headings.push({ id, title, level: Number(token.tag.slice(1)), line: token.map?.[0] || 0 })
  })
  return { html: markdown.renderer.render(tokens, markdown.options, env), headings }
}

const defaultFence = markdown.renderer.rules.fence
markdown.renderer.rules.fence = (tokens, index, options, env, self) => {
  const language = markdown.utils.escapeHtml(tokens[index].info.trim().split(/\s+/)[0] || 'text')
  return `<div class="code-block" data-source-line="${tokens[index].map?.[0] || 0}"><div class="code-toolbar"><span>${language}</span><button type="button" data-copy-code aria-label="复制代码">复制</button></div>${defaultFence(tokens, index, options, env, self)}</div>`
}

export function renderMarkdown(content) {
  return renderMarkdownDocument(content).html
}

export function markdownToPlainText(content, options) {
  if (!content) {
    return ''
  }

  const container = document.createElement('div')
  container.innerHTML = renderMarkdownDocument(content, options).html
  container.querySelectorAll('.code-toolbar, .markdown-title-echo').forEach(element => element.remove())
  return (container.textContent || '').replace(/\s+/g, ' ').trim()
}
