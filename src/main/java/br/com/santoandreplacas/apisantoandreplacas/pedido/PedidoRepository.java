package br.com.santoandreplacas.apisantoandreplacas.pedido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByStatus(StatusPedido status);
    List<Pedido> findByClienteId(Long clienteId);

    @Query("""
            SELECT p FROM Pedido p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND (:de IS NULL OR p.criadoEm >= :de)
              AND (:ate IS NULL OR p.criadoEm < :ate)
              AND (:busca IS NULL
                   OR UPPER(p.veiculo.placa) LIKE UPPER(CONCAT('%', :busca, '%'))
                   OR LOWER(p.cliente.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR (:buscaId IS NOT NULL AND p.id = :buscaId))
            ORDER BY p.criadoEm DESC
            """)
    Page<Pedido> buscar(@Param("status") StatusPedido status,
                         @Param("clienteId") Long clienteId,
                         @Param("de") LocalDateTime de,
                         @Param("ate") LocalDateTime ate,
                         @Param("busca") String busca,
                         @Param("buscaId") Long buscaId,
                         Pageable pageable);
}