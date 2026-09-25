package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        Long pedidoId,
        long valorCentavos,
        FormaPagamento formaPagamento,
        StatusPagamento status,
        LocalDateTime pagoEm
) {
    public static PagamentoResponse fromEntity(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getPedido().getId(),
                pagamento.getValorCentavos(),
                pagamento.getFormaPagamento(),
                pagamento.getStatus(),
                pagamento.getPagoEm()
        );
    }
}
