package com.andre.apiheroi.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// =====================================================================================================
// notFoundException = "o registro pedido não existe" -> HTTP 404 (Not Found).
// =====================================================================================================
//
// Onde é lançada: nos services, quando um findById não encontra nada. Exemplo do HeroiService:
//
//     repository.findById(id).orElseThrow(() -> new notFoundException("Herói não encontrado, ID: " + id));
//
//   - findById devolve um Optional (uma "caixa" que pode estar vazia);
//   - orElseThrow abre a caixa: se tiver o herói, devolve ele; se estiver vazia, lança esta exceção.
//
// "extends RuntimeException": é uma exceção "não verificada". Por isso não precisamos declarar
// "throws notFoundException" em cada método; ela simplesmente "sobe" pela pilha de chamadas
// (service -> controller -> Spring) até alguém tratar. Quem trata é o ExceptionHandlerModel.
//
// super(message): passa a mensagem para a classe pai (RuntimeException), que guarda o texto.
// Depois, no handler, e.getMessage() devolve exatamente esse texto, que vira o campo "mensagem" do JSON.
//
// @ResponseStatus(NOT_FOUND): diz ao Spring "se ninguém tratar esta exceção, responda 404".
// Como o ExceptionHandlerModel trata, essa anotação funciona só como reforço/documentação do status.
// =====================================================================================================
@ResponseStatus(HttpStatus.NOT_FOUND)
public class notFoundException extends RuntimeException {

    // Construtor usado em quase todo o projeto: a mensagem já vem pronta, em português.
    public notFoundException(String message) {
        super(message);
    }

    // Construtor de atalho: monta a mensagem padrão a partir do ID.
    public notFoundException(Long id) {
        super("Objeto não encontrado com id " + id);
    }
}
