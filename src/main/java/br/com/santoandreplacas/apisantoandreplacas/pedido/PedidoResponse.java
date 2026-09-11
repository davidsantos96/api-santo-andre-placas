package br.com.santoandreplacas.apisantoandreplacas.pedido;

import br.com.santoandreplacas.apisantoandreplacas.cliente.ClienteResponse;
import br.com.santoandreplacas.apisantoandreplacas.veiculo.VeiculoResponse;
import br.com.santoandreplacas.apisantoandreplacas.servico.ServicoResponse;
import java.time.LocalDateTime;

public record PedidoResponse(
        Long id,
        StatusPedido status,
        String origem,
        ClienteResponse cliente,
        VeiculoResponse veiculo,
        ServicoResponse servico,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static PedidoResponse fromEntity(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getOrigem(),
                ClienteResponse.fromEntity(pedido.getCliente()),
                VeiculoResponse.fromEntity(pedido.getVeiculo()),
                ServicoResponse.fromEntity(pedido.getServico()),
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm()
        );
    }
}