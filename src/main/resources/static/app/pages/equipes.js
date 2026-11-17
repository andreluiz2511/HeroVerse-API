import { api, items, pageInfo, lookup, query, newIdempotencyKey, invalidateLookups } from '../api.js';
import {
    esc, iniciais, loading, emptyState, errorState, toast, showError,
    formModal, confirmDialog, withLogin, pagination, fillForm
} from '../ui.js';
import { equipeFicticia } from '../ficticio.js';
import { meuId } from './herois.js';

const PAGE_SIZE = 8;

export function abrirFormEquipe({ equipe = null, onSaved }) {
    const editando = Boolean(equipe);
    const idempotencyKey = newIdempotencyKey();
    const modal = formModal({
        title: editando ? 'Renomear equipe' : 'Nova equipe',
        aside: '<h3>Equipe</h3><p>A equipe nasce no seu perfil e começa vazia. Depois é só adicionar os seus heróis.</p><ul><li>Só heróis do seu perfil entram nas suas equipes.</li><li>Um herói pode estar em várias equipes.</li></ul>',
        fields: [{ name: 'nome', label: 'Nome da equipe', required: true, maxLength: 100, placeholder: 'Ex.: Patrulha Boreal' }],
        initial: editando ? { nome: equipe.nome } : {},
        submitLabel: editando ? 'Salvar' : 'Criar equipe',
        extraActions: editando ? '' : '<button type="button" class="btn btn-ghost" data-ficticio>Gerar fictício</button>',
        onSubmit: async (values, current) => {
            if (editando) {
                const salvo = await withLogin(() => api.update(`/api/equipes/v1/${equipe.id}`, values));
                if (!salvo) return;
                toast('Equipe atualizada.', 'success');
                current.close();
                onSaved(salvo);
            } else {
                const resultado = await withLogin(() => api.create('/api/equipes/v1', values, idempotencyKey, true), 'Criar uma equipe exige login: ela nasce no seu perfil.');
                if (!resultado) return;
                toast('Equipe criada!', 'success');
                current.close();
                onSaved(resultado.data);
            }
            invalidateLookups('equipes');
        }
    });
    const botao = modal.form.querySelector('[data-ficticio]');
    if (botao) botao.addEventListener('click', () => fillForm(modal.form, equipeFicticia()));
    return modal;
}

async function adicionarMembro(equipe, onSaved) {
    let meus = [];
    try {
        const data = await api.get(`/api/herois/v1/perfil/${meuId()}${query({ size: 200, sort: 'nomeHeroico,asc' })}`);
        const atuais = new Set(equipe.herois.map(heroi => heroi.id));
        meus = items(data, 'herois').filter(heroi => !atuais.has(heroi.id));
    } catch (error) {
        showError(error);
        return;
    }
    if (!meus.length) {
        toast('Todos os seus heróis já estão nesta equipe (ou você ainda não recrutou nenhum).', 'warn');
        return;
    }
    formModal({
        title: `Adicionar a ${equipe.nome}`,
        fields: [{ name: 'heroiId', label: 'Herói', type: 'select', required: true, numeric: true, options: meus.map(heroi => ({ value: heroi.id, label: heroi.nomeHeroico })) }],
        submitLabel: 'Adicionar',
        onSubmit: async (values, current) => {
            const salvo = await withLogin(() => api.create(`/api/equipes/v1/${equipe.id}/herois/${values.heroiId}`, undefined, null, true));
            if (!salvo) return;
            invalidateLookups('equipes', 'herois');
            toast('Herói adicionado à equipe.', 'success');
            current.close();
            onSaved();
        }
    });
}

