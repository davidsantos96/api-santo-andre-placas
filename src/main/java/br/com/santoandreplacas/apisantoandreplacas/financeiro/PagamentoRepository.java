package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoId(Long pedidoId);
    List<Pagamento> findByStatusAndPagoEmBetween(StatusPagamento status, LocalDateTime inicio, LocalDateTime fim);
}
