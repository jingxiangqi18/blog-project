import assert from 'node:assert/strict'
import { test } from 'node:test'
import { metadataCreationError, metadataCreationPayload, suggestMetadataSlug } from '../src/utils/publishing.js'

test('metadata slugs fit the existing backend contract', () => {
  assert.equal(suggestMetadataSlug('Spring Boot + Java 21', 'tag-fallback'), 'spring-boot-java-21')
  assert.equal(suggestMetadataSlug('Café', 'tag-fallback'), 'cafe')
  assert.equal(suggestMetadataSlug('123 SQL', 'tag-fallback'), 'note-123-sql')
  assert.equal(suggestMetadataSlug('后端学习', 'tag-fallback'), 'tag-fallback')
  assert.equal(suggestMetadataSlug('   ', 'tag-fallback'), 'tag-fallback')
  const longSlug = suggestMetadataSlug('A '.repeat(101), 'tag-fallback')
  assert(longSlug.length <= 100)
  assert.match(longSlug, /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/)
})

test('creation validation respects kind-specific required fields', () => {
  const form = { name: 'Java', slug: 'java', sectionId: null, description: '', categoryId: null }
  assert.equal(metadataCreationError('tag', form), '')
  assert.equal(metadataCreationError('category', { ...form, slug: '' }), '')
  assert(metadataCreationError('series', form))
  assert.equal(metadataCreationError('series', { ...form, sectionId: 1 }), '')
  for (const slug of ['Java', '-java', 'java--boot', 'java_', 'java-', '9java', 'a'.repeat(101)]) {
    assert(metadataCreationError('tag', { ...form, slug }), slug)
  }
  assert(metadataCreationError('category', { ...form, name: ' '.repeat(3) }))
  assert(metadataCreationError('tag', { ...form, name: 'a'.repeat(101) }))
  assert.equal(metadataCreationError('series', { ...form, name: 'a'.repeat(200), sectionId: 1 }), '')
})

test('creation payloads use existing APIs without changing article content', () => {
  const form = { name: ' Java ', slug: ' java ', description: '  ', sectionId: 2, categoryId: 3 }
  assert.deepEqual(metadataCreationPayload('category', form), { name: 'Java', sectionId: 2 })
  assert.deepEqual(metadataCreationPayload('tag', form), { name: 'Java', slug: 'java', description: null })
  assert.deepEqual(metadataCreationPayload('section', form), { name: 'Java', slug: 'java', description: null, sortOrder: 0, enabled: true })
  assert.deepEqual(metadataCreationPayload('series', form), { title: 'Java', slug: 'java', description: null, sectionId: 2, categoryId: 3 })
  assert.equal(metadataCreationPayload('category', { ...form, sectionId: null }).sectionId, null)
})
