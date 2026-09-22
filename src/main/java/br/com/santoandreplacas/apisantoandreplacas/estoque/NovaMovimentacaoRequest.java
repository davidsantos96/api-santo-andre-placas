package br.com.santoandreplacas.apisantoandreplacas.estoque;

public record NovaMovimentacaoRequest(
        Long itemEstoqueId,
        TipoMovimentacao tipo,
        int quantidade,
        Long pedidoId // nullable — só quando a movimentação está ligada a um pedido
) {
}
