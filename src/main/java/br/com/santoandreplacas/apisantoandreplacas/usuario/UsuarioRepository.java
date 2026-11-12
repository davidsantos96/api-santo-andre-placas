package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    long countByPapelAndAtivoTrue(Papel papel);
}