package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.PerfilAssembler;
import com.andre.apiheroi.dto.request.PerfilRequestDTO;
import com.andre.apiheroi.dto.response.PerfilResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.service.AuthService;
import com.andre.apiheroi.service.IdempotencyService;
import com.andre.apiheroi.service.PerfilService;
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

@Tag(name = "v1 - Perfis")
@RestController
@RequestMapping("/api/perfis/v1")
public class PerfilController {

    private final PerfilService service;
    private final PerfilAssembler assembler;
    private final PagedResourcesAssembler<PerfilModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);
    private final IdempotencyService idempotencyService;
    private final AuthService authService;

    public PerfilController(PerfilService service, PerfilAssembler assembler, IdempotencyService idempotencyService, AuthService authService) {
        this.service = service;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.authService = authService;
    }

    @Operation(summary = "Cadastrar perfil",
            description = "Cadastro do usuário da API. Não exige login. Exige o header X-Idempotency-Key. A senha é obrigatória aqui, é guardada como hash BCrypt e nunca é devolvida. Use dados fictícios.")
    @ApiResponse(responseCode = "201", description = "Perfil cadastrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PerfilResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Repetição: esta X-Idempotency-Key já foi usada; devolve o perfil cadastrado antes", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PerfilResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, JSON mal formatado ou header X-Idempotency-Key ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado ou X-Idempotency-Key reutilizada com dados diferentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PostMapping
    public ResponseEntity<EntityModel<PerfilResponseDTO>> create(
            @Parameter(description = "Valor único deste cadastro (ex.: um UUID novo)", example = "3f2b8c1e-7d4a-4e9b-a6c2-1f0e9d8b7a65")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do perfil", required = true,
                    content = @Content(schema = @Schema(implementation = PerfilRequestDTO.class)))
            @RequestBody @Valid PerfilRequestDTO dto) {
        IdempotencyService.Resultado resultado = idempotencyService.executar(
                "perfil", "publico", idempotencyKey, dto,
                () -> service.create(dto).getId());
        return ResponseEntity.status(resultado.criadoAgora() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("X-Idempotency-Replayed", String.valueOf(!resultado.criadoAgora()))
                .body(assembler.toModel(service.findById(resultado.idCriado())));
    }

    @Operation(summary = "Listar perfis",
            description = "Lista paginada. Parâmetros: page (começa em 0), size (padrão 10) e sort (ex.: nome,asc).")
    @ApiResponse(responseCode = "200", description = "Página de perfis", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = PerfilResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PerfilResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler));
    }

    @Operation(summary = "Buscar perfil por ID",
            description = "Devolve o perfil com os links HATEOAS para as próximas ações.")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PerfilResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PerfilResponseDTO>> findById(@Parameter(description = "ID do perfil", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.findById(id)));
    }

    @Operation(summary = "Buscar perfis por e-mail",
            description = "Busca por parte do e-mail, sem diferenciar maiúsculas de minúsculas. Paginada.")
    @ApiResponse(responseCode = "200", description = "Página com o resultado", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = PerfilResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "400", description = "Parâmetro email não enviado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/busca")
    public ResponseEntity<PagedModel<EntityModel<PerfilResponseDTO>>> findByEmail(
            @Parameter(description = "Parte do e-mail", example = "alfa") @RequestParam String email,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findByEmail(email, pageable), assembler));
    }

    @Operation(summary = "Atualizar perfil",
            description = "Exige login (X-API-Key). Cada perfil só altera o PRÓPRIO cadastro. Senha vazia mantém a atual.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = PerfilResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON mal formatado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "A X-API-Key é de outro perfil: só é possível alterar ou excluir o próprio", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já usado por outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<PerfilResponseDTO>> update(
            @Parameter(description = "ID do perfil", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do perfil", required = true,
                    content = @Content(schema = @Schema(implementation = PerfilRequestDTO.class)))
            @RequestBody @Valid PerfilRequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toModel(service.update(id, dto, idPerfilLogado)));
    }

    @Operation(summary = "Excluir perfil",
            description = "Exige login (X-API-Key). Cada perfil só exclui o PRÓPRIO cadastro. Os heróis (com poderes e fraquezas) e as equipes do perfil são excluídos junto.")
    @ApiResponse(responseCode = "204", description = "Perfil excluído", content = @Content)
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "A X-API-Key é de outro perfil: só é possível alterar ou excluir o próprio", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do perfil", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        service.delete(id, idPerfilLogado);
        return ResponseEntity.noContent().build();
    }
}
