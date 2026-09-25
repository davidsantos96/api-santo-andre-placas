package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
    }

    public Usuario criar(NovoUsuarioRequest request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("Já existe um usuário com o e-mail " + request.email());
        });

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPapel(request.papel());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setAtivo(true);

        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long id, AtualizarUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);

        usuarioRepository.findByEmail(request.email())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("Já existe um usuário com o e-mail " + request.email());
                });

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPapel(request.papel());

        return usuarioRepository.save(usuario);
    }

    public Usuario atualizarStatus(Long id, boolean ativo) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(ativo);
        return usuarioRepository.save(usuario);
    }
}
