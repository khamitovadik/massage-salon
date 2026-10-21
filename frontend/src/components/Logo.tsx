// Знак Em.Del.Relaxx: ладонь с ростком. variant="soft" — лавандовый фон для светлых карточек.
export default function Logo({ size = 40, variant = 'solid' }: { size?: number; variant?: 'solid' | 'soft' }) {
  const bg = variant === 'solid' ? 'var(--brand)' : 'var(--brand-light)'
  const fg = variant === 'solid' ? '#fff' : 'var(--brand)'
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" fill="none" aria-label="Em.Del.Relaxx" role="img" style={{ flexShrink: 0 }}>
      <rect width="48" height="48" rx="12" fill={bg} />
      <g stroke={fg} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M24 26V15" />
        <path d="M24 19c-1-4-4.5-6.5-9-6.5 0 4.5 3.5 7 9 6.5Z" />
        <path d="M24 16c1-4.5 4.5-7 9.5-7 0 5-4 7.5-9.5 7Z" />
        <path d="M9 27.5c2.5 5 7 8 13 8h5.5c2.4 0 4.6-1 6.3-2.6L39 28" />
        <path d="M23 31h6.2a2.3 2.3 0 0 0 0-4.6H20.5c-1.6 0-3-.6-4.2-1.5L14 23.5" />
      </g>
    </svg>
  )
}
