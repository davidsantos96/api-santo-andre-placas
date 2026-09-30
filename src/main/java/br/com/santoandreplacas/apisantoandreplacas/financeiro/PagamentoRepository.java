package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoId(Long pedidoId);
    List<Pagamento> findByStatusAndPagoEmBetween(StatusPagamento status, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
            SELECT COALESCE(SUM(p.valorCentavos), 0) FROM Pagamento p
            WHERE p.pedido.id = :pedidoId AND p.status = :status
            """)
    long somarValorPorPedidoEStatus(@Param("pedidoId") Long pedidoId, @Param("status") StatusPagamento status);
}
