package com.andre.apiheroi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequestDTO", example = """
        {
          "email": "lider.alfa@exemplo.com",
          "senha": "senha123"
        }""")
public record LoginRequestDTO(
        @Schema(description = "E-mail do perfil", example = "lider.alfa@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,

        @Schema(description = "Senha", example = "senha123")
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
