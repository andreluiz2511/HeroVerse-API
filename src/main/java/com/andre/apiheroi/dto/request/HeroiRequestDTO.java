package com.andre.apiheroi.dto.request;

import com.andre.apiheroi.model.NivelForca;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "HeroiRequestDTO", example = """
        {
          "nome": "Lia Moreira",
          "nomeHeroico": "Faísca Azul",
          "nivelForca": "MUTANTE"
        }""")
public record HeroiRequestDTO(
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
        NivelForca nivelForca
) {
}
