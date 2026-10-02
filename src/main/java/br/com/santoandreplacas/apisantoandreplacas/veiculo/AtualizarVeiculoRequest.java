package br.com.santoandreplacas.apisantoandreplacas.veiculo;

// Só os campos não-nulos são aplicados: serve para completar um cadastro parcial.
public record AtualizarVeiculoRequest(
        String placa,
        String marcaModelo,
        Integer anoFabricacao,
        Integer anoModelo,
        String chassi
) {
}
