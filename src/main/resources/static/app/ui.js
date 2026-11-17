import { ApiError, api, auth } from './api.js';

export function esc(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

export function formatDate(value) {
    if (!value) return '—';
    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (!match) return '—';
    return `${match[3]}/${match[2]}/${match[1]}`;
}

export const NIVEIS = {
    HUMANA: { label: 'Humana', cor: 'nivel-humana' },
    SUPERHUMANO: { label: 'Super-humano', cor: 'nivel-superhumano' },
    ALIENIGENA: { label: 'Alienígena', cor: 'nivel-alienigena' },
    MUTANTE: { label: 'Mutante', cor: 'nivel-mutante' }
};

export function iniciais(nome) {
    const partes = String(nome || '?').trim().split(/\s+/).filter(parte => parte.length > 2 || /^[A-ZÁÉÍÓÚ]/.test(parte));
    const letras = (partes.length > 1 ? [partes[0], partes[partes.length - 1]] : [partes[0] || '?']).map(parte => parte[0]);
    return letras.join('').toUpperCase();
}

export function nivelBadge(nivel) {
    const info = NIVEIS[nivel] || { label: nivel || '—', cor: '' };
    return `<span class="tag ${info.cor}">${esc(info.label)}</span>`;
}

export function forcaBar(forca) {
    if (forca === null || forca === undefined) {
        return '<div class="power"><span class="power-label">Força</span><span class="power-empty">sem medição (v1)</span></div>';
    }
    return `<div class="power" title="Força ${esc(forca)} de 100"><span class="power-label">Força</span>
        <span class="power-track"><span class="power-fill" style="width:${Math.max(0, Math.min(100, forca))}%"></span></span>
        <span class="power-value">${esc(forca)}</span></div>`;
}

export function loading(text = 'Carregando...') {
    return `<div class="state"><div class="radar" aria-hidden="true"></div><p>${esc(text)}</p></div>`;
}

export function emptyState(title, text = '') {
    return `<div class="state"><p class="state-title">${esc(title)}</p>${text ? `<p>${esc(text)}</p>` : ''}</div>`;
}

export function errorState(error, retryLabel = 'Tentar de novo') {
    const message = error instanceof ApiError ? error.message : 'Algo deu errado ao carregar esta tela.';
    return `<div class="state state-error"><p class="state-title">Não foi possível carregar</p><p>${esc(message)}</p>
        <button class="btn btn-secondary" data-action="retry">${esc(retryLabel)}</button></div>`;
}

let toastHost;
export function toast(message, type = 'info') {
    if (!toastHost) {
        toastHost = document.createElement('div');
        toastHost.className = 'toast-host';
        toastHost.setAttribute('role', 'status');
        toastHost.setAttribute('aria-live', 'polite');
        document.body.appendChild(toastHost);
    }
    while (toastHost.children.length >= 3) toastHost.firstElementChild.remove();
    const item = document.createElement('div');
    item.className = `toast toast-${type}`;
    item.textContent = message;
    toastHost.appendChild(item);
    setTimeout(() => item.classList.add('toast-hide'), 4200);
    setTimeout(() => item.remove(), 4700);
}

export function showError(error) {
    if (error instanceof ApiError && error.status === 429) return;
    toast(error instanceof ApiError ? error.message : 'Algo deu errado. Tente novamente.', 'error');
}

export function openModal({ title, body, wide = false, confirm = false, aside = '', onClose }) {
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop';
    backdrop.innerHTML = `
        <div class="modal ${wide ? 'modal-wide' : ''} ${confirm ? 'modal-confirm' : ''} ${aside ? 'modal-split' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-title">
            ${aside ? `<aside class="modal-aside">${aside}</aside>` : ''}
            <div class="modal-main">
                <div class="modal-header">
                    <h2 id="modal-title">${esc(title)}</h2>
                    <button class="icon-btn" data-close aria-label="Fechar">✕</button>
                </div>
                <div class="modal-body">${body}</div>
            </div>
        </div>`;
    const previousFocus = document.activeElement;
    const close = () => {
        backdrop.remove();
        document.removeEventListener('keydown', onKey);
        if (previousFocus && previousFocus.focus) previousFocus.focus();
        if (onClose) onClose();
    };
    const onKey = event => {
        const open = document.querySelectorAll('.modal-backdrop');
        if (event.key === 'Escape' && open[open.length - 1] === backdrop) close();
    };
    backdrop.addEventListener('mousedown', event => {
        if (event.target === backdrop) close();
    });
    backdrop.querySelector('[data-close]').addEventListener('click', close);
    document.addEventListener('keydown', onKey);
    document.body.appendChild(backdrop);
    const firstInput = backdrop.querySelector('input, select, textarea, button:not([data-close])');
    if (firstInput) firstInput.focus();
    return { element: backdrop, close };
}

export function confirmDialog({ title, message, confirmLabel = 'Confirmar', danger = false }) {
    return new Promise(resolve => {
        let answered = false;
        const modal = openModal({
            title,
            confirm: true,
            body: `<p class="confirm-text">${esc(message)}</p>
                <div class="form-actions">
                    <button class="btn btn-ghost" data-answer="no">Cancelar</button>
                    <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-answer="yes">${esc(confirmLabel)}</button>
                </div>`,
            onClose: () => {
                if (!answered) resolve(false);
            }
        });
        modal.element.querySelectorAll('[data-answer]').forEach(button => {
            button.addEventListener('click', () => {
                answered = true;
                resolve(button.dataset.answer === 'yes');
                modal.close();
            });
        });
    });
}

function fieldHtml(field, value) {
    const id = `f-${field.name}`;
    const required = field.required ? 'required' : '';
    const attrs = [
        field.minLength ? `minlength="${field.minLength}"` : '',
        field.maxLength ? `maxlength="${field.maxLength}"` : '',
        field.min !== undefined ? `min="${field.min}"` : '',
        field.max !== undefined ? `max="${field.max}"` : '',
        field.pattern ? `pattern="${esc(field.pattern)}"` : '',
        field.inputmode ? `inputmode="${field.inputmode}"` : '',
        field.placeholder ? `placeholder="${esc(field.placeholder)}"` : '',
        field.autocomplete ? `autocomplete="${field.autocomplete}"` : '',
        field.title ? `title="${esc(field.title)}"` : ''
    ].join(' ');
    let input;
    if (field.type === 'textarea') {
        input = `<textarea id="${id}" name="${field.name}" rows="3" ${required} ${attrs}>${esc(value ?? '')}</textarea>`;
    } else if (field.type === 'select') {
        const options = field.options.map(option =>
            `<option value="${esc(option.value)}" ${String(option.value) === String(value ?? '') ? 'selected' : ''}>${esc(option.label)}</option>`).join('');
        input = `<select id="${id}" name="${field.name}" ${required}>
            <option value="" ${value === undefined || value === null || value === '' ? 'selected' : ''} disabled>Selecione...</option>${options}</select>`;
    } else if (field.type === 'checkboxes') {
        const selected = new Set((value || []).map(String));
        input = field.options.length
            ? `<div class="check-grid" id="${id}">${field.options.map(option => `
                <label class="check">
                    <input type="checkbox" name="${field.name}" value="${esc(option.value)}" ${selected.has(String(option.value)) ? 'checked' : ''}>
                    <span>${esc(option.label)}</span>
                </label>`).join('')}</div>`
            : `<p class="muted">${esc(field.emptyText || 'Nenhuma opção cadastrada.')}</p>`;
    } else {
        input = `<input id="${id}" name="${field.name}" type="${field.type || 'text'}" value="${esc(value ?? '')}" ${required} ${attrs}>`;
        if (field.action) {
            input = `<div class="input-with-btn">${input}<button type="button" class="btn btn-ghost btn-sm" data-field-action="${field.name}">${esc(field.action.label)}</button></div>`;
        }
    }
    return `<div class="field" data-field="${field.name}">
        <label for="${id}">${esc(field.label)}${field.required ? ' <span class="req">*</span>' : ''}</label>
        ${input}
        ${field.help ? `<small class="help">${esc(field.help)}</small>` : ''}
        <small class="field-error" aria-live="polite"></small>
    </div>`;
}

function readValues(form, fields) {
    const values = {};
    fields.forEach(field => {
        if (field.type === 'checkboxes') {
            values[field.name] = [...form.querySelectorAll(`input[name="${field.name}"]:checked`)].map(input => Number(input.value));
        } else {
            const raw = form.elements[field.name].value;
            if (field.type === 'number' || field.numeric) {
                values[field.name] = raw === '' ? null : Number(raw);
            } else if (field.type === 'date') {
                values[field.name] = raw === '' ? null : raw;
            } else {
                values[field.name] = field.trim === false ? raw : raw.trim();
            }
        }
    });
    return values;
}

function showFieldErrors(form, message) {
    const general = form.querySelector('.form-error');
    let matchedAny = false;
    message.split(';').map(part => part.trim()).forEach(part => {
        const match = part.match(/^([\w\[\].]+):\s*(.+)$/);
        if (!match) return;
        const fieldName = match[1].replace(/\[\d+\]$/, '').replace(/\[\d+\]\.?.*$/, '');
        const box = form.querySelector(`[data-field="${fieldName}"] .field-error`);
        if (box) {
            box.textContent = match[2];
            box.closest('.field').classList.add('has-error');
            matchedAny = true;
        }
    });
    general.textContent = matchedAny ? '' : message;
    general.hidden = matchedAny;
}

export function formMarkup({ fields, initial = {}, submitLabel = 'Salvar', extraActions = '', cancel = true }) {
    return `<form class="form" novalidate>
        ${fields.map(field => fieldHtml(field, initial[field.name])).join('')}
        <p class="form-error" hidden></p>
        <div class="form-actions">
            ${extraActions}
            ${cancel ? '<button type="button" class="btn btn-ghost" data-cancel>Cancelar</button>' : ''}
            <button type="submit" class="btn btn-primary">${esc(submitLabel)}</button>
        </div>
    </form>`;
}

export function attachForm(form, fields, onSubmit) {
    fields.filter(field => field.action).forEach(field => {
        const button = form.querySelector(`[data-field-action="${field.name}"]`);
        if (button) button.addEventListener('click', () => { form.elements[field.name].value = field.action.generate(); });
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        form.querySelectorAll('.field-error').forEach(box => { box.textContent = ''; });
        form.querySelectorAll('.has-error').forEach(box => box.classList.remove('has-error'));
        const generalError = form.querySelector('.form-error');
        generalError.hidden = true;

        if (!form.checkValidity()) {
            [...form.elements].forEach(element => {
                if (element.validity && !element.validity.valid) {
                    const box = element.closest('.field')?.querySelector('.field-error');
                    if (box) {
                        box.textContent = element.title && element.validity.patternMismatch ? element.title : element.validationMessage;
                        box.closest('.field').classList.add('has-error');
                    }
                }
            });
            return;
        }

        const submit = form.querySelector('button[type="submit"]');
        submit.disabled = true;
        submit.classList.add('is-loading');
        try {
            await onSubmit(readValues(form, fields));
        } catch (error) {
            if (error instanceof ApiError && error.status !== 429) {
                showFieldErrors(form, error.message);
            } else {
                generalError.textContent = error instanceof ApiError ? error.message : 'Algo deu errado. Tente novamente.';
                generalError.hidden = false;
            }
        } finally {
            if (form.isConnected) {
                submit.disabled = false;
                submit.classList.remove('is-loading');
            }
        }
    });
}

export function fillForm(form, values) {
    Object.entries(values).forEach(([name, value]) => {
        if (form.elements[name]) form.elements[name].value = value;
    });
}

export function formModal({ title, intro = '', fields, initial = {}, submitLabel = 'Salvar', onSubmit, wide = false, extraActions = '', aside = '' }) {
    const modal = openModal({
        title,
        wide,
        aside,
        body: `${intro ? `<div class="form-intro">${intro}</div>` : ''}${formMarkup({ fields, initial, submitLabel, extraActions })}`
    });
    const form = modal.element.querySelector('form');
    form.querySelector('[data-cancel]').addEventListener('click', modal.close);
    attachForm(form, fields, values => onSubmit(values, modal));
    return { ...modal, form };
}

export const TEST_ACCOUNT = '<strong>lider.alfa@exemplo.com</strong> / <strong>senha123</strong>';

export function loginModal({ reason = 'Faça login para continuar.' } = {}) {
    return new Promise(resolve => {
        let done = false;
        const modal = formModal({
            title: 'Entrar',
            intro: `<p>${esc(reason)}</p><p class="muted">Conta de teste: ${TEST_ACCOUNT}. Ainda não tem perfil? <a href="#/conta" data-go-signup>Crie um com dados fictícios</a>.</p>`,
            fields: [
                { name: 'email', label: 'E-mail', type: 'email', required: true, autocomplete: 'username' },
                { name: 'senha', label: 'Senha', type: 'password', required: true, autocomplete: 'current-password', trim: false }
            ],
            submitLabel: 'Entrar',
            onSubmit: async (values, current) => {
                const login = await api.login(values.email, values.senha);
                done = true;
                toast(`Bem-vindo(a), ${login.nome}!`, 'success');
                current.close();
                resolve(true);
            }
        });
        modal.element.querySelector('[data-go-signup]').addEventListener('click', () => modal.close());
        const observer = new MutationObserver(() => {
            if (!document.body.contains(modal.element)) {
                observer.disconnect();
                if (!done) resolve(false);
            }
        });
        observer.observe(document.body, { childList: true });
    });
}

export async function withLogin(action, reason) {
    if (!auth.isLogged()) {
        const logged = await loginModal({ reason: reason || 'Esta ação exige login.' });
        if (!logged) return undefined;
    }
    try {
        return await action();
    } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
            const logged = await loginModal({ reason: 'Sua sessão expirou ou a chave não é mais válida. Entre novamente.' });
            if (!logged) return undefined;
            return action();
        }
        throw error;
    }
}

