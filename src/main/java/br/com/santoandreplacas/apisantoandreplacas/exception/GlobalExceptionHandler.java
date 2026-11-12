package br.com.santoandreplacas.apisantoandreplacas.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErroResponse erro = new ErroResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResponse> handleIllegalState(IllegalStateException ex) {
        ErroResponse erro = new ErroResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // As entidades validam campos nos próprios setters (ex.: Servico.setNome),
    // que são chamados pelo Jackson durante a desserialização do corpo da
    // requisição. Quando o setter lança IllegalArgumentException/IllegalStateException
    // nesse momento, o Spring embrulha em HttpMessageNotReadableException antes de
    // chegar nos handlers acima — sem isso aqui, a mensagem real se perdia e o
    // front só recebia "Bad Request" sem explicação.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();
        String mensagem = (causa instanceof IllegalArgumentException || causa instanceof IllegalStateException)
                ? causa.getMessage()
                : "Corpo da requisição inválido.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(mensagem));
    }

    // ---------------------------------------------------------------------
    // Daqui pra baixo: erros que antes não tinham handler. Sem handler, o
    // Spring responde com sendError(...), que faz um forward interno para
    // /error; como /error exigia autenticação e o JwtAuthFilter não roda de
    // novo nesse forward, QUALQUER um deles saía como 401 — e o front, ao ver
    // 401, abria o modal de "sessão expirada" no meio do trabalho do usuário.
    // ---------------------------------------------------------------------

    // O @PreAuthorize nega o acesso DENTRO da chamada do controller, então a
    // exceção passa por aqui antes de chegar no accessDeniedHandler do
    // SecurityConfig. Precisa de handler próprio: sem ele, o handler genérico
    // de Exception (lá embaixo) transformaria "sem permissão" em 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErroResponse("Você não tem permissão para acessar este recurso."));
    }

    // Parâmetro com tipo errado na URL: /pedidos?status=QUALQUER, /clientes/abc, ?de=31-12-2026...
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("Valor inválido para o parâmetro \"" + ex.getName() + "\"."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> handleParametroAusente(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("Parâmetro obrigatório ausente: \"" + ex.getParameterName() + "\"."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> handleRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse("Rota não encontrada."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErroResponse("Método " + ex.getMethod() + " não é aceito nesta rota."));
    }

    // Rede de segurança para restrição do banco (campo obrigatório nulo, valor
    // único repetido) que alguma validação de serviço tenha deixado passar.
    // A mensagem do banco não vai para o cliente: fica só no log.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleRestricaoDoBanco(DataIntegrityViolationException ex) {
        log.warn("Restrição do banco violada: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("Dados inválidos ou duplicados. Confira os campos e tente novamente."));
    }

    // Qualquer outra coisa é bug nosso: 500 de verdade, com o stack trace no log
    // do servidor e uma mensagem neutra para o cliente (nunca o texto da exceção,
    // que pode expor detalhe interno).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleErroInesperado(Exception ex) {
        // As exceções do próprio Spring MVC (tipo de conteúdo não suportado, etc.)
        // já sabem qual status HTTP representam: respeita esse status.
        if (ex instanceof ErrorResponse erroDoSpring && erroDoSpring.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(erroDoSpring.getStatusCode())
                    .body(new ErroResponse("Requisição inválida."));
        }

        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("Erro interno no servidor. Tente novamente em instantes."));
    }
}
