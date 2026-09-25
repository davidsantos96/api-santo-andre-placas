package br.com.santoandreplacas.apisantoandreplacas.financeiro;

public record TotalPorFormaPagamento(
        FormaPagamento formaPagamento,
        long totalCentavos,
        int quantidade
) {
}
