package br.com.santoandreplacas.apisantoandreplacas.dashboard;

public record ServicoMaisVendidoResponse(
        Long servicoId,
        String servicoNome,
        long quantidadePedidos,
        long faturamentoNominalCentavos
) {
}
