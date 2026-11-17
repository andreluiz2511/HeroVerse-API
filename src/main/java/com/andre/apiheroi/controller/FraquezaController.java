package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.FraquezaAssembler;
import com.andre.apiheroi.dto.request.FraquezaRequestDTO;
import com.andre.apiheroi.dto.response.FraquezaResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.FraquezaModel;
import com.andre.apiheroi.service.AuthService;
import com.andre.apiheroi.service.FraquezaService;
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

@Tag(name = "v1 - Fraquezas")
@RestController
@RequestMapping("/api/fraquezas/v1")
public class FraquezaController {

    private final FraquezaService service;
    private final FraquezaAssembler assembler;
    private final PagedResourcesAssembler<FraquezaModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);
    private final IdempotencyService idempotencyService;
    private final AuthService authService;

    public FraquezaController(FraquezaService service, FraquezaAssembler assembler, IdempotencyService idempotencyService, AuthService authService) {
        this.service = service;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.authService = authService;
    }

    @Operation(summary = "Cadastrar fraqueza",
            description = "Exige login (X-API-Key) e o header X-Idempotency-Key. O heroiId precisa ser de um herói do perfil logado, e cada herói tem no máximo uma fraqueza (relação 1:1).")
    @ApiResponse(responseCode = "201", description = "Fraqueza cadastrada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = FraquezaResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Repetição: esta X-Idempotency-Key já foi usada; devolve a fraqueza cadastrada antes", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = FraquezaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, JSON mal formatado ou header X-Idempotency-Key ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "409", description = "O herói já tem uma fraqueza ou X-Idempotency-Key reutilizada com dados diferentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PostMapping
    public ResponseEntity<EntityModel<FraquezaResponseDTO>> create(
            @Parameter(description = "Valor único deste cadastro (ex.: um UUID novo)", example = "3f2b8c1e-7d4a-4e9b-a6c2-1f0e9d8b7a65")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados da fraqueza", required = true,
                    content = @Content(schema = @Schema(implementation = FraquezaRequestDTO.class)))
            @RequestBody @Valid FraquezaRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        IdempotencyService.Resultado resultado = idempotencyService.executar(
                "fraqueza", String.valueOf(idPerfilLogado), idempotencyKey, dto,
                () -> service.create(dto, idPerfilLogado).getId());
        return ResponseEntity.status(resultado.criadoAgora() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("X-Idempotency-Replayed", String.valueOf(!resultado.criadoAgora()))
                .body(assembler.toModel(service.findById(resultado.idCriado())));
    }

    @Operation(summary = "Listar fraquezas",
            description = "Lista paginada. Parâmetros: page (começa em 0), size (padrão 10) e sort (ex.: nome,asc).")
    @ApiResponse(responseCode = "200", description = "Página de fraquezas", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = FraquezaResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<FraquezaResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler));
    }

    @Operation(summary = "Buscar fraqueza por ID",
            description = "Devolve a fraqueza com os links HATEOAS para as próximas ações.")
    @ApiResponse(responseCode = "200", description = "Fraqueza encontrada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = FraquezaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Fraqueza não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<FraquezaResponseDTO>> findById(@Parameter(description = "ID da fraqueza", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.findById(id)));
    }

    @Operation(summary = "Buscar fraquezas por nome",
            description = "Busca por parte do nome, sem diferenciar maiúsculas de minúsculas. Paginada.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = FraquezaResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "400", description = "Parâmetro nome não enviado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/busca")
    public ResponseEntity<PagedModel<EntityModel<FraquezaResponseDTO>>> findByName(
            @Parameter(description = "Texto a procurar no nome", example = "água") @RequestParam String nome,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByName(nome, pageable), assembler));
    }

    @Operation(summary = "Fraqueza de um herói",
            description = "Devolve a fraqueza do herói informado (relação 1:1).")
    @ApiResponse(responseCode = "200", description = "Fraqueza encontrada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = FraquezaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado, ou herói sem fraqueza", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/heroi/{heroiId}")
    public ResponseEntity<EntityModel<FraquezaResponseDTO>> findByHeroi(
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long heroiId) {
        return ResponseEntity.ok(assembler.toModel(service.findByHeroi(heroiId)));
    }

    @Operation(summary = "Atualizar fraqueza",
            description = "Exige login (X-API-Key). Só o dono do herói altera. Trocar o heroiId move a fraqueza para outro herói seu (que ainda não tenha uma).")
    @ApiResponse(responseCode = "200", description = "Fraqueza atualizada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = FraquezaResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON mal formatado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "409", description = "O novo herói já tem uma fraqueza", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Fraqueza não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<FraquezaResponseDTO>> update(
            @Parameter(description = "ID da fraqueza", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados da fraqueza", required = true,
                    content = @Content(schema = @Schema(implementation = FraquezaRequestDTO.class)))
            @RequestBody @Valid FraquezaRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.update(id, dto, idPerfilLogado)));
    }

    @Operation(summary = "Excluir fraqueza",
            description = "Exige login (X-API-Key). Só o dono do herói exclui. O herói continua cadastrado.")
    @ApiResponse(responseCode = "204", description = "Fraqueza excluída", content = @Content)
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Fraqueza não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da fraqueza", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        service.delete(id, idPerfilLogado);
        return ResponseEntity.noContent().build();
    }
}
