import { useState } from 'react'
import api from '../api/client'

export default function BroadcastPage() {
  const [message, setMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState('')

  function send() {
    if (!message.trim()) return
    setSending(true)
    setResult('')
    api.post('/broadcast', { message, allClients: true })
      .then(r => setResult(`✅ Отправлено: ${r.data.sent ?? 0} получателей`))
      .catch(() => setResult('❌ Ошибка при отправке'))
      .finally(() => setSending(false))
  }

  return (
    <div>
      <h1 style={{ marginBottom: 8 }}>📢 Рассылка</h1>
      <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>
        Отправить сообщение всем клиентам у которых привязан Telegram.
      </p>

      <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: 12, padding: 24, maxWidth: 560 }}>
        <label style={{ display: 'block', fontWeight: 600, marginBottom: 8 }}>Сообщение</label>
        <textarea
          className="input"
          rows={6}
          placeholder="Текст рассылки... (поддерживается *жирный*, _курсив_)"
          value={message}
          onChange={e => setMessage(e.target.value)}
          style={{ width: '100%', resize: 'vertical', marginBottom: 16 }}
        />

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <button className="btn btn-primary" onClick={send} disabled={sending || !message.trim()}>
            {sending ? 'Отправка...' : '📤 Отправить всем'}
          </button>
          {result && <span style={{ color: result.startsWith('✅') ? '#22c55e' : '#ef4444' }}>{result}</span>}
        </div>

        <div style={{ marginTop: 16, padding: 12, background: 'var(--brand-light)', borderRadius: 8, fontSize: 13, color: 'var(--text-muted)' }}>
          💡 Получат сообщение только те клиенты, которые привязали Telegram командой <code>/start email</code> в боте <strong>@MassageSalonAldikBot</strong>
        </div>
      </div>
    </div>
  )
}
