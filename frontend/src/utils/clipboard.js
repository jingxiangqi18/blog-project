export async function copyCode(event) {
  const button = event.target.closest?.('[data-copy-code]')
  if (!button) return
  const text = button.closest('.code-block')?.querySelector('code')?.textContent || ''
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const selection = document.createElement('textarea')
      selection.value = text
      selection.style.cssText = 'position:fixed;left:-10000px;top:0;'
      document.body.append(selection)
      selection.select()
      try { if (!document.execCommand('copy')) throw new Error('Copy unavailable') }
      finally { selection.remove(); button.focus({ preventScroll: true }) }
    }
    button.textContent = '已复制'
  } catch {
    button.textContent = '请手动复制'
  }
  window.setTimeout(() => { if (button.isConnected) button.textContent = '复制' }, 1800)
}
