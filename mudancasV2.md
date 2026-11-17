# HeroVerse — mudanças da v1 para a v2

**Objetivo:** apresentar ao professor, de forma objetiva, o que muda entre as versões **v1** e **v2** dos recursos da API e quais recursos da Parte 2 foram aprimorados nesta entrega.

> **Atenção:** “v1” e “v2” abaixo são versões de **endpoints da API**. Isso não é a mesma coisa que a **Parte 1** e a **Parte 2** da atividade. A Parte 2 trata de recursos avançados (idempotência, autenticação, rate limiting, CORS e versionamento), que podem ser utilizados com endpoints de ambas as versões.

## 1. Resumo das diferenças

| Recurso | v1 | v2 |
|---|---|---|
| **Heróis — consulta** | Exibe dados detalhados dos relacionamentos, incluindo IDs de poder e fraqueza e objetos das equipes | Exibe informações mais diretas: nomes dos relacionamentos, nomes das equipes e o campo numérico `forca` |
| **Heróis — cadastro/edição** | Recebe `nome`, `nomeHeroico` e `nivelForca` | Recebe os mesmos campos **mais `forca`**, obrigatória entre **0 e 100** |
| **Equipes — consulta** | Exibe a lista de heróis da equipe | Exibe `quantidadeHerois` e `forcaMedia`, úteis como indicadores da equipe |
| **Equipes — escrita** | Possui endpoints para criar e atualizar equipes | Os endpoints próprios de equipes v2 são **somente de consulta**; criação e alterações continuam usando v1 |
| **Paginação/HATEOAS** | Listagens paginadas e links para navegação | Listagens paginadas e links para navegação também |

**O que permanece igual:** as cinco entidades (Perfil, Herói, Poder, Fraqueza e Equipe), o banco H2, as regras de negócio e os recursos CRUD já implementados na v1. As versões coexistem para que clientes antigos continuem funcionando.

## 2. Exemplos concretos de resposta

Os exemplos abaixo mostram **trechos dos dados**, para facilitar a comparação. As respostas reais também contêm links HATEOAS (`_links`) e, nas listagens, informações de paginação.

### Herói na v1

`GET /api/herois/v1/1`

```json
{
  "id": 1,
  "nome": "Lia Moreira",
  "nomeHeroico": "Faísca Azul",
  "nivelForca": "MUTANTE",
  "perfilId": 1,
  "perfilNome": "Líder Alfa",
  "poderId": 1,
  "poderNome": "Rajada Elétrica",
  "fraquezaId": 1,
  "fraquezaNome": "Água Salgada",
  "equipes": [{"id": 1, "nome": "Liga do Amanhecer"}]
}
```

### Herói na v2

`GET /api/herois/v2/1`

```json
{
  "id": 1,
  "nomeHeroico": "Faísca Azul",
  "nome": "Lia Moreira",
  "nivelForca": "MUTANTE",
  "forca": 78,
  "poder": "Rajada Elétrica",
  "fraqueza": "Água Salgada",
  "equipes": ["Liga do Amanhecer"],
  "perfilId": 1,
  "perfil": "Líder Alfa"
}
```

**Diferença principal:** a v2 informa a força numérica do herói e simplifica a representação dos relacionamentos. Na v1, a estrutura detalhada é preservada para compatibilidade.

### Equipe na v1

`GET /api/equipes/v1/1`

```json
{
  "id": 1,
  "nome": "Liga do Amanhecer",
  "perfilId": 1,
  "perfilNome": "Líder Alfa",
  "herois": [{"id": 1, "nomeHeroico": "Faísca Azul"}]
}
```

### Equipe na v2

`GET /api/equipes/v2/1`

```json
{
  "id": 1,
  "nome": "Liga do Amanhecer",
  "perfilId": 1,
  "perfil": "Líder Alfa",
  "quantidadeHerois": 3,
  "forcaMedia": 84.7
}
```

**Diferença principal:** na v1 a consulta detalha integrantes; na v2 a resposta traz **indicadores consolidados**. Os valores acima são ilustrativos e dependem dos dados cadastrados. A média considera os heróis que têm força cadastrada.

## 3. Como escolher a versão pelo header

A API oferece um segundo modo de versionamento, exigido na Parte 2: enviar o cabeçalho **`X-API-Version`** para o **mesmo endereço**.

| Requisição | Header | Resultado |
|---|---|---|
| `GET /api/herois` | `X-API-Version: 1` | Formato v1 dos heróis |
| `GET /api/herois` | `X-API-Version: 2` | Formato v2 dos heróis |
| `GET /api/herois/{id}` | `X-API-Version: 1` ou `2` | Herói no formato escolhido |
| `GET /api/equipes` | `X-API-Version: 1` ou `2` | Equipes no formato escolhido |
| `GET /api/equipes/{id}` | `X-API-Version: 1` ou `2` | Equipe no formato escolhido |

