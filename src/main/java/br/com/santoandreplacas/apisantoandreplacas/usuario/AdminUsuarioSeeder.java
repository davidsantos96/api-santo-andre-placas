package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUsuarioSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String emailAdmin;
    private final String senhaAdmin;
    private final String nomeAdmin;

    public AdminUsuarioSeeder(UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.seed.admin.email}") String emailAdmin,
                               @Value("${app.seed.admin.senha}") String senhaAdmin,
                               @Value("${app.seed.admin.nome}") String nomeAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailAdmin = emailAdmin;
        this.senhaAdmin = senhaAdmin;
        this.nomeAdmin = nomeAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByEmail(emailAdmin).isPresent()) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome(nomeAdmin);
        admin.setEmail(emailAdmin);
        admin.setSenhaHash(passwordEncoder.encode(senhaAdmin));
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);

        usuarioRepository.save(admin);
    }
}
