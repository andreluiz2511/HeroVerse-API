package com.andre.apiheroi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponseDTO", example = LoginResponseDTO.EXEMPLO)
public record LoginResponseDTO(
        String apiKey,
        Long perfilId,
        String nome,
        String email,
        String mensagem
) {
    public static final String EXEMPLO = """
            {
              "apiKey": "5f0c7a2e-3b1d-4c8e-9a6f-2d4b8e1c7a90",
              "perfilId": 1,
              "nome": "Líder Alfa",
              "email": "lider.alfa@exemplo.com",
              "mensagem": "Login realizado. Envie a apiKey no header X-API-Key nas operações protegidas.",
              "_links": {
                "perfil": {
                  "href": "http://localhost:8080/api/perfis/v1/1",
                  "type": "GET"
                },
                "logout": {
                  "href": "http://localhost:8080/api/auth/logout",
                  "type": "POST"
                }
              }
            }""";
}
