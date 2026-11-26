import {useCallback, useEffect, useState} from 'react'

export type Phase =
    | 'PREPARING'
    | 'WAITING_QR'
    | 'SCAN'
    | 'CONNECTING'
    | 'ONLINE'
    | 'ERROR'
    | 'RETRYING'
export type Session = {
    phase: Phase
    message: string
    qr?: string
    accountId?: string
}

export function useSession() {
    const [session, setSession] = useState<Session>()
    const [error, setError] = useState('')
    const [updated, setUpdated] = useState<Date>()
    const [busy, setBusy] = useState(true)
    const [revision, setRevision] = useState(0)
    const refresh = useCallback(() => setRevision((value) => value + 1), [])
    useEffect(() => {
        const controller = new AbortController()
        let timer: ReturnType<typeof setTimeout>

        async function poll() {
            try {
                const res = await fetch('./status', {
                    cache: 'no-store',
                    signal: AbortSignal.any([
                        controller.signal,
                        AbortSignal.timeout(10000),
                    ]),
                })
                if (!res.ok) {
                    if (controller.signal.aborted) return
                    setSession(undefined)
                    setError(
                        res.status === 401
                            ? '请重新验证 Falowp 管理密码'
                            : '会话服务暂时无法访问',
                    )
                    return
                }
                const data: Session = await res.json()
                if (controller.signal.aborted) return
                setSession(data)
                setUpdated(new Date())
                setError('')
            } catch {
                if (controller.signal.aborted) return
                setSession(undefined)
                setError('连接中断，正在尝试重新连接')
            } finally {
                if (!controller.signal.aborted) {
                    setBusy(false)
                    timer = setTimeout(poll, 2000)
                }
            }
        }

        setBusy(true)
        void poll()
        return () => {
            controller.abort()
            clearTimeout(timer)
        }
    }, [revision])
    return {session, error, updated, busy, refresh}
}
