package br.com.santoandreplacas.apisantoandreplacas.financeiro;

public record NovoPagamentoRequest(
        long valorCentavos,
        FormaPagamento formaPagamento
) {
}
