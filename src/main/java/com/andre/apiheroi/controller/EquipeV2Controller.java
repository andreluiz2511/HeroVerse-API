package com.andre.apiheroi.controller;

import com.andre.apiheroi.assembler.EquipeAssembler;
import com.andre.apiheroi.dto.response.EquipeV2ResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.model.EquipeModel;
import com.andre.apiheroi.service.EquipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "v2 - Equipes")
@RestController
@RequestMapping("/api/equipes/v2")
public class EquipeV2Controller {

    private final EquipeService service;
    private final EquipeAssembler assembler;
    private final PagedResourcesAssembler<EquipeModel> pagedAssembler =
            new PagedResourcesAssembler<>(new HateoasPageableHandlerMethodArgumentResolver(), null);

    public EquipeV2Controller(EquipeService service, EquipeAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @Operation(summary = "Listar equipes (v2)",
            description = "Cada equipe traz quantidadeHerois e forcaMedia (média da força dos heróis que têm força cadastrada). Só consulta.")
    @ApiResponse(responseCode = "200", description = "Página de equipes (v2)", content = @Content(mediaType = "application/hal+json", schema = @Schema(type = "object", example = EquipeV2ResponseDTO.EXEMPLO_PAGINA)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<EquipeV2ResponseDTO>>> findAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.findAll(pageable), assembler::toV2Model));
    }

    @Operation(summary = "Buscar equipe por ID (v2)",
            description = "Devolve a equipe com quantidadeHerois, forcaMedia e todos os links (os de alteração apontam para a v1).")
    @ApiResponse(responseCode = "200", description = "Equipe encontrada", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = EquipeV2ResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido (não numérico)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de 20 requisições por minuto excedido; aguarde os segundos do header Retry-After", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"erro\":\"Muitas requisições. Tente novamente em 30s\"}")))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<EquipeV2ResponseDTO>> findById(@Parameter(description = "ID da equipe", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toV2Model(service.findById(id)));
    }
}
