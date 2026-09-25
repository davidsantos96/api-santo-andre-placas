package br.com.santoandreplacas.apisantoandreplacas.usuario;

public record AtualizarUsuarioRequest(
        String nome,
        String email,
        Papel papel
) {
}
