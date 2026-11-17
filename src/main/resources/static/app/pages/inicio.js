import { api, items, pageInfo, lookup, query } from '../api.js';
import { esc, NIVEIS, loading, errorState, TEST_ACCOUNT } from '../ui.js';
import { heroCard } from './herois.js';

export async function renderInicio(root, ctx) {
    root.innerHTML = `<section class="page">
        <div class="bento">
            <article class="panel corner panel-glow b-spot">
                <div class="spotlight">
                    <div>
                        <span class="kicker">Central HeroVerse</span>
                        <h1>Monte o seu <span>esquadrão</span></h1>
                        <p>Recrute heróis, dê a eles um poder e uma fraqueza e organize tudo em equipes. Consultar é livre; para criar, entre com o seu perfil.</p>
                        <div class="actions">
                            <a class="btn btn-primary" href="#/herois">Ver heróis</a>
                            <a class="btn btn-secondary" href="#/conta">Criar perfil</a>
                        </div>
                    </div>
                    <div class="spot-card" data-destaque></div>
                </div>
            </article>
            <div class="b-stat" data-stats>${['Heróis', 'Equipes', 'Poderes', 'Perfis'].map(label =>
                `<div class="mini-stat"><b>…</b><span>${label}</span></div>`).join('')}</div>
            <article class="panel b-rank">
                <h2 class="section-title">Ranking de força</h2>
                <div data-ranking>${loading()}</div>
            </article>
            <article class="panel b-niveis">
                <h2 class="section-title">Heróis por nível</h2>
                <div data-niveis>${loading()}</div>
            </article>
            <article class="panel b-equipes">
                <h2 class="section-title">Equipes mais fortes</h2>
                <div data-equipes>${loading()}</div>
            </article>
            <article class="panel b-steps">
                <div class="steps">
                    <div class="step"><span>01</span><div><h3>Crie o seu perfil</h3><p>Ele é o seu login e o dono de tudo o que você criar. Use dados fictícios.</p></div></div>
                    <div class="step"><span>02</span><div><h3>Recrute e equipe</h3><p>Cadastre heróis, poderes, fraquezas e equipes. Cada envio leva uma chave única, então nada duplica.</p></div></div>
                    <div class="step"><span>03</span><div><h3>Só você altera o que é seu</h3><p>Para testar, entre com ${TEST_ACCOUNT}.</p></div></div>
                </div>
            </article>
        </div>
    </section>`;

    const caixa = nome => root.querySelector(`[data-${nome}]`);

    async function load() {
        const [herois, equipes, poderes, perfis] = await Promise.allSettled([
            lookup('herois'),
            lookup('equipes'),
            api.get(`/api/poderes/v1${query({ size: 1 })}`),
            api.get(`/api/perfis/v1${query({ size: 1 })}`)
        ]);
        if (!ctx.isCurrent()) return;
        const valor = (resultado, ler) => (resultado.status === 'fulfilled' ? ler(resultado.value) : '—');
        const stats = [
            { label: 'Heróis', value: valor(herois, data => data.total), href: '#/herois' },
            { label: 'Equipes', value: valor(equipes, data => data.total), href: '#/equipes' },
            { label: 'Poderes', value: valor(poderes, page => pageInfo(page).totalElements), href: '#/arsenal' },
            { label: 'Perfis', value: valor(perfis, page => pageInfo(page).totalElements), href: '#/conta' }
        ];
        caixa('stats').innerHTML = stats.map(stat => `<a class="mini-stat" href="${stat.href}"><b>${esc(stat.value)}</b><span>${stat.label}</span></a>`).join('');

        if (herois.status === 'rejected') {
            caixa('ranking').innerHTML = errorState(herois.reason);
            caixa('ranking').querySelector('[data-action="retry"]').addEventListener('click', load);
            caixa('niveis').innerHTML = '';
        } else {
            const lista = herois.value.list;
            const ranking = [...lista].filter(heroi => heroi.forca !== null && heroi.forca !== undefined).sort((a, b) => b.forca - a.forca).slice(0, 5);
            caixa('ranking').innerHTML = ranking.length
                ? `<ol class="rank">${ranking.map(heroi => `<li class="${(NIVEIS[heroi.nivelForca] || {}).cor || ''}"><span><a href="#/herois/${heroi.id}">${esc(heroi.nomeHeroico)}</a><span class="bar"><i style="width:${heroi.forca}%"></i></span></span><b>${esc(heroi.forca)}</b></li>`).join('')}</ol>`
                : '<p class="muted">Nenhum herói com força medida (v2).</p>';
            const maximo = Math.max(1, ...Object.keys(NIVEIS).map(nivel => lista.filter(heroi => heroi.nivelForca === nivel).length));
            caixa('niveis').innerHTML = `<div class="bars">${Object.entries(NIVEIS).map(([nivel, info]) => {
                const total = lista.filter(heroi => heroi.nivelForca === nivel).length;
                return `<a class="${info.cor}" href="#/herois?nivel=${nivel}"><b>${total}</b><i style="height:${(total / maximo) * 100}%"></i>${info.label}</a>`;
            }).join('')}</div>`;
            if (ranking[0]) caixa('destaque').innerHTML = heroCard(ranking[0]);
        }

        if (equipes.status === 'rejected') {
            caixa('equipes').innerHTML = errorState(equipes.reason);
        } else {
            const fortes = [...equipes.value.list].sort((a, b) => (b.forcaMedia ?? -1) - (a.forcaMedia ?? -1)).slice(0, 4);
            caixa('equipes').innerHTML = fortes.length
                ? `<div class="mini-list">${fortes.map(equipe => `<a href="#/equipes?busca=${encodeURIComponent(equipe.nome)}"><strong>${esc(equipe.nome)}</strong><span class="muted">${equipe.quantidadeHerois} herói(s) · média ${equipe.forcaMedia ?? '—'}</span></a>`).join('')}</div>`
                : '<p class="muted">Nenhuma equipe formada ainda.</p>';
        }
    }
    load();
}
