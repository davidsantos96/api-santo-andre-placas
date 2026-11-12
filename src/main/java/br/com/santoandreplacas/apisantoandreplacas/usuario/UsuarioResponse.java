package br.com.santoandreplacas.apisantoandreplacas.usuario;

import java.time.LocalDateTime;

// email é opcional (só os donos/ADMIN precisam ter): pode vir null.
public record UsuarioResponse(
        Long id,
        String login,
        String nome,
        String email,
        Papel papel,
        boolean ativo,
        LocalDateTime ultimoAcessoEm
) {
    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.isAtivo(),
                usuario.getUltimoAcessoEm()
        );
    }
}
