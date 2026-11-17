import { api, items, pageInfo, query } from '../api.js';
import { esc, loading, emptyState, errorState, toast, showError, pagination } from '../ui.js';
import { meuId, abrirFormHabilidade, excluirHabilidade } from './herois.js';

const PAGE_SIZE = 10;

const TIPOS = {
    poderes: { tipo: 'poder', titulo: 'Poderes', rel: 'poderes', path: '/api/poderes/v1', novo: '+ Novo poder', campo: 'poderId' },
    fraquezas: { tipo: 'fraqueza', titulo: 'Fraquezas', rel: 'fraquezas', path: '/api/fraquezas/v1', novo: '+ Nova fraqueza', campo: 'fraquezaId' }
};

export async function renderArsenal(root, ctx) {
    const chave = TIPOS[ctx.params.get('tipo')] ? ctx.params.get('tipo') : 'poderes';
    const config = TIPOS[chave];
    const busca = ctx.params.get('busca') || '';
    const page = Math.max(0, Number(ctx.params.get('page')) || 0);

    root.innerHTML = `<section class="page">
        <div class="page-header">
            <div>
                <span class="kicker">Arsenal</span>
                <h1 class="page-title">${config.titulo}</h1>
                <p class="page-subtitle">Cada herói tem no máximo um poder e uma fraqueza. Só o dono do herói cadastra, altera ou exclui.</p>
            </div>
            <button class="btn btn-primary" data-novo>${config.novo}</button>
        </div>
        <div class="controls">
            <nav class="segmented" aria-label="Tipo">
                <a href="#/arsenal?tipo=poderes" class="${chave === 'poderes' ? 'is-active' : ''}">Poderes</a>
                <a href="#/arsenal?tipo=fraquezas" class="${chave === 'fraquezas' ? 'is-active' : ''}">Fraquezas</a>
            </nav>
            <form class="search" data-toolbar role="search">
                <label class="sr-only" for="busca">Buscar</label>
                <input id="busca" name="busca" type="search" value="${esc(busca)}" placeholder="Nome ${chave === 'poderes' ? 'do poder' : 'da fraqueza'}" maxlength="100">
                <button class="btn btn-secondary" type="submit">Buscar</button>
            </form>
        </div>
        <div data-results>${loading()}</div>
    </section>`;

    root.querySelector('[data-toolbar]').addEventListener('submit', event => {
        event.preventDefault();
        ctx.navigate('/arsenal', { tipo: chave, busca: event.target.busca.value.trim() });
    });

    root.querySelector('[data-novo]').addEventListener('click', async () => {
        if (meuId() === null) {
            toast('Entre com o seu perfil para cadastrar: só o dono do herói pode.', 'warn');
            ctx.navigate('/conta');
            return;
        }
        try {
            const data = await api.get(`/api/herois/v1/perfil/${meuId()}${query({ size: 200, sort: 'nomeHeroico,asc' })}`);
            const livres = items(data, 'herois').filter(heroi => !heroi[config.campo]);
            if (!livres.length) {
                toast(`Todos os seus heróis já têm ${config.tipo === 'poder' ? 'poder' : 'fraqueza'} (ou você ainda não recrutou nenhum).`, 'warn');
                return;
            }
            abrirFormHabilidade({ tipo: config.tipo, heroiId: livres[0].id, herois: livres, onSaved: () => ctx.refresh() });
        } catch (error) {
            showError(error);
        }
    });

    const results = root.querySelector('[data-results]');
    async function load() {
        results.innerHTML = loading();
        const paging = { page, size: PAGE_SIZE, sort: 'nome,asc' };
        const path = busca ? `${config.path}/busca${query({ nome: busca, ...paging })}` : `${config.path}${query(paging)}`;
        try {
            const [data, meus] = await Promise.all([
                api.get(path),
                meuId() !== null ? api.get(`/api/herois/v1/perfil/${meuId()}${query({ size: 200 })}`) : Promise.resolve(null)
            ]);
            if (!ctx.isCurrent()) return;
            const meusIds = new Set(items(meus, 'herois').map(heroi => heroi.id));
            const lista = items(data, config.rel);
            if (!lista.length) {
                results.innerHTML = emptyState(busca ? 'Nada encontrado' : `Nenhum registro em ${config.titulo.toLowerCase()}`);
                return;
            }
            const pager = pagination(pageInfo(data), newPage => ctx.navigate('/arsenal', { tipo: chave, busca, page: newPage }));
            results.innerHTML = `${busca ? `<p class="results-info">Resultados para “${esc(busca)}” · <a href="#/arsenal?tipo=${chave}">limpar</a></p>` : ''}
                <div class="table-wrap"><table>
                    <thead><tr><th>ID</th><th>Nome</th><th>Descrição</th><th>Herói</th><th><span class="sr-only">Ações</span></th></tr></thead>
                    <tbody>${lista.map((item, index) => {
                        const meu = meusIds.has(item.heroiId);
                        return `<tr class="${meu ? 'row-mine' : ''}">
                            <td class="num">#${esc(item.id)}</td>
                            <td class="cell-strong">${esc(item.nome)}${meu ? '<span class="tag-mine">seu</span>' : ''}</td>
                            <td class="cell-clip" title="${esc(item.descricao)}">${esc(item.descricao)}</td>
                            <td><a href="#/herois/${item.heroiId}">${esc(item.heroiNomeHeroico)}</a></td>
                            <td><div class="actions">${meu ? `<button class="btn btn-secondary btn-sm" data-edit="${index}">Editar</button><button class="btn btn-danger btn-sm" data-remove="${index}">Excluir</button>` : ''}</div></td>
                        </tr>`;
                    }).join('')}</tbody>
                </table></div>
                <div style="margin-top:18px">${pager.html}</div>`;
            pager.bind(results);
            results.querySelectorAll('[data-edit]').forEach(botao => botao.addEventListener('click', () => {
                const item = lista[Number(botao.dataset.edit)];
                abrirFormHabilidade({ tipo: config.tipo, item, heroiId: item.heroiId, onSaved: () => ctx.refresh() });
            }));
            results.querySelectorAll('[data-remove]').forEach(botao => botao.addEventListener('click', () =>
                excluirHabilidade(config.tipo, lista[Number(botao.dataset.remove)], () => ctx.refresh())));
        } catch (error) {
            if (!ctx.isCurrent()) return;
            results.innerHTML = errorState(error);
            results.querySelector('[data-action="retry"]').addEventListener('click', load);
        }
    }
    load();
}
