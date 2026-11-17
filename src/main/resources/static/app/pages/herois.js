import { api, auth, items, pageInfo, lookup, query, newIdempotencyKey, invalidateLookups } from '../api.js';
import {
    esc, iniciais, NIVEIS, nivelBadge, forcaBar, loading, emptyState, errorState, toast, showError,
    formModal, confirmDialog, withLogin, pagination, jsonBlock, linksPanel, fillForm
} from '../ui.js';
import { heroiFicticio, poderFicticio, fraquezaFicticia } from '../ficticio.js';

const PAGE_SIZE = 12;

const ORDENS = [
    { value: 'nomeHeroico,asc', label: 'Nome (A–Z)' },
    { value: 'nomeHeroico,desc', label: 'Nome (Z–A)' },
    { value: 'forca,desc', label: 'Mais fortes' },
    { value: 'id,desc', label: 'Recrutados por último' }
];

export function meuId() {
    return auth.isLogged() && auth.perfil ? auth.perfil.id : null;
}

export function deV1(heroi, forcas = new Map()) {
    return {
        id: heroi.id,
        nome: heroi.nome,
        nomeHeroico: heroi.nomeHeroico,
        nivelForca: heroi.nivelForca,
        forca: forcas.has(heroi.id) ? forcas.get(heroi.id) : undefined,
        poder: heroi.poderNome,
        fraqueza: heroi.fraquezaNome,
        equipes: (heroi.equipes || []).map(equipe => equipe.nome),
        perfilId: heroi.perfilId,
        perfil: heroi.perfilNome
    };
}

export function heroCard(heroi) {
    const nivel = NIVEIS[heroi.nivelForca] || { cor: '' };
    const meu = heroi.perfilId === meuId();
    return `<a class="hero-card ${nivel.cor} ${meu ? 'is-mine' : ''}" href="#/herois/${heroi.id}">
        <span class="hero-inner">
            <span class="hero-top">${nivelBadge(heroi.nivelForca)}<span class="hero-id">${meu ? '<span class="mine-flag">seu · </span>' : ''}#${esc(String(heroi.id).padStart(3, '0'))}</span></span>
            <span class="hero-art" aria-hidden="true"><span class="hero-mono">${esc(iniciais(heroi.nomeHeroico))}</span></span>
            <span>
                <span class="hero-name" style="display:block">${esc(heroi.nomeHeroico)}</span>
                <span class="hero-civil">${esc(heroi.nome)} · ${esc(heroi.perfil || '')}</span>
            </span>
            <span class="hero-traits">
                <span><b>Poder</b>${esc(heroi.poder || '—')}</span>
                <span><b>Fraqueza</b>${esc(heroi.fraqueza || '—')}</span>
            </span>
            ${heroi.forca === undefined ? '' : forcaBar(heroi.forca)}
        </span>
    </a>`;
}

async function mapaForcas() {
    try {
        const { list } = await lookup('herois');
        return new Map(list.map(heroi => [heroi.id, heroi.forca]));
    } catch {
        return new Map();
    }
}

