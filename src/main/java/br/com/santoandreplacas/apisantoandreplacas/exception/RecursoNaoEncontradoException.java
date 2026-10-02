package br.com.santoandreplacas.apisantoandreplacas.exception;

// Recurso identificado na URL (/{id}) que não existe -> 404.
// Ids inexistentes dentro do corpo da requisição continuam IllegalArgumentException (400).
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
