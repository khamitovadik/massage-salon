import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Clock, Globe, Hand, Instagram, Leaf, MapPin, Phone, Sparkles, Timer, Users } from 'lucide-react'
import api from '../api/client'
import Logo from '../components/Logo'
import { getAuth } from '../store/auth'
import type { SalonService } from '../types'

interface PublicSettings {
  name?: string
  address?: string
  phone?: string
  description?: string
  instagramUrl?: string
  workingHours?: string
}

const DEFAULT_DESCRIPTION =
  'Em.Del.Relaxx — место, где можно выдохнуть. Классический, расслабляющий и спортивный массаж ' +
  'от опытных мастеров в спокойной атмосфере. Подберём программу под ваше самочувствие и цель.'

const BENEFITS = [
  { icon: Hand, title: 'Опытные мастера', text: 'Сертифицированные специалисты с многолетней практикой' },
  { icon: Leaf, title: 'Натуральные масла', text: 'Гипоаллергенная косметика и чистое бельё для каждого гостя' },
  { icon: Sparkles, title: 'Атмосфера покоя', text: 'Тёплые кабинеты, мягкий свет и спокойная музыка' },
  { icon: Users, title: 'Абонементы', text: 'Курсы массажа по выгодной цене для вас и близких' },
]

// Публичные запросы идут без interceptor'а api-клиента, чтобы 401 не уводил гостя на /login
async function fetchPublic<T>(path: string): Promise<T | null> {
  try {
    const res = await fetch(`${api.defaults.baseURL}${path}`)
    return res.ok ? await res.json() : null
  } catch {
    return null
  }
}

function WhatsAppIcon({ size = 30 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M17.47 14.38c-.3-.15-1.75-.86-2.02-.96-.27-.1-.47-.15-.67.15-.2.3-.77.96-.94 1.16-.17.2-.35.22-.64.07-.3-.15-1.26-.46-2.4-1.48-.88-.79-1.48-1.76-1.66-2.06-.17-.3-.02-.46.13-.6.13-.14.3-.35.45-.52.15-.18.2-.3.3-.5.1-.2.05-.37-.03-.52-.07-.15-.67-1.61-.92-2.2-.24-.58-.49-.5-.67-.51h-.57c-.2 0-.52.07-.79.37-.27.3-1.04 1.02-1.04 2.48s1.07 2.88 1.21 3.08c.15.2 2.1 3.2 5.08 4.49.71.31 1.26.49 1.7.63.71.22 1.36.19 1.87.12.57-.09 1.75-.72 2-1.41.25-.69.25-1.29.17-1.41-.07-.12-.27-.2-.57-.35M12.05 21.5h-.01a9.43 9.43 0 0 1-4.81-1.32l-.35-.2-3.57.93.96-3.48-.23-.36a9.42 9.42 0 0 1-1.44-5.02c0-5.2 4.24-9.44 9.46-9.44a9.4 9.4 0 0 1 6.68 2.77 9.38 9.38 0 0 1 2.76 6.68c0 5.21-4.24 9.44-9.45 9.44m8.04-17.48A11.3 11.3 0 0 0 12.05.7C5.78.7.68 5.8.68 12.06c0 2 .52 3.96 1.52 5.68L.58 23.3l5.69-1.49a11.33 11.33 0 0 0 5.43 1.38h.01c6.26 0 11.36-5.1 11.36-11.36 0-3.04-1.18-5.89-3.32-8.04" />
    </svg>
  )
}

