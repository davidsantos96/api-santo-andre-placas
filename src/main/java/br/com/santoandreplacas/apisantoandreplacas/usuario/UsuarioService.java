package br.com.santoandreplacas.apisantoandreplacas.usuario;
import br.com.santoandreplacas.apisantoandreplacas.exception.RecursoNaoEncontradoException;

import br.com.santoandreplacas.apisantoandreplacas.auditoria.AcaoAuditada;
import br.com.santoandreplacas.apisantoandreplacas.auditoria.AuditoriaService;
import br.com.santoandreplacas.apisantoandreplacas.auditoria.EntidadeAuditada;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }

    @Transactional
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

        Usuario salvo = usuarioRepository.save(usuario);
        auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getEmail(),
                AcaoAuditada.CRIACAO);
        return salvo;
    }

    @Transactional
    public Usuario atualizar(Long id, AtualizarUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);

        usuarioRepository.findByEmail(request.email())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("Já existe um usuário com o e-mail " + request.email());
                });

        String nomeAnterior = usuario.getNome();
        String emailAnterior = usuario.getEmail();
        Papel papelAnterior = usuario.getPapel();

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPapel(request.papel());
        Usuario salvo = usuarioRepository.save(usuario);

        auditar(salvo, "nome", nomeAnterior, salvo.getNome());
        auditar(salvo, "email", emailAnterior, salvo.getEmail());
        auditar(salvo, "papel", papelAnterior, salvo.getPapel());

        return salvo;
    }

    @Transactional
    public Usuario atualizarStatus(Long id, boolean ativo) {
        Usuario usuario = buscarPorId(id);
        boolean mudou = usuario.isAtivo() != ativo;
        usuario.setAtivo(ativo);
        Usuario salvo = usuarioRepository.save(usuario);

        if (mudou) {
            auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getEmail(),
                    ativo ? AcaoAuditada.ATIVACAO : AcaoAuditada.DESATIVACAO);
        }

        return salvo;
    }

    @Transactional
    public Usuario redefinirSenha(Long id, String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("Informe a nova senha.");
        }

        Usuario usuario = buscarPorId(id);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        Usuario salvo = usuarioRepository.save(usuario);

        // Registra só que houve reset; nunca a senha nem o hash.
        auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getEmail(),
                AcaoAuditada.RESET_SENHA);

        return salvo;
    }

    private void auditar(Usuario usuario, String campo, Object anterior, Object novo) {
        auditoriaService.registrarAlteracao(EntidadeAuditada.USUARIO, usuario.getId(), usuario.getEmail(),
                campo, anterior, novo);
    }
}
