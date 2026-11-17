import { api, auth, items, query, newIdempotencyKey } from '../api.js';
import { esc, iniciais, loading, errorState, toast, confirmDialog, formMarkup, attachForm, TEST_ACCOUNT } from '../ui.js';
import { camposPerfil, AVISO_FICTICIO, BOTAO_FICTICIO, ligarFicticio, abrirFormPerfil, excluirPerfil } from './perfis.js';
import { abrirFormHeroi } from './herois.js';
import { abrirFormEquipe } from './equipes.js';

const CAMPOS_LOGIN = [
    { name: 'email', label: 'E-mail', type: 'email', required: true, autocomplete: 'username' },
    { name: 'senha', label: 'Senha', type: 'password', required: true, autocomplete: 'current-password', trim: false }
];

function renderVisitante(root, ctx) {
    root.innerHTML = `<section class="page">
        <div class="page-header">
            <div>
                <span class="kicker">Acesso</span>
                <h1 class="page-title">Minha base</h1>
                <p class="page-subtitle">Entre com o seu perfil para recrutar heróis e montar equipes, ou crie um perfil novo em segundos.</p>
            </div>
        </div>
        <div class="auth-grid">
            <article class="panel corner panel-glow">
                <h2 class="section-title">Entrar</h2>
                <p class="muted" style="margin-top:-6px">Perfil de teste: ${TEST_ACCOUNT}</p>
                <div data-login>${formMarkup({ fields: CAMPOS_LOGIN, submitLabel: 'Entrar', cancel: false })}</div>
            </article>
            <article class="panel">
                <h2 class="section-title">Criar perfil</h2>
                ${AVISO_FICTICIO}
                <div data-cadastro>${formMarkup({ fields: camposPerfil(), submitLabel: 'Criar perfil', cancel: false, extraActions: BOTAO_FICTICIO })}</div>
            </article>
        </div>
    </section>`;

    attachForm(root.querySelector('[data-login] form'), CAMPOS_LOGIN, async values => {
        const login = await api.login(values.email, values.senha);
        toast(`Bem-vindo(a), ${login.nome}!`, 'success');
        renderConta(root, ctx);
    });
    const formCadastro = root.querySelector('[data-cadastro] form');
    const idempotencyKey = newIdempotencyKey();
    ligarFicticio(formCadastro);
    attachForm(formCadastro, camposPerfil(), async values => {
        const { status } = await api.create('/api/perfis/v1', values, idempotencyKey);
        toast(status === 200 ? 'Este perfil já tinha sido criado.' : 'Perfil criado! Entrando...', 'success');
        const login = await api.login(values.email, values.senha);
        toast(`Bem-vindo(a), ${login.nome}!`, 'success');
        renderConta(root, ctx);
    });
}

