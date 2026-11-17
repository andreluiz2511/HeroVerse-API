package com.andre.apiheroi.controller;

import com.andre.apiheroi.dto.request.LoginRequestDTO;
import com.andre.apiheroi.dto.response.LoginResponseDTO;
import com.andre.apiheroi.excecao.exceptionCustomModel;
import com.andre.apiheroi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Login",
            description = "Confere e-mail e senha do perfil e devolve uma chave de API nova. Envie-a no header X-API-Key nas operações protegidas (cadastros de herói, poder, fraqueza e equipe, e todos os PUT e DELETE). Um novo login invalida a chave anterior.")
    @ApiResponse(responseCode = "200", description = "Login realizado", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = LoginResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "E-mail ou senha não enviados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @PostMapping("/login")
    public ResponseEntity<EntityModel<LoginResponseDTO>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Credenciais", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class)))
            @RequestBody @Valid LoginRequestDTO dto) {
        LoginResponseDTO login = service.login(dto.email(), dto.senha());
        EntityModel<LoginResponseDTO> modelo = EntityModel.of(login,
                linkTo(methodOn(PerfilController.class).findById(login.perfilId())).withRel("perfil").withType("GET"),
                linkTo(methodOn(AuthController.class).logout(null)).withRel("logout").withType("POST"));
        return ResponseEntity.ok(modelo);
    }

    @Operation(summary = "Logout", description = "Apaga a chave enviada no header X-API-Key; ela para de funcionar.")
    @ApiResponse(responseCode = "204", description = "Logout realizado", content = @Content)
    @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = exceptionCustomModel.class)))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Chave obtida no login")
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        service.logout(apiKey);
        return ResponseEntity.noContent().build();
    }
}
