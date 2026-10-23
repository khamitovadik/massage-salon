import { useEffect, useState } from 'react'
import { Moon, Sun } from 'lucide-react'
import { currentTheme, setTheme } from '../utils/theme'

export default function ThemeToggle({ className = 'icon-btn' }: { className?: string }) {
  const [theme, setState] = useState(currentTheme)

  useEffect(() => {
    const sync = () => setState(currentTheme())
    window.addEventListener('themechange', sync)
    return () => window.removeEventListener('themechange', sync)
  }, [])

  const next = theme === 'dark' ? 'light' : 'dark'
  return (
    <button
      type="button"
      className={className}
      onClick={() => setTheme(next)}
      aria-label={next === 'dark' ? 'Включить ночную тему' : 'Включить дневную тему'}
      title={next === 'dark' ? 'Ночная тема' : 'Дневная тема'}
    >
      {theme === 'dark' ? <Sun size={18} strokeWidth={1.75} /> : <Moon size={18} strokeWidth={1.75} />}
    </button>
  )
}
