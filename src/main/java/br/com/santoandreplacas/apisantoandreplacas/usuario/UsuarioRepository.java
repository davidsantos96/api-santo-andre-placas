package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByLoginIgnoreCase(String login);

    /**
     * Quem entra pode digitar o login ou o e-mail. Como o login nunca tem "@",
     * a presença do "@" decide onde procurar, sem ambiguidade. Tokens emitidos
     * antes do campo login (assunto = e-mail) continuam sendo resolvidos.
     */
    default Optional<Usuario> buscarPorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        String valor = identificador.trim();
        return valor.contains("@") ? findByEmailIgnoreCase(valor) : findByLoginIgnoreCase(valor);
    }

    long countByPapelAndAtivoTrue(Papel papel);
}