- Se o cabeçalho **não for enviado**, a API utiliza a **v1**.
- Se o cabeçalho tiver um valor diferente de `1` ou `2`, retorna **400 Bad Request**.
- As rotas antigas, como `/api/herois/v1`, `/api/herois/v2`, `/api/equipes/v1` e `/api/equipes/v2`, **continuam funcionando**.
- **Importante:** esse versionamento por header foi implementado nas **consultas GET de heróis e equipes**, não em todos os endpoints de cadastro e edição.

### Demonstração no terminal do IntelliJ (PowerShell / Windows)

Com a aplicação executando em `http://localhost:8080`, faça as requisições abaixo:

```powershell
# Mesmo endpoint, formatos diferentes:
curl.exe -H "X-API-Version: 1" "http://localhost:8080/api/herois/1"
curl.exe -H "X-API-Version: 2" "http://localhost:8080/api/herois/1"

# Versões de equipes:
curl.exe -H "X-API-Version: 1" "http://localhost:8080/api/equipes/1"
curl.exe -H "X-API-Version: 2" "http://localhost:8080/api/equipes/1"

# Listagem paginada na v2:
curl.exe -H "X-API-Version: 2" "http://localhost:8080/api/herois?page=0&size=5"

# Comprovação de validação da versão (resposta 400):
curl.exe -i -H "X-API-Version: 3" "http://localhost:8080/api/herois"
```

Os IDs acima dependem dos registros disponíveis no banco H2. Se o ID `1` não existir, utilize um ID retornado pela listagem. Também é possível executar essas chamadas pelo **Swagger UI** em `http://localhost:8080/swagger-ui/index.html`, na seção de versionamento por header.

## 4. Evolução da Parte 2 da atividade (além das diferenças de formato)

Estas mudanças são recursos técnicos do projeto; **não devem ser apresentadas como se existissem somente na v2**.

| Requisito da Parte 2 | Implementação atual e alteração relevante |
|---|---|
| **Idempotência (`X-Idempotency-Key`)** | Os POSTs de cadastro usam uma chave para evitar repetição. A proteção foi reforçada para requisições simultâneas; cadastro novo retorna **201**, repetição com mesmos dados retorna **200** e reutilização com dados diferentes retorna **409**. O header `X-Idempotency-Replayed` sinaliza o reaproveitamento. |
| **Chave de API (`X-API-Key`)** | A autenticação por chave continua disponível em `POST /api/auth/login` e a invalidação em `POST /api/auth/logout`. Recursos protegidos exigem uma chave válida. |
| **Rate limiting** | O limite é de **20 requisições por minuto por IP** em `/api/**`. Quando excedido, retorna **429** e `Retry-After`; também informa `X-Rate-Limit-Limit` e `X-Rate-Limit-Remaining`. |
| **CORS** | Mantém origens permitidas configuradas e passou a aceitar `X-API-Version`, além de `X-API-Key` e `X-Idempotency-Key`. |
| **Versionamento** | Além das rotas `/v1` e `/v2`, as consultas de heróis e equipes aceitam agora `X-API-Version`. |
| **Java** | O `pom.xml` foi ajustado para compilar com **Java 17**. |

**Limitação importante:** o controle das chaves de idempotência é feito **em memória**. Ao reiniciar a aplicação, os registros dessas chaves são perdidos; para uso em produção, seria necessária uma estratégia persistente e transacional.

## 5. Onde encontrar as mudanças no código

| Arquivo | O que demonstrar |
|---|---|
| `controller/HeroiController.java` e `controller/HeroiV2Controller.java` | Diferenças entre as rotas de heróis v1 e v2 |
| `dto/request/HeroiRequestDTO.java` e `HeroiV2RequestDTO.java` | Campo `forca` obrigatório na v2 |
| `dto/response/HeroiResponseDTO.java` e `HeroiV2ResponseDTO.java` | Diferenças no formato de resposta dos heróis |
| `controller/EquipeController.java` e `controller/EquipeV2Controller.java` | Equipes v1 com CRUD; equipes v2 com consultas |
| `dto/response/EquipeResponseDTO.java` e `EquipeV2ResponseDTO.java` | Lista de heróis na v1 versus indicadores da v2 |
| `controller/VersionamentoController.java` | Seleção entre v1 e v2 pelo header `X-API-Version` |
| `service/IdempotencyService.java` | Proteção contra operações POST repetidas e simultâneas |
| `configuracoes/CorsConfig.java` e `RateLimitFilter.java` | CORS e limite de requisições |

*Todos os caminhos Java acima são relativos a `src/main/java/com/andre/apiheroi/`.*

## 6. Resumo para apresentação

> A v1 do HeroVerse mantém os endpoints e as representações originais dos recursos. Na v2, a API de heróis adiciona a força numérica, e a API de equipes apresenta indicadores como quantidade de heróis e força média. As duas versões podem ser consultadas tanto pelas URLs versionadas quanto, nas operações GET de heróis e equipes, pelo cabeçalho `X-API-Version`. Na Parte 2 também reforçamos a idempotência e ajustamos os cabeçalhos de rate limiting e CORS, preservando autenticação, paginação, HATEOAS e compatibilidade com a v1.
