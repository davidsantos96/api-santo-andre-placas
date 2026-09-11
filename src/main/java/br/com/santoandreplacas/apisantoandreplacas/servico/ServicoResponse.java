package br.com.santoandreplacas.apisantoandreplacas.servico;

public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        long precoCentavos,
        String categoria,
        boolean ativo
) {
    public static ServicoResponse fromEntity(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPrecoCentavos(),
                servico.getCategoria(),
                servico.isAtivo()
        );
    }
}