package com.andre.apiheroi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "EquipeRequestDTO", example = """
        {
          "nome": "Liga do Amanhecer"
        }""")
public record EquipeRequestDTO(
        @Schema(description = "Nome da equipe", example = "Liga do Amanhecer")
        @NotBlank(message = "O nome da equipe é obrigatório")
        @Size(max = 100, message = "O nome da equipe deve ter no máximo 100 caracteres")
        String nome
) {
}
