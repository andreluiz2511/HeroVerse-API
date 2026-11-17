package com.andre.apiheroi.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// =====================================================================================================
// unauthorizedException = "não sei quem você é" -> HTTP 401 (Unauthorized).
// =====================================================================================================
//
// Onde é lançada: no AuthService.
//   - login com e-mail ou senha errados -> "E-mail ou senha inválidos";
//   - operação protegida (POST de herói/poder/fraqueza/equipe, PUT, DELETE) sem o header X-API-Key;
//   - X-API-Key que não existe mais (depois de um novo login ou de um logout).
//
// Exemplo do AuthService.validar(apiKey):
//
//     return repositoryPerfil.findByApiKey(apiKey)
//             .orElseThrow(() -> new unauthorizedException("X-API-Key inválida. Faça login novamente em POST /api/auth/login"));
//
// Segurança: no login, a mensagem é a MESMA para "e-mail não existe" e "senha errada". Se fossem diferentes,
// alguém poderia descobrir quais e-mails estão cadastrados só olhando a mensagem de erro.
//
// Ela "sobe" até o ExceptionHandlerModel, que a transforma no JSON padrão com status 401.
// =====================================================================================================
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class unauthorizedException extends RuntimeException {
    public unauthorizedException(String message) {
        super(message);
    }
}
