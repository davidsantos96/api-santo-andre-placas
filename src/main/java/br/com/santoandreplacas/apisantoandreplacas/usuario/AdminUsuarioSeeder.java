package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Roda depois de UsuarioLoginMigracao (ordem 1): o banco já tem a coluna login ajustada.
@Component
@Order(2)
public class AdminUsuarioSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String loginAdmin;
    private final String emailAdmin;
    private final String senhaAdmin;
    private final String nomeAdmin;
    private final Environment environment;

    public AdminUsuarioSeeder(UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.seed.admin.login}") String loginAdmin,
                               @Value("${app.seed.admin.email}") String emailAdmin,
                               @Value("${app.seed.admin.senha}") String senhaAdmin,
                               @Value("${app.seed.admin.nome}") String nomeAdmin,
                               Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAdmin = loginAdmin;
        this.emailAdmin = emailAdmin;
        this.senhaAdmin = senhaAdmin;
        this.nomeAdmin = nomeAdmin;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Já existe se bate o login OU o e-mail (o banco antigo só tinha e-mail; a
        // migração já deu login a ele, mas não duplicar é mais importante que ser esperto).
        if (usuarioRepository.findByLoginIgnoreCase(loginAdmin).isPresent()
                || usuarioRepository.findByEmailIgnoreCase(emailAdmin).isPresent()) {
            return;
        }

        // So aqui, quando o admin precisa nascer: em producao nao pode ser com senha padrao ou fraca.
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && ("admin123".equals(senhaAdmin) || senhaAdmin.length() < 10)) {
            throw new IllegalStateException(
                    "Banco sem administrador: defina ADMIN_SEED_PASSWORD proprio (minimo 10 caracteres).");
        }

        Usuario admin = new Usuario();
        admin.setLogin(loginAdmin);
        admin.setNome(nomeAdmin);
        admin.setEmail(emailAdmin);
        admin.setSenhaHash(passwordEncoder.encode(senhaAdmin));
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);

        usuarioRepository.save(admin);
    }
}