export async function renderHerois(root, ctx) {
    const params = ctx.params;
    const busca = params.get('busca') || '';
    const nivel = NIVEIS[params.get('nivel')] ? params.get('nivel') : '';
    const meus = params.get('meus') === '1' && meuId() !== null;
    const page = Math.max(0, Number(params.get('page')) || 0);
    const sort = ORDENS.some(option => option.value === params.get('sort')) ? params.get('sort') : 'nomeHeroico,asc';
    const link = extra => {
        const search = new URLSearchParams({ sort, ...extra });
        return `#/herois?${search.toString()}`;
    };

    root.innerHTML = `<section class="page">
        <div class="page-header">
            <div>
                <span class="kicker">Arquivo de heróis</span>
                <h1 class="page-title">Heróis</h1>
                <p class="page-subtitle">Todos os heróis do universo. Qualquer pessoa consulta; para recrutar, entre com o seu perfil.</p>
            </div>
            <button class="btn btn-primary" data-novo>+ Recrutar herói</button>
        </div>
        <div class="controls">
            <nav class="segmented" aria-label="Filtrar por nível">
                <a href="${link({})}" class="${!nivel && !meus && !busca ? 'is-active' : ''}">Todos</a>
                ${Object.entries(NIVEIS).map(([value, info]) => `<a href="${link({ nivel: value })}" class="${nivel === value ? 'is-active' : ''}">${info.label}</a>`).join('')}
                ${meuId() !== null ? `<a href="${link({ meus: '1' })}" class="${meus ? 'is-active' : ''}" data-meus>Meus</a>` : ''}
            </nav>
            <form class="search" data-toolbar role="search">
                <label class="sr-only" for="busca">Buscar heróis</label>
                <input id="busca" name="busca" type="search" value="${esc(busca)}" placeholder="Nome ou nome heroico" maxlength="100">
                <button class="btn btn-secondary" type="submit">Buscar</button>
            </form>
            <label class="select-inline">Ordenar
                <select id="sort">${ORDENS.map(option => `<option value="${option.value}" ${option.value === sort ? 'selected' : ''}>${option.label}</option>`).join('')}</select>
            </label>
        </div>
        <div data-results>${loading('Varrendo o arquivo...')}</div>
    </section>`;

    root.querySelector('[data-toolbar]').addEventListener('submit', event => {
        event.preventDefault();
        ctx.navigate('/herois', { busca: event.target.busca.value.trim(), sort });
    });
    root.querySelector('#sort').addEventListener('change', event => {
        ctx.navigate('/herois', { busca, nivel, meus: meus ? '1' : '', sort: event.target.value });
    });
    root.querySelector('[data-novo]').addEventListener('click', () =>
        abrirFormHeroi({ onSaved: heroi => ctx.navigate(`/herois/${heroi.id}`) }));

    const results = root.querySelector('[data-results]');
    async function load() {
        results.innerHTML = loading('Varrendo o arquivo...');
        const paging = { page, size: PAGE_SIZE, sort };
        let path = `/api/herois/v2${query(paging)}`;
        let v1 = true;
        let descricao = '';
        if (busca) {
            path = `/api/herois/v1/busca${query({ nome: busca, ...paging })}`;
            descricao = `Resultados para “${busca}”`;
        } else if (nivel) {
            path = `/api/herois/v1/nivel/${nivel}${query(paging)}`;
            descricao = `Nível ${NIVEIS[nivel].label}`;
        } else if (meus) {
            path = `/api/herois/v1/perfil/${meuId()}${query(paging)}`;
            descricao = 'Os seus heróis';
        } else {
            v1 = false;
        }
        try {
            const [data, forcas] = await Promise.all([api.get(path), v1 ? mapaForcas() : Promise.resolve(null)]);
            if (!ctx.isCurrent()) return;
            const lista = items(data, 'herois').map(heroi => (v1 ? deV1(heroi, forcas) : heroi));
            const info = pageInfo(data);
            if (!lista.length) {
                results.innerHTML = emptyState(descricao ? 'Nenhum herói encontrado' : 'O arquivo está vazio',
                    meus ? 'Recrute o seu primeiro herói.' : (descricao ? 'Tente outro filtro.' : 'Recrute o primeiro herói.'));
                return;
            }
            const pager = pagination(info, newPage => ctx.navigate('/herois', { busca, nivel, meus: meus ? '1' : '', sort, page: newPage }));
            results.innerHTML = `<p class="results-info">${descricao ? `${esc(descricao)} · ` : ''}${info.totalElements} herói(s)${descricao ? ' · <a href="#/herois">limpar</a>' : ''}</p>
                <div class="card-grid">${lista.map(heroCard).join('')}</div>
                <div style="margin-top:24px">${pager.html}</div>`;
            pager.bind(results);
        } catch (error) {
            if (!ctx.isCurrent()) return;
            results.innerHTML = errorState(error);
            results.querySelector('[data-action="retry"]').addEventListener('click', load);
        }
    }
    load();
}

const ASIDE_HEROI = `<h3>Ficha de recrutamento</h3>
    <p>O herói nasce no seu perfil, e só você poderá alterá-lo ou excluí-lo.</p>
    <ul><li>Use nomes inventados.</li><li>A força (0 a 100) é um campo da v2 da API.</li><li>Poder e fraqueza são cadastrados depois, no dossiê.</li></ul>`;

