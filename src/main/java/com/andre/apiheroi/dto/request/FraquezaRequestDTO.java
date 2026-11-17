package com.andre.apiheroi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "FraquezaRequestDTO", example = """
        {
          "nome": "Água Salgada",
          "descricao": "Perde os poderes quando molhada com água do mar.",
          "heroiId": 1
        }""")
public record FraquezaRequestDTO(
        @Schema(description = "Nome da fraqueza", example = "Água Salgada")
        @NotBlank(message = "O nome da fraqueza é obrigatório")
        @Size(max = 100, message = "O nome da fraqueza deve ter no máximo 100 caracteres")
        String nome,

        @Schema(description = "Descrição da fraqueza", example = "Perde os poderes quando molhada com água do mar.")
        @NotBlank(message = "A descrição da fraqueza é obrigatória")
        @Size(max = 500, message = "A descrição da fraqueza deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "ID do herói (precisa ser um herói do perfil logado). Cada herói tem no máximo uma fraqueza.", example = "1")
        @NotNull(message = "O heroiId é obrigatório")
        Long heroiId
) {
}
