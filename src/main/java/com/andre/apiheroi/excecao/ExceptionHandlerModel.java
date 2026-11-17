package com.andre.apiheroi.excecao;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Date;
import java.util.stream.Collectors;

// =====================================================================================================
// ExceptionHandlerModel = o "tradutor" de exceções em respostas HTTP.
// =====================================================================================================
//
// O CAMINHO DE UMA EXCEÇÃO, DO SERVICE ATÉ O JSON
// ------------------------------------------------
// 1. Um service lança uma exceção. Ex.: HeroiService.findById(99) -> throw new notFoundException("Herói não encontrado, ID: 99").
// 2. Ninguém faz try/catch, então a exceção "sobe": sai do service, sai do controller e chega ao Spring MVC
//    (mais exatamente ao DispatcherServlet, a classe que recebe todas as requisições).
// 3. O Spring procura quem sabe tratar aquele TIPO de exceção. Ele olha as classes marcadas com
//    @RestControllerAdvice (esta aqui) e, dentro delas, os métodos com @ExceptionHandler(Tipo.class).
// 4. Ele escolhe o método MAIS ESPECÍFICO. Uma notFoundException cai em naoEncontrado(...), e não
//    em generico(...), mesmo sendo também uma Exception, porque o Spring prefere o tipo mais próximo.
// 5. O método monta um ResponseEntity<exceptionCustomModel>:
//       - o ResponseEntity carrega o STATUS HTTP (404, 400...) e o CORPO da resposta;
//       - o corpo é um exceptionCustomModel(data, mensagem, detalhes).
// 6. O Jackson transforma o exceptionCustomModel em JSON e o Spring envia para o cliente.
//
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody:
//   - @ControllerAdvice: "estes tratamentos valem para TODOS os controllers da aplicação" (tratamento global);
//   - @ResponseBody: "o que os métodos devolverem vira o corpo da resposta (JSON)".
//
// DE ONDE VEM CADA CAMPO DO JSON
// ------------------------------
//   - data      -> new Date(): data e hora do momento em que o erro foi tratado;
//   - mensagem  -> e.getMessage() nas nossas exceções (o texto passado no "new XxxException(...)"),
//                  ou um texto montado aqui nos erros padrão do Spring (ver montarMensagem);
//   - detalhes  -> request.getDescription(false): o Spring devolve "uri=/api/herois/v1/99".
//                  O "false" significa "não inclua dados do cliente", como IP e id da sessão.
//
// AS DUAS FAMÍLIAS DE ERRO
// ------------------------
//   A) Erros NOSSOS (notFound, badRequest, unauthorized, forbidden, conflict) e alguns
//      de bibliotecas (validação da entidade, violação do banco): cada um tem o seu método @ExceptionHandler abaixo.
//   B) Erros PADRÃO do Spring MVC (JSON inválido, @Valid falhou, header ou parâmetro faltando, ID não numérico,
//      método não suportado, rota inexistente...). Esses o Spring já sabe tratar, através da classe
//      ResponseEntityExceptionHandler, que esta classe ESTENDE (ver handleExceptionInternal, mais abaixo).
//
// IMPORTANTE: erros que acontecem nos FILTROS (o RateLimitFilter, que devolve 429) NÃO passam por aqui,
// porque os filtros rodam antes do Spring MVC. Por isso o 429 escreve o próprio JSON, dentro do filtro.
// =====================================================================================================
@RestControllerAdvice
public class ExceptionHandlerModel extends ResponseEntityExceptionHandler {

    // -------------------------------------------------------------------------------------------------
    // FAMÍLIA A: um método para cada tipo de exceção
    // -------------------------------------------------------------------------------------------------
    // Como ler a assinatura:
    //   @ExceptionHandler(notFoundException.class) -> "este método trata notFoundException";
    //   WebRequest request -> o Spring injeta os dados da requisição que falhou (usamos para pegar a URI);
    //   Exception e        -> a exceção que foi lançada (usamos e.getMessage()).

