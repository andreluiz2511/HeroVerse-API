# HeroVerse API — mapa do projeto

Com a API rodando:
- **Front-end:** http://localhost:8080/
- **Swagger (visual padrão, com o cabeçalho e o logo do projeto):** http://localhost:8080/swagger-ui/index.html
- **Guia de uso:** http://localhost:8080/ajuda.html
- **Termos de Serviço:** http://localhost:8080/termos.html
- **Console do H2:** http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:heroverse`, usuário `sa`, sem senha)

Perfis de teste (senha `senha123`): **lider.alfa@exemplo.com**, **guardia.beta@exemplo.com**, **recruta.gama@exemplo.com**

| Requisito | Onde olhar |
|---|---|
| One-to-One | `PoderModel.heroi` e `FraquezaModel.heroi` ↔ `HeroiModel.poder` / `HeroiModel.fraqueza` |
| One-to-Many | `PerfilModel.herois` e `PerfilModel.equipes` |
| Many-to-Many | `EquipeModel.herois` ↔ `HeroiModel.equipes` |
| Enum | `NivelForca` |
| Bean Validation | entidades (`model/`) e `dto/request/` |
| DTOs | `dto/request/` e `dto/response/` (com exemplos para o Swagger) |
| Consultas personalizadas | `RepositoryHeroi` e `RepositoryEquipe` (`@Query`), métodos derivados nos outros repositórios |
| Paginação + HATEOAS | controllers (`PagedResourcesAssembler`) e `assembler/` |
| Idempotência (`X-Idempotency-Key`) | `IdempotencyService` + método `create` dos controllers |
| Login (`X-API-Key`) | `AuthController`, `AuthService` |
| Regra de dono (403) | `HeroiService.conferirDono`, `EquipeService`, `PerfilService` (o poder e a fraqueza usam o dono do herói) |
| Rate limiting | `configuracoes/RateLimitFilter` (20 req/min por IP, só em `/api/**`) |
| CORS | `configuracoes/CorsConfig` + `app.cors.allowed-origins` |
| Versionamento (URL) | `/api/{recurso}/v1`, `/api/herois/v2` e `/api/equipes/v2` |
| Tratamento de erros | pasta `excecao/` (comentada em detalhes) |
| Front-end | `static/index.html` + `static/app/` |
| Documentação | `configuracoes/configSwagger`, `static/swagger-ui/index.html` + `controller/SwaggerPageController`, `static/guia.md`, `static/termos.html` |
