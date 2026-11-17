package com.andre.apiheroi.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// =====================================================================================================
// forbiddenException = "sei quem você é, mas você não pode fazer isso" -> HTTP 403 (Forbidden).
// =====================================================================================================
//
// Diferença entre 401 e 403:
//   - 401 (unauthorizedException): a API NÃO sabe quem você é (sem X-API-Key, ou chave inválida).
//   - 403 (forbiddenException):    a API SABE quem você é (a chave é válida), mas o registro não é seu.
//
// Neste projeto tudo tem dono: o herói e a equipe pertencem a um perfil, e o poder e a fraqueza pertencem
// a um herói. A regra é "cada perfil só mexe no que é dele". Exemplo no HeroiService:
//
//     if (!heroi.getPerfil().getId().equals(idPerfilLogado)) {
//         throw new forbiddenException("Este herói pertence a outro perfil: você só pode alterar ou excluir os seus");
//     }
//
//   - idPerfilLogado é o id do dono da X-API-Key enviada (o controller pega com authService.validar(apiKey).getId());
//   - a exceção interrompe o método ANTES de qualquer alteração no banco.
//
// Ela "sobe" até o ExceptionHandlerModel, que a transforma no JSON padrão com status 403.
// =====================================================================================================
@ResponseStatus(HttpStatus.FORBIDDEN)
public class forbiddenException extends RuntimeException {
    public forbiddenException(String message) {
        super(message);
    }
}