    // 404: registro não encontrado. A mensagem é a que o service escreveu no "new notFoundException(...)".
    @ExceptionHandler(notFoundException.class)
    public ResponseEntity<exceptionCustomModel> naoEncontrado(WebRequest request, Exception e) {
        return montar(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    // 400: regra de negócio violada (cadastro de perfil sem senha).
    @ExceptionHandler(badRequestException.class)
    public ResponseEntity<exceptionCustomModel> requisicaoInvalida(WebRequest request, Exception e) {
        return montar(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // 401: login inválido, ou operação protegida sem uma X-API-Key válida.
    @ExceptionHandler(unauthorizedException.class)
    public ResponseEntity<exceptionCustomModel> naoAutorizado(WebRequest request, Exception e) {
        return montar(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    // 403: a chave é válida, mas o registro é de outro perfil.
    // Ex.: tentar alterar ou excluir o herói, a equipe ou o perfil de outra pessoa.
    @ExceptionHandler(forbiddenException.class)
    public ResponseEntity<exceptionCustomModel> acessoNegado(WebRequest request, Exception e) {
        return montar(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    // 409 com mensagem CLARA: o service conferiu antes de salvar e viu que o valor já existe
    // (e-mail de perfil, segundo poder ou segunda fraqueza do mesmo herói).
    @ExceptionHandler(conflictException.class)
    public ResponseEntity<exceptionCustomModel> conflito(WebRequest request, Exception e) {
        return montar(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    // 400: a validação da ENTIDADE falhou.
    // As entidades (model/) têm @NotBlank, @Pattern etc. O Hibernate confere essas regras logo antes do INSERT/UPDATE
    // e, se alguma falhar, lança ConstraintViolationException. Normalmente o RequestDTO já barrou antes, no @Valid;
    // isto é a "rede de segurança".
    // getConstraintViolations() traz TODAS as regras que falharam. Para cada uma:
    //   - getPropertyPath() -> o campo (ex.: "nomeHeroico");
    //   - getMessage()      -> o texto do "message" da anotação (ex.: "O nome heroico é obrigatório").
    // O stream junta tudo numa linha: "nome: O nome é obrigatório; nomeHeroico: O nome heroico é obrigatório".
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<exceptionCustomModel> validacaoEntidade(WebRequest request, ConstraintViolationException e) {
        String mensagem = e.getConstraintViolations().stream()
                .map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
                .collect(Collectors.joining("; "));
        return montar(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    // 409 GENÉRICO: o banco recusou a operação por causa de uma regra dele (chave estrangeira ou campo unique).
    // Com os services conferindo tudo antes, isso só aparece em casos raros (ex.: dois cadastros iguais ao mesmo
    // tempo). Quando acontece, o banco recusa e o Spring transforma o erro do banco em DataIntegrityViolationException.
    // A mensagem original do banco é técnica (SQL, nomes de tabelas), por isso escrevemos uma em português.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<exceptionCustomModel> violacaoDoBanco(WebRequest request, Exception e) {
        return montar(HttpStatus.CONFLICT, "O registro está em uso por outro recurso (ou já existe) e a operação não pode ser feita", request);
    }

    // 500: qualquer exceção que nenhum outro método tratou (é o "pega-tudo").
    // Não devolvemos e.getMessage() ao cliente: pode conter detalhes internos (SQL, nomes de classes).
    // O detalhe técnico vai para o log do servidor (logger.error, campo herdado de ResponseEntityExceptionHandler),
    // e o cliente recebe só uma mensagem genérica.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<exceptionCustomModel> generico(WebRequest request, Exception e) {
        logger.error("Erro inesperado", e);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor, tente novamente mais tarde", request);
    }

    // -------------------------------------------------------------------------------------------------
    // FAMÍLIA B: erros padrão do Spring MVC
    // -------------------------------------------------------------------------------------------------
    // Como o Spring monta esses erros "por baixo dos panos":
    //   1. A classe ResponseEntityExceptionHandler já tem um @ExceptionHandler para cada exceção padrão
    //      (MethodArgumentNotValidException, HttpMessageNotReadableException, MissingRequestHeaderException...).
    //   2. Cada um chama um método "handleXxx" que decide o STATUS (quase sempre 400; 405 para método errado;
    //      404 para rota inexistente...) e cria um corpo do tipo ProblemDetail, um JSON padrão (RFC 9457)
    //      com os campos type, title, status, detail e instance. O "detail" vem em INGLÊS.
    //   3. TODOS esses métodos terminam chamando handleExceptionInternal(ex, body, headers, status, request).
    //
    // Por isso basta sobrescrever (@Override) só o handleExceptionInternal: é o "funil" por onde todos passam.
    // Nele, deixamos o Spring decidir o status e os headers, mas trocamos o corpo ProblemDetail (em inglês)
    // pelo nosso exceptionCustomModel (em português). Assim TODOS os erros da API ficam no mesmo formato.
    //
    // Antes, havia um @Override de handleMethodArgumentNotValid aqui. Ele funcionava, mas só para esse erro:
    // os outros (JSON inválido, header faltando...) continuavam saindo em inglês. Sobrescrevendo o funil, todos
    // passam pelo mesmo lugar. O tratamento do @Valid agora está em montarMensagem.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // super.handleExceptionInternal(...) = deixa a classe pai fazer o trabalho normal dela: montar o
        // ResponseEntity com o status certo e o ProblemDetail. Ele só devolve null num caso raro: quando a
        // resposta já começou a ser enviada ao cliente e não dá mais para mudar nada.
        ResponseEntity<Object> respostaPadrao = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (respostaPadrao == null) {
            return null;
        }
        // Montamos o NOSSO corpo e reaproveitamos os headers e o status que o Spring escolheu.
        exceptionCustomModel resposta = new exceptionCustomModel(
                new Date(),
                montarMensagem(ex, respostaPadrao.getBody()),
                request.getDescription(false));
        return new ResponseEntity<>(resposta, respostaPadrao.getHeaders(), respostaPadrao.getStatusCode());
    }

    // -------------------------------------------------------------------------------------------------
    // Métodos auxiliares
    // -------------------------------------------------------------------------------------------------

    // Monta a resposta padrão. Existe para não repetir as mesmas linhas em todos os métodos da família A.
    // new ResponseEntity<>(corpo, status): o "<>" (diamond) deixa o Java descobrir o tipo sozinho.
    private ResponseEntity<exceptionCustomModel> montar(HttpStatus status, String mensagem, WebRequest request) {
        return new ResponseEntity<>(new exceptionCustomModel(new Date(), mensagem, request.getDescription(false)), status);
    }

    // Escreve a mensagem em português de cada erro padrão do Spring.
    // "instanceof Tipo variavel" (pattern matching, Java 16+) faz duas coisas de uma vez: testa o tipo e,
    // se for desse tipo, já cria a variável convertida. Assim podemos chamar métodos específicos de cada exceção.
    private String montarMensagem(Exception ex, Object body) {

        // @Valid falhou num RequestDTO (ex.: POST de herói com "nomeHeroco" no lugar de "nomeHeroico": o campo fica null
        // e o @NotBlank reclama). getBindingResult() guarda o resultado da validação; getFieldErrors() lista
        // cada campo inválido com a mensagem que escrevemos na anotação do DTO.
        if (ex instanceof MethodArgumentNotValidException validacao) {
            return validacao.getBindingResult().getFieldErrors().stream()
                    .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }

        // O Jackson não conseguiu ler o corpo: JSON com erro de sintaxe (vírgula sobrando, aspas faltando),
        // tipo errado (texto onde deveria ser número) ou valor de enum inexistente (nivelForca "DEUS").
        if (ex instanceof HttpMessageNotReadableException) {
            return "JSON inválido: verifique a sintaxe, os nomes dos campos e os tipos dos valores " +
                    "(ex.: nivelForca deve ser HUMANA, SUPERHUMANO, ALIENIGENA ou MUTANTE; forca é um número de 0 a 100)";
        }

        // Faltou um header obrigatório (@RequestHeader). Ex.: POST sem o X-Idempotency-Key.
        if (ex instanceof MissingRequestHeaderException header) {
            return "O header " + header.getHeaderName() + " é obrigatório nesta requisição";
        }

        // Faltou um parâmetro obrigatório da URL (@RequestParam). Ex.: GET /api/herois/v1/busca sem ?nome=
        if (ex instanceof MissingServletRequestParameterException parametro) {
            return "O parâmetro " + parametro.getParameterName() + " é obrigatório nesta requisição";
        }

        // Um valor da URL não pôde ser convertido para o tipo esperado.
        // Ex.: GET /api/herois/v1/abc ("abc" não é um Long) ou /api/herois/v1/nivel/DEUS (não existe no enum).
        if (ex instanceof MethodArgumentTypeMismatchException tipo) {
            return "O valor '" + tipo.getValue() + "' não é válido para o parâmetro " + tipo.getName();
        }

        // Método HTTP não existe para aquela rota (status 405). Ex.: DELETE /api/equipes/v2/1 (a v2 de equipes só tem GET).
        if (ex instanceof HttpRequestMethodNotSupportedException metodo) {
            return "O método " + metodo.getMethod() + " não é suportado nesta rota";
        }

        // Content-Type não suportado (status 415). Ex.: enviar o corpo como texto puro em vez de application/json.
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "Content-Type não suportado: envie o corpo como application/json";
        }

        // Rota que não existe (status 404). Ex.: GET /api/herois (o certo é /api/herois/v1).
        if (ex instanceof NoResourceFoundException) {
            return "Rota não encontrada: confira o endereço da API. As rotas antigas /v1 e /v2 também estão disponíveis";
        }

        // Qualquer outro erro padrão: usamos o "detail" do ProblemDetail que o Spring montou (pode vir em inglês).
        if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
            return problemDetail.getDetail();
        }
        return ex.getMessage();
    }
}
