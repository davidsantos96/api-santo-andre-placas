package br.com.santoandreplacas.apisantoandreplacas.usuario;

public record NovoUsuarioRequest(
        String nome,
        String email,
        Papel papel,
        String senha
) {
}
