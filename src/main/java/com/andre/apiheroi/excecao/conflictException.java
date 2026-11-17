package com.andre.apiheroi.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// =====================================================================================================
// conflictException = "isso já existe" -> HTTP 409 (Conflict).
// =====================================================================================================
//
// Onde é lançada: nos services, ANTES de salvar, quando algo que não pode se repetir já está em uso:
//   - e-mail de perfil já cadastrado;
//   - herói que já tem poder (ou fraqueza): as relações são 1:1, cada herói tem no máximo um de cada.
//
// Exemplo do PoderService:
//
//     if (repository.existsByHeroiId(heroi.getId())) {
//         throw new conflictException("heroiId: Este herói já tem um poder (cada herói pode ter só um)");
//     }
//
// Por que conferir no service se o banco já tem unique = true?
//   - O banco também barraria, mas lançaria DataIntegrityViolationException, que vira um 409 com uma mensagem
//     GENÉRICA (o handler não sabe o que repetiu, porque a mensagem original do banco é técnica).
//   - Conferindo antes, a mensagem diz exatamente o que aconteceu e qual campo corrigir.
//   - O unique do banco continua lá como "rede de segurança" (ex.: dois cadastros iguais no mesmo instante).
//
// Ela "sobe" até o ExceptionHandlerModel, que a transforma no JSON padrão com status 409.
// =====================================================================================================
@ResponseStatus(HttpStatus.CONFLICT)
public class conflictException extends RuntimeException {
    public conflictException(String message) {
        super(message);
    }
}
