package br.com.santoandreplacas.apisantoandreplacas.veiculo;

public record VeiculoResponse(
        Long id,
        String placa,
        String marcaModelo,
        int anoFabricacao,
        int anoModelo,
        String chassi,
        Long clienteId,
        String clienteNome
) {
    public static VeiculoResponse fromEntity(Veiculo veiculo) {
        return new VeiculoResponse(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getMarcaModelo(),
                veiculo.getAnoFabricacao(),
                veiculo.getAnoModelo(),
                veiculo.getChassi(),
                veiculo.getCliente().getId(),
                veiculo.getCliente().getNome()
        );
    }
}