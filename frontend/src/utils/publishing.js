const slugPattern = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/

export function suggestMetadataSlug(name, fallback) {
  const value = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 100).replace(/-+$/g, '')
  return value ? (/^[a-z]/.test(value) ? value : `note-${value}`.slice(0, 100).replace(/-+$/g, '')) : fallback
}

export function metadataCreationError(kind, form) {
  if (!form.name.trim()) return '请输入名称'
  if (form.name.trim().length > (kind === 'series' ? 200 : 100)) return '名称过长，请缩短后重试'
  if (kind !== 'category' && (!slugPattern.test(form.slug.trim()) || form.slug.trim().length > 100)) return '链接标识需以小写字母开头，只能包含小写字母、数字和连字符'
  if (kind === 'series' && !form.sectionId) return '请选择系列所属分区'
  return ''
}

export function metadataCreationPayload(kind, form) {
  if (kind === 'category') return { name: form.name.trim(), sectionId: form.sectionId || null }
  const common = { slug: form.slug.trim(), description: form.description.trim() || null }
  if (kind === 'series') return { ...common, title: form.name.trim(), sectionId: form.sectionId, categoryId: form.categoryId || null }
  return { ...common, name: form.name.trim(), ...(kind === 'section' ? { sortOrder: 0, enabled: true } : {}) }
}
