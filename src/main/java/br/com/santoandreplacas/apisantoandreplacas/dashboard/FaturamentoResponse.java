package br.com.santoandreplacas.apisantoandreplacas.dashboard;

import java.time.LocalDate;
import java.util.List;

public record FaturamentoResponse(
        LocalDate de,
        LocalDate ate,
        long totalCentavos,
        List<FaturamentoPorDia> porDia
) {
}
