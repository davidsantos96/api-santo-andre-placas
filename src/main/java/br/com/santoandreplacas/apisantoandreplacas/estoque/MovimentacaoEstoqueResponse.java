package br.com.santoandreplacas.apisantoandreplacas.estoque;

import java.time.LocalDateTime;

public record MovimentacaoEstoqueResponse(
        Long id,
        Long itemEstoqueId,
        String itemEstoqueNome,
        TipoMovimentacao tipo,
        int quantidade,
        Long pedidoId,
        LocalDateTime criadoEm
) {
    public static MovimentacaoEstoqueResponse fromEntity(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoEstoqueResponse(
                movimentacao.getId(),
                movimentacao.getItemEstoque().getId(),
                movimentacao.getItemEstoque().getNome(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getPedido() != null ? movimentacao.getPedido().getId() : null,
                movimentacao.getCriadoEm()
        );
    }
}
