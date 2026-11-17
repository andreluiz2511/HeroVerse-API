package com.andre.apiheroi.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// =====================================================================================================
// badRequestException = "os dados chegaram, mas quebram uma regra de negócio" -> HTTP 400 (Bad Request).
// =====================================================================================================
//
// Diferença para o 400 da validação (@Valid):
//   - o @Valid confere regras FIXAS de cada campo (@NotBlank, @Size, @Email...) antes de entrar no método;
//   - esta exceção é para regras que dependem do CONTEXTO e só o service consegue conferir.
//
// Onde é lançada: no PerfilService, quando o cadastro chega sem senha. Na alteração a senha é opcional
// (vazia = "mantém a atual"), por isso o DTO não pode exigir com @NotBlank e quem confere é o service:
//
//     if (dto.senha() == null || dto.senha().isBlank()) {
//         throw new badRequestException("senha: A senha é obrigatória no cadastro");
//     }
//
// O formato "campo: mensagem" é proposital: é o mesmo usado nos erros do @Valid, e o front-end usa o
// nome antes dos dois-pontos para mostrar o erro embaixo do campo certo do formulário.
//
// Ela "sobe" até o ExceptionHandlerModel, que a transforma no JSON padrão com status 400.
// =====================================================================================================
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class badRequestException extends RuntimeException {
    public badRequestException(String message) {
        super(message);
    }
}
