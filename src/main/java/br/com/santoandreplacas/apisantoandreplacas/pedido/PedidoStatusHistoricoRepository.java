package br.com.santoandreplacas.apisantoandreplacas.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoStatusHistoricoRepository extends JpaRepository<PedidoStatusHistorico, Long> {
    List<PedidoStatusHistorico> findByPedidoIdOrderByAlteradoEmAsc(Long pedidoId);
    List<PedidoStatusHistorico> findByStatusNovoIn(List<StatusPedido> statusNovo);
}