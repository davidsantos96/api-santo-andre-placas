package br.com.santoandreplacas.apisantoandreplacas.usuario;

public record NovoUsuarioRequest(
        String login,
        String nome,
        String email, // opcional, exceto para ADMIN
        Papel papel,
        String senha
) {
}
