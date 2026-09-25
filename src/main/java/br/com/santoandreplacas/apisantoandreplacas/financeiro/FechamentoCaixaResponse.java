package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import java.time.LocalDate;
import java.util.List;

public record FechamentoCaixaResponse(
        LocalDate de,
        LocalDate ate,
        long totalGeral,
        int quantidadePagamentos,
        List<TotalPorFormaPagamento> porFormaPagamento
) {
}
