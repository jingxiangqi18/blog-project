export const tagPalettes = {
  blue: { background: '#eaf1fb', border: '#d2deee', ink: '#355980', solid: '#536f96' },
  green: { background: '#eaf3ec', border: '#cfe0d3', ink: '#365c46', solid: '#4e775c' },
  amber: { background: '#faf0df', border: '#ecddbd', ink: '#795724', solid: '#896b3d' },
  rose: { background: '#faebed', border: '#edcfd6', ink: '#8d4857', solid: '#9b5869' },
  violet: { background: '#f0ebf8', border: '#ddd3ea', ink: '#665184', solid: '#796593' },
  cyan: { background: '#e7f3f4', border: '#c9e0e3', ink: '#38666b', solid: '#4b7a80' },
}
const known = {
  java: ['amber', 'coffee'], spring: ['green', 'leaf'], springboot: ['green', 'leaf'], 'spring-boot': ['green', 'leaf'],
  redis: ['rose', 'database'], mysql: ['blue', 'database'], postgresql: ['blue', 'database'], sql: ['blue', 'database'],
  vue: ['green', 'code'], vuejs: ['green', 'code'], react: ['cyan', 'code'], typescript: ['blue', 'code'], javascript: ['amber', 'code'],
  oop: ['violet', 'braces'], '面向对象': ['violet', 'braces'], markdown: ['violet', 'code'], docker: ['cyan', 'code'],
}

export function tagAppearance(tag = {}) {
  const key = String(tag.slug || tag.name || '').trim().toLowerCase()
  const named = known[key] || known[String(tag.name || '').trim().toLowerCase()]
  let hash = 0
  for (const character of key) hash = (hash * 31 + character.codePointAt(0)) >>> 0
  const [tone, icon] = named || [Object.keys(tagPalettes)[hash % Object.keys(tagPalettes).length], 'hash']
  return { tone, icon, ...tagPalettes[tone] }
}
