package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.EquipeAssembler;
import com.andre.apiheroi.dto.request.EquipeRequestDTO;
import com.andre.apiheroi.dto.response.EquipeResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.EquipeModel;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.service.AuthService;
import com.andre.apiheroi.service.EquipeService;
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

@Tag(name = "v1 - Equipes")
@RestController
@RequestMapping("/api/equipes/v1")
public class EquipeController {

    private final EquipeService service;
    private final EquipeAssembler assembler;
    private final PagedResourcesAssembler<EquipeModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);
    private final IdempotencyService idempotencyService;
    private final AuthService authService;

    public EquipeController(EquipeService service, EquipeAssembler assembler, IdempotencyService idempotencyService, AuthService authService) {
        this.service = service;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.authService = authService;
    }

    @Operation(summary = "Cadastrar equipe",
            description = "Exige login (X-API-Key) e o header X-Idempotency-Key. A equipe é criada no perfil logado e começa vazia; adicione heróis em POST /{equipeId}/herois/{heroiId}.")
    @ApiResponse(responseCode = "201", description = "Equipe cadastrada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Repetição: esta X-Idempotency-Key já foi usada; devolve a equipe cadastrada antes", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, JSON mal formatado ou header X-Idempotency-Key ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @ApiResponse(responseCode = "409", description = "A mesma X-Idempotency-Key foi reutilizada com dados diferentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @PostMapping
    public ResponseEntity<EntityModel<EquipeResponseDTO>> create(
            @Parameter(description = "Valor único deste cadastro (ex.: um UUID novo)", example = "3f2b8c1e-7d4a-4e9b-a6c2-1f0e9d8b7a65")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados da equipe", required = true,
                    content = @Content(schema = @Schema(implementation = EquipeRequestDTO.class)))
            @RequestBody @Valid EquipeRequestDTO dto) {
        PerfilModel dono = authService.validar(apiKey);
        IdempotencyService.Resultado resultado = idempotencyService.executar(
                "equipe", String.valueOf(dono.getId()), idempotencyKey, dto,
                () -> service.create(dto, dono).getId());
        return ResponseEntity.status(resultado.criadoAgora() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("X-Idempotency-Replayed", String.valueOf(!resultado.criadoAgora()))
                .body(assembler.toModel(service.findById(resultado.idCriado())));
    }

    @Operation(summary = "Listar equipes",
            description = "Lista paginada. Parâmetros: page (começa em 0), size (padrão 10) e sort (ex.: nome,asc).")
    @ApiResponse(responseCode = "200", description = "Página de equipes", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = EquipeResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<EquipeResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler));
    }

    @Operation(summary = "Buscar equipe por ID",
            description = "Devolve a equipe com os links HATEOAS para as próximas ações.")
    @ApiResponse(responseCode = "200", description = "Equipe encontrada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<EquipeResponseDTO>> findById(@Parameter(description = "ID da equipe", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.findById(id)));
    }

    @Operation(summary = "Buscar equipes por nome",
            description = "Busca por parte do nome, sem diferenciar maiúsculas de minúsculas. Paginada.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = EquipeResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "400", description = "Parâmetro nome não enviado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/busca")
    public ResponseEntity<PagedModel<EntityModel<EquipeResponseDTO>>> findByNome(
            @Parameter(description = "Texto a procurar no nome", example = "liga") @RequestParam String nome,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByNome(nome, pageable), assembler));
    }

    @Operation(summary = "Equipes de um perfil",
            description = "Consulta personalizada com JPQL (@Query): equipes do perfil informado.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = EquipeResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<PagedModel<EntityModel<EquipeResponseDTO>>> findByPerfil(
            @Parameter(description = "ID do perfil", example = "1") @PathVariable Long perfilId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByPerfil(perfilId, pageable), assembler));
    }

    @Operation(summary = "Equipes de um herói",
            description = "Consulta personalizada com JPQL e JOIN (@Query): equipes de que o herói participa (relação N:N).")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = EquipeResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/heroi/{heroiId}")
    public ResponseEntity<PagedModel<EntityModel<EquipeResponseDTO>>> findByHeroi(
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long heroiId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByHeroi(heroiId, pageable), assembler));
    }

    @Operation(summary = "Adicionar herói à equipe",
            description = "Exige login (X-API-Key). A equipe e o herói precisam ser do perfil logado. Adicionar um herói que já está na equipe não muda nada.")
    @ApiResponse(responseCode = "200", description = "Herói adicionado (ou já estava na equipe)", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeResponseDTO.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "A equipe ou o herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe ou herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PostMapping("/{equipeId}/herois/{heroiId}")
    public ResponseEntity<EntityModel<EquipeResponseDTO>> adicionarHeroi(
            @Parameter(description = "ID da equipe", example = "1") @PathVariable Long equipeId,
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long heroiId,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.adicionarHeroi(equipeId, heroiId, idPerfilLogado)));
    }

    @Operation(summary = "Remover herói da equipe",
            description = "Exige login (X-API-Key). Só o perfil dono da equipe remove. O herói continua cadastrado.")
    @ApiResponse(responseCode = "200", description = "Herói removido da equipe", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeResponseDTO.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "A equipe pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe ou herói não encontrado, ou herói fora da equipe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @DeleteMapping("/{equipeId}/herois/{heroiId}")
    public ResponseEntity<EntityModel<EquipeResponseDTO>> removerHeroi(
            @Parameter(description = "ID da equipe", example = "1") @PathVariable Long equipeId,
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long heroiId,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.removerHeroi(equipeId, heroiId, idPerfilLogado)));
    }

    @Operation(summary = "Atualizar equipe",
            description = "Exige login (X-API-Key). Só o perfil dono altera o nome da equipe.")
    @ApiResponse(responseCode = "200", description = "Equipe atualizada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON mal formatado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "A equipe pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<EquipeResponseDTO>> update(
            @Parameter(description = "ID da equipe", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados da equipe", required = true,
                    content = @Content(schema = @Schema(implementation = EquipeRequestDTO.class)))
            @RequestBody @Valid EquipeRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.update(id, dto, idPerfilLogado)));
    }

    @Operation(summary = "Excluir equipe",
            description = "Exige login (X-API-Key). Só o perfil dono exclui. Os heróis continuam cadastrados.")
    @ApiResponse(responseCode = "204", description = "Equipe excluída", content = @Content)
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "A equipe pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da equipe", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        service.delete(id, idPerfilLogado);
        return ResponseEntity.noContent().build();
    }
}
