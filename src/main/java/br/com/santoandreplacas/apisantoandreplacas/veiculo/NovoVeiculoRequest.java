package br.com.santoandreplacas.apisantoandreplacas.veiculo;

public record NovoVeiculoRequest(
        Long clienteId,
        String placa,
        String marcaModelo,
        int anoFabricacao,
        int anoModelo,
        String chassi
) {
}
