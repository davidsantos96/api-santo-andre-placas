package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Resolve o Usuario autenticado da requisição atual, pra registrar quem fez
 * uma ação (mudança de status de pedido, pagamento) — antes disso, esses
 * campos ficavam com um valor fixo ("sistema") ou nem existiam.
 */
@Component
public class UsuarioAutenticadoProvider {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticadoProvider(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Usuário autenticado da requisição atual, ou vazio quando não há
     * autenticação no contexto (ex.: chamada feita fora de uma requisição HTTP,
     * como o seeder na subida). Nesse caso a autoria fica com FK nula e o nome
     * "sistema" — o que distingue a ausência de autor de um usuário real.
     */
    public Optional<Usuario> usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmail(authentication.getName());
    }

    /** Nome do usuário autenticado, ou "sistema". Guardado como snapshot junto da FK. */
    public String nomeUsuarioAtual() {
        return usuarioAtual().map(Usuario::getNome).orElse("sistema");
    }
}
