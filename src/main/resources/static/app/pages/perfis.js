import { api, auth, items, pageInfo, query, newIdempotencyKey } from '../api.js';
import { esc, loading, emptyState, errorState, toast, showError, formModal, confirmDialog, withLogin, pagination, fillForm } from '../ui.js';
import { perfilFicticio } from '../ficticio.js';
import { meuId } from './herois.js';

const PAGE_SIZE = 10;

export function camposPerfil({ editando = false } = {}) {
    return [
        { name: 'nome', label: 'Nome do perfil', required: true, minLength: 3, maxLength: 60, placeholder: 'Ex.: Comandante Fictício', autocomplete: 'off' },
        { name: 'email', label: 'E-mail', type: 'email', required: true, maxLength: 120, placeholder: 'ex.: comandante@exemplo.com', autocomplete: 'off' },
        {
            name: 'senha',
            label: editando ? 'Nova senha' : 'Senha',
            type: 'password',
            required: !editando,
            minLength: 6,
            maxLength: 100,
            trim: false,
            autocomplete: 'new-password',
            help: editando ? 'Deixe em branco para manter a senha atual.' : 'Mínimo de 6 caracteres. Crie uma senha só para este teste.'
        }
    ];
}

export const AVISO_FICTICIO = '<p class="muted" style="margin-top:0">Use <strong>dados fictícios</strong>: nome inventado, e-mail de exemplo e uma senha criada só para este teste.</p>';
export const BOTAO_FICTICIO = '<button type="button" class="btn btn-ghost" data-ficticio>Gerar fictício</button>';

export function ligarFicticio(form) {
    const botao = form.querySelector('[data-ficticio]');
    if (!botao) return;
    botao.addEventListener('click', () => {
        fillForm(form, perfilFicticio());
        if (form.elements.senha) form.elements.senha.type = 'text';
        toast('Dados fictícios gerados. Anote a senha para entrar depois!', 'info');
    });
}

export function abrirFormPerfil({ perfil = null, onSaved }) {
    const editando = Boolean(perfil);
    const idempotencyKey = newIdempotencyKey();
    const modal = formModal({
        title: editando ? 'Editar perfil' : 'Novo perfil',
        aside: '<h3>Perfil</h3><p>O perfil é o seu usuário: é com ele que você entra, e é dono dos seus heróis e equipes.</p><ul><li>Cada perfil só altera o próprio cadastro.</li><li>A senha nunca é devolvida pela API.</li></ul>',
        fields: camposPerfil({ editando }),
        initial: editando ? { nome: perfil.nome, email: perfil.email } : {},
        submitLabel: editando ? 'Salvar' : 'Cadastrar',
        extraActions: editando ? '' : BOTAO_FICTICIO,
        onSubmit: async (values, current) => {
            if (editando) {
                const body = { ...values };
                if (!body.senha) delete body.senha;
                const salvo = await withLogin(() => api.update(`/api/perfis/v1/${perfil.id}`, body));
                if (!salvo) return;
                if (auth.perfil && auth.perfil.id === salvo.id) auth.updatePerfil(salvo);
                toast('Perfil atualizado.', 'success');
                current.close();
                onSaved(salvo);
            } else {
                const { status, data } = await api.create('/api/perfis/v1', values, idempotencyKey);
                toast(status === 200 ? 'Este cadastro já tinha sido feito.' : 'Perfil cadastrado!', 'success');
                current.close();
                onSaved(data);
            }
        }
    });
    ligarFicticio(modal.form);
    return modal;
}

export async function excluirPerfil(perfil, { onDeleted }) {
    const ok = await confirmDialog({
        title: 'Excluir perfil',
        message: `Excluir o perfil “${perfil.nome}”? Os heróis (com poderes e fraquezas) e as equipes dele são excluídos junto. Não dá para desfazer.`,
        confirmLabel: 'Excluir tudo',
        danger: true
    });
    if (!ok) return;
    try {
        const result = await withLogin(() => api.remove(`/api/perfis/v1/${perfil.id}`));
        if (!result) return;
        if (auth.perfil && auth.perfil.id === perfil.id) auth.clear();
        toast('Perfil excluído.', 'success');
        onDeleted();
    } catch (error) {
        showError(error);
    }
}

