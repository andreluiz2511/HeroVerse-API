package com.andre.apiheroi.excecao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

// =====================================================================================================
// exceptionCustomModel = o "molde" (formato) de TODA resposta de erro da API.
// =====================================================================================================
//
// Não importa qual erro aconteceu (400, 401, 403, 404, 409, 500...): o cliente sempre recebe um JSON assim:
//
//     {
//       "data":     "2026-11-16T20:30:00.000+00:00",
//       "mensagem": "Herói não encontrado, ID: 99",
//       "detalhes": "uri=/api/herois/v1/99"
//     }
//
// Por que um formato único? Quem consome a API (o front-end, o Postman, o professor no Swagger) só precisa
// aprender a ler um formato: basta pegar o campo "mensagem" para mostrar o erro ao usuário.
//
// Por que um "record"? Um record é uma classe imutável e enxuta: o Java gera sozinho o construtor,
// os "getters" (data(), mensagem(), detalhes()), equals, hashCode e toString.
// O Jackson (biblioteca que transforma objeto Java <-> JSON) entende records e usa os nomes dos
// componentes (data, mensagem, detalhes) como nomes dos campos do JSON.
//
// Os três campos:
//   - data:     quando o erro aconteceu. É um java.util.Date; o Jackson converte para texto no padrão ISO-8601.
//   - mensagem: a explicação do erro, em português. É ela que o ExceptionHandlerModel monta para cada caso.
//   - detalhes: qual endereço foi chamado, no formato "uri=/api/...". Vem do request.getDescription(false).
//
// O @Schema abaixo é só documentação: faz o Swagger mostrar um exemplo desse JSON nas respostas de erro.
// =====================================================================================================
@Schema(name = "exceptionCustomModel", example = """
        {
          "data": "2026-11-16T20:30:00.000+00:00",
          "mensagem": "nomeHeroico: O nome heroico é obrigatório",
          "detalhes": "uri=/api/herois/v1"
        }""")
public record exceptionCustomModel(Date data, String mensagem, String detalhes) {
}
