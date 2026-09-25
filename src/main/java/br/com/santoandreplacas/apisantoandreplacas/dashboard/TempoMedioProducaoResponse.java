package br.com.santoandreplacas.apisantoandreplacas.dashboard;

public record TempoMedioProducaoResponse(
        double horasMedia,
        long pedidosConsiderados
) {
}