export async function renderEquipes(root, ctx) {
    const busca = ctx.params.get('busca') || ctx.params.get('q') || '';
    const minhas = ctx.params.get('minhas') === '1' && meuId() !== null;
    const page = Math.max(0, Number(ctx.params.get('page')) || 0);

    root.innerHTML = `<section class="page">
        <div class="page-header">
            <div>
                <span class="kicker">Formações</span>
                <h1 class="page-title">Equipes</h1>
                <p class="page-subtitle">Cada equipe pertence a um perfil e reúne heróis desse perfil. O medidor mostra a força média da equipe.</p>
            </div>
            <button class="btn btn-primary" data-novo>+ Nova equipe</button>
        </div>
        <div class="controls">
            <nav class="segmented" aria-label="Mostrar">
                <a href="#/equipes" class="${!minhas && !busca ? 'is-active' : ''}">Todas</a>
                ${meuId() !== null ? `<a href="#/equipes?minhas=1" class="${minhas ? 'is-active' : ''}">Minhas</a>` : ''}
            </nav>
            <form class="search" data-toolbar role="search">
                <label class="sr-only" for="busca">Buscar equipes</label>
                <input id="busca" name="busca" type="search" value="${esc(busca)}" placeholder="Nome da equipe" maxlength="100">
                <button class="btn btn-secondary" type="submit">Buscar</button>
            </form>
        </div>
        <div data-results>${loading('Reunindo as equipes...')}</div>
    </section>`;

    root.querySelector('[data-toolbar]').addEventListener('submit', event => {
        event.preventDefault();
        ctx.navigate('/equipes', { busca: event.target.busca.value.trim() });
    });
    root.querySelector('[data-novo]').addEventListener('click', () => abrirFormEquipe({ onSaved: () => ctx.navigate('/equipes', { minhas: '1' }) }));

    const results = root.querySelector('[data-results]');
    async function load() {
        results.innerHTML = loading('Reunindo as equipes...');
        const paging = { page, size: PAGE_SIZE, sort: 'nome,asc' };
        const path = busca
            ? `/api/equipes/v1/busca${query({ nome: busca, ...paging })}`
            : minhas ? `/api/equipes/v1/perfil/${meuId()}${query(paging)}` : `/api/equipes/v1${query(paging)}`;
        try {
            const [data, resumo] = await Promise.all([api.get(path), lookup('equipes').catch(() => ({ list: [] }))]);
            const lista = items(data, 'equipes');
            const medias = new Map(resumo.list.map(equipe => [equipe.id, equipe.forcaMedia]));
            if (!ctx.isCurrent()) return;
            if (!lista.length) {
                results.innerHTML = emptyState(busca || minhas ? 'Nenhuma equipe encontrada' : 'Nenhuma equipe formada', 'Crie a primeira com o botão acima.');
                return;
            }
            const pager = pagination(pageInfo(data), newPage => ctx.navigate('/equipes', { busca, minhas: minhas ? '1' : '', page: newPage }));
            results.innerHTML = `${busca ? `<p class="results-info">Resultados para “${esc(busca)}” · <a href="#/equipes">limpar</a></p>` : ''}
                <div class="squads">${lista.map((equipe, index) => {
                    const minha = equipe.perfilId === meuId();
                    const media = medias.has(equipe.id) ? medias.get(equipe.id) : null;
                    return `<article class="panel squad ${minha ? 'panel-glow' : ''}" data-equipe="${equipe.id}">
                        <div class="squad-head">
                            <div>
                                <h2>${esc(equipe.nome)}</h2>
                                <span class="squad-owner">perfil ${esc(equipe.perfilNome)}${minha ? ' · <span class="mine-flag">sua equipe</span>' : ''} · ${equipe.herois.length} herói(s)</span>
                            </div>
                            <div class="gauge" style="--v:${media ?? 0}" title="Força média"><b>${media === null || media === undefined ? '—' : Math.round(media)}<small>MÉDIA</small></b></div>
                        </div>
                        <div class="roster">${equipe.herois.length
                            ? equipe.herois.map(heroi => `<span class="member"><span class="emblem" aria-hidden="true">${esc(iniciais(heroi.nomeHeroico))}</span>
                                <a href="#/herois/${heroi.id}" style="color:inherit;text-decoration:none">${esc(heroi.nomeHeroico)}</a>
                                ${minha ? `<button type="button" title="Remover da equipe" aria-label="Remover ${esc(heroi.nomeHeroico)}" data-sair="${index}:${heroi.id}">✕</button>` : ''}</span>`).join('')
                            : '<span class="muted">Equipe vazia.</span>'}</div>
                        ${minha ? `<div class="squad-foot">
                            <button class="btn btn-secondary btn-sm" data-add="${index}">+ Adicionar herói</button>
                            <span class="actions"><button class="btn btn-ghost btn-sm" data-edit="${index}">Renomear</button><button class="btn btn-danger btn-sm" data-remove="${index}">Excluir</button></span>
                        </div>` : ''}
                    </article>`;
                }).join('')}</div>
                <div style="margin-top:22px">${pager.html}</div>`;
            pager.bind(results);
            results.querySelectorAll('[data-add]').forEach(botao => botao.addEventListener('click', () => adicionarMembro(lista[Number(botao.dataset.add)], () => ctx.refresh())));
            results.querySelectorAll('[data-edit]').forEach(botao => botao.addEventListener('click', () => abrirFormEquipe({ equipe: lista[Number(botao.dataset.edit)], onSaved: () => ctx.refresh() })));
            results.querySelectorAll('[data-sair]').forEach(botao => botao.addEventListener('click', async () => {
                const [index, heroiId] = botao.dataset.sair.split(':').map(Number);
                try {
                    const salvo = await withLogin(() => api.remove(`/api/equipes/v1/${lista[index].id}/herois/${heroiId}`));
                    if (!salvo) return;
                    invalidateLookups('equipes', 'herois');
                    toast('Herói removido da equipe.', 'success');
                    ctx.refresh();
                } catch (error) {
                    showError(error);
                }
            }));
            results.querySelectorAll('[data-remove]').forEach(botao => botao.addEventListener('click', async () => {
                const equipe = lista[Number(botao.dataset.remove)];
                const ok = await confirmDialog({ title: 'Excluir equipe', message: `Excluir ${equipe.nome}? Os heróis continuam cadastrados.`, confirmLabel: 'Excluir', danger: true });
                if (!ok) return;
                try {
                    const result = await withLogin(() => api.remove(`/api/equipes/v1/${equipe.id}`));
                    if (!result) return;
                    invalidateLookups('equipes', 'herois');
                    toast('Equipe excluída.', 'success');
                    ctx.refresh();
                } catch (error) {
                    showError(error);
                }
            }));
        } catch (error) {
            if (!ctx.isCurrent()) return;
            results.innerHTML = errorState(error);
            results.querySelector('[data-action="retry"]').addEventListener('click', load);
        }
    }
    load();
}
