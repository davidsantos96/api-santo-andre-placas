package br.com.santoandreplacas.apisantoandreplacas.dashboard;

import java.time.LocalDate;

public record FaturamentoPorDia(
        LocalDate data,
        long totalCentavos
) {
}
