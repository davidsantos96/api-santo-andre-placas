package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
     * Nome do usuário autenticado, ou "sistema" se não houver autenticação
     * no contexto atual (ex.: chamada feita fora de uma requisição HTTP).
     */
    public String nomeUsuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "sistema";
        }

        return usuarioRepository.findByEmail(authentication.getName())
                .map(Usuario::getNome)
                .orElse("sistema");
    }
}
