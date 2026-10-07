package br.com.santoandreplacas.apisantoandreplacas.estoque;

import java.time.LocalDateTime;

public record ItemEstoqueResponse(
        Long id,
        String nome,
        String sku,
        String unidade,
        int quantidade,
        int quantidadeMinima,
        LocalDateTime criadoEm,
        String criadoPor,
        Long criadoPorId
) {
    public static ItemEstoqueResponse fromEntity(ItemEstoque item) {
        return new ItemEstoqueResponse(
                item.getId(),
                item.getNome(),
                item.getSku(),
                item.getUnidade(),
                item.getQuantidade(),
                item.getQuantidadeMinima(),
                item.getCriadoEm(),
                item.getCriadoPor(),
                item.getCriadoPorUsuario() != null ? item.getCriadoPorUsuario().getId() : null
        );
    }
}
