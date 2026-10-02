package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    List<Veiculo> findByPlacaIgnoreCase(String placa);

    @Query("""
            SELECT v FROM Veiculo v
            WHERE (:placa = '' OR UPPER(v.placa) LIKE UPPER(CONCAT('%', :placa, '%')))
              AND (:clienteId IS NULL OR v.cliente.id = :clienteId)
            """)
    List<Veiculo> buscar(@Param("placa") String placa, @Param("clienteId") Long clienteId);
}