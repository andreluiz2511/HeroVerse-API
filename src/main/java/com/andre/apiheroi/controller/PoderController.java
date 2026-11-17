package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.PoderAssembler;
import com.andre.apiheroi.dto.request.PoderRequestDTO;
import com.andre.apiheroi.dto.response.PoderResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.PoderModel;
import com.andre.apiheroi.service.AuthService;
import com.andre.apiheroi.service.IdempotencyService;
import com.andre.apiheroi.service.PoderService;
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

@Tag(name = "v1 - Poderes")
@RestController
@RequestMapping("/api/poderes/v1")
public class PoderController {

    private final PoderService service;
    private final PoderAssembler assembler;
    private final PagedResourcesAssembler<PoderModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);
    private final IdempotencyService idempotencyService;
    private final AuthService authService;

    public PoderController(PoderService service, PoderAssembler assembler, IdempotencyService idempotencyService, AuthService authService) {
        this.service = service;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.authService = authService;
    }

    @Operation(summary = "Cadastrar poder",
            description = "Exige login (X-API-Key) e o header X-Idempotency-Key. O heroiId precisa ser de um herói do perfil logado, e cada herói tem no máximo um poder (relação 1:1).")
    @ApiResponse(responseCode = "201", description = "Poder cadastrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PoderResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Repetição: esta X-Idempotency-Key já foi usada; devolve o poder cadastrado antes", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PoderResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, JSON mal formatado ou header X-Idempotency-Key ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "409", description = "O herói já tem um poder ou X-Idempotency-Key reutilizada com dados diferentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PostMapping
    public ResponseEntity<EntityModel<PoderResponseDTO>> create(
            @Parameter(description = "Valor único deste cadastro (ex.: um UUID novo)", example = "3f2b8c1e-7d4a-4e9b-a6c2-1f0e9d8b7a65")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do poder", required = true,
                    content = @Content(schema = @Schema(implementation = PoderRequestDTO.class)))
            @RequestBody @Valid PoderRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        IdempotencyService.Resultado resultado = idempotencyService.executar(
                "poder", String.valueOf(idPerfilLogado), idempotencyKey, dto,
                () -> service.create(dto, idPerfilLogado).getId());
        return ResponseEntity.status(resultado.criadoAgora() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("X-Idempotency-Replayed", String.valueOf(!resultado.criadoAgora()))
                .body(assembler.toModel(service.findById(resultado.idCriado())));
    }

    @Operation(summary = "Listar poderes",
            description = "Lista paginada. Parâmetros: page (começa em 0), size (padrão 10) e sort (ex.: nome,asc).")
    @ApiResponse(responseCode = "200", description = "Página de poderes", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = PoderResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PoderResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler));
    }

    @Operation(summary = "Buscar poder por ID",
            description = "Devolve o poder com os links HATEOAS para as próximas ações.")
    @ApiResponse(responseCode = "200", description = "Poder encontrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PoderResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Poder não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PoderResponseDTO>> findById(@Parameter(description = "ID do poder", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.findById(id)));
    }

    @Operation(summary = "Buscar poderes por nome",
            description = "Busca por parte do nome, sem diferenciar maiúsculas de minúsculas. Paginada.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = PoderResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "400", description = "Parâmetro nome não enviado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/busca")
    public ResponseEntity<PagedModel<EntityModel<PoderResponseDTO>>> findByName(
            @Parameter(description = "Texto a procurar no nome", example = "rajada") @RequestParam String nome,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByName(nome, pageable), assembler));
    }

    @Operation(summary = "Poder de um herói",
            description = "Devolve o poder do herói informado (relação 1:1).")
    @ApiResponse(responseCode = "200", description = "Poder encontrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PoderResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado, ou herói sem poder", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/heroi/{heroiId}")
    public ResponseEntity<EntityModel<PoderResponseDTO>> findByHeroi(
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long heroiId) {
        return ResponseEntity.ok(assembler.toModel(service.findByHeroi(heroiId)));
    }

    @Operation(summary = "Atualizar poder",
            description = "Exige login (X-API-Key). Só o dono do herói altera. Trocar o heroiId move o poder para outro herói seu (que ainda não tenha um).")
    @ApiResponse(responseCode = "200", description = "Poder atualizado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PoderResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON mal formatado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "409", description = "O novo herói já tem um poder", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Poder não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<PoderResponseDTO>> update(
            @Parameter(description = "ID do poder", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do poder", required = true,
                    content = @Content(schema = @Schema(implementation = PoderRequestDTO.class)))
            @RequestBody @Valid PoderRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.update(id, dto, idPerfilLogado)));
    }

    @Operation(summary = "Excluir poder",
            description = "Exige login (X-API-Key). Só o dono do herói exclui. O herói continua cadastrado.")
    @ApiResponse(responseCode = "204", description = "Poder excluído", content = @Content)
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Poder não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do poder", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        service.delete(id, idPerfilLogado);
        return ResponseEntity.noContent().build();
    }
}
