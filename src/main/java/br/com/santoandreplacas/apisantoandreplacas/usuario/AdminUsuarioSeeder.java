package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
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

    public AdminUsuarioSeeder(UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.seed.admin.login}") String loginAdmin,
                               @Value("${app.seed.admin.email}") String emailAdmin,
                               @Value("${app.seed.admin.senha}") String senhaAdmin,
                               @Value("${app.seed.admin.nome}") String nomeAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAdmin = loginAdmin;
        this.emailAdmin = emailAdmin;
        this.senhaAdmin = senhaAdmin;
        this.nomeAdmin = nomeAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Já existe se bate o login OU o e-mail (o banco antigo só tinha e-mail; a
        // migração já deu login a ele, mas não duplicar é mais importante que ser esperto).
        if (usuarioRepository.findByLoginIgnoreCase(loginAdmin).isPresent()
                || usuarioRepository.findByEmailIgnoreCase(emailAdmin).isPresent()) {
            return;
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
