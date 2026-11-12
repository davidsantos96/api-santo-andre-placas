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
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    // Mesma regra que o front já aplicava na senha provisória; agora vale
    // também para quem chama a API direto.
    private static final int TAMANHO_MINIMO_SENHA = 6;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService,
                          UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
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
        String nome = exigirTexto(request.nome(), "Informe o nome.");
        String email = exigirTexto(request.email(), "Informe o e-mail.");
        exigirPapel(request.papel());
        validarSenha(request.senha());

        // Sem diferenciar maiúsculas de minúsculas: "Ana@x.com" e "ana@x.com"
        // são a mesma caixa postal (B18).
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            throw new IllegalArgumentException("Já existe um usuário com o e-mail " + email);
        });

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
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

        String nome = exigirTexto(request.nome(), "Informe o nome.");
        String email = exigirTexto(request.email(), "Informe o e-mail.");
        exigirPapel(request.papel());

        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("Já existe um usuário com o e-mail " + email);
                });

        // Na prática: o único ADMIN tentando rebaixar a si mesmo. Sem ADMIN
        // ativo, ninguém mais entra em /usuarios para desfazer.
        if (request.papel() != Papel.ADMIN && ehUltimoAdminAtivo(usuario)) {
            throw new IllegalStateException("Não é possível rebaixar o último administrador ativo.");
        }

        String nomeAnterior = usuario.getNome();
        String emailAnterior = usuario.getEmail();
        Papel papelAnterior = usuario.getPapel();

        usuario.setNome(nome);
        usuario.setEmail(email);
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

        if (!ativo) {
            boolean ehOProprio = usuarioAutenticadoProvider.usuarioAtual()
                    .map(atual -> atual.getId().equals(id))
                    .orElse(false);
            // Também é o que garante que sempre sobra um ADMIN ativo: só ADMIN
            // chama esta rota, então quem desativa outro ADMIN continua ativo.
            if (ehOProprio) {
                throw new IllegalStateException("Você não pode desativar o próprio usuário.");
            }
        }

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
        validarSenha(novaSenha);

        Usuario usuario = buscarPorId(id);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        Usuario salvo = usuarioRepository.save(usuario);

        // Registra só que houve reset; nunca a senha nem o hash.
        auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getEmail(),
                AcaoAuditada.RESET_SENHA);

        return salvo;
    }

    private boolean ehUltimoAdminAtivo(Usuario usuario) {
        return usuario.getPapel() == Papel.ADMIN
                && usuario.isAtivo()
                && usuarioRepository.countByPapelAndAtivoTrue(Papel.ADMIN) <= 1;
    }

    private String exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private void exigirPapel(Papel papel) {
        if (papel == null) {
            throw new IllegalArgumentException("Informe o papel do usuário.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    private void auditar(Usuario usuario, String campo, Object anterior, Object novo) {
        auditoriaService.registrarAlteracao(EntidadeAuditada.USUARIO, usuario.getId(), usuario.getEmail(),
                campo, anterior, novo);
    }
}
