package br.com.santoandreplacas.apisantoandreplacas.estoque;

public record ItemEstoqueResponse(
        Long id,
        String nome,
        int quantidade,
        int quantidadeMinima
) {
    public static ItemEstoqueResponse fromEntity(ItemEstoque item) {
        return new ItemEstoqueResponse(
                item.getId(),
                item.getNome(),
                item.getQuantidade(),
                item.getQuantidadeMinima()
        );
    }
}
