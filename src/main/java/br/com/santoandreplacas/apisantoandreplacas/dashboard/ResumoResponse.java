package br.com.santoandreplacas.apisantoandreplacas.dashboard;

import br.com.santoandreplacas.apisantoandreplacas.pedido.StatusPedido;
import java.util.Map;

public record ResumoResponse(
        long pedidosHoje,
        Map<StatusPedido, Long> pedidosPorStatus,
        long faturamentoHojeCentavos,
        long itensBaixoEstoque
) {
}
