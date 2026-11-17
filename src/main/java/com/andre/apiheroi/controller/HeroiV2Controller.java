package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.HeroiAssembler;
import com.andre.apiheroi.dto.request.HeroiV2RequestDTO;
import com.andre.apiheroi.dto.response.HeroiV2ResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.HeroiModel;
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

@Tag(name = "v2 - Heróis")
@RestController
@RequestMapping("/api/herois/v2")
public class HeroiV2Controller {

    private final HeroiService service;
    private final HeroiAssembler assembler;
    private final PagedResourcesAssembler<HeroiModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);
    private final IdempotencyService idempotencyService;
    private final AuthService authService;

    public HeroiV2Controller(HeroiService service, HeroiAssembler assembler, IdempotencyService idempotencyService, AuthService authService) {
        this.service = service;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.authService = authService;
    }

    @Operation(summary = "Listar heróis (v2)",
            description = "Formato resumido (nomes do poder, da fraqueza, das equipes e do perfil) e com o campo forca (0 a 100). Paginação: page, size e sort (ex.: forca,desc).")
    @ApiResponse(responseCode = "200", description = "Página de heróis (v2)", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = HeroiV2ResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<HeroiV2ResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler::toV2Model));
    }

    @Operation(summary = "Buscar herói por ID (v2)",
            description = "Devolve o herói no formato da v2, com todos os links.")
    @ApiResponse(responseCode = "200", description = "Herói encontrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiV2ResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<HeroiV2ResponseDTO>> findById(@Parameter(description = "ID do herói", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toV2Model(service.findById(id)));
    }

    @Operation(summary = "Cadastrar herói (v2)",
            description = "Igual à v1, mas com o campo obrigatório forca (0 a 100). Exige login (X-API-Key) e o header X-Idempotency-Key.")
    @ApiResponse(responseCode = "201", description = "Herói cadastrado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiV2ResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Repetição: esta X-Idempotency-Key já foi usada; devolve o herói cadastrado antes", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiV2ResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: forca ausente), JSON mal formatado ou header X-Idempotency-Key ausente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @ApiResponse(responseCode = "409", description = "A mesma X-Idempotency-Key foi reutilizada com dados diferentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @PostMapping
    public ResponseEntity<EntityModel<HeroiV2ResponseDTO>> create(
            @Parameter(description = "Valor único deste cadastro (ex.: um UUID novo)", example = "3f2b8c1e-7d4a-4e9b-a6c2-1f0e9d8b7a65")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do herói (v2)", required = true,
                    content = @Content(schema = @Schema(implementation = HeroiV2RequestDTO.class)))
            @RequestBody @Valid HeroiV2RequestDTO dto) {
        PerfilModel dono = authService.validar(apiKey);
        IdempotencyService.Resultado resultado = idempotencyService.executar(
                "heroi", String.valueOf(dono.getId()), idempotencyKey, dto,
                () -> service.createV2(dto, dono).getId());
        return ResponseEntity.status(resultado.criadoAgora() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("X-Idempotency-Replayed", String.valueOf(!resultado.criadoAgora()))
                .body(assembler.toV2Model(service.findById(resultado.idCriado())));
    }

    @Operation(summary = "Atualizar herói (v2)",
            description = "Exige login (X-API-Key). Só o perfil dono altera. Substitui todos os campos, inclusive forca.")
    @ApiResponse(responseCode = "200", description = "Herói atualizado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = HeroiV2ResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON mal formatado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida (faça login em POST /api/auth/login)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "403", description = "O herói pertence a outro perfil", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Herói não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<HeroiV2ResponseDTO>> update(
            @Parameter(description = "ID do herói", example = "1") @PathVariable Long id,
            @Parameter(description = "Chave obtida em POST /api/auth/login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do herói (v2)", required = true,
                    content = @Content(schema = @Schema(implementation = HeroiV2RequestDTO.class)))
            @RequestBody @Valid HeroiV2RequestDTO dto) {
        Long idPerfilLogado = authService.validar(apiKey).getId();
        return ResponseEntity.ok(assembler.toV2Model(service.updateV2(id, dto, idPerfilLogado)));
    }
}
