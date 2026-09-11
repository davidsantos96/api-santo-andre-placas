package br.com.santoandreplacas.apisantoandreplacas.pedido;

import java.time.LocalDateTime;

public record PedidoStatusHistoricoResponse(
        Long id,
        StatusPedido statusAnterior,
        StatusPedido statusNovo,
        String alteradoPor,
        LocalDateTime alteradoEm
) {
    public static PedidoStatusHistoricoResponse fromEntity(PedidoStatusHistorico historico) {
        return new PedidoStatusHistoricoResponse(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getAlteradoPor(),
                historico.getAlteradoEm()
        );
    }
}