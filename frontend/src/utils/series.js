export function buildSeriesTree(directory) {
  const groups = new Map((directory?.chapters || []).map(chapter => [chapter.id, { ...chapter, type: 'group', children: [] }]))
  const roots = []
  const sort = (a, b) => a.sortOrder - b.sortOrder || a.id - b.id
  for (const group of [...groups.values()].sort(sort)) {
    const parent = groups.get(group.parentId)
    if (parent) parent.children.push(group)
    else roots.push(group)
  }
  for (const article of [...(directory?.articles || [])].sort(sort)) {
    const parent = groups.get(article.chapterId)
    const node = { ...article, type: 'article' }
    if (parent) parent.children.push(node)
    else roots.push(node)
  }
  return roots
}

export function flattenSeriesArticles(nodes) {
  return nodes.flatMap(node => node.type === 'article' ? [node] : flattenSeriesArticles(node.children))
}
