package br.com.santoandreplacas.apisantoandreplacas.servico;

// pedidosNoMes (pedidos não cancelados criados no mês corrente) só vem preenchido
// nos endpoints de /servicos; quando o serviço aparece aninhado em outra resposta
// (ex.: PedidoResponse) fica null.
public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        long precoCentavos,
        String categoria,
        boolean ativo,
        Long pedidosNoMes
) {
    public static ServicoResponse fromEntity(Servico servico) {
        return fromEntity(servico, null);
    }

    public static ServicoResponse fromEntity(Servico servico, Long pedidosNoMes) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPrecoCentavos(),
                servico.getCategoria(),
                servico.isAtivo(),
                pedidosNoMes
        );
    }
}
