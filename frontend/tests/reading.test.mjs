import test from 'node:test'
import assert from 'node:assert/strict'
import { renderMarkdownDocument } from '../src/utils/markdown.js'
import { buildSeriesTree, flattenSeriesArticles } from '../src/utils/series.js'

test('heading IDs are stable, unique and safe with Chinese, punctuation and collisions', () => {
  const source = '## Java 基础\n## Java 基础\n## java-基础-2\n### `JVM` & JDK\n#### Java 基础'
  const first = renderMarkdownDocument(source)
  assert.deepEqual(first.headings, renderMarkdownDocument(source).headings)
  assert.equal(new Set(first.headings.map(h => h.id)).size, 5)
  assert.deepEqual(first.headings.map(h => h.level), [2, 2, 2, 3, 4])
  assert.match(first.html, /id="java-基础"/)
  assert.match(first.headings[3].title, /JVM/)
})

test('rendering retains source lines, safe references and escaped code', () => {
  const result = renderMarkdownDocument('## Title\n\n[Article](article:12) [Card](card:4)\n\n```js\nconst value = "<script>"\n```')
  assert.match(result.html, /data-source-line="0"/)
  assert.match(result.html, /data-article-id="12"/)
  assert.match(result.html, /data-knowledge-card-id="4"/)
  assert.match(result.html, /data-copy-code/)
  assert.doesNotMatch(result.html, /<script>/)
})

test('series traversal follows nested group order then article order and retains ungrouped entries', () => {
  const tree = buildSeriesTree({ chapters: [{id:2,parentId:1,title:'Child',sortOrder:1},{id:1,parentId:null,title:'Root',sortOrder:1}], articles: [{id:10,chapterId:2,sortOrder:2},{id:11,chapterId:2,sortOrder:1},{id:12,chapterId:null,sortOrder:3}] })
  assert.equal(tree[0].children[0].id, 2)
  assert.deepEqual(flattenSeriesArticles(tree).map(a => a.id), [11, 10, 12])
  assert.deepEqual(buildSeriesTree(null), [])
})

test('only a repeated leading H1 is marked for display suppression, without altering source', () => {
  const source = '# `Java` 基础\n\n## 章节\n\n# Java 基础'
  const normal = renderMarkdownDocument(source)
  const reader = renderMarkdownDocument(source, { title: 'Java 基础' })
  assert.doesNotMatch(normal.html, /markdown-title-echo/)
  assert.equal((reader.html.match(/markdown-title-echo/g) || []).length, 1)
  assert.deepEqual(reader.headings, normal.headings)
  assert.doesNotMatch(renderMarkdownDocument(source, { title: '其他标题' }).html, /markdown-title-echo/)
  assert.equal(source, '# `Java` 基础\n\n## 章节\n\n# Java 基础')
})
