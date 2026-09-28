package br.com.santoandreplacas.apisantoandreplacas.estoque;

public record ItemEstoqueResponse(
        Long id,
        String nome,
        String sku,
        String unidade,
        int quantidade,
        int quantidadeMinima
) {
    public static ItemEstoqueResponse fromEntity(ItemEstoque item) {
        return new ItemEstoqueResponse(
                item.getId(),
                item.getNome(),
                item.getSku(),
                item.getUnidade(),
                item.getQuantidade(),
                item.getQuantidadeMinima()
        );
    }
}