export function pagination(info, onChange) {
    if (!info || info.totalPages <= 1) {
        return { html: info && info.totalElements ? `<p class="page-summary" style="text-align:center">${info.totalElements} registro(s)</p>` : '', bind: () => {} };
    }
    const current = info.number;
    const last = info.totalPages - 1;
    const pages = new Set([0, last, current - 1, current, current + 1].filter(page => page >= 0 && page <= last));
    const sorted = [...pages].sort((a, b) => a - b);
    let buttons = '';
    let previous = -1;
    sorted.forEach(page => {
        if (page - previous > 1) buttons += '<span class="page-gap">…</span>';
        buttons += `<button class="page-btn ${page === current ? 'is-current' : ''}" data-page="${page}" ${page === current ? 'aria-current="page"' : ''}>${page + 1}</button>`;
        previous = page;
    });
    const html = `<nav class="pagination" aria-label="Paginação">
        <button class="page-btn" data-page="${current - 1}" ${current === 0 ? 'disabled' : ''} aria-label="Página anterior">‹</button>
        ${buttons}
        <button class="page-btn" data-page="${current + 1}" ${current === last ? 'disabled' : ''} aria-label="Próxima página">›</button>
        <span class="page-summary">${info.totalElements} registro(s)</span>
    </nav>`;
    return {
        html,
        bind: root => root.querySelectorAll('.pagination [data-page]').forEach(button => {
            button.addEventListener('click', () => onChange(Number(button.dataset.page)));
        })
    };
}

export function jsonBlock(data) {
    return `<pre class="json">${esc(JSON.stringify(data, null, 2))}</pre>`;
}

export function linksPanel(links) {
    if (!links) return '<p class="muted">Sem links.</p>';
    const rows = Object.entries(links).flatMap(([rel, value]) => (Array.isArray(value) ? value : [value]).map(link => ({ rel, ...link })));
    return `<div class="table-wrap"><table>
        <thead><tr><th>rel</th><th>Método</th><th>Endereço</th></tr></thead>
        <tbody>${rows.map(link => `<tr>
            <td class="cell-strong">${esc(link.rel)}</td>
            <td><span class="method method-${esc(link.type || 'GET')}">${esc(link.type || 'GET')}</span></td>
            <td class="cell-clip"><code>${esc(link.href)}</code></td>
        </tr>`).join('')}</tbody>
    </table></div>`;
}
