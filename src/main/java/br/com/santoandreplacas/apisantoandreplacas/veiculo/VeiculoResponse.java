package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import java.time.LocalDateTime;

public record VeiculoResponse(
        Long id,
        String placa,
        String marcaModelo,
        Integer anoFabricacao,
        Integer anoModelo,
        String chassi,
        Long clienteId,
        String clienteNome,
        LocalDateTime criadoEm,
        String criadoPor,
        Long criadoPorId,
        LocalDateTime atualizadoEm,
        String atualizadoPor,
        Long atualizadoPorId
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
                veiculo.getCliente().getNome(),
                veiculo.getCriadoEm(),
                veiculo.getCriadoPor(),
                veiculo.getCriadoPorUsuario() != null ? veiculo.getCriadoPorUsuario().getId() : null,
                veiculo.getAtualizadoEm(),
                veiculo.getAtualizadoPor(),
                veiculo.getAtualizadoPorUsuario() != null ? veiculo.getAtualizadoPorUsuario().getId() : null
        );
    }
}
