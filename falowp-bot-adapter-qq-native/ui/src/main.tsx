import {createRoot} from 'react-dom/client'
import {Button, Chip, Spinner} from '@heroui/react'
import {useSession, type Phase} from './session'
import './style.css'

const labels: Record<Phase, string> = {
    PREPARING: '准备运行环境',
    WAITING_QR: '更新二维码',
    SCAN: '等待扫码',
    CONNECTING: '正在登录',
    ONLINE: '会话在线',
    ERROR: '登录失败',
    RETRYING: '等待重试',
}

function App() {
    const {session, error, updated, busy, refresh} = useSession()
    const phase = session?.phase
    const online = phase === 'ONLINE'
    const trouble = !!error || phase === 'ERROR' || phase === 'RETRYING'
    const title = error ? '连接中断' : phase ? labels[phase] : '正在读取会话'
    return (
        <div className="shell">
            <header>
                <div className="wrap brand">
          <span className="mark" aria-hidden="true">
            F
          </span>
                    <b>Falowp</b>
                    <span className="divider"/>
                    QQ Native
                </div>
            </header>
            <main className="wrap">
                <div className="page-title">
                    <h1>QQ 会话</h1>
                    <Button variant="secondary" onPress={refresh} isDisabled={busy}>
                        刷新状态
                    </Button>
                </div>
                <div className="layout">
                    <section className="session-panel" aria-labelledby="session-title">
                        <div className="panel-top">
                            <span>账号连接</span>
                            <Chip
                                size="sm"
                                variant="soft"
                                color={trouble ? 'warning' : online ? 'success' : 'accent'}
                            >
                                {title}
                            </Chip>
                        </div>
                        <div className="focus">
                            {phase === 'SCAN' && session?.qr ? (
                                <div className="qr">
                                    <img
                                        src={'data:image/png;base64,' + session.qr}
                                        alt="使用手机 QQ 扫描登录二维码"
                                    />
                                </div>
                            ) : online ? (
                                <div className="connected" aria-hidden="true">
                                    ✓
                                </div>
                            ) : trouble ? (
                                <div className="notice" aria-hidden="true">
                                    !
                                </div>
                            ) : (
                                <Spinner size="lg" aria-label="连接中"/>
                            )}
                            <h2 id="session-title">
                                {phase === 'SCAN' ? '扫码登录 QQ' : title}
                            </h2>
                            <p role="status">
                                {error ||
                                    (online ? 'QQ ' + session?.accountId : session?.message) ||
                                    '正在连接 Falowp 服务'}
                            </p>
                        </div>
                    </section>
                    <aside>
                        <h2>会话信息</h2>
                        <dl>
                            <div>
                                <dt>适配器</dt>
                                <dd>QQ Native</dd>
                            </div>
                            <div>
                                <dt>运行环境</dt>
                                <dd>Linux NTQQ</dd>
                            </div>
                            <div>
                                <dt>当前账号</dt>
                                <dd>{session?.accountId || '—'}</dd>
                            </div>
                            <div>
                                <dt>最近同步</dt>
                                <dd>
                                    {updated?.toLocaleTimeString('zh-CN', {hour12: false}) ||
                                        '—'}
                                </dd>
                            </div>
                        </dl>
                    </aside>
                </div>
            </main>
        </div>
    )
}

createRoot(document.getElementById('root')!).render(<App/>)
