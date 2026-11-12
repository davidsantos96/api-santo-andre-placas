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
        String login = validarLogin(request.login());
        String nome = exigirTexto(request.nome(), "Informe o nome.");
        String email = validarEmail(request.email());
        exigirPapel(request.papel());
        exigirEmailParaAdmin(request.papel(), email);
        validarSenha(request.senha());

        // Sem diferenciar maiúsculas de minúsculas (B18): "Ana" e "ana" são o mesmo login.
        usuarioRepository.findByLoginIgnoreCase(login).ifPresent(u -> {
            throw new IllegalArgumentException("Já existe um usuário com o login " + login);
        });
        exigirEmailLivre(email, null);

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPapel(request.papel());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setAtivo(true);

        Usuario salvo = usuarioRepository.save(usuario);
        auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getLogin(),
                AcaoAuditada.CRIACAO);
        return salvo;
    }

    @Transactional
    public Usuario atualizar(Long id, AtualizarUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);

        String login = validarLogin(request.login());
        String nome = exigirTexto(request.nome(), "Informe o nome.");
        String email = validarEmail(request.email());
        exigirPapel(request.papel());
        exigirEmailParaAdmin(request.papel(), email);

        usuarioRepository.findByLoginIgnoreCase(login)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("Já existe um usuário com o login " + login);
                });
        exigirEmailLivre(email, id);

        // O login é o assunto do token: trocá-lo derrubaria a sessão de quem está
        // editando. Para mudar o próprio login, peça a outro administrador.
        boolean ehOProprio = usuarioAutenticadoProvider.usuarioAtual()
                .map(atual -> atual.getId().equals(id))
                .orElse(false);
        if (ehOProprio && !login.equals(usuario.getLogin())) {
            throw new IllegalStateException("Você não pode alterar o próprio login.");
        }

        // Na prática: o único ADMIN tentando rebaixar a si mesmo. Sem ADMIN
        // ativo, ninguém mais entra em /usuarios para desfazer.
        if (request.papel() != Papel.ADMIN && ehUltimoAdminAtivo(usuario)) {
            throw new IllegalStateException("Não é possível rebaixar o último administrador ativo.");
        }

        String loginAnterior = usuario.getLogin();
        String nomeAnterior = usuario.getNome();
        String emailAnterior = usuario.getEmail();
        Papel papelAnterior = usuario.getPapel();

        usuario.setLogin(login);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPapel(request.papel());
        Usuario salvo = usuarioRepository.save(usuario);

        auditar(salvo, "login", loginAnterior, salvo.getLogin());
        auditar(salvo, "nome", nomeAnterior, salvo.getNome());
        auditar(salvo, "email", emailAnterior, salvo.getEmail());
        auditar(salvo, "papel", papelAnterior, salvo.getPapel());

        return salvo;
    }

    // Login de rede: minúsculas, números, ponto, underscore e hífen; 3 a 40 caracteres.
    // Nunca tem "@", e é isso que permite entrar digitando login OU e-mail sem ambiguidade.
    private static final java.util.regex.Pattern PADRAO_LOGIN = java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9._-]{2,39}$");
    private static final java.util.regex.Pattern PADRAO_EMAIL = java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private String validarLogin(String valor) {
        String login = exigirTexto(valor, "Informe o login.").toLowerCase();
        if (!PADRAO_LOGIN.matcher(login).matches()) {
            throw new IllegalArgumentException(
                    "O login deve ter de 3 a 40 caracteres: letras, números, ponto, hífen ou underscore (sem espaços).");
        }
        return login;
    }

    /** E-mail é opcional: vazio vira null. Quando vem, precisa ter formato válido. */
    private String validarEmail(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String email = valor.trim();
        if (!PADRAO_EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        return email;
    }

    private void exigirEmailParaAdmin(Papel papel, String email) {
        if (papel == Papel.ADMIN && email == null) {
            throw new IllegalArgumentException("Administradores precisam ter um e-mail (usado na recuperação de acesso).");
        }
    }

    private void exigirEmailLivre(String email, Long idIgnorado) {
        if (email == null) {
            return;
        }
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(idIgnorado))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("Já existe um usuário com o e-mail " + email);
                });
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
            auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getLogin(),
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
        auditoriaService.registrarAcao(EntidadeAuditada.USUARIO, salvo.getId(), salvo.getLogin(),
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
        auditoriaService.registrarAlteracao(EntidadeAuditada.USUARIO, usuario.getId(), usuario.getLogin(),
                campo, anterior, novo);
    }
}
