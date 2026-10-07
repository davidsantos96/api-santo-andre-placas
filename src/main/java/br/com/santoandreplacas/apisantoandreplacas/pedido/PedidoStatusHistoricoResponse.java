package br.com.santoandreplacas.apisantoandreplacas.pedido;

import java.time.LocalDateTime;

public record PedidoStatusHistoricoResponse(
        Long id,
        StatusPedido statusAnterior,
        StatusPedido statusNovo,
        String alteradoPor,
        Long alteradoPorId,
        LocalDateTime alteradoEm
) {
    public static PedidoStatusHistoricoResponse fromEntity(PedidoStatusHistorico historico) {
        return new PedidoStatusHistoricoResponse(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getAlteradoPor(),
                historico.getAlteradoPorUsuario() != null ? historico.getAlteradoPorUsuario().getId() : null,
                historico.getAlteradoEm()
        );
    }
}