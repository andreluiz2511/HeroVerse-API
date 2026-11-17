import { api, auth, onRateLimit } from './api.js';
import { esc, toast, confirmDialog, iniciais } from './ui.js';
import { renderInicio } from './pages/inicio.js';
import { renderHerois, renderHeroiDetalhe } from './pages/herois.js';
import { renderArsenal } from './pages/arsenal.js';
import { renderEquipes } from './pages/equipes.js';
import { renderPerfis } from './pages/perfis.js';
import { renderConta } from './pages/conta.js';

const view = document.getElementById('view');
let navigationId = 0;

const routes = [
    { pattern: /^\/?$/, nav: 'inicio', title: 'Central', render: renderInicio },
    { pattern: /^\/herois\/(\d+)$/, nav: 'herois', title: 'Dossiê', render: (root, ctx) => renderHeroiDetalhe(root, ctx, Number(ctx.match[1])) },
    { pattern: /^\/herois$/, nav: 'herois', title: 'Heróis', render: renderHerois },
    { pattern: /^\/arsenal$/, nav: 'arsenal', title: 'Arsenal', render: renderArsenal },
    { pattern: /^\/equipes$/, nav: 'equipes', title: 'Equipes', render: renderEquipes },
    { pattern: /^\/perfis$/, nav: 'perfis', title: 'Perfis', render: renderPerfis, hidden: true },
    { pattern: /^\/conta$/, nav: 'conta', title: 'Minha base', render: renderConta }
];

function parseHash() {
    const raw = location.hash.replace(/^#/, '') || '/';
    const [path, search = ''] = raw.split('?');
    return { path, params: new URLSearchParams(search) };
}

export function navigate(path, params = {}) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') search.set(key, value);
    });
    const text = search.toString();
    const target = `#${path}${text ? `?${text}` : ''}`;
    if (location.hash === target) {
        render();
    } else {
        location.hash = target;
    }
}

function setActiveNav(name) {
    document.querySelectorAll('[data-nav]').forEach(link => {
        const active = link.dataset.nav === name;
        link.classList.toggle('is-active', active);
        if (active) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    });
}

async function render() {
    const { path, params } = parseHash();
    const route = routes.find(item => item.pattern.test(path));
    const currentId = ++navigationId;
    
    if (!route || route.hidden) {
        setActiveNav('');
        document.title = 'Página não encontrada · HeroVerse';
        view.innerHTML = `<section class="page"><div class="state"><p class="state-title">Página não encontrada</p>
            <p>O endereço <code>${esc(path)}</code> não existe.</p><a class="btn btn-primary" href="#/">Voltar ao início</a></div></section>`;
        return;
    }

    setActiveNav(route.nav);
    document.title = `${route.title} · HeroVerse`;
    const ctx = {
        params,
        match: path.match(route.pattern),
        isCurrent: () => currentId === navigationId,
        navigate,
        refresh: render
    };
    window.scrollTo({ top: 0 });
    await route.render(view, ctx);
    view.focus({ preventScroll: true });
}

function renderAuthArea() {
    const area = document.getElementById('auth-area');
    if (auth.isLogged()) {
        const perfil = auth.perfil || {};
        area.innerHTML = `
            <a class="agent" href="#/conta" title="Minha base">
                <span class="emblem" aria-hidden="true">${esc(iniciais(perfil.nome))}</span>
                <span><strong class="auth-name">${esc(perfil.nome || 'Minha base')}</strong><small>perfil #${esc(perfil.id ?? '')}</small></span>
            </a>
            <button class="btn btn-ghost btn-sm" data-logout>Sair</button>`;
        area.querySelector('[data-logout]').addEventListener('click', async () => {
            const ok = await confirmDialog({ title: 'Sair', message: 'Deseja sair? A sua chave de acesso será invalidada.', confirmLabel: 'Sair' });
            if (!ok) return;
            await api.logout().catch(() => {});
            toast('Você saiu da sua base.', 'info');
            if (parseHash().path === '/conta') render();
        });
    } else {
        area.innerHTML = `<a class="btn btn-primary btn-sm" href="#/conta" data-entrar>Entrar</a>`;
    }
}

let rateTimer;
function showRateLimit(seconds) {
    const banner = document.getElementById('rate-banner');
    let remaining = seconds;
    clearInterval(rateTimer);
    const update = () => {
        banner.innerHTML = `<strong>Limite de requisições atingido.</strong> A API aceita 20 requisições por minuto. Tente novamente em <strong>${remaining}s</strong>.`;
    };
    update();
    banner.hidden = false;
    rateTimer = setInterval(() => {
        remaining -= 1;
        if (remaining <= 0) {
            clearInterval(rateTimer);
            banner.hidden = true;
            return;
        }
        update();
    }, 1000);
}

auth.onChange(renderAuthArea);
onRateLimit(showRateLimit);
window.addEventListener('hashchange', render);
renderAuthArea();
render();