export default function LandingPage() {
  const [settings, setSettings] = useState<PublicSettings>({})
  const [services, setServices] = useState<SalonService[]>([])
  const { token } = getAuth()

  useEffect(() => {
    fetchPublic<PublicSettings>('/settings').then(s => s && setSettings(s))
    fetchPublic<SalonService[]>('/services').then(s => s && setServices(s))
  }, [])

  // wa.me требует международный формат: 8 777… → 7 777…
  const phoneDigits = (settings.phone || '').replace(/\D/g, '').replace(/^8(\d{10})$/, '7$1')
  const whatsappUrl = phoneDigits
    ? `https://wa.me/${phoneDigits}?text=${encodeURIComponent('Здравствуйте! Хочу записаться на массаж.')}`
    : null

  return (
    <div className="landing">
      <header className="landing-hero">
        <div className="landing-container">
          <div className="landing-brand">
            <Logo size={64} />
            <h1 className="landing-title">Em.Del.Relaxx</h1>
            <p className="landing-tagline">Массажный салон · расслабление и восстановление</p>
          </div>

          <div className="landing-actions">
            {whatsappUrl ? (
              <a className="landing-action landing-action-wa" href={whatsappUrl} target="_blank" rel="noopener noreferrer">
                <span className="landing-action-icon"><WhatsAppIcon /></span>
                <span className="landing-action-text">
                  <strong>WhatsApp</strong>
                  <small>Написать и записаться</small>
                </span>
              </a>
            ) : (
              <span className="landing-action landing-action-wa is-disabled" title="Укажите телефон в настройках салона">
                <span className="landing-action-icon"><WhatsAppIcon /></span>
                <span className="landing-action-text">
                  <strong>WhatsApp</strong>
                  <small>Номер скоро появится</small>
                </span>
              </span>
            )}
            <Link className="landing-action landing-action-site" to={token ? '/' : '/login'}>
              <span className="landing-action-icon"><Globe size={30} /></span>
              <span className="landing-action-text">
                <strong>Перейти на сайт</strong>
                <small>Онлайн-запись и абонементы</small>
              </span>
            </Link>
          </div>
        </div>
      </header>

      <main className="landing-container">
        <section className="landing-section">
          <h2 className="landing-h2">О салоне</h2>
          <p className="landing-lead">{settings.description || DEFAULT_DESCRIPTION}</p>
          <div className="landing-benefits">
            {BENEFITS.map(({ icon: Icon, title, text }) => (
              <div key={title} className="landing-benefit">
                <span className="landing-benefit-icon"><Icon size={22} /></span>
                <strong>{title}</strong>
                <span>{text}</span>
              </div>
            ))}
          </div>
        </section>

        {services.length > 0 && (
          <section className="landing-section">
            <h2 className="landing-h2">Услуги и цены</h2>
            <div className="landing-services">
              {services.map(s => (
                <div key={s.id} className="landing-service">
                  <div className="landing-service-head">
                    <strong>{s.name}</strong>
                    <span className="landing-price">{Number(s.price).toLocaleString('ru-RU')} ₸</span>
                  </div>
                  {s.description && <p>{s.description}</p>}
                  <span className="landing-duration"><Timer size={14} /> {s.durationMinutes} мин</span>
                </div>
              ))}
            </div>
          </section>
        )}

        <section className="landing-section">
          <h2 className="landing-h2">Как нас найти</h2>
          <div className="landing-contacts">
            {settings.address && (
              <div className="landing-contact"><MapPin size={20} /><div><small>Адрес</small>{settings.address}</div></div>
            )}
            <div className="landing-contact">
              <Clock size={20} /><div><small>Часы работы</small>{settings.workingHours || 'Пн–Вс 09:00–21:00'}</div>
            </div>
            {settings.phone && (
              <a className="landing-contact" href={`tel:+${phoneDigits}`}>
                <Phone size={20} /><div><small>Телефон</small>{settings.phone}</div>
              </a>
            )}
            {settings.instagramUrl && (
              <a className="landing-contact" href={settings.instagramUrl} target="_blank" rel="noopener noreferrer">
                <Instagram size={20} /><div><small>Instagram</small>Наши работы и акции</div>
              </a>
            )}
          </div>
        </section>
      </main>

      <footer className="landing-footer">© {new Date().getFullYear()} Em.Del.Relaxx</footer>
    </div>
  )
}
