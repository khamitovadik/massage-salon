import { useEffect, useState } from 'react'
import { getBranches, getManagedBranches } from '../api/branches'
import type { Branch } from '../types'

// Филиалы меняются редко — грузим один раз за сессию страницы
let managedCache: Promise<Branch[]> | null = null
let publicCache: Promise<Branch[]> | null = null

export function invalidateBranches() {
  managedCache = null
  publicCache = null
}

/** Филиалы в панели: владельцу все, руководителю — свои */
export function useManagedBranches(enabled = true): Branch[] {
  const [list, setList] = useState<Branch[]>([])
  useEffect(() => {
    if (!enabled) return
    managedCache ??= getManagedBranches().catch(() => { managedCache = null; return [] })
    managedCache.then(setList)
  }, [enabled])
  return list
}

/** Работающие филиалы — для записи и сайта */
export function usePublicBranches(): { branches: Branch[]; loaded: boolean } {
  const [state, setState] = useState<{ branches: Branch[]; loaded: boolean }>({ branches: [], loaded: false })
  useEffect(() => {
    publicCache ??= getBranches().catch(() => { publicCache = null; return [] })
    publicCache.then(branches => setState({ branches, loaded: true }))
  }, [])
  return state
}
