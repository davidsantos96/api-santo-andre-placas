package br.com.santoandreplacas.apisantoandreplacas.estoque;

public record VinculoServicoItemResponse(
        Long id,
        Long servicoId,
        String servicoNome,
        Long itemEstoqueId,
        String itemEstoqueNome,
        int quantidadeNecessaria
) {
    public static VinculoServicoItemResponse fromEntity(ServicoItemEstoque vinculo) {
        return new VinculoServicoItemResponse(
                vinculo.getId(),
                vinculo.getServico().getId(),
                vinculo.getServico().getNome(),
                vinculo.getItemEstoque().getId(),
                vinculo.getItemEstoque().getNome(),
                vinculo.getQuantidadeNecessaria()
        );
    }
}
