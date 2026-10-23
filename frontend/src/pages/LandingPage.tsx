import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight, Clock, Hand, Instagram, Leaf, MapPin, MessageCircle, Phone, Sparkles, Star, Timer, Users,
} from 'lucide-react'
import api from '../api/client'
import Logo from '../components/Logo'
import ThemeToggle from '../components/ThemeToggle'
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
  'Классический, расслабляющий и спортивный массаж от опытных мастеров в спокойной атмосфере. ' +
  'Подберём программу под ваше самочувствие и цель.'

const BENEFITS = [
  { icon: Hand, title: 'Опытные мастера', text: 'Сертифицированные специалисты с многолетней практикой' },
  { icon: Leaf, title: 'Натуральные масла', text: 'Гипоаллергенная косметика и чистое бельё для каждого гостя' },
  { icon: Sparkles, title: 'Атмосфера покоя', text: 'Тёплые кабинеты, мягкий свет и спокойная музыка' },
  { icon: Users, title: 'Абонементы', text: 'Курсы массажа по выгодной цене для вас и близких' },
]

// Рейтинг из карточки 2ГИС — обновлять вручную, когда меняется
const RATING = { score: '5.0', reviews: 99, url: 'https://2gis.kz/almaty/firm/70000001094543090/tab/reviews' }

// Фото с Unsplash (бесплатная лицензия, можно без указания автора). Заменить на фото салона, когда будут.
const PHOTO = (id: string, w: number) => `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${w}&q=75`
const HERO_PHOTO = '1600334129128-685c5582fd35' // unsplash.com/photos/SMwCQZWayj0
const ABOUT_PHOTO = '1745327883508-b6cd32e5dde5' // unsplash.com/photos/7yeqemd-p90

