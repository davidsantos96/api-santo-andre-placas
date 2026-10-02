package br.com.santoandreplacas.apisantoandreplacas.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Parâmetros de texto "vazios" chegam como "" (nunca null): no PostgreSQL um
    // String nulo dentro de LOWER(CONCAT(...)) perde o tipo e a consulta falha.
    // cpfCnpj e telefone ficam guardados como o front manda (mascarados); a
    // comparação por dígitos remove a pontuação dos dois lados.
    @Query("""
            SELECT c FROM Cliente c
            WHERE :busca = ''
               OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR (:buscaDigitos <> '' AND
                   REPLACE(REPLACE(REPLACE(REPLACE(c.telefone, '(', ''), ')', ''), '-', ''), ' ', '')
                       LIKE CONCAT('%', :buscaDigitos, '%'))
               OR (:buscaDigitos <> '' AND
                   REPLACE(REPLACE(REPLACE(c.cpfCnpj, '.', ''), '-', ''), '/', '')
                       LIKE CONCAT('%', :buscaDigitos, '%'))
            """)
    List<Cliente> buscar(@Param("busca") String busca, @Param("buscaDigitos") String buscaDigitos);

    @Query("""
            SELECT c FROM Cliente c
            WHERE REPLACE(REPLACE(REPLACE(c.cpfCnpj, '.', ''), '-', ''), '/', '') = :digitos
            """)
    List<Cliente> buscarPorCpfCnpjDigitos(@Param("digitos") String digitos);
}
