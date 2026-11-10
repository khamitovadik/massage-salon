import type { Branch } from '../types'

/** Выбор нескольких филиалов галочками-чипами */
export default function BranchChecks({ branches, value, onChange }: {
  branches: Branch[]
  value: number[]
  onChange: (ids: number[]) => void
}) {
  if (branches.length === 0) return <span className="text-muted text-sm">Филиалов пока нет</span>
  return (
    <div className="branch-checks">
      {branches.map(b => {
        const on = value.includes(b.id)
        return (
          <label key={b.id} className={`branch-check${on ? ' is-on' : ''}${b.active ? '' : ' is-closed'}`}>
            <input type="checkbox" checked={on}
              onChange={() => onChange(on ? value.filter(x => x !== b.id) : [...value, b.id])} />
            {b.name}{b.active ? '' : ' (закрыт)'}
          </label>
        )
      })}
    </div>
  )
}
