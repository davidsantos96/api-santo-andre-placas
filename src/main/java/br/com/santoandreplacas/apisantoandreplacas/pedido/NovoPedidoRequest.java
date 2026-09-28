package br.com.santoandreplacas.apisantoandreplacas.pedido;

public record NovoPedidoRequest(
        Long clienteId,
        Long veiculoId,
        Long servicoId,
        String origem
) {
}
