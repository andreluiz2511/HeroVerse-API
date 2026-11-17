# HeroVerse API — universo de heróis

> **Comparação das versões v1 e v2:** veja [`mudancasV2.md`](mudancasV2.md), com diferenças de DTOs, exemplos de requisições e melhorias da Parte 2.

API REST desenvolvida com Spring Boot para gerenciar um universo de heróis: perfis, heróis, poderes, fraquezas e equipes. O projeto oferece cadastro, consulta, atualização e remoção, com paginação, HATEOAS, versionamento, login por chave de API, regra de dono, idempotência, limite de requisições, front-end próprio e documentação interativa com Swagger.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Como executar](#como-executar)
- [Front-end](#front-end)
- [Documentação Swagger](#documentação-swagger)
- [Autenticação, dono e headers](#autenticação-dono-e-headers)
- [Versionamento](#versionamento)
- [Relacionamentos](#relacionamentos)
- [Endpoints](#endpoints)
- [Dados e validações](#dados-e-validações)
- [Códigos HTTP](#códigos-http)
- [Licença](#licença)

## Funcionalidades

- Cadastro, consulta, atualização e remoção de perfis, heróis, poderes, fraquezas e equipes, com DTOs de entrada e saída.
- Consultas personalizadas: heróis por nome civil ou heroico, por nível de força, por perfil e por equipe; equipes por nome, por perfil e por herói; poder e fraqueza de um herói; perfis por e-mail.
- Paginação e ordenação em todas as listagens (`page`, `size` e `sort`).
- HATEOAS: cada resposta traz `_links` para navegar entre os recursos, e as listagens trazem links de paginação.
- Versionamento pela URL (`/v1` e `/v2`). A v2 traz a força dos heróis e a força média das equipes.
- O **perfil** é o usuário: o login com e-mail e senha gera uma chave de API (`X-API-Key`).
- **Regra de dono:** cada perfil só altera e exclui o que é seu (o próprio perfil, os seus heróis com poder e fraqueza, e as suas equipes). Nos dos outros, a resposta é `403`.
- Senhas armazenadas como hash BCrypt.
- Idempotência nos cadastros com o header `X-Idempotency-Key`.
- Limite de 20 requisições por minuto por IP (rate limiting), com o header `Retry-After`.
- CORS liberado apenas para origens configuradas.
- Validação com Bean Validation nos DTOs e nas entidades.
- Tratamento global de erros, com respostas em um formato único e mensagens em português.
- Dados de exemplo (fictícios) criados ao iniciar, incluindo perfis de teste.
- Front-end completo em `/`, com todas as operações da API.
- Documentação com OpenAPI e Swagger UI, guia de uso e termos de serviço.

## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Validation
- Spring HATEOAS
- Spring Security Crypto (BCrypt)
- Springdoc OpenAPI 3.1.0 / Swagger UI
- Bucket4j 8.10.1 (rate limiting)
- H2 (banco em memória)
- Maven

## Como executar

Pelo IntelliJ, rode a classe `ApiHeroiApplication` (veja o `LEIA-ME_PRIMEIRO.txt`). Pelo terminal, na raiz do projeto:

```bash
./mvnw spring-boot:run
```

No Windows, use `.\mvnw.cmd spring-boot:run` ou dê dois cliques em `INICIAR_API.bat`.

A aplicação sobe em `http://localhost:8080` com um banco H2 em memória. Os dados são apagados quando a aplicação é encerrada e, a cada inicialização, o `LoadDatabase` cria dados de exemplo fictícios. Todos os perfis de teste usam a senha `senha123`:

| Perfil | E-mail | O que já tem |
|---|---|---|
| Líder Alfa | `lider.alfa@exemplo.com` | 3 heróis e a equipe Liga do Amanhecer |
| Guardiã Beta | `guardia.beta@exemplo.com` | 2 heróis e a equipe Esquadrão Noturno |
| Recruta Gama | `recruta.gama@exemplo.com` | nada ainda |

O console H2 fica em [http://localhost:8080/h2-console](http://localhost:8080/h2-console), com a JDBC URL `jdbc:h2:mem:heroverse`, o usuário `sa` e a senha em branco.

Os front-ends que podem chamar a API de outro endereço pelo navegador ficam em `app.cors.allowed-origins`, no `application.yaml`, separados por vírgula.

## Front-end

Com a aplicação em execução, acesse [http://localhost:8080/](http://localhost:8080/). O front-end fica em `src/main/resources/static/` e é servido pelo próprio Spring Boot, sem instalação nem build.

O layout tem um cabeçalho fino com o perfil logado e a navegação num **dock flutuante** na parte de baixo da tela. Os formulários abrem num diálogo dividido, com as regras ao lado dos campos.

| Tela | Endereço | O que dá para fazer |
|---|---|---|
| Central | `/#/` | Painel com os números, um herói em destaque, ranking de força, gráfico de heróis por nível e as equipes mais fortes |
| Heróis | `/#/herois` | Cartas de herói com nível, poder, fraqueza e barra de força; filtro por nível, "Meus", busca, ordenação e recrutamento |
| Dossiê do herói | `/#/herois/{id}` | Abas de visão geral, poder, fraqueza e API (links HATEOAS e JSON da v1 e da v2); o dono edita o herói e cadastra, edita ou exclui o poder e a fraqueza |
| Arsenal | `/#/arsenal` | Poderes e fraquezas de todos os heróis; o dono cadastra, edita e exclui |
| Equipes | `/#/equipes` | Equipes com medidor de força média e os membros; o dono cria, renomeia, exclui e adiciona ou remove heróis |
| Perfis _(oculta)_ | `/#/perfis` | Listar e buscar perfis; editar ou excluir o próprio |
| Minha base | `/#/conta` | Entrar, criar perfil, ver os seus heróis e equipes, editar o perfil, copiar a chave X-API-Key, sair e excluir o perfil |

A tela de **Perfis** está oculta: o atalho não aparece no dock e o endereço mostra "página não encontrada". O código continua em `pages/perfis.js`; para reativar, remova o atributo `hidden` do link no `index.html` e o `hidden: true` da rota no `app/main.js`. A criação e a edição do próprio perfil continuam em **Minha base**.

O front-end cuida sozinho dos headers da API: gera uma `X-Idempotency-Key` para cada formulário, envia a `X-API-Key` (e abre o login quando ela falta ou expira), mostra os erros junto do campo certo e avisa quando o limite de requisições é atingido. Os botões "Gerar fictício" preenchem perfis, heróis, poderes, fraquezas e equipes inventados.

## Documentação Swagger

- Swagger UI (visual padrão, com o cabeçalho e o logo do projeto): [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Especificação OpenAPI em JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Guia de uso: [http://localhost:8080/ajuda.html](http://localhost:8080/ajuda.html) (texto em `static/guia.md`)
- Termos de Serviço: [http://localhost:8080/termos.html](http://localhost:8080/termos.html)

A página do Swagger mantém o visual padrão do Swagger UI, com o cabeçalho e o logo do HeroVerse no topo. O arquivo fica em `src/main/resources/static/swagger-ui/index.html` e é entregue no endereço padrão pelo `SwaggerPageController`; os arquivos do Swagger UI continuam vindo do springdoc. Isso substituiu o antigo `SwaggerIndexPageTransformer` do `configSwagger`.

A seção **Schemas** (no fim da página) está oculta pela opção `defaultModelsExpandDepth: -1` do `SwaggerUIBundle`. Para mostrar, troque o valor para `1`.

## Autenticação, dono e headers

| Header | Quando enviar | Se faltar |
|---|---|---|
| `X-Idempotency-Key` | Em todo **POST** de cadastro | `400 Bad Request` |
| `X-API-Key` | No **POST** de herói, poder, fraqueza e equipe, em todo **PUT** e **DELETE**, e no logout | `401 Unauthorized` |
| `Content-Type: application/json` | Em todo POST e PUT com corpo | `415 Unsupported Media Type` |

O único cadastro sem login é o de perfil. Todo o resto nasce no perfil logado: o herói e a equipe ficam com o perfil como dono, e o poder e a fraqueza ficam com o dono do herói.

| Registro | Quem altera e exclui |
|---|---|
| Perfil | só o próprio perfil |
| Herói | só o perfil dono |
| Poder e fraqueza | só o dono do herói |
| Equipe | só o perfil dono (e só com heróis dele) |

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "lider.alfa@exemplo.com",
  "senha": "senha123"
}
```

## Versionamento

- **v1** (`/api/{recurso}/v1`): CRUD completo de todos os recursos.
- **v2**:
  - `/api/herois/v2`: formato resumido e o campo `forca` (0 a 100), obrigatório no cadastro e na alteração pela v2. Consulta, cadastro e alteração.
  - `/api/equipes/v2`: `quantidadeHerois` e `forcaMedia`. Só consulta.

Um herói criado pela v1 fica sem força (`null`) até ser alterado pela v2.

## Relacionamentos

| Tipo | Entidades | Onde está |
|---|---|---|
| One-to-One | Herói ↔ Poder, Herói ↔ Fraqueza | `PoderModel.heroi`, `FraquezaModel.heroi` (únicos por herói) |
| One-to-Many | Perfil → Heróis, Perfil → Equipes | `HeroiModel.perfil`, `EquipeModel.perfil` |
| Many-to-Many | Equipes ↔ Heróis | `EquipeModel.herois` (tabela `equipe_heroi`) |
| Enum | Nível de força | `NivelForca` (HUMANA, SUPERHUMANO, ALIENIGENA, MUTANTE) |

## Endpoints

Base local: `http://localhost:8080`. Nas listagens, use `?page=0&size=10&sort=nomeHeroico,asc`.

| Recurso | Método | Rota | Descrição | Header |
|---|---|---|---|---|
| Autenticação | POST | `/api/auth/login` | Login, devolve a chave de API | — |
| Autenticação | POST | `/api/auth/logout` | Invalida a chave atual | `X-API-Key` |
| Perfis | GET | `/api/perfis/v1` | Lista os perfis | — |
| Perfis | GET | `/api/perfis/v1/{id}` | Consulta um perfil | — |
| Perfis | GET | `/api/perfis/v1/busca?email=` | Busca por parte do e-mail | — |
| Perfis | POST | `/api/perfis/v1` | Cria um perfil (cadastro) | `X-Idempotency-Key` |
| Perfis | PUT | `/api/perfis/v1/{id}` | Atualiza o **próprio** perfil | `X-API-Key` |
| Perfis | DELETE | `/api/perfis/v1/{id}` | Exclui o **próprio** perfil (com heróis e equipes) | `X-API-Key` |
| Heróis | GET | `/api/herois/v1` | Lista os heróis | — |
| Heróis | GET | `/api/herois/v1/{id}` | Consulta um herói | — |
| Heróis | GET | `/api/herois/v1/busca?nome=` | Busca no nome civil ou heroico | — |
| Heróis | GET | `/api/herois/v1/nivel/{nivelForca}` | Heróis de um nível | — |
| Heróis | GET | `/api/herois/v1/perfil/{perfilId}` | Heróis de um perfil | — |
| Heróis | GET | `/api/herois/v1/equipe/{equipeId}` | Heróis de uma equipe | — |
| Heróis | POST | `/api/herois/v1` | Recruta um herói no seu perfil | `X-Idempotency-Key` e `X-API-Key` |
| Heróis | PUT | `/api/herois/v1/{id}` | Atualiza um herói **seu** | `X-API-Key` |
| Heróis | DELETE | `/api/herois/v1/{id}` | Exclui um herói **seu** (com poder e fraqueza) | `X-API-Key` |
| Poderes | GET | `/api/poderes/v1` | Lista os poderes | — |
| Poderes | GET | `/api/poderes/v1/{id}` | Consulta um poder | — |
| Poderes | GET | `/api/poderes/v1/busca?nome=` | Busca por nome | — |
| Poderes | GET | `/api/poderes/v1/heroi/{heroiId}` | Poder de um herói | — |
| Poderes | POST | `/api/poderes/v1` | Cadastra o poder de um herói **seu** | `X-Idempotency-Key` e `X-API-Key` |
| Poderes | PUT | `/api/poderes/v1/{id}` | Atualiza | `X-API-Key` |
| Poderes | DELETE | `/api/poderes/v1/{id}` | Exclui | `X-API-Key` |
| Fraquezas | GET/POST/PUT/DELETE | `/api/fraquezas/v1/...` | Mesmas rotas e regras dos poderes | — |
| Equipes | GET | `/api/equipes/v1` | Lista as equipes | — |
| Equipes | GET | `/api/equipes/v1/{id}` | Consulta uma equipe | — |
| Equipes | GET | `/api/equipes/v1/busca?nome=` | Busca por nome | — |
| Equipes | GET | `/api/equipes/v1/perfil/{perfilId}` | Equipes de um perfil | — |
| Equipes | GET | `/api/equipes/v1/heroi/{heroiId}` | Equipes de um herói | — |
| Equipes | POST | `/api/equipes/v1` | Cria uma equipe no seu perfil | `X-Idempotency-Key` e `X-API-Key` |
| Equipes | POST | `/api/equipes/v1/{equipeId}/herois/{heroiId}` | Adiciona um herói **seu** à equipe **sua** | `X-API-Key` |
| Equipes | DELETE | `/api/equipes/v1/{equipeId}/herois/{heroiId}` | Remove o herói da equipe | `X-API-Key` |
| Equipes | PUT | `/api/equipes/v1/{id}` | Renomeia uma equipe **sua** | `X-API-Key` |
| Equipes | DELETE | `/api/equipes/v1/{id}` | Exclui uma equipe **sua** | `X-API-Key` |
| Heróis v2 | GET | `/api/herois/v2` e `/api/herois/v2/{id}` | Heróis no formato v2, com `forca` | — |
| Heróis v2 | POST | `/api/herois/v2` | Recruta com `forca` | `X-Idempotency-Key` e `X-API-Key` |
| Heróis v2 | PUT | `/api/herois/v2/{id}` | Atualiza com `forca` | `X-API-Key` |
| Equipes v2 | GET | `/api/equipes/v2` e `/api/equipes/v2/{id}` | Equipes com `quantidadeHerois` e `forcaMedia` | — |

## Dados e validações

- **Perfil:** `nome` (3 a 60), `email` válido e único, `senha` de 6 a 100 caracteres (obrigatória no cadastro, opcional no PUT). A senha nunca é devolvida.
- **Herói (v1):** `nome` e `nomeHeroico` (até 100) e `nivelForca`. O perfil vem do login.
- **Herói (v2):** os mesmos campos, mais `forca` de 0 a 100.
- **Poder e fraqueza:** `nome` (até 100), `descricao` (até 500) e `heroiId` (de um herói seu, que ainda não tenha um).
- **Equipe:** `nome` (até 100). Os membros são adicionados pelas rotas `/herois/{heroiId}`.

Todos os erros seguem o mesmo formato:

```json
{
  "data": "2026-11-16T20:30:00.000+00:00",
  "mensagem": "nomeHeroico: O nome heroico é obrigatório",
  "detalhes": "uri=/api/herois/v1"
}
```

## Códigos HTTP

- `200 OK`: consulta, atualização, login, membros de equipe e reenvio de um POST com a mesma `X-Idempotency-Key`.
- `201 Created`: cadastro criado.
- `204 No Content`: remoção ou logout.
- `400 Bad Request`: dados inválidos, JSON mal formatado, ID não numérico, parâmetro de busca ausente, perfil sem senha ou header `X-Idempotency-Key` ausente.
- `401 Unauthorized`: operação protegida sem `X-API-Key` válida, ou login incorreto.
- `403 Forbidden`: chave válida, mas o registro é de outro perfil.
- `404 Not Found`: registro ou rota inexistente, ou herói fora da equipe.
- `405 Method Not Allowed`: método não disponível na rota (ex.: `DELETE /api/equipes/v2/{id}`).
- `409 Conflict`: e-mail já cadastrado, ou herói que já tem poder (ou fraqueza).
- `415 Unsupported Media Type`: corpo sem `Content-Type: application/json`.
- `429 Too Many Requests`: mais de 20 requisições por minuto do mesmo IP.
- `500 Internal Server Error`: erro inesperado.

## Licença

Distribuído sob a licença MIT. Consulte os termos em [mit-license.org](https://mit-license.org/).
