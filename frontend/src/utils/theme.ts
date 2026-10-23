// Светлая тема «Мокко и камень» и ночная «Мягкие сумерки».
// Пока пользователь сам не выбрал тему — следуем системной (тёмный режим телефона).
export type Theme = 'light' | 'dark'

const KEY = 'theme'
const META_COLOR: Record<Theme, string> = { light: '#efe9e2', dark: '#23211f' }
const media = window.matchMedia('(prefers-color-scheme: dark)')

function stored(): Theme | null {
  try {
    const t = localStorage.getItem(KEY)
    return t === 'light' || t === 'dark' ? t : null
  } catch {
    return null
  }
}

export function currentTheme(): Theme {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

function apply(theme: Theme) {
  document.documentElement.dataset.theme = theme
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', META_COLOR[theme])
  window.dispatchEvent(new Event('themechange'))
}

export function setTheme(theme: Theme) {
  try { localStorage.setItem(KEY, theme) } catch { /* приватный режим — тема просто не запомнится */ }
  apply(theme)
}

export function initTheme() {
  apply(stored() ?? (media.matches ? 'dark' : 'light'))
  media.addEventListener('change', e => { if (!stored()) apply(e.matches ? 'dark' : 'light') })
}
