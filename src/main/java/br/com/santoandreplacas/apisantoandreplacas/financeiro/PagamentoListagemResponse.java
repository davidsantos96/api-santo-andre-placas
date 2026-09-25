package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import br.com.santoandreplacas.apisantoandreplacas.pedido.Pedido;
import java.time.LocalDateTime;

public record PagamentoListagemResponse(
        Long id,
        Long pedidoId,
        String placa,
        String clienteNome,
        String servicoNome,
        FormaPagamento formaPagamento,
        long valorCentavos,
        LocalDateTime pagoEm
) {
    public static PagamentoListagemResponse fromEntity(Pagamento pagamento) {
        Pedido pedido = pagamento.getPedido();
        return new PagamentoListagemResponse(
                pagamento.getId(),
                pedido.getId(),
                pedido.getVeiculo().getPlaca(),
                pedido.getCliente().getNome(),
                pedido.getServico().getNome(),
                pagamento.getFormaPagamento(),
                pagamento.getValorCentavos(),
                pagamento.getPagoEm()
        );
    }
}
