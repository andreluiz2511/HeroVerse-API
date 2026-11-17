const CHAVE_STORAGE = 'hv.apiKey';
const PERFIL_STORAGE = 'hv.perfil';
const LOOKUP_TTL_MS = 2 * 60 * 1000;

export class ApiError extends Error {
    constructor(status, message, retryAfter = null) {
        super(message);
        this.status = status;
        this.retryAfter = retryAfter;
    }
}

const authListeners = new Set();
const rateLimitListeners = new Set();

export const auth = {
    get key() {
        return sessionStorage.getItem(CHAVE_STORAGE);
    },
    get perfil() {
        try {
            return JSON.parse(sessionStorage.getItem(PERFIL_STORAGE));
        } catch {
            return null;
        }
    },
    isLogged() {
        return Boolean(this.key);
    },
    save(login) {
        sessionStorage.setItem(CHAVE_STORAGE, login.apiKey);
        sessionStorage.setItem(PERFIL_STORAGE, JSON.stringify({ id: login.perfilId, nome: login.nome, email: login.email }));
        authListeners.forEach(listener => listener());
    },
    updatePerfil(perfil) {
        if (!this.isLogged()) return;
        sessionStorage.setItem(PERFIL_STORAGE, JSON.stringify({ id: perfil.id, nome: perfil.nome, email: perfil.email }));
        authListeners.forEach(listener => listener());
    },
    clear() {
        sessionStorage.removeItem(CHAVE_STORAGE);
        sessionStorage.removeItem(PERFIL_STORAGE);
        authListeners.forEach(listener => listener());
    },
    onChange(listener) {
        authListeners.add(listener);
    }
};

export function onRateLimit(listener) {
    rateLimitListeners.add(listener);
}

export function newIdempotencyKey() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID();
    }
    const bytes = new Uint8Array(16);
    window.crypto.getRandomValues(bytes);
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = [...bytes].map(b => b.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

async function request(method, path, { body, idempotencyKey, authenticated = false } = {}) {
    const headers = { 'Accept': 'application/hal+json, application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (idempotencyKey) headers['X-Idempotency-Key'] = idempotencyKey;
    if (authenticated) {
        if (!auth.isLogged()) throw new ApiError(401, 'Esta operação exige login.');
        headers['X-API-Key'] = auth.key;
    }

    let response;
    try {
        response = await fetch(path, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined
        });
    } catch {
        throw new ApiError(0, 'Não foi possível conectar à API. Verifique se o servidor está rodando.');
    }

    const text = response.status === 204 ? '' : await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = null;
        }
    }

    if (!response.ok) {
        if (response.status === 429) {
            const retryAfter = Number(response.headers.get('Retry-After')) || 60;
            rateLimitListeners.forEach(listener => listener(retryAfter));
            throw new ApiError(429, `Muitas requisições em pouco tempo. Aguarde ${retryAfter}s e tente de novo.`, retryAfter);
        }
        if (response.status === 401 && authenticated) {
            auth.clear();
        }
        const message = (data && (data.mensagem || data.erro)) || `Erro ${response.status} ao chamar a API.`;
        throw new ApiError(response.status, message);
    }

    return { status: response.status, data };
}

export function query(params) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') search.set(key, value);
    });
    const text = search.toString();
    return text ? `?${text}` : '';
}

export const api = {
    get: path => request('GET', path).then(result => result.data),
    create: (path, body, idempotencyKey, authenticated = false) => request('POST', path, { body, idempotencyKey, authenticated }),
    update: (path, body) => request('PUT', path, { body, authenticated: true }).then(result => result.data),
    remove: path => request('DELETE', path, { authenticated: true }),
    async login(email, senha) {
        const { data } = await request('POST', '/api/auth/login', { body: { email, senha } });
        auth.save(data);
        return data;
    },
    async logout() {
        try {
            if (auth.isLogged()) await request('POST', '/api/auth/logout', { authenticated: true });
        } finally {
            auth.clear();
        }
    }
};

export function items(page, rel) {
    return (page && page._embedded && page._embedded[rel]) || [];
}

export function pageInfo(page) {
    return (page && page.page) || { size: 0, totalElements: 0, totalPages: 0, number: 0 };
}

const lookupSources = {
    herois: { path: '/api/herois/v2', rel: 'herois', sort: 'nomeHeroico,asc' },
    equipes: { path: '/api/equipes/v2', rel: 'equipes', sort: 'nome,asc' },
    perfis: { path: '/api/perfis/v1', rel: 'perfis', sort: 'nome,asc' }
};
const lookupCache = new Map();

export async function lookup(name, { force = false } = {}) {
    const cached = lookupCache.get(name);
    if (!force && cached && Date.now() - cached.time < LOOKUP_TTL_MS) {
        return cached.promise;
    }
    const source = lookupSources[name];
    const promise = api.get(`${source.path}${query({ size: 200, sort: source.sort })}`)
        .then(page => ({ list: items(page, source.rel), total: pageInfo(page).totalElements }));
    lookupCache.set(name, { time: Date.now(), promise });
    promise.catch(() => lookupCache.delete(name));
    return promise;
}

export function invalidateLookups(...names) {
    (names.length ? names : Object.keys(lookupSources)).forEach(name => lookupCache.delete(name));
}
