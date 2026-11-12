package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ponte até existirem migrations versionadas (Flyway, ainda pendente): leva um banco
 * criado quando o e-mail era o login para o modelo com login separado.
 *
 * 1. Tira o NOT NULL de usuario.email (o Hibernate com ddl-auto=update não relaxa
 *    restrição existente; mesmo caso do veiculo.ano_fabricacao).
 * 2. Dá login a quem não tem, a partir da parte local do e-mail ("admin@x.com" -> "admin").
 *
 * É idempotente: em banco novo ou já migrado não muda nada.
 */
@Component
@Order(1)
public class UsuarioLoginMigracao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioLoginMigracao.class);

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarioRepository;

    public UsuarioLoginMigracao(JdbcTemplate jdbc, UsuarioRepository usuarioRepository) {
        this.jdbc = jdbc;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL");
        } catch (Exception e) {
            // Sem a tabela/coluna esperada não há o que migrar; o aviso ajuda a diagnosticar.
            log.warn("Não foi possível relaxar usuario.email (ok se já estava nulável): {}", e.getMessage());
        }

        List<Usuario> semLogin = usuarioRepository.findAll().stream()
                .filter(u -> u.getLogin() == null || u.getLogin().isBlank())
                .toList();
        if (semLogin.isEmpty()) {
            return;
        }

        Set<String> usados = new HashSet<>();
        usuarioRepository.findAll().forEach(u -> {
            if (u.getLogin() != null) {
                usados.add(u.getLogin().toLowerCase());
            }
        });

        for (Usuario usuario : semLogin) {
            String base = derivar(usuario);
            String login = base;
            int n = 2;
            while (!usados.add(login)) {
                login = base + n++;
            }
            usuario.setLogin(login);
            usuarioRepository.save(usuario);
            log.info("Usuário {} recebeu o login \"{}\".", usuario.getId(), login);
        }
    }

    private String derivar(Usuario usuario) {
        String email = usuario.getEmail();
        String parte = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "usuario" + usuario.getId();
        String limpo = parte.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (limpo.length() < 3) {
            limpo = "usuario" + usuario.getId();
        }
        return limpo.length() > 40 ? limpo.substring(0, 40) : limpo;
    }
}
