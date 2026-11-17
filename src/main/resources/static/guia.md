API REST do **HeroVerse**: perfis, heróis, poderes, fraquezas e equipes.

> 🛡️ [Usar pelo site](/) · 🧪 [Testar no Swagger](/swagger-ui/index.html) · 📜 [Termos de Serviço](/termos.html)

---

## 🚀 Primeiros passos

> 🛡️ **Prefere uma interface?** A página inicial ([/](/)) faz tudo isto por você, com os headers enviados automaticamente.

1. **Crie um perfil com dados fictícios** em `POST /api/perfis/v1`. O perfil é o seu usuário: use um nome inventado (ex.: `Comandante Fictício`) e um e-mail de exemplo (ex.: `comandante@exemplo.com`). **Não use seus dados reais.**
2. **Faça login** em `POST /api/auth/login`. A resposta traz uma `apiKey`.
3. **Use a chave** no header `X-API-Key` para **criar** heróis, poderes, fraquezas e equipes, e em todo **PUT** e **DELETE**.
4. Em todo **cadastro (POST)**, envie também o header `X-Idempotency-Key` com um valor único (ex.: um UUID novo).

> 💡 Perfis de teste (senha **senha123**): **lider.alfa@exemplo.com** (3 heróis e 1 equipe), **guardia.beta@exemplo.com** (2 heróis e 1 equipe) e **recruta.gama@exemplo.com** (sem nada ainda).

### Testando pelo Swagger

Abra [/swagger-ui/index.html](/swagger-ui/index.html), escolha um endpoint e clique em **Testar**. Preencha os parâmetros, os headers e o corpo JSON e clique em **Executar**.

---

## 🔑 Headers da API

| Header | Quando enviar | O que acontece se faltar |
|---|---|---|
| `X-Idempotency-Key` | Em todo **POST** de cadastro (v1 e v2) | **400** – "O header X-Idempotency-Key é obrigatório" |
| `X-API-Key` | No **POST** de herói, poder, fraqueza e equipe, em todo **PUT** e **DELETE**, e no logout | **401** – "Esta operação exige login" |
| `Content-Type: application/json` | Em todo POST e PUT com corpo | **415** – "Content-Type não suportado" |

O único cadastro sem login é o de **perfil** (é por ele que você ganha acesso).

**Idempotência:** reenviar um POST com a **mesma** `X-Idempotency-Key` não cria outro registro: a API devolve o criado na primeira vez, com **200** em vez de 201.

---

## 👑 Quem pode alterar o quê

Tudo no HeroVerse tem dono:

| Registro | Dono | Quem altera e exclui |
|---|---|---|
| Perfil | ele mesmo | só o próprio perfil |
| Herói | o perfil que recrutou | só esse perfil |
| Poder e fraqueza | o dono do herói | só o dono do herói |
| Equipe | o perfil que criou | só esse perfil (e só com heróis dele) |

Consultar (GET) é livre para todos. Mexer no que é de outro perfil devolve **403**.

---

## 🧭 Versões (v1 e v2)

| Recurso | v1 | v2 |
|---|---|---|
| Heróis | `/api/herois/v1` (CRUD completo) | `/api/herois/v2` (consulta, cadastro e alteração, com `forca`) |
| Equipes | `/api/equipes/v1` (CRUD e membros) | `/api/equipes/v2` (só consulta, com `quantidadeHerois` e `forcaMedia`) |
| Perfis, poderes, fraquezas | `/api/perfis/v1`, `/api/poderes/v1`, `/api/fraquezas/v1` | – |
| Login | `/api/auth/login` e `/api/auth/logout` | – |

- **Heróis v2:** formato resumido (nomes do poder, da fraqueza, das equipes e do perfil) e o campo **`forca`** (0 a 100), obrigatório no cadastro e na alteração pela v2. Esse campo **não existe na v1**: um herói criado pela v1 fica sem força (`null`) até ser alterado pela v2.
- **Equipes v2:** `forcaMedia` é a média da força dos heróis da equipe que têm força cadastrada.

---

## 🔗 Relacionamentos

| Tipo | Entre | Na prática |
|---|---|---|
| **1:1** | Herói ↔ Poder, Herói ↔ Fraqueza | Cada herói tem no máximo um poder e uma fraqueza. |
| **1:N** | Perfil → Heróis, Perfil → Equipes | Cada herói e cada equipe pertence a um perfil. |
| **N:N** | Equipes ↔ Heróis | Uma equipe tem vários heróis e um herói pode estar em várias equipes. |
| **enum** | `nivelForca` | `HUMANA`, `SUPERHUMANO`, `ALIENIGENA`, `MUTANTE` |

---

## 📚 Exemplos

