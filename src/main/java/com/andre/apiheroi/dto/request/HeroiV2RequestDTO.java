package com.andre.apiheroi.dto.request;

import com.andre.apiheroi.model.NivelForca;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "HeroiV2RequestDTO", example = """
        {
          "nome": "Lia Moreira",
          "nomeHeroico": "Faísca Azul",
          "nivelForca": "MUTANTE",
          "forca": 78
        }""")
public record HeroiV2RequestDTO(
        @Schema(description = "Nome civil do herói", example = "Lia Moreira")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Schema(description = "Nome heroico", example = "Faísca Azul")
        @NotBlank(message = "O nome heroico é obrigatório")
        @Size(max = 100, message = "O nome heroico deve ter no máximo 100 caracteres")
        String nomeHeroico,

        @Schema(description = "Nível de força", example = "MUTANTE")
        @NotNull(message = "O nível de força é obrigatório")
        NivelForca nivelForca,

        @Schema(description = "Força de 0 a 100. Obrigatória na v2.", example = "78")
        @NotNull(message = "A força é obrigatória na v2")
        @Min(value = 0, message = "A força deve ser de 0 a 100")
        @Max(value = 100, message = "A força deve ser de 0 a 100")
        Integer forca
) {
}
