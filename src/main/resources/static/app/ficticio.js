const PRENOMES = ['Lia', 'Caio', 'Nara', 'Téo', 'Iara', 'Davi', 'Luna', 'Rui', 'Maia', 'Enzo', 'Bia', 'Otto'];
const SOBRENOMES = ['Moreira', 'Rocha', 'Vasques', 'Prado', 'Lopes', 'Brandão', 'Teixeira', 'Duarte', 'Farias', 'Queiroz'];
const TITULOS = ['Capitã', 'Agente', 'Doutor', 'Lady', 'Mestre', 'Sentinela', 'Guardião', 'Patrulheira'];
const NOMES = ['Cometa', 'Relâmpago', 'Eclipse', 'Maré', 'Brasa', 'Nebulosa', 'Granito', 'Aurora', 'Ciclone', 'Prisma', 'Faísca', 'Vórtice'];
const CORES = ['Azul', 'Rubro', 'Dourada', 'Esmeralda', 'Violeta', 'de Prata', 'Noturno', 'Solar'];
const PODERES = [
    ['Teletransporte Curto', 'Salta até 50 metros num piscar de olhos.'],
    ['Escudo de Luz', 'Cria uma barreira de energia que bloqueia impactos.'],
    ['Supervelocidade', 'Corre mais rápido que um trem-bala.'],
    ['Telecinese', 'Move objetos pesados com a mente.'],
    ['Criocinese', 'Congela o ar e a água ao redor.'],
    ['Eco-localização', 'Enxerga no escuro pelo som.']
];
const FRAQUEZAS = [
    ['Barulho Alto', 'Fica atordoado com sons muito fortes.'],
    ['Calor Extremo', 'Perde a concentração em lugares muito quentes.'],
    ['Cansaço Rápido', 'Precisa descansar depois de usar o poder três vezes.'],
    ['Luz Ultravioleta', 'Fica enfraquecido sob luz UV.'],
    ['Distração', 'Perde o foco quando alguém está em perigo.']
];
const EQUIPES = ['Liga', 'Esquadrão', 'Patrulha', 'Aliança', 'Ordem', 'Guarda'];
const COMPLEMENTOS = ['do Crepúsculo', 'Estelar', 'da Maré Alta', 'Relâmpago', 'do Horizonte', 'Boreal'];

function pick(list) {
    return list[Math.floor(Math.random() * list.length)];
}

function slug(text) {
    return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '.').replace(/^\.|\.$/g, '');
}

export function perfilFicticio() {
    const nome = `${pick(TITULOS)} ${pick(NOMES)}`;
    const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789';
    return {
        nome,
        email: `${slug(nome)}${Math.floor(100 + Math.random() * 900)}@exemplo.com`,
        senha: Array.from({ length: 10 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
    };
}

export function heroiFicticio() {
    return {
        nome: `${pick(PRENOMES)} ${pick(SOBRENOMES)}`,
        nomeHeroico: `${pick(NOMES)} ${pick(CORES)}`,
        nivelForca: pick(['HUMANA', 'SUPERHUMANO', 'ALIENIGENA', 'MUTANTE']),
        forca: Math.floor(30 + Math.random() * 70)
    };
}

export function poderFicticio() {
    const [nome, descricao] = pick(PODERES);
    return { nome, descricao };
}

export function fraquezaFicticia() {
    const [nome, descricao] = pick(FRAQUEZAS);
    return { nome, descricao };
}

export function equipeFicticia() {
    return { nome: `${pick(EQUIPES)} ${pick(COMPLEMENTOS)}` };
}
