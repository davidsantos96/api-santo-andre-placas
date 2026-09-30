package br.com.santoandreplacas.apisantoandreplacas.exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErroResponse erro = new ErroResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
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
}
