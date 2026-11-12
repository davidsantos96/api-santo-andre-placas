package br.com.santoandreplacas.apisantoandreplacas.usuario;

public record AtualizarUsuarioRequest(
        String login,
        String nome,
        String email, // opcional, exceto para ADMIN
        Papel papel
) {
}