export function abrirFormHeroi({ heroi = null, onSaved }) {
    const editando = Boolean(heroi);
    const idempotencyKey = newIdempotencyKey();
    const fields = [
        { name: 'nomeHeroico', label: 'Nome heroico', required: true, maxLength: 100, placeholder: 'Ex.: Cometa Violeta' },
        { name: 'nome', label: 'Nome civil', required: true, maxLength: 100, placeholder: 'Ex.: Maia Duarte' },
        { name: 'nivelForca', label: 'Nível de força', type: 'select', required: true, options: Object.entries(NIVEIS).map(([value, info]) => ({ value, label: info.label })) },
        { name: 'forca', label: 'Força (0 a 100)', type: 'number', required: true, min: 0, max: 100, help: 'Campo da v2.' }
    ];
    const modal = formModal({
        title: editando ? `Editar ${heroi.nomeHeroico}` : 'Recrutar herói',
        aside: ASIDE_HEROI,
        fields,
        initial: editando ? heroi : { forca: 50 },
        submitLabel: editando ? 'Salvar' : 'Recrutar',
        extraActions: editando ? '' : '<button type="button" class="btn btn-ghost" data-ficticio>Gerar fictício</button>',
        onSubmit: async (values, current) => {
            if (editando) {
                const salvo = await withLogin(() => api.update(`/api/herois/v2/${heroi.id}`, values));
                if (!salvo) return;
                invalidateLookups('herois', 'equipes');
                toast('Herói atualizado.', 'success');
                current.close();
                onSaved(salvo);
            } else {
                const resultado = await withLogin(() => api.create('/api/herois/v2', values, idempotencyKey, true), 'Recrutar um herói exige login: ele nasce no seu perfil.');
                if (!resultado) return;
                invalidateLookups('herois', 'equipes');
                toast(resultado.status === 200 ? 'Este recrutamento já tinha sido feito.' : 'Herói recrutado!', resultado.status === 200 ? 'warn' : 'success');
                current.close();
                onSaved(resultado.data);
            }
        }
    });
    const botao = modal.form.querySelector('[data-ficticio]');
    if (botao) botao.addEventListener('click', () => fillForm(modal.form, heroiFicticio()));
    return modal;
}

export function abrirFormHabilidade({ tipo, item = null, heroiId, herois = null, onSaved }) {
    const poder = tipo === 'poder';
    const base = poder ? '/api/poderes/v1' : '/api/fraquezas/v1';
    const editando = Boolean(item);
    const idempotencyKey = newIdempotencyKey();
    const fields = [
        { name: 'nome', label: poder ? 'Nome do poder' : 'Nome da fraqueza', required: true, maxLength: 100 },
        { name: 'descricao', label: 'Descrição', type: 'textarea', required: true, maxLength: 500 }
    ];
    if (herois) {
        fields.push({ name: 'heroiId', label: 'Herói', type: 'select', required: true, numeric: true, options: herois.map(heroi => ({ value: heroi.id, label: heroi.nomeHeroico })) });
    }
    const modal = formModal({
        title: editando ? `Editar ${poder ? 'poder' : 'fraqueza'}` : (poder ? 'Novo poder' : 'Nova fraqueza'),
        aside: `<h3>${poder ? 'Poder' : 'Fraqueza'}</h3><p>Cada herói tem no máximo ${poder ? 'um poder' : 'uma fraqueza'}, e só o dono do herói pode cadastrar ou alterar.</p>`,
        fields,
        initial: editando ? { nome: item.nome, descricao: item.descricao, heroiId: item.heroiId } : { heroiId },
        submitLabel: editando ? 'Salvar' : 'Cadastrar',
        extraActions: editando ? '' : '<button type="button" class="btn btn-ghost" data-ficticio>Gerar fictício</button>',
        onSubmit: async (values, current) => {
            const body = { heroiId, ...values };
            if (editando) {
                const salvo = await withLogin(() => api.update(`${base}/${item.id}`, body));
                if (!salvo) return;
                toast('Alteração salva.', 'success');
                current.close();
                onSaved(salvo);
            } else {
                const resultado = await withLogin(() => api.create(base, body, idempotencyKey, true));
                if (!resultado) return;
                toast(poder ? 'Poder cadastrado!' : 'Fraqueza cadastrada!', 'success');
                current.close();
                onSaved(resultado.data);
            }
            invalidateLookups('herois');
        }
    });
    const botao = modal.form.querySelector('[data-ficticio]');
    if (botao) botao.addEventListener('click', () => fillForm(modal.form, poder ? poderFicticio() : fraquezaFicticia()));
    return modal;
}

