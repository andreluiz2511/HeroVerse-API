package com.andre.apiheroi.controller;

import com.andre.apiheroi.excecao.badRequestException;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mesmo endereco, duas representacoes selecionadas pelo header X-API-Version.
 *
 * As rotas antigas /v1 e /v2 permanecem funcionando para nao quebrar o site,
 * o Swagger nem os links HATEOAS existentes.
 */
@Tag(name = "Versionamento por header", description = "Use X-API-Version: 1 ou 2. Sem o header, a API usa v1.")
@RestController
public class VersionamentoController {

    private final HeroiController heroiV1;
    private final HeroiV2Controller heroiV2;
    private final EquipeController equipeV1;
    private final EquipeV2Controller equipeV2;

    public VersionamentoController(HeroiController heroiV1, HeroiV2Controller heroiV2,
                                   EquipeController equipeV1, EquipeV2Controller equipeV2) {
        this.heroiV1 = heroiV1;
        this.heroiV2 = heroiV2;
        this.equipeV1 = equipeV1;
        this.equipeV2 = equipeV2;
    }

    @Operation(summary = "Listar herois por versao (header)",
            description = "Mesmo endpoint para v1 (dados completos) e v2 (resumo e forca). " +
                    "Envie X-API-Version: 1 ou X-API-Version: 2. Sem o header, utiliza v1. " +
                    "Exemplo: GET /api/herois com X-API-Version: 2. Resultado sempre paginado.")
    @ApiResponse(responseCode = "200", description = "Pagina de herois na versao solicitada")
    @ApiResponse(responseCode = "400", description = "Versao invalida: escolha 1 ou 2", content = @Content(schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de requisicoes excedido; veja Retry-After")
    @GetMapping("/api/herois")
    public ResponseEntity<?> listarHerois(
            @Parameter(description = "Versao desejada (1 ou 2)", example = "2")
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String versao,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return switch (validarVersao(versao)) {
            case "1" -> heroiV1.findAll(pageable);
            case "2" -> heroiV2.findAll(pageable);
            default -> throw new IllegalStateException("Versao nao tratada");
        };
    }

    @Operation(summary = "Buscar heroi por ID usando X-API-Version",
            description = "Para o mesmo ID, a v1 apresenta os dados completos e a v2 inclui forca e dados resumidos.")
    @ApiResponse(responseCode = "200", description = "Heroi encontrado na versao solicitada")
    @ApiResponse(responseCode = "400", description = "Versao ou ID invalidos", content = @Content(schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Heroi nao encontrado", content = @Content(schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de requisicoes excedido; veja Retry-After")
    @GetMapping("/api/herois/{id}")
    public ResponseEntity<?> buscarHeroi(
            @Parameter(description = "ID do heroi", example = "1") @PathVariable Long id,
            @Parameter(description = "Versao desejada (1 ou 2)", example = "2")
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String versao) {
        return switch (validarVersao(versao)) {
            case "1" -> heroiV1.findById(id);
            case "2" -> heroiV2.findById(id);
            default -> throw new IllegalStateException("Versao nao tratada");
        };
    }

    @Operation(summary = "Listar equipes por versao (header)",
            description = "v1 mostra dados basicos; v2 inclui quantidadeHerois e forcaMedia. " +
                    "Envie X-API-Version: 1 ou 2. Sem o header, utiliza v1. Pagina via page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Pagina de equipes na versao solicitada")
    @ApiResponse(responseCode = "400", description = "Versao invalida: escolha 1 ou 2", content = @Content(schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de requisicoes excedido; veja Retry-After")
    @GetMapping("/api/equipes")
    public ResponseEntity<?> listarEquipes(
            @Parameter(description = "Versao desejada (1 ou 2)", example = "2")
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String versao,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return switch (validarVersao(versao)) {
            case "1" -> equipeV1.findAll(pageable);
            case "2" -> equipeV2.findAll(pageable);
            default -> throw new IllegalStateException("Versao nao tratada");
        };
    }

    @Operation(summary = "Buscar equipe por ID usando X-API-Version",
            description = "A v2 adiciona quantidadeHerois e forcaMedia a resposta da equipe.")
    @ApiResponse(responseCode = "200", description = "Equipe encontrada na versao solicitada")
    @ApiResponse(responseCode = "400", description = "Versao ou ID invalidos", content = @Content(schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "404", description = "Equipe nao encontrada", content = @Content(schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "429", description = "Limite de requisicoes excedido; veja Retry-After")
    @GetMapping("/api/equipes/{id}")
    public ResponseEntity<?> buscarEquipe(
            @Parameter(description = "ID da equipe", example = "1") @PathVariable Long id,
            @Parameter(description = "Versao desejada (1 ou 2)", example = "2")
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String versao) {
        return switch (validarVersao(versao)) {
            case "1" -> equipeV1.findById(id);
            case "2" -> equipeV2.findById(id);
            default -> throw new IllegalStateException("Versao nao tratada");
        };
    }

    private String validarVersao(String versao) {
        if (!"1".equals(versao) && !"2".equals(versao)) {
            throw new badRequestException("X-API-Version invalido: envie 1 ou 2");
        }
        return versao;
    }
}
