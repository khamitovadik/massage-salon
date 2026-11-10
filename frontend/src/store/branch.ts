// Выбранный в шапке филиал для руководителя и владельца. Пусто — «все мои филиалы».
const KEY = 'branchId'

export function getCurrentBranchId(): number | null {
  try {
    const v = localStorage.getItem(KEY)
    return v ? Number(v) : null
  } catch {
    return null
  }
}

export function setCurrentBranchId(id: number | null): void {
  try {
    if (id == null) localStorage.removeItem(KEY)
    else localStorage.setItem(KEY, String(id))
  } catch {
    /* хранилище недоступно — фильтр просто не запомнится */
  }
}
