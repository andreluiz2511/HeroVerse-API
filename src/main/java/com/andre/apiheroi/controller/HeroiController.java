package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.HeroiAssembler;
import com.andre.apiheroi.dto.request.HeroiRequestDTO;
import com.andre.apiheroi.dto.response.HeroiResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.NivelForca;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.service.AuthService;
import com.andre.apiheroi.service.HeroiService;
import com.andre.apiheroi.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "v1 - Heróis")
@RestController
@RequestMapping("/api/herois/v1")
public class HeroiController {

    private final HeroiService service;
    private final HeroiAssembler assembler;
    private final PagedResourcesAssembler<HeroiModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);
    private final IdempotencyService idempotencyService;
    private final AuthService authService;

    public HeroiController(HeroiService service, HeroiAssembler assembler, IdempotencyService idempotencyService, AuthService authService) {
        this.service = service;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.authService = authService;
    }

    @Operation(summary = "Cadastrar herói",
            description = "Exige login (X-API-Key) e o header X-Idempotency-Key. O herói é criado no perfil logado. O campo forca só existe na v2 (aqui o herói fica sem força).")
    @ApiResponse(responseCode = "201", description = "Herói cadastrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Repetição: esta X-Idempotency-Key já foi usada; devolve o herói cadastrado antes", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, JSON mal formatado ou header X-Idempotency-Key ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @ApiResponse(responseCode = "409", description = "A mesma X-Idempotency-Key foi reutilizada com dados diferentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @PostMapping
    public ResponseEntity<EntityModel<HeroiResponseDTO>> create(
            @Parameter(description = "Valor único deste cadastro (ex.: um UUID novo)", example = "3f2b8c1e-7d4a-4e9b-a6c2-1f0e9d8b7a65")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do herói", required = true,
                    content = @Content(schema = @Schema(implementation = HeroiRequestDTO.class)))
            @RequestBody @Valid HeroiRequestDTO dto) {
        PerfilModel dono = authService.validar(apiKey);
        IdempotencyService.Resultado resultado = idempotencyService.executar(
                "heroi", String.valueOf(dono.getId()), idempotencyKey, dto,
                () -> service.create(dto, dono).getId());
        return ResponseEntity.status(resultado.criadoAgora() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("X-Idempotency-Replayed", String.valueOf(!resultado.criadoAgora()))
                .body(assembler.toModel(service.findById(resultado.idCriado())));
    }

    @Operation(summary = "Listar heróis",
            description = "Lista paginada. Parâmetros: page (começa em 0), size (padrão 10) e sort (ex.: nomeHeroico,asc).")
    @ApiResponse(responseCode = "200", description = "Página de heróis", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = HeroiResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<HeroiResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler));
    }

    @Operation(summary = "Buscar herói por ID",
            description = "Devolve o herói com os links HATEOAS para as próximas ações.")
    @ApiResponse(responseCode = "200", description = "Herói encontrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<HeroiResponseDTO>> findById(@Parameter(description = "ID do herói", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.findById(id)));
    }

    @Operation(summary = "Buscar heróis por nome",
            description = "Consulta personalizada com JPQL (@Query): procura o texto no nome civil OU no nome heroico, sem diferenciar maiúsculas de minúsculas.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = HeroiResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "400", description = "Parâmetro nome não enviado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/busca")
    public ResponseEntity<PagedModel<EntityModel<HeroiResponseDTO>>> findByName(
            @Parameter(description = "Texto a procurar", example = "faísca") @RequestParam String nome,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByName(nome, pageable), assembler));
    }

    @Operation(summary = "Heróis por nível de força",
            description = "Consulta paginada pelo enum. Valores aceitos: HUMANA, SUPERHUMANO, ALIENIGENA, MUTANTE.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = HeroiResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "400", description = "Nível de força inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/nivel/{nivelForca}")
    public ResponseEntity<PagedModel<EntityModel<HeroiResponseDTO>>> findByNivelForca(
            @Parameter(description = "Nível de força (enum)", example = "MUTANTE") @PathVariable NivelForca nivelForca,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByNivelForca(nivelForca, pageable), assembler));
    }

    @Operation(summary = "Heróis de um perfil",
            description = "Consulta personalizada com JPQL (@Query): heróis do perfil informado (relação 1:N).")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = HeroiResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<PagedModel<EntityModel<HeroiResponseDTO>>> findByPerfil(
            @Parameter(description = "ID do perfil", example = "1") @PathVariable Long perfilId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByPerfil(perfilId, pageable), assembler));
    }

    @Operation(summary = "Heróis de uma equipe",
            description = "Consulta personalizada com JPQL e JOIN (@Query): heróis da equipe informada (relação N:N).")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = HeroiResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "404", description = "Equipe não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/equipe/{equipeId}")
    public ResponseEntity<PagedModel<EntityModel<HeroiResponseDTO>>> findByEquipe(
            @Parameter(description = "ID da equipe", example = "1") @PathVariable Long equipeId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByEquipe(equipeId, pageable), assembler));
    }

    @Operation(summary = "Atualizar herói",
            description = "Exige login (X-API-Key). Só o perfil dono altera o herói. A força (v2) não é alterada por aqui.")
    @ApiResponse(responseCode = "200", description = "Herói atualizado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON mal formatado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<HeroiResponseDTO>> update(
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do herói", required = true,
                    content = @Content(schema = @Schema(implementation = HeroiRequestDTO.class)))
            @RequestBody @Valid HeroiRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.update(id, dto, idPerfilLogado)));
    }

    @Operation(summary = "Excluir herói",
            description = "Exige login (X-API-Key). Só o perfil dono exclui. O poder e a fraqueza do herói são excluídos junto, e ele sai das equipes.")
    @ApiResponse(responseCode = "204", description = "Herói excluído", content = @Content)
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        service.delete(id, idPerfilLogado);
        return ResponseEntity.noContent().build();
    }
}