const STEPS = [
  { title: 'Выберите услугу', text: 'Посмотрите программы и цены — или напишите нам, поможем выбрать.' },
  { title: 'Забронируйте время', text: 'Свободные окошки мастеров видны онлайн, запись занимает минуту.' },
  { title: 'Приходите отдыхать', text: 'Мы подготовим кабинет, а вы просто расслабьтесь.' },
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

function WhatsAppIcon({ size = 20 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M17.47 14.38c-.3-.15-1.75-.86-2.02-.96-.27-.1-.47-.15-.67.15-.2.3-.77.96-.94 1.16-.17.2-.35.22-.64.07-.3-.15-1.26-.46-2.4-1.48-.88-.79-1.48-1.76-1.66-2.06-.17-.3-.02-.46.13-.6.13-.14.3-.35.45-.52.15-.18.2-.3.3-.5.1-.2.05-.37-.03-.52-.07-.15-.67-1.61-.92-2.2-.24-.58-.49-.5-.67-.51h-.57c-.2 0-.52.07-.79.37-.27.3-1.04 1.02-1.04 2.48s1.07 2.88 1.21 3.08c.15.2 2.1 3.2 5.08 4.49.71.31 1.26.49 1.7.63.71.22 1.36.19 1.87.12.57-.09 1.75-.72 2-1.41.25-.69.25-1.29.17-1.41-.07-.12-.27-.2-.57-.35M12.05 21.5h-.01a9.43 9.43 0 0 1-4.81-1.32l-.35-.2-3.57.93.96-3.48-.23-.36a9.42 9.42 0 0 1-1.44-5.02c0-5.2 4.24-9.44 9.46-9.44a9.4 9.4 0 0 1 6.68 2.77 9.38 9.38 0 0 1 2.76 6.68c0 5.21-4.24 9.44-9.45 9.44m8.04-17.48A11.3 11.3 0 0 0 12.05.7C5.78.7.68 5.8.68 12.06c0 2 .52 3.96 1.52 5.68L.58 23.3l5.69-1.49a11.33 11.33 0 0 0 5.43 1.38h.01c6.26 0 11.36-5.1 11.36-11.36 0-3.04-1.18-5.89-3.32-8.04" />
    </svg>
  )
}

export default function LandingPage() {
  const [settings, setSettings] = useState<PublicSettings>({})
  const [services, setServices] = useState<SalonService[]>([])
  const [showDock, setShowDock] = useState(false)
  const heroCtaRef = useRef<HTMLDivElement>(null)
  const { token } = getAuth()

  // Нижняя панель на телефоне появляется, когда кнопки первого экрана ушли за пределы видимости
  useEffect(() => {
    const el = heroCtaRef.current
    if (!el) return
    const io = new IntersectionObserver(([e]) => setShowDock(!e.isIntersecting))
    io.observe(el)
    return () => io.disconnect()
  }, [])

  useEffect(() => {
    fetchPublic<PublicSettings>('/settings').then(s => s && setSettings(s))
    fetchPublic<SalonService[]>('/services').then(s => s && setServices(s))
  }, [])

  // wa.me требует международный формат: 8 777… → 7 777…
  const phoneDigits = (settings.phone || '').replace(/\D/g, '').replace(/^8(\d{10})$/, '7$1')
  const whatsappUrl = phoneDigits
    ? `https://wa.me/${phoneDigits}?text=${encodeURIComponent('Здравствуйте! Хочу записаться на массаж.')}`
    : null
  const bookUrl = token ? '/appointments/new' : '/login'
  const hours = settings.workingHours || 'Ежедневно 11:00–23:00'
  const minPrice = services.length ? Math.min(...services.map(s => Number(s.price))) : null

  const waButton = (label: string, extraClass = '') =>
    whatsappUrl ? (
      <a className={`l-btn l-btn-wa ${extraClass}`} href={whatsappUrl} target="_blank" rel="noopener noreferrer">
        <WhatsAppIcon /> {label}
      </a>
    ) : (
      <span className={`l-btn l-btn-wa is-disabled ${extraClass}`} title="Укажите телефон в настройках салона">
        <WhatsAppIcon /> {label}
      </span>
    )

  return (
    <div className="landing">
      <nav className="l-nav">
        <div className="l-container l-nav-inner">
          <a href="#top" className="l-nav-brand"><Logo size={34} /> Em.Del.Relaxx</a>
          <div className="l-nav-links">
            <a href="#about">О салоне</a>
            <a href="#services">Услуги</a>
            <a href="#contacts">Контакты</a>
          </div>
          <div className="l-nav-actions">
            <ThemeToggle className="l-theme" />
            <Link className="l-btn l-btn-ghost l-btn-sm" to={token ? '/' : '/login'}>
              {token ? 'Кабинет' : 'Войти'}
            </Link>
          </div>
        </div>
      </nav>

      <header className="l-hero" id="top">
        <div className="l-container l-hero-grid">
          <div className="l-hero-copy">
            <span className="l-eyebrow">Массажный салон · Алматы</span>
            <h1 className="l-display">
              Час тишины<br />и <em>заботы о себе</em>
            </h1>
            <p className="l-hero-lead">{settings.description || DEFAULT_DESCRIPTION}</p>

            {/* На телефоне фото и рейтинг идут между текстом и кнопками */}
            <div className="l-duo">
              <img src={PHOTO(HERO_PHOTO, 480)} alt="Массаж горячими камнями" loading="eager" />
              <a className="l-rating" href={RATING.url} target="_blank" rel="noopener noreferrer">
                <span className="l-stars">{[0, 1, 2, 3, 4].map(i => <Star key={i} size={13} fill="currentColor" strokeWidth={0} />)}</span>
                <b>{RATING.score}</b>
                <small>{RATING.reviews} отзывов<br />в 2ГИС</small>
              </a>
            </div>

            <div className="l-hero-cta" ref={heroCtaRef}>
              <Link className="l-btn l-btn-primary" to={bookUrl}>
                Записаться онлайн <ArrowRight size={18} />
              </Link>
              {waButton('Написать в WhatsApp')}
            </div>
            <ul className="l-hero-facts">
              <li><Clock size={16} /> {hours}</li>
              {settings.address && <li><MapPin size={16} /> {settings.address}</li>}
            </ul>
          </div>

          <div className="l-hero-visual">
            <img className="l-hero-photo" src={PHOTO(HERO_PHOTO, 1100)} alt="Массаж горячими камнями" />
            <a className="l-float l-float-top" href={RATING.url} target="_blank" rel="noopener noreferrer">
              <span className="l-float-icon"><Star size={18} fill="currentColor" strokeWidth={0} /></span>
              <div><strong>{RATING.score} в 2ГИС</strong><small>{RATING.reviews} отзывов гостей</small></div>
            </a>
            <div className="l-float l-float-bottom">
              <span className="l-float-icon is-warm"><Clock size={18} /></span>
              <div><strong>{hours}</strong><small>{minPrice ? `массаж от ${minPrice.toLocaleString('ru-RU')} ₸` : 'без выходных'}</small></div>
            </div>
          </div>
        </div>
      </header>

      <section className="l-section" id="about">
        <div className="l-container">
          <div className="l-about">
            <div className="l-section-head">
              <span className="l-eyebrow">О салоне</span>
              <h2 className="l-h2">Забота о теле — без суеты</h2>
              <p className="l-about-text">
                Подберём массаж под ваше самочувствие: от лёгкого релакса до глубокой проработки спины.
                Можно прийти к нам или вызвать мастера на дом.
              </p>
            </div>
            <img className="l-about-photo" src={PHOTO(ABOUT_PHOTO, 900)} alt="Мастер делает массаж спины" loading="lazy" />
          </div>
          <div className="l-benefits">
            {BENEFITS.map(({ icon: Icon, title, text }) => (
              <div key={title} className="l-benefit">
                <span className="l-benefit-icon"><Icon size={22} /></span>
                <strong>{title}</strong>
                <p>{text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {services.length > 0 && (
        <section className="l-section l-section-tint" id="services">
          <div className="l-container">
            <div className="l-section-head">
              <span className="l-eyebrow">Услуги и цены</span>
              <h2 className="l-h2">Выберите свою программу</h2>
            </div>
            <div className="l-services">
              {services.map(s => (
                <article key={s.id} className="l-service">
                  <div className="l-service-top">
                    <h3>{s.name}</h3>
                    <span className="l-duration"><Timer size={14} /> {s.durationMinutes} мин</span>
                  </div>
                  {s.description && <p>{s.description}</p>}
                  <div className="l-service-foot">
                    <span className="l-price">{Number(s.price).toLocaleString('ru-RU')} ₸</span>
                    <Link to={bookUrl} className="l-service-link">Записаться <ArrowRight size={16} /></Link>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="l-section">
        <div className="l-container">
          <div className="l-section-head">
            <span className="l-eyebrow">Как записаться</span>
            <h2 className="l-h2">Три простых шага</h2>
          </div>
          <ol className="l-steps">
            {STEPS.map((s, i) => (
              <li key={s.title} className="l-step">
                <span className="l-step-num">{String(i + 1).padStart(2, '0')}</span>
                <strong>{s.title}</strong>
                <p>{s.text}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="l-section" id="contacts">
        <div className="l-container">
          <div className="l-cta">
            <div className="l-cta-copy">
              <h2 className="l-h2">Ждём вас в гости</h2>
              <p>Напишите нам — подскажем свободное время и подберём массаж.</p>
              <div className="l-hero-cta">
                {waButton('WhatsApp', 'is-on-dark')}
                <Link className="l-btn l-btn-light" to={bookUrl}>
                  <MessageCircle size={18} /> Онлайн-запись
                </Link>
              </div>
            </div>
            <div className="l-contacts">
              {settings.address && (
                <div className="l-contact"><MapPin size={20} /><div><small>Адрес</small>{settings.address}</div></div>
              )}
              <div className="l-contact"><Clock size={20} /><div><small>Часы работы</small>{hours}</div></div>
              {settings.phone && (
                <a className="l-contact" href={`tel:+${phoneDigits}`}>
                  <Phone size={20} /><div><small>Телефон</small>{settings.phone}</div>
                </a>
              )}
              {settings.instagramUrl && (
                <a className="l-contact" href={settings.instagramUrl} target="_blank" rel="noopener noreferrer">
                  <Instagram size={20} /><div><small>Instagram</small>Наши работы и акции</div>
                </a>
              )}
            </div>
          </div>
        </div>
      </section>

      <footer className="l-footer">
        <div className="l-container l-footer-inner">
          <span className="l-nav-brand"><Logo size={28} /> Em.Del.Relaxx</span>
          <span>© {new Date().getFullYear()} · Массажный салон</span>
        </div>
      </footer>

      <div className={`l-dock${showDock ? ' is-visible' : ''}`}>
        <Link className="l-btn l-btn-primary" to={bookUrl}>Записаться <ArrowRight size={18} /></Link>
        {whatsappUrl && (
          <a className="l-dock-wa" href={whatsappUrl} target="_blank" rel="noopener noreferrer" aria-label="Написать в WhatsApp">
            <WhatsAppIcon size={24} />
          </a>
        )}
        {settings.phone && (
          <a className="l-dock-call" href={`tel:+${phoneDigits}`} aria-label="Позвонить"><Phone size={22} /></a>
        )}
      </div>
    </div>
  )
}
