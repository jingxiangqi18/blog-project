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
