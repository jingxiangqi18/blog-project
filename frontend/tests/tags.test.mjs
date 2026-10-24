import test from 'node:test'
import assert from 'node:assert/strict'
import { tagAppearance, tagPalettes } from '../src/utils/tags.js'

test('tag colors are stable across pages and do not depend on database IDs', () => {
  assert.deepEqual(tagAppearance({ slug: 'custom-tag', id: 1 }), tagAppearance({ slug: 'custom-tag', id: 100 }))
  assert.deepEqual(tagAppearance({ name: '面向对象' }), tagAppearance({ name: '面向对象' }))
  assert.equal(new Set(['a', 'b', 'c', 'd', 'e', 'f'].map(slug => tagAppearance({ slug }).tone)).size, 6)
  assert.ok(tagAppearance({}).ink)
})

test('known technology labels use consistent palettes and generic semantic icons', () => {
  assert.equal(tagAppearance({ slug: 'java' }).tone, 'amber')
  assert.equal(tagAppearance({ slug: 'spring-boot' }).tone, 'green')
  assert.equal(tagAppearance({ slug: 'redis' }).tone, 'rose')
  assert.equal(tagAppearance({ slug: 'oop' }).tone, tagAppearance({ name: '面向对象' }).tone)
})

function luminance(hex) {
  return hex.slice(1).match(/../g).map(value => parseInt(value, 16) / 255).map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4).reduce((sum, value, i) => sum + value * [.2126, .7152, .0722][i], 0)
}
function contrast(a, b) { const values = [luminance(a), luminance(b)].sort((x, y) => y - x); return (values[0] + .05) / (values[1] + .05) }
test('every badge palette meets text and icon contrast thresholds', () => {
  for (const [name, palette] of Object.entries(tagPalettes)) {
    assert(contrast(palette.background, palette.ink) >= 4.5, `${name} text contrast`)
    assert(contrast(palette.solid, '#ffffff') >= 3, `${name} icon contrast`)
  }
})
