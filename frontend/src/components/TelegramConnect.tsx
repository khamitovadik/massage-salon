import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Send, ShieldCheck } from 'lucide-react'
import { createTelegramLink, getMe, type Me } from '../api/users'

/**
 * Карточка на главной: подключить Telegram и подтвердить номер.
 * Пока человек в боте, опрашиваем статус, чтобы карточка сама сменилась на «готово».
 */
export default function TelegramConnect() {
  const [me, setMe] = useState<Me | null>(null)
  const [waiting, setWaiting] = useState(false)
  const [opening, setOpening] = useState(false)

  useEffect(() => { getMe().then(setMe).catch(() => {}) }, [])

  useEffect(() => {
    if (!waiting) return
    const t = setInterval(() => {
      getMe().then(m => {
        setMe(m)
        if (m.telegramLinked && m.phoneVerified) {
          setWaiting(false)
          toast.success('Telegram подключён, номер подтверждён')
        }
      }).catch(() => {})
    }, 4000)
    // Через 10 минут перестаём опрашивать — ссылка к тому времени может устареть
    const stop = setTimeout(() => setWaiting(false), 600_000)
    return () => { clearInterval(t); clearTimeout(stop) }
  }, [waiting])

  async function connect() {
    // Окно открываем сразу по клику, иначе браузер заблокирует его как всплывающее
    const win = window.open('', '_blank')
    setOpening(true)
    try {
      const { url } = await createTelegramLink()
      if (win) win.location.href = url
      else window.location.href = url
      setWaiting(true)
    } catch (err: any) {
      win?.close()
      toast.error(err.response?.data?.error || 'Не удалось получить ссылку на бота')
    } finally {
      setOpening(false)
    }
  }

  if (!me || (me.telegramLinked && me.phoneVerified)) return null

  const linkedOnly = me.telegramLinked && !me.phoneVerified
  return (
    <div className="card mb-4 tg-connect">
      <div className="tg-connect-icon">{linkedOnly ? <ShieldCheck size={22} /> : <Send size={22} />}</div>
      <div style={{ flex: 1, minWidth: 200 }}>
        <div style={{ fontWeight: 600 }}>
          {linkedOnly ? 'Подтвердите номер телефона' : 'Подключите Telegram'}
        </div>
        <div className="text-muted text-sm">
          {linkedOnly
            ? 'Откройте бота и нажмите «Поделиться номером» — так мы убедимся, что номер ваш.'
            : 'Напоминания о записи, подтверждения и новости салона будут приходить в Telegram. Займёт 10 секунд.'}
        </div>
        {waiting && <div className="text-sm mt-2">Ждём подтверждения в Telegram…</div>}
      </div>
      <button className="btn btn-primary" onClick={connect} disabled={opening}>
        {opening ? 'Открываем…' : linkedOnly ? 'Открыть бота' : 'Подключить Telegram'}
      </button>
    </div>
  )
}