export async function renderPerfis(root, ctx) {
    const busca = ctx.params.get('busca') || '';
    const page = Math.max(0, Number(ctx.params.get('page')) || 0);
    root.innerHTML = `<section class="page">
        <div class="page-header">
            <div>
                <span class="kicker">Comando</span>
                <h1 class="page-title">Perfis</h1>
                <p class="page-subtitle">Cada perfil só altera e exclui o próprio cadastro.</p>
            </div>
            <button class="btn btn-primary" data-novo>+ Novo perfil</button>
        </div>
        <form class="search" data-toolbar role="search">
            <input id="busca" name="busca" type="search" value="${esc(busca)}" placeholder="Parte do e-mail" maxlength="100">
            <button class="btn btn-secondary" type="submit">Buscar</button>
        </form>
        <div data-results>${loading()}</div>
    </section>`;
    root.querySelector('[data-toolbar]').addEventListener('submit', event => {
        event.preventDefault();
        ctx.navigate('/perfis', { busca: event.target.busca.value.trim() });
    });
    root.querySelector('[data-novo]').addEventListener('click', () => abrirFormPerfil({ onSaved: () => ctx.refresh() }));

    const results = root.querySelector('[data-results]');
    async function load() {
        results.innerHTML = loading();
        const paging = { page, size: PAGE_SIZE, sort: 'nome,asc' };
        const path = busca ? `/api/perfis/v1/busca${query({ email: busca, ...paging })}` : `/api/perfis/v1${query(paging)}`;
        try {
            const data = await api.get(path);
            if (!ctx.isCurrent()) return;
            const lista = items(data, 'perfis');
            if (!lista.length) {
                results.innerHTML = emptyState('Nenhum perfil encontrado');
                return;
            }
            const pager = pagination(pageInfo(data), newPage => ctx.navigate('/perfis', { busca, page: newPage }));
            results.innerHTML = `<div class="table-wrap"><table>
                <thead><tr><th>ID</th><th>Nome</th><th>E-mail</th><th>Heróis</th><th>Equipes</th><th><span class="sr-only">Ações</span></th></tr></thead>
                <tbody>${lista.map((perfil, index) => {
                    const meu = perfil.id === meuId();
                    return `<tr class="${meu ? 'row-mine' : ''}">
                        <td class="num">#${esc(perfil.id)}</td>
                        <td class="cell-strong">${esc(perfil.nome)}${meu ? '<span class="tag-mine">você</span>' : ''}</td>
                        <td>${esc(perfil.email)}</td>
                        <td>${esc(perfil.quantidadeHerois)}</td>
                        <td>${esc(perfil.quantidadeEquipes)}</td>
                        <td><div class="actions">${meu ? `<button class="btn btn-secondary btn-sm" data-edit="${index}">Editar</button><button class="btn btn-danger btn-sm" data-remove="${index}">Excluir</button>` : ''}</div></td>
                    </tr>`;
                }).join('')}</tbody>
            </table></div>
            <div style="margin-top:18px">${pager.html}</div>`;
            pager.bind(results);
            results.querySelectorAll('[data-edit]').forEach(botao => botao.addEventListener('click', () => abrirFormPerfil({ perfil: lista[Number(botao.dataset.edit)], onSaved: () => ctx.refresh() })));
            results.querySelectorAll('[data-remove]').forEach(botao => botao.addEventListener('click', () => excluirPerfil(lista[Number(botao.dataset.remove)], { onDeleted: () => ctx.refresh() })));
        } catch (error) {
            if (!ctx.isCurrent()) return;
            results.innerHTML = errorState(error);
            results.querySelector('[data-action="retry"]').addEventListener('click', load);
        }
    }
    load();
}