async function renderLogado(root, ctx) {
    const sessao = auth.perfil || {};
    root.innerHTML = `<section class="page">${loading('Abrindo a sua base...')}</section>`;
    let perfil;
    let herois;
    let equipes;
    try {
        [perfil, herois, equipes] = await Promise.all([
            api.get(`/api/perfis/v1/${sessao.id}`),
            api.get(`/api/herois/v1/perfil/${sessao.id}${query({ size: 50, sort: 'nomeHeroico,asc' })}`),
            api.get(`/api/equipes/v1/perfil/${sessao.id}${query({ size: 50, sort: 'nome,asc' })}`)
        ]);
    } catch (error) {
        if (!ctx.isCurrent()) return;
        if (error.status === 404) {
            auth.clear();
            toast('O seu perfil não existe mais (a API pode ter reiniciado). Entre novamente.', 'warn');
            renderVisitante(root, ctx);
            return;
        }
        root.innerHTML = `<section class="page">${errorState(error)}</section>`;
        root.querySelector('[data-action="retry"]').addEventListener('click', () => renderLogado(root, ctx));
        return;
    }
    if (!ctx.isCurrent()) return;
    const listaHerois = items(herois, 'herois');
    const listaEquipes = items(equipes, 'equipes');
    const chave = auth.key || '';
    const mascarada = chave ? `${chave.slice(0, 8)}••••••••••••${chave.slice(-4)}` : '';

    root.innerHTML = `<section class="page">
        <div class="page-header">
            <div>
                <span class="kicker">Minha base</span>
                <h1 class="page-title">Olá, ${esc(perfil.nome)}</h1>
                <p class="page-subtitle">Tudo o que é seu: heróis, equipes e a chave de acesso. Só você altera o que é do seu perfil.</p>
            </div>
            <button class="btn btn-ghost" data-logout>Sair</button>
        </div>
        <div class="base">
            <aside class="panel corner panel-glow id-card">
                <span class="emblem emblem-lg" aria-hidden="true">${esc(iniciais(perfil.nome))}</span>
                <h2>${esc(perfil.nome)}</h2>
                <span class="muted">${esc(perfil.email)}</span>
                <dl class="id-meta">
                    <div><dt>Perfil</dt><dd>#${esc(perfil.id)}</dd></div>
                    <div><dt>Heróis</dt><dd>${esc(perfil.quantidadeHerois)}</dd></div>
                    <div><dt>Equipes</dt><dd>${esc(perfil.quantidadeEquipes)}</dd></div>
                </dl>
                <button class="btn btn-secondary" data-edit style="width:100%">Editar perfil</button>
            </aside>
            <div class="base-stack">
                <div class="auth-grid">
                    <section class="panel">
                        <h2 class="section-title">Meus heróis</h2>
                        <div class="mini-list" data-meus-herois>${listaHerois.length
                            ? listaHerois.map(heroi => `<a href="#/herois/${heroi.id}"><strong>${esc(heroi.nomeHeroico)}</strong><span class="muted">${esc(heroi.poderNome || 'sem poder')}</span></a>`).join('')
                            : '<p class="muted" style="margin:0">Nenhum herói ainda.</p>'}</div>
                        <button class="btn btn-primary btn-sm" data-novo-heroi style="margin-top:14px">+ Recrutar herói</button>
                    </section>
                    <section class="panel">
                        <h2 class="section-title">Minhas equipes</h2>
                        <div class="mini-list">${listaEquipes.length
                            ? listaEquipes.map(equipe => `<a href="#/equipes?busca=${encodeURIComponent(equipe.nome)}"><strong>${esc(equipe.nome)}</strong><span class="muted">${equipe.herois.length} herói(s)</span></a>`).join('')
                            : '<p class="muted" style="margin:0">Nenhuma equipe ainda.</p>'}</div>
                        <button class="btn btn-secondary btn-sm" data-nova-equipe style="margin-top:14px">+ Nova equipe</button>
                    </section>
                </div>
                <section class="panel">
                    <h2 class="section-title">Chave de acesso (X-API-Key)</h2>
                    <p class="muted" style="margin-top:-6px">Libera os cadastros e alterações. Também serve no <a href="/swagger-ui/index.html">Swagger</a>: cole no campo X-API-Key. Um novo login gera outra chave e invalida esta.</p>
                    <div class="account-key">
                        <code data-key>${esc(mascarada)}</code>
                        <button class="btn btn-ghost btn-sm" data-show>Mostrar</button>
                        <button class="btn btn-secondary btn-sm" data-copy>Copiar</button>
                    </div>
                </section>
                <section class="panel" style="border-color:rgba(255,107,129,0.35)">
                    <h2 class="section-title" style="color:var(--danger)">Zona de perigo</h2>
                    <p class="muted" style="margin-top:-6px">Excluir o perfil apaga também os seus heróis (com poderes e fraquezas) e as suas equipes.</p>
                    <button class="btn btn-danger" data-delete>Excluir meu perfil</button>
                </section>
            </div>
        </div>
    </section>`;

    const recarregar = () => renderConta(root, ctx);
    const caixa = root.querySelector('[data-key]');
    root.querySelector('[data-show]').addEventListener('click', event => {
        const mostrando = caixa.textContent === chave;
        caixa.textContent = mostrando ? mascarada : chave;
        event.target.textContent = mostrando ? 'Mostrar' : 'Ocultar';
    });
    root.querySelector('[data-copy]').addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(chave);
            toast('Chave copiada.', 'success');
        } catch {
            caixa.textContent = chave;
            toast('Não foi possível copiar automaticamente. A chave está visível para você copiar.', 'warn');
        }
    });
    root.querySelector('[data-edit]').addEventListener('click', () => abrirFormPerfil({ perfil, onSaved: recarregar }));
    root.querySelector('[data-novo-heroi]').addEventListener('click', () => abrirFormHeroi({ onSaved: heroi => ctx.navigate(`/herois/${heroi.id}`) }));
    root.querySelector('[data-nova-equipe]').addEventListener('click', () => abrirFormEquipe({ onSaved: recarregar }));
    root.querySelector('.page-header [data-logout]').addEventListener('click', async () => {
        const ok = await confirmDialog({ title: 'Sair', message: 'Deseja sair? A sua chave de acesso será invalidada.', confirmLabel: 'Sair' });
        if (!ok) return;
        await api.logout().catch(() => {});
        toast('Você saiu da sua base.', 'info');
        renderVisitante(root, ctx);
    });
    root.querySelector('[data-delete]').addEventListener('click', () => excluirPerfil(perfil, { onDeleted: () => renderVisitante(root, ctx) }));
}

export function renderConta(root, ctx) {
    if (auth.isLogged()) return renderLogado(root, ctx);
    return renderVisitante(root, ctx);
}
