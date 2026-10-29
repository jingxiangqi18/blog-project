import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright')
const origin = process.env.BLOG_TEST_URL || 'http://127.0.0.1:5178'
const output = '/tmp/blog-publishing'
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ headless: true })
const errors = [], checks = []
const check = name => { checks.push(name); console.log(`PASS ${name}`) }

async function setup(role = 'ADMIN', options = {}) {
  const { emptyCatalog = false, newArticle = false, ...contextOptions } = options
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, ...contextOptions })
  const user = { id: role === 'ADMIN' ? 1 : 2, username: role === 'ADMIN' ? 'Jakin' : 'Linda', role }
  if (role) await context.addInitScript(user => localStorage.setItem('blog-studio-session', JSON.stringify({ token: 'fixture-only', user })), user)
  const sections = [{ id: 1, slug: 'technology', name: '技术', enabled: true }, { id: 2, slug: 'life', name: '生活', enabled: true }]
  const categories = [{ id: 1, name: 'Java', sectionId: null }, { id: 2, name: 'Spring Boot', sectionId: 1 }, { id: 3, name: '随笔', sectionId: 2 }]
  const tags = [{ id: 1, name: 'Java', slug: 'java' }, { id: 2, name: 'Spring Boot', slug: 'spring-boot' }]
  const series = [{ id: 1, title: '后端实践', slug: 'backend', sectionId: 1, sectionName: '技术', createdBy: 1 }]
  if (emptyCatalog) { sections.length = 0; categories.length = 0; tags.length = 0; series.length = 0 }
  const article = { id: 1, authorId: 1, authorName: 'Jakin', title: 'Spring Boot 请求链路', content: '## 从一次请求开始\n\n理解 Controller、Service 与 Repository 的边界。\n\n```java\n@GetMapping("/articles")\npublic List<Article> list() {\n  return articleService.list();\n}\n```\n\n> 保持清晰，保持简单。\n\n' + '把每次学习整理为可以再次阅读的笔记。\n\n'.repeat(15), categoryId: 1, sectionId: 1, sectionSlug: 'technology', sectionName: '技术', tagIds: [1], tags: [tags[0]], summary: '从一次请求理解后端的分层职责。', contentType: 'general', createdAt: '2026-10-25T12:00:00', updatedAt: '2026-10-25T12:00:00' }
  const writes = []
  let delayCreate = false
  await context.route('**/api/**', async route => {
    const request = route.request(), url = new URL(request.url()), path = url.pathname, method = request.method()
    if (!path.startsWith('/api/')) return route.continue()
    const body = request.postData() ? request.postDataJSON() : null
    if (method !== 'GET') writes.push({ path, method, body })
    let result
    if (path === '/api/auth/me') result = user
    else if (['/api/sections', '/api/categories', '/api/tags', '/api/series'].includes(path) && method === 'POST') {
      if (path !== '/api/series' && role !== 'ADMIN') return route.fulfill({ status: 403, json: { message: 'Forbidden' } })
      if (delayCreate) await new Promise(resolve => setTimeout(resolve, 700))
      if (body.slug === 'taken') return route.fulfill({ status: 409, json: { message: 'Slug already exists' } })
      const list = { '/api/sections': sections, '/api/categories': categories, '/api/tags': tags, '/api/series': series }[path]
      result = { ...body, id: list.length + 1, ...(path === '/api/series' ? { createdBy: user.id, sectionName: sections.find(item => item.id === body.sectionId)?.name } : {}) }
      list.push(result)
    } else if (path === '/api/sections') result = sections
    else if (path === '/api/categories') result = categories
    else if (path === '/api/tags') result = tags
    else if (path === '/api/series') result = { content: series.filter(item => !url.searchParams.get('keyword') || item.title.includes(url.searchParams.get('keyword'))), totalElements: series.length, totalPages: 1 }
    else if (path === '/api/articles/1') result = method === 'PUT' ? { ...article, ...body } : article
    else if (path === '/api/articles/1/series') result = []
    else if (path === '/api/articles/1/draft' && method === 'GET') return route.fulfill({ status: 204 })
    else if (path === '/api/articles/1/draft' || path === '/api/articles/drafts' || path.startsWith('/api/articles/drafts/')) result = { id: 90, ...body, updatedAt: '2026-10-26T09:00:00' }
    else if (path === '/api/articles') result = { content: [article], totalElements: 1, totalPages: 1 }
    else result = []
    return route.fulfill({ json: result })
  })
  const page = await context.newPage()
  page.on('pageerror', error => errors.push(error.message))
  page.on('dialog', dialog => dialog.accept())
  await page.goto(origin + '/#' + (role === 'ADMIN' && !newArticle ? '/articles/1/edit' : '/articles/new'))
  await page.locator('.cm-content').waitFor().catch(async error => {
    console.log(JSON.stringify({ errors, body: (await page.locator('body').innerText()).slice(0, 2000) }))
    await page.screenshot({ path: output + '/setup-failure.png' })
    throw error
  })
  return { page, context, writes, setDelay: value => { delayCreate = value } }
}
const dialog = page => page.locator('.metadata-create-dialog')
const nameInput = page => dialog(page).getByRole('textbox', { name: '新建名称', exact: true })
const submit = page => dialog(page).getByRole('button', { name: '创建并选中', exact: true })
async function noOverflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 2))
}
try {
  const { page, context, writes, setDelay } = await setup()
  const initialContent = await page.locator('.cm-content').innerText()
  const initialTitle = await page.getByPlaceholder('输入文章标题').inputValue()
  await page.getByPlaceholder('输入文章标题').press('Enter')
  assert.match(page.url(), /articles\/1\/edit/)
  assert.equal(writes.some(item => item.path === '/api/articles/1' && item.method === 'PUT'), false)
  assert.equal(await page.getByRole('button', { name: '新建标签', exact: true }).count(), 1)
  assert.equal(await page.locator('.publish-details-trigger').getAttribute('aria-expanded'), 'false')
  await page.locator('.publish-details-trigger').click()
  await page.waitForTimeout(90)
  const during = await page.locator('.publish-details-expansion').evaluate(el => el.getBoundingClientRect().height)
  await page.waitForTimeout(450)
  const after = await page.locator('.publish-details-expansion').evaluate(el => el.getBoundingClientRect().height)
  assert(during < after && during > 0)
  assert.equal(await page.getByRole('textbox', { name: '文章摘要', exact: true }).inputValue(), '从一次请求理解后端的分层职责。')
  await page.locator('.publish-details-trigger').click()
  check('details expand continuously and preserve values')

  await page.getByRole('combobox', { name: '文章标签', exact: true }).fill('Redis')
  await page.locator('.publish-select-menu:visible').getByRole('button', { name: '新建“Redis”', exact: true }).click()
  await nameInput(page).waitFor()
  assert.equal(await nameInput(page).inputValue(), 'Redis')
  assert.equal(await dialog(page).getByRole('textbox', { name: '链接标识', exact: true }).inputValue(), 'redis')
  await page.waitForTimeout(450)
  await page.screenshot({ path: output + '/create-tag-desktop.png' })
  await submit(page).click()
  await dialog(page).waitFor({ state: 'hidden' })
  await page.getByRole('button', { name: '移除标签 Redis', exact: true }).waitFor()
  assert.equal(writes.filter(item => item.path === '/api/tags').length, 1)
  assert.deepEqual(writes.find(item => item.path === '/api/tags').body, { name: 'Redis', slug: 'redis', description: null })
  await page.getByRole('button', { name: '移除标签 Redis', exact: true }).click()
  await page.getByRole('button', { name: '移除标签 Redis', exact: true }).waitFor({ state: 'hidden' })
  assert.equal(await page.getByRole('button', { name: '移除标签 Redis', exact: true }).count(), 0)
  check('create from tag search, select colored badge and remove')

  await page.getByRole('button', { name: '新建分类', exact: true }).click()
  await nameInput(page).fill('数据访问')
  await submit(page).click()
  await dialog(page).waitFor({ state: 'hidden' })
  assert.equal(writes.find(item => item.path === '/api/categories').body.sectionId, 1)
  assert.match(await page.locator('.publish-field').filter({ has: page.getByRole('combobox', { name: '文章分类', exact: true }) }).innerText(), /数据访问/)
  await page.getByRole('button', { name: '新建分类', exact: true }).click()
  await nameInput(page).fill('数据访问')
  await dialog(page).getByRole('button', { name: '使用已有分类', exact: true }).click()
  await dialog(page).waitFor({ state: 'hidden' })
  assert.equal(writes.filter(item => item.path === '/api/categories').length, 1)
  check('create category in current section and reuse duplicates without POST')

  await page.getByRole('button', { name: '新建分区', exact: true }).click()
  await nameInput(page).fill('架构')
  await dialog(page).getByRole('textbox', { name: '链接标识', exact: true }).fill('architecture')
  await submit(page).click()
  await dialog(page).waitFor({ state: 'hidden' })
  assert.equal(writes.find(item => item.path === '/api/sections').body.enabled, true)
  assert.match(await page.locator('.publish-field').first().innerText(), /架构/)
  assert.doesNotMatch(await page.locator('.publish-field').nth(1).innerText(), /数据访问/)
  assert.equal(await page.locator('.publish-field.is-error').count(), 0)
  await page.getByRole('button', { name: '文章分区', exact: true }).click()
  await page.locator('.navigation-dropdown:visible').getByRole('menuitem', { name: '架构', exact: true }).waitFor()
  await page.keyboard.press('Escape')
  check('create section, clear incompatible category and refresh navigation')

  await page.getByRole('button', { name: '新建系列', exact: true }).click()
  await nameInput(page).fill('架构实践')
  await submit(page).click()
  await dialog(page).waitFor({ state: 'hidden' })
  assert.equal(writes.find(item => item.path === '/api/series').body.sectionId, 3)
  assert.match(await page.locator('.publish-field').nth(2).innerText(), /架构实践/)
  assert.equal(await page.locator('.cm-content').innerText(), initialContent)
  assert.equal(await page.getByPlaceholder('输入文章标题').inputValue(), initialTitle)
  check('create and select compatible series without losing article text')

  await page.getByRole('button', { name: '新建标签', exact: true }).click()
  await submit(page).click()
  await dialog(page).getByText('请输入名称', { exact: true }).waitFor()
  await nameInput(page).fill('Duplicate slug')
  await dialog(page).getByRole('textbox', { name: '链接标识', exact: true }).fill('taken')
  await submit(page).click()
  await dialog(page).getByText('链接标识已存在，请修改后重试', { exact: true }).waitFor()
  assert.equal(await nameInput(page).inputValue(), 'Duplicate slug')
  await nameInput(page).fill('Conflict recovered')
  assert.equal(await dialog(page).getByRole('textbox', { name: '链接标识', exact: true }).inputValue(), 'taken')
  await dialog(page).getByRole('textbox', { name: '链接标识', exact: true }).fill('conflict-recovered')
  setDelay(true)
  await submit(page).click()
  await page.keyboard.press('Escape')
  assert(await dialog(page).isVisible())
  await dialog(page).waitFor({ state: 'hidden' })
  setDelay(false)
  check('validation, conflict recovery, edited slug retention and pending-close guard')

  for (const width of [1440, 1024, 768, 390]) {
    await page.setViewportSize({ width, height: 900 })
    await page.locator('.editor-side-panel').scrollIntoViewIfNeeded()
    await page.waitForTimeout(450)
    await noOverflow(page)
    await page.screenshot({ path: `${output}/editor-${width}.png` })
    await page.getByRole('button', { name: '新建标签', exact: true }).click()
    await nameInput(page).fill('Vue.js')
    await page.waitForTimeout(450)
    assert(await dialog(page).evaluate(el => { const rect = el.getBoundingClientRect(); return rect.left >= 0 && rect.right <= innerWidth && rect.top >= 0 && rect.bottom <= innerHeight }))
    await page.screenshot({ path: `${output}/dialog-${width}.png` })
    await page.keyboard.press('Escape')
    await dialog(page).waitFor({ state: 'hidden' })
    assert.equal(await page.getByRole('button', { name: '新建标签', exact: true }).evaluate(el => el === document.activeElement), true)
  }
  await page.setViewportSize({ width: 1440, height: 600 })
  await page.locator('.editor-side-panel').scrollIntoViewIfNeeded()
  assert(await page.locator('.publish-panel-footer').evaluate(el => { const rect = el.getBoundingClientRect(); return rect.top >= 0 && rect.bottom <= innerHeight }))
  await page.setViewportSize({ width: 390, height: 568 })
  await page.getByRole('button', { name: '新建系列', exact: true }).click()
  await nameInput(page).waitFor()
  await page.waitForTimeout(450)
  assert(await dialog(page).evaluate(el => { const rect = el.getBoundingClientRect(); return rect.top >= 0 && rect.bottom <= innerHeight }))
  assert(await dialog(page).locator('.el-dialog__footer').evaluate(el => el.getBoundingClientRect().bottom <= innerHeight))
  await page.keyboard.press('Escape')
  await dialog(page).waitFor({ state: 'hidden' })
  check('desktop, tablet, mobile, short viewport and keyboard focus')
  await context.close()

  for (const role of ['USER', null]) {
    const { page, context } = await setup(role)
    for (const name of ['新建分类', '新建标签', '新建分区']) assert.equal(await page.getByRole('button', { name, exact: true }).count(), 0)
    assert.equal(await page.getByRole('button', { name: '新建系列', exact: true }).count(), role ? 1 : 0)
    if (role) {
      await page.getByRole('button', { name: '新建系列', exact: true }).click()
      await nameInput(page).fill('Linda 的学习路径')
      await submit(page).click()
      await dialog(page).getByText('请选择系列所属分区', { exact: true }).waitFor()
      await dialog(page).getByRole('combobox', { name: '新建内容所属分区', exact: true }).focus()
      await dialog(page).getByRole('combobox', { name: '新建内容所属分区', exact: true }).press('ArrowDown')
      await page.getByRole('option', { name: '技术', exact: true }).click()
      await submit(page).click()
      await dialog(page).waitFor({ state: 'hidden' })
      assert.match(await page.locator('.publish-field').first().innerText(), /技术/)
      assert.match(await page.locator('.publish-field').nth(2).innerText(), /Linda 的学习路径/)
    }
    await context.close()
  }
  check('non-admin and signed-out users never render forbidden creation controls')

  const empty = await setup('ADMIN', { emptyCatalog: true, newArticle: true })
  await empty.page.getByRole('button', { name: '新建分类', exact: true }).click()
  await nameInput(empty.page).fill('第一份笔记')
  await submit(empty.page).click()
  await dialog(empty.page).waitFor({ state: 'hidden' })
  assert.deepEqual(empty.writes.find(item => item.path === '/api/categories').body, { name: '第一份笔记', sectionId: null })
  assert.match(await empty.page.locator('.publish-field').nth(1).innerText(), /第一份笔记/)
  await empty.context.close()
  check('create the first category without leaving an empty editor')

  const reduced = await setup('ADMIN', { reducedMotion: 'reduce' })
  await reduced.page.getByRole('button', { name: '新建标签', exact: true }).click()
  await nameInput(reduced.page).waitFor()
  await reduced.page.waitForTimeout(100)
  assert(await dialog(reduced.page).evaluate(el => el.getAnimations({ subtree: true }).every(animation => animation.effect.getTiming().duration <= 1)))
  await reduced.context.close()
  check('reduced-motion preference')
  assert.deepEqual(errors, [])
  console.log(JSON.stringify({ checks: checks.length, javascriptErrors: errors, screenshots: output }))
} finally { await browser.close() }
