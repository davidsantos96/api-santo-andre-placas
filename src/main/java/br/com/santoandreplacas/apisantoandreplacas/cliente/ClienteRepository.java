package br.com.santoandreplacas.apisantoandreplacas.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("""
            SELECT c FROM Cliente c
            WHERE :busca IS NULL
               OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR c.telefone LIKE CONCAT('%', :busca, '%')
               OR c.cpfCnpj LIKE CONCAT('%', :busca, '%')
            """)
    List<Cliente> buscar(@Param("busca") String busca);
}