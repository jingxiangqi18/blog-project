import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

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
const defaultLinkOpenRenderer = markdown.renderer.rules.link_open

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

  if (/^https?:\/\//i.test(href)) {
    tokens[index].attrSet('target', '_blank')
    tokens[index].attrSet('rel', 'noopener noreferrer')
  }

  return defaultLinkOpenRenderer(tokens, index, options, env, self)
}

export function renderMarkdown(content) {
  return markdown.render(content || '')
}

export function markdownToPlainText(content) {
  if (!content) {
    return ''
  }

  const container = document.createElement('div')
  container.innerHTML = renderMarkdown(content)
  return (container.textContent || '').replace(/\s+/g, ' ').trim()
}
