package br.com.santoandreplacas.apisantoandreplacas.pedido;

public record PedidoCompletoRequest(
        Long clienteId,
        NovoClienteRequest cliente,
        NovoVeiculoRequest veiculo,
        Long servicoId,
        String origem
) {}