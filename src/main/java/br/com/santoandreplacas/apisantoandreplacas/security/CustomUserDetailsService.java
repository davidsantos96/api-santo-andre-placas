package br.com.santoandreplacas.apisantoandreplacas.security;

import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.buscarPorIdentificador(identificador)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + identificador));

        return new User(
                usuario.getLogin(),
                usuario.getSenhaHash(),
                usuario.isAtivo(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel()))
        );
    }
}