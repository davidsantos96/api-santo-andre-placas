package br.com.santoandreplacas.apisantoandreplacas.usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Papel papel,
        boolean ativo,
        LocalDateTime ultimoAcessoEm
) {
    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.isAtivo(),
                usuario.getUltimoAcessoEm()
        );
    }
}