export async function excluirHabilidade(tipo, item, onDeleted) {
    const poder = tipo === 'poder';
    const ok = await confirmDialog({ title: poder ? 'Excluir poder' : 'Excluir fraqueza', message: `Excluir “${item.nome}”? O herói continua cadastrado.`, confirmLabel: 'Excluir', danger: true });
    if (!ok) return;
    try {
        const result = await withLogin(() => api.remove(`${poder ? '/api/poderes/v1' : '/api/fraquezas/v1'}/${item.id}`));
        if (!result) return;
        invalidateLookups('herois');
        toast(poder ? 'Poder excluído.' : 'Fraqueza excluída.', 'success');
        onDeleted();
    } catch (error) {
        showError(error);
    }
}

function habilidade(tipo, id, meu) {
    const poder = tipo === 'poder';
    if (!id) {
        return `<div class="state" style="padding:34px 20px"><p class="state-title" style="font-size:22px">Sem ${poder ? 'poder' : 'fraqueza'}</p>
            ${meu ? `<button class="btn btn-primary" data-novo-${tipo}>${poder ? '+ Cadastrar poder' : '+ Cadastrar fraqueza'}</button>` : '<p>Só o dono do herói pode cadastrar.</p>'}</div>`;
    }
    return `<div data-${tipo}-box>${loading()}</div>`;
}

