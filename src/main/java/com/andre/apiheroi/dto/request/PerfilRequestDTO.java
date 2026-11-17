package com.andre.apiheroi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "PerfilRequestDTO", example = """
        {
          "nome": "Comandante Fictício",
          "email": "comandante@exemplo.com",
          "senha": "senha123"
        }""")
public record PerfilRequestDTO(
        @Schema(description = "Nome do perfil (use um nome fictício)", example = "Comandante Fictício")
        @NotBlank(message = "O nome do perfil é obrigatório")
        @Size(min = 3, max = 60, message = "O nome do perfil deve ter entre 3 e 60 caracteres")
        String nome,

        @Schema(description = "E-mail (use um e-mail de exemplo). É o login.", example = "comandante@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres")
        String email,

        @Schema(description = "Senha (mínimo 6 caracteres). Obrigatória no cadastro; na alteração, deixe vazia para manter a atual.", example = "senha123")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String senha
) {
}
