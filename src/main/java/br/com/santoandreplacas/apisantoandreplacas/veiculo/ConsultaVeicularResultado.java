package br.com.santoandreplacas.apisantoandreplacas.veiculo;

public record ConsultaVeicularResultado(
        String marcaModelo,
        int anoFabricacao,
        int anoModelo,
        String chassi
) {
}