export async function renderHeroiDetalhe(root, ctx, id) {
    root.innerHTML = `<section class="page">${loading('Abrindo o dossiê...')}</section>`;
    let v1;
    let v2;
    try {
        [v2, v1] = await Promise.all([api.get(`/api/herois/v2/${id}`), api.get(`/api/herois/v1/${id}`)]);
    } catch (error) {
        if (!ctx.isCurrent()) return;
        root.innerHTML = `<section class="page"><p class="breadcrumb"><a href="#/herois">Heróis</a> / #${id}</p>${errorState(error)}</section>`;
        root.querySelector('[data-action="retry"]').addEventListener('click', () => renderHeroiDetalhe(root, ctx, id));
        return;
    }
    if (!ctx.isCurrent()) return;
    const meu = v2.perfilId === meuId();
    const recarregar = () => renderHeroiDetalhe(root, ctx, id);

    root.innerHTML = `<section class="page">
        <p class="breadcrumb"><a href="#/herois">Heróis</a> / ${esc(v2.nomeHeroico)}</p>
        <div class="dossier">
            <div>${heroCard(v2)}</div>
            <div class="panel corner">
                <div class="page-header" style="margin-bottom:18px">
                    <div>
                        <span class="kicker">Dossiê #${esc(String(v2.id).padStart(3, '0'))}</span>
                        <h1 class="page-title">${esc(v2.nomeHeroico)}</h1>
                        <p class="page-subtitle">${esc(v2.nome)} · perfil <strong>${esc(v2.perfil)}</strong>${meu ? ' · <span class="mine-flag">seu herói</span>' : ''}</p>
                    </div>
                    ${meu ? `<div class="actions"><button class="btn btn-secondary" data-edit>Editar</button><button class="btn btn-danger" data-delete>Excluir</button></div>` : ''}
                </div>
                <div class="tabs" role="tablist">
                    <button type="button" class="is-active" data-tab="geral">Visão geral</button>
                    <button type="button" data-tab="poder">Poder</button>
                    <button type="button" data-tab="fraqueza">Fraqueza</button>
                    <button type="button" data-tab="api">API</button>
                </div>
                <div data-pane="geral">
                    <dl class="spec">
                        <div><dt>Nível</dt><dd>${nivelBadge(v2.nivelForca)}</dd></div>
                        <div><dt>Força</dt><dd>${v2.forca ?? '<span class="muted">sem medição</span>'}</dd></div>
                        <div><dt>Poder</dt><dd>${esc(v2.poder || '—')}</dd></div>
                        <div><dt>Fraqueza</dt><dd>${esc(v2.fraqueza || '—')}</dd></div>
                        <div style="grid-column:1/-1"><dt>Equipes</dt><dd>${(v1.equipes || []).length
                            ? `<span class="roster">${v1.equipes.map(equipe => `<a class="member" href="#/equipes?q=${encodeURIComponent(equipe.nome)}">${esc(equipe.nome)}</a>`).join('')}</span>`
                            : '<span class="muted">Ainda não faz parte de nenhuma equipe.</span>'}</dd></div>
                    </dl>
                    ${meu ? '' : '<p class="muted" style="margin:16px 0 0;font-size:13px">Este herói é de outro perfil: você pode consultar, mas não alterar.</p>'}
                </div>
                <div data-pane="poder" hidden>${habilidade('poder', v1.poderId, meu)}</div>
                <div data-pane="fraqueza" hidden>${habilidade('fraqueza', v1.fraquezaId, meu)}</div>
                <div data-pane="api" hidden>
                    <p class="muted" style="margin-top:0">Links HATEOAS da v1: cada resposta traz os próximos passos possíveis.</p>
                    ${linksPanel(v1._links)}
                    <details class="raw" style="margin-top:14px"><summary>Resposta JSON da v1</summary>${jsonBlock(v1)}</details>
                    <details class="raw" style="margin-top:8px"><summary>Resposta JSON da v2</summary>${jsonBlock(v2)}</details>
                </div>
            </div>
        </div>
    </section>`;

    root.querySelectorAll('[data-tab]').forEach(botao => botao.addEventListener('click', () => {
        root.querySelectorAll('[data-tab]').forEach(outro => outro.classList.toggle('is-active', outro === botao));
        root.querySelectorAll('[data-pane]').forEach(pane => { pane.hidden = pane.dataset.pane !== botao.dataset.tab; });
    }));

    for (const [tipo, itemId, path] of [['poder', v1.poderId, '/api/poderes/v1'], ['fraqueza', v1.fraquezaId, '/api/fraquezas/v1']]) {
        const novo = root.querySelector(`[data-novo-${tipo}]`);
        if (novo) novo.addEventListener('click', () => abrirFormHabilidade({ tipo, heroiId: id, onSaved: recarregar }));
        if (!itemId) continue;
        api.get(`${path}/${itemId}`).then(item => {
            const box = root.querySelector(`[data-${tipo}-box]`);
            if (!box || !ctx.isCurrent()) return;
            box.innerHTML = `<div class="ability ${tipo === 'fraqueza' ? 'weak' : ''}">
                <span class="kicker">${tipo === 'poder' ? 'Poder' : 'Fraqueza'}</span>
                <h3>${esc(item.nome)}</h3><p>${esc(item.descricao)}</p>
                ${meu ? `<div class="actions"><button class="btn btn-secondary btn-sm" data-edit-${tipo}>Editar</button><button class="btn btn-danger btn-sm" data-remove-${tipo}>Excluir</button></div>` : ''}
            </div>`;
            const editar = box.querySelector(`[data-edit-${tipo}]`);
            if (editar) editar.addEventListener('click', () => abrirFormHabilidade({ tipo, item, heroiId: id, onSaved: recarregar }));
            const remover = box.querySelector(`[data-remove-${tipo}]`);
            if (remover) remover.addEventListener('click', () => excluirHabilidade(tipo, item, recarregar));
        }).catch(error => {
            const box = root.querySelector(`[data-${tipo}-box]`);
            if (box) box.innerHTML = errorState(error);
        });
    }

    const editar = root.querySelector('[data-edit]');
    if (editar) editar.addEventListener('click', () => abrirFormHeroi({
        heroi: { id: v2.id, nome: v2.nome, nomeHeroico: v2.nomeHeroico, nivelForca: v2.nivelForca, forca: v2.forca ?? 50 },
        onSaved: recarregar
    }));
    const excluir = root.querySelector('[data-delete]');
    if (excluir) excluir.addEventListener('click', async () => {
        const ok = await confirmDialog({ title: 'Excluir herói', message: `Excluir ${v2.nomeHeroico}? O poder e a fraqueza vão junto, e ele sai das equipes.`, confirmLabel: 'Excluir', danger: true });
        if (!ok) return;
        try {
            const result = await withLogin(() => api.remove(`/api/herois/v1/${id}`));
            if (!result) return;
            invalidateLookups('herois', 'equipes');
            toast('Herói excluído.', 'success');
            ctx.navigate('/herois');
        } catch (error) {
            showError(error);
        }
    });
}
