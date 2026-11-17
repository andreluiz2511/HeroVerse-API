package com.andre.apiheroi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "PoderRequestDTO", example = """
        {
          "nome": "Rajada Elétrica",
          "descricao": "Dispara descargas elétricas pelas mãos.",
          "heroiId": 1
        }""")
public record PoderRequestDTO(
        @Schema(description = "Nome do poder", example = "Rajada Elétrica")
        @NotBlank(message = "O nome do poder é obrigatório")
        @Size(max = 100, message = "O nome do poder deve ter no máximo 100 caracteres")
        String nome,

        @Schema(description = "Descrição do poder", example = "Dispara descargas elétricas pelas mãos.")
        @NotBlank(message = "A descrição do poder é obrigatória")
        @Size(max = 500, message = "A descrição do poder deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "ID do herói (precisa ser um herói do perfil logado). Cada herói tem no máximo um poder.", example = "1")
        @NotNull(message = "O heroiId é obrigatório")
        Long heroiId
) {
}