**Criar perfil** — `POST /api/perfis/v1` (header `X-Idempotency-Key`)
```json
{ "nome": "Comandante Fictício", "email": "comandante@exemplo.com", "senha": "senha123" }
```

**Login** — `POST /api/auth/login`
```json
{ "email": "comandante@exemplo.com", "senha": "senha123" }
```

**Recrutar herói (v2)** — `POST /api/herois/v2` (headers `X-API-Key` e `X-Idempotency-Key`)
```json
{ "nome": "Maia Duarte", "nomeHeroico": "Cometa Violeta", "nivelForca": "ALIENIGENA", "forca": 82 }
```

**Dar um poder ao herói** — `POST /api/poderes/v1`
```json
{ "nome": "Telecinese", "descricao": "Move objetos pesados com a mente.", "heroiId": 6 }
```

**Criar equipe e adicionar herói**
- `POST /api/equipes/v1` com `{ "nome": "Patrulha Boreal" }`
- `POST /api/equipes/v1/{equipeId}/herois/{heroiId}` (sem corpo) — e `DELETE` no mesmo endereço para remover

**Buscas e filtros**
- `GET /api/herois/v1/busca?nome=faísca` — no nome civil ou heroico
- `GET /api/herois/v1/nivel/MUTANTE` — por nível de força
- `GET /api/herois/v1/perfil/1` e `/api/herois/v1/equipe/1` — heróis de um perfil ou de uma equipe
- `GET /api/equipes/v1/perfil/1` e `/api/equipes/v1/heroi/1` — equipes de um perfil ou de um herói
- `GET /api/poderes/v1/heroi/1` e `/api/fraquezas/v1/heroi/1` — poder e fraqueza de um herói
- `GET /api/perfis/v1/busca?email=alfa`, `/api/poderes/v1/busca?nome=rajada`
- Paginação em todas as listagens: `?page=0&size=10&sort=nomeHeroico,asc` (na v2 dá para ordenar por `forca,desc`)

---

## ⛔ Erros

Todos os erros vêm no mesmo formato:
```json
{ "data": "2026-11-16T20:30:00.000+00:00", "mensagem": "explicação do erro", "detalhes": "uri=/api/..." }
```

| Status | Situação | Como resolver |
|---|---|---|
| **400** | Campo obrigatório vazio, nome de campo errado ou tamanho inválido | Corrija o campo indicado na `mensagem` |
| **400** | `nivelForca` com valor que não existe, ou `forca` fora de 0 a 100 / ausente na v2 | Use um dos 4 níveis, em maiúsculas; força de 0 a 100 |
| **400** | Perfil sem senha no cadastro, ou senha com menos de 6 caracteres | Envie a `senha` |
| **400** | JSON mal formatado, ID não numérico, busca sem o parâmetro ou POST sem `X-Idempotency-Key` | Confira a requisição |
| **401** | Operação protegida sem `X-API-Key`, chave antiga (depois de novo login ou logout) ou login errado | Faça login e use a chave mais recente |
| **403** | Alterar ou excluir perfil, herói, poder, fraqueza ou equipe de **outro** perfil | Cada perfil só mexe no que é seu |
| **403** | Criar poder/fraqueza para herói de outro perfil, ou pôr herói de outro perfil na sua equipe | Use os seus heróis |
| **404** | ID que não existe, herói fora da equipe, ou rota sem versão (`/api/herois` sem `/v1`) | Consulte as listagens; use `/v1` ou `/v2` |
| **405** | Método que a rota não tem (ex.: `DELETE /api/equipes/v2/1`) | Para alterar e excluir, use a v1 |
| **409** | E-mail de perfil já cadastrado, ou herói que já tem poder (ou fraqueza) | Use outro e-mail; altere o poder existente |
| **415** | Corpo sem `Content-Type: application/json` | Envie o header |
| **429** | Mais de **20 requisições por minuto** do mesmo IP | Espere os segundos do header `Retry-After` |
| **500** | Erro inesperado | Tente de novo; se continuar, verifique o log |

**Cuidados importantes:**
- ⚠️ Excluir um **herói** exclui o poder e a fraqueza dele e o tira das equipes. Excluir uma **equipe** não exclui os heróis.
- ⚠️ Excluir o **perfil** exclui **tudo** dele: heróis, poderes, fraquezas e equipes.
- ⚠️ O banco é **em memória (H2)**: ao reiniciar, tudo volta aos dados de exemplo e as chaves são apagadas.
- ⚠️ Num PUT, o corpo é validado **antes** da chave: corpo inválido dá **400** mesmo sem `X-API-Key`.
- ⚠️ Use sempre **dados fictícios** e heróis criados por você.